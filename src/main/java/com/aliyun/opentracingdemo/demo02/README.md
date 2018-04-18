# Demo 2 - 嵌套 Span

## 目标
* 了解如何在单个 trace 中整合多个 span
* 如何在进程内传递 context

## 样例说明
样例中涉及三个方法 `sayHello`，`formatString`，`printHello`。方法 `sayHello` 位于最外层，它会依次调用 `formatString` 和 `printHello`。三者的调用关系如下图所示。
~~~
––|–––––––|–––––––|–––––––|–––––––|–––––––|–––––––|–––––––|–> time

 [sayHello ···············································]
  [formatString ·············]
                              [printHello ···············]
~~~
我们需要为这三个方法分别创建 span，并且 `sayHello` 方法对应的 span 需要作为 `formatString`，`printHello` 对应 span 的 父 span。

## 方式一 - 手动传递 span

`sayHello` 中创建 span，并显示传递给 `formatString` 和 `printHello`。
```
private void sayHello(String helloTo) {
  Span span = TracerHelper.buildSpan("say-hello").start();

  span.setTag("hello-to", helloTo);

  String helloStr = formatString(span, helloTo);
  printHello(span, helloStr);

  span.finish();
}
```

`formatString` 和 `printHello` 在创建 span 的时候通过 `asChildOf` 方法指定父 span。
```
private String formatString(Span rootSpan, String helloTo) {
  Span span = TracerHelper.buildSpan("formatString").asChildOf(rootSpan).start();
  try {
    String helloStr = String.format("Hello, %s!", helloTo);
    span.log(ImmutableMap.of("event", "string-format", "value", helloStr));
    return helloStr;
  } finally {
  	span.finish();
  }
}

private void printHello(Span rootSpan, String helloStr) {
  Span span = TracerHelper.buildSpan("printHello").asChildOf(rootSpan).start();
  try {
    System.out.println(helloStr);
    span.log(ImmutableMap.of("event", "println"));
  } finally {
  	span.finish();
  }
}
```

如果我们把 trace 想象成一个有向无环图（DAG），`asChildOf` 方法相当于在 span 和 rootSpan 之间创建了一条边。

参阅 [HelloManual.java](./HelloManual.java)


## 方式二 - 使用 scope

### 方式一的缺陷
* 需要将 span 作为函数参数显示地传递
* 需要通过 try/finally 语法显示地结束一个 span

**OpenTracing API for Java** 提供了一个更优雅地方式。
```
private void sayHello(String helloTo) {
  try (Scope scope = TracerHelper.buildSpan("say-hello").startActive(true)) {
    scope.span().setTag("hello-to", helloTo);

    String helloStr = formatString(helloTo);
    printHello(helloStr);
  }
}

private String formatString(String helloTo) {
  try (Scope scope = TracerHelper.buildSpan("formatString").startActive(true)) {
    String helloStr = String.format("Hello, %s!", helloTo);
    scope.span().log(ImmutableMap.of("event", "string-format", "value", helloStr));
    return helloStr;
  }
}

private void printHello(String helloStr) {
  try (Scope scope = TracerHelper.buildSpan("printHello").startActive(true)) {
    System.out.println(helloStr);
    scope.span().log(ImmutableMap.of("event", "println"));
  }
}
```
上面的代码和方式一相比有如下改动
* 使用 SpanBuilder 的 `startActive()` 方法，它会将 Span 存储到 ThreadLocalScope 对象中。
* `startActive()` 方法返回一个 Scope 对象而不是一个 Span 对象。Scope 会作为当前活跃 span 的容器。我们可以通过 `scope.span()` 方法获取当前活跃 span。我们可以把创建 scope 想象成入栈操作，关闭 scope 想象成出栈操作。
* `Scope` 继承了 AutoCloseable 接口，能让我们使用 Java 1.7 中的 try-with-resource 语法自动关闭一个资源对象。
* 这里将 `startActive(true)` 方法的布尔参数 finishSpanOnClose 设为 true，会让 Scope 在关闭时调用 span 的 `finish()` 方法自动结束一个 span。
* `startActive()` 方法会自动地将当前 span 设为前一个活跃 span 的子 span，因此这里我们不需要显示地调用 `asChildOf()` 方法了。

参阅 [HelloActive.java](./HelloActive.java)

## 方式三 - 使用 traceLatency() 方法
直接使用 scope 还是有些繁琐，为此 TracerHelper 提供了 `traceLatency()` 方法简化 scope 的创建过程。
```
private void sayHello(String helloTo) {
  try (Scope scope = TracerHelper.traceLatency("say-hello")) {
    scope.span().setTag("hello-to", helloTo);

    String helloStr = formatString(helloTo);
    printHello(helloStr);
  }
}

private String formatString(String helloTo) {
  try (Scope scope = TracerHelper.traceLatency("formatString")) {
    String helloStr = String.format("Hello, %s!", helloTo);
    scope.span().log(ImmutableMap.of("event", "string-format", "value", helloStr));
    return helloStr;
  }
}

private void printHello(String helloStr) {
  try (Scope scope = TracerHelper.traceLatency("printHello")) {
    System.out.println(helloStr);
    scope.span().log(ImmutableMap.of("event", "println"));
  }
}
```
参阅 [HelloSimple.java](./HelloSimple.java)
