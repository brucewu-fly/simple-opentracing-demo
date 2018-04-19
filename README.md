# Simple OpenTracing Demo

## 背景
现代应用逐渐从单体系统转变为微服务，如何在分布式环境下定位应用的性能瓶颈变得越来越复杂。为此，诞生了一批优秀的分布式追踪系统帮助我们解决上述问题。

使用这些分布式追踪系统的第一步是在应用程序中埋点，本文将介绍绍如何使用 OpenTracing API 为您的应用程序埋点。由于 OpenTracing API 使用起来比较复杂，为此我们提供了一个 [TracerHelper](https://github.com/aliyun/aliyun-log-jaeger-sender/blob/master/src/main/java/com/aliyun/openservices/log/jaeger/sender/util/TracerHelper.java) 类，它对 OpenTracing API 进行了封装，能让您以更低的成本接入应用程序。

## 样例介绍
下图展示了一个常见的分布式系统，这里有三个进程 `Hello`，`Formatter` 和 `Publisher`。`Hello` 中包含三个方法 `sayHello`，`formatString`，`printHello`。方法 `sayHello` 位于最外层，它会依次调用 `formatString` 和 `printHello`。`formatString` 会向 `Formatter` 发起一个远程方法调用。`printHello` 会向 `Publisher`  发起一个远程方法调用。

![p1.png](/pics/p1.png)

## 埋点方法
为了追踪分布式请求，我们需要对 `sayHello`，`formatString`，`printHello`，`format`，`publish` 等方法进行埋点，埋点方式如下。

```
// 为 sayHello 方法创建 rootSpan
private void sayHello(String helloTo) {
  try (Scope scope = TracerHelper.traceLatency("say-hello")) {
	...
  }
}

// 为 formatString 方法创建 span
private String formatString(String helloTo) {
  try (Scope scope = TracerHelper.traceLatency("formatString")) {
    ...
  }
}

// 为 printHello 方法创建 span
private void printHello(String helloStr) {
  try (Scope scope = TracerHelper.traceLatency("printHello")) {
    ...
  }
}

// 客户端通过 TracerHelper.getActiveSpanContextString() 方法获取 spanContextString，
// 然后将 spanContextString 作为网络协议里的字段发往服务端。此例将 spanContextString 放入了 HTTP Header 里。
String spanContextString = TracerHelper.getActiveSpanContextString();
requestBuilder.addHeader("trace-id", spanContextString);

// 对于服务端程序 Formatter 和 Publisher，它们将 spanContextString 从网络协议的字段中提取出来，此例为 HTTP Header，
// 然后通过 TracerHelper.traceLatency(String operationName, String spanContextString) 方法创建 scope
String spanContextString = rawHeaders.get("trace-id").get(0);
try (Scope scope = TracerHelper.traceLatency("format", spanContextString)) {
  ...
}

String spanContextString = rawHeaders.get("trace-id").get(0);
try (Scope scope = TracerHelper.traceLatency("publish", spanContextString)) {
  ...
}
```

埋点完成后会生成如下图所示的 trace 结构。
~~~
––|–––––––|–––––––|–––––––|–––––––|–––––––|–––––––|–––––––|–> time

 [sayHello ···············································]
  [formatString ·············]
    [format ···············]
                              [printHello ···············]
			       [publish ···············]
~~~


## 样例

这里提供了一系列额外的样例让您由浅入深了解 OpenTracing API 的使用方法。

* [Demo 1 - Hello World](./src/main/java/com/aliyun/opentracingdemo/demo01)
  * 如何初始化一个 tracer
  * 如何创建一个简单的 trace
* [Demo 2 - 嵌套 Span](./src/main/java/com/aliyun/opentracingdemo/demo02)
  * 了解如何在单个 trace 中整合多个 span
  * 如何在进程内传递 context
* [Demo 3 - 异常处理](./src/main/java/com/aliyun/opentracingdemo/demo03)
  * 了解如果待追踪的方法在执行过程中抛出异常该如何处理
  * 不同创建 span 的方法在遇到未捕获异常时所表现出的行为
* [Demo 4 - 异步回调](./src/main/java/com/aliyun/opentracingdemo/demo04)
  * 了解在异步情况下如何结束一个 Span
* [Demo 5 - 跨进程追踪](./src/main/java/com/aliyun/opentracingdemo/demo05)
  * 了解如何追踪一个跨进程调用
  * 如何跨进程传递 SpanContext

## 常见问题
**Q: 如何打开或关闭 trace 功能？**

**A**: 在您构造 Tracer 实例的时候，传入一个 ConstSampler 对象。当 ConstSampler 的布尔参数 decision 被设置成 true 时，框架会记录每一条 trace；反之，框架不会记录任何一条 trace。
```
// 框架会记录每一条 trace
withSampler(new ConstSampler(true))

// 框架不会记录任何一条 trace
withSampler(new ConstSampler(false))
```

**Q: 框架中判断是否记录 trace 的逻辑复杂吗，是否会影响程序性能？**

**A**: 框架在构建 Span 的过程中，会调用 `sampler.sample(String operation, long id)` 方法构建出 SamplingStatus 对象来判断是否需要记录当前 trace。

jaeger-client-java 提供了多种 Sampler 实现，包括 ConstSampler、GuaranteedThroughputSampler、PerOperationSampler、ProbabilisticSampler、RateLimitingSampler。

* ConstSampler - 直接通过构造过程中传入的布尔参数来判断是否采样，逻辑非常简单，不影响性能。
* 其余几种 Sampler 会有一些额外的判断逻辑，但逻辑也比较简单，不影响性能。

**Q: 如何控制采样频率，如我只想记录 10% 的 trace？**

**A**: 在您构造 Tracer 实例的时候，传入一个 ProbabilisticSampler 对象，设置参数 samplingRate 为您希望的采样频率。处于计算效率的考虑 ProbabilisticSampler 控制的是一个近似值，不是一个精确值。
```
// 只记录大约 10% 的 trace
withSampler(new ProbabilisticSampler(0.1))
```

## 其他资源
[spring-boot-opentracing-demo](https://github.com/brucewu-fly/spring-boot-opentracing-demo)
