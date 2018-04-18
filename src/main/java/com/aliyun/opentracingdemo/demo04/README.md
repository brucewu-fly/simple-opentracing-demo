# Demo 4 - 异步回调

## 目标
* 了解在异步情况下如何结束一个 Span

## 方式一
使用 scope
```
private void sayHello(String helloTo) {
  final Scope scope = TracerHolder.get().buildSpan("sayHello").startActive(false);

  String helloStr = String.format("Hello, %s!", helloTo);
  scope.span().log(ImmutableMap.of("event", "string-format", "value", helloStr));

  System.out.println(helloStr);
  scope.span().log(ImmutableMap.of("event", "println"));

  executorService.submit(new Runnable() {
    @Override
    public void run() {
      try (Scope asyncScope = TracerHolder.get().scopeManager().activate(scope.span(), true)) {
        asyncScope.span().setTag("async", true);
        try {
          Thread.sleep(1000 * 2);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        System.out.println("task finished");
      }
    }
  });
}
```
说明
1. 通过 Scope 开启一个 Span，`startActive()` 方法的布尔参数被设置成 false
2. 主线程不负责关闭 Scope 或 结束 Span
3. 异步方法激活主线程的 Span，然后结束 Span

参阅 [HelloAsync.java](./HelloAsync.java)

## 方式二
通过 TracerHelper 的 traceLatency() 方法和 asyncTraceLatency() 方法简化代码。
```
private void sayHello(String helloTo) {
  final Scope scope = TracerHelper.asyncTraceLatency("sayHello");

  try {
    Thread.sleep(1000);
  } catch (InterruptedException e) {
    e.printStackTrace();
  }

  String helloStr = String.format("Hello, %s!", helloTo);
  scope.span().log(ImmutableMap.of("event", "string-format", "value", helloStr));

  System.out.println(helloStr);
  scope.span().log(ImmutableMap.of("event", "println"));

  executorService.submit(new Runnable() {
    @Override
    public void run() {
      try (Scope asyncScope = TracerHelper.restoreAsyncTraceLatency(scope)) {
        asyncScope.span().setTag("async", true);
        try {
          Thread.sleep(1000 * 2);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        System.out.println("task finished");
      }
    }
  });
}
```
说明
1. 通过 `TracerHelper.traceLatency()` 开启一个 Span
2. 主线程不负责关闭 Scope 或 结束 Span
3. 异步方法内通过 `TracerHelper.asyncTraceLatency()` 方法激活主线程的 Span，然后结束 Span

参阅 [HelloAsyncSimple.java](./HelloAsyncSimple.java)