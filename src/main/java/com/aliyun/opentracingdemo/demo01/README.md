# Demo 1 - Hello World

## 目标
* 如何初始化一个 tracer
* 如何创建一个简单的 trace

## TracerHelper
`aliyun-log-jaeger-sender` 提供了一个工具类 `com.aliyun.openservices.log.jaeger.sender.util.TracerHelper` 用来全局保存 tracer。


可以参考 TracerBuilder 的 `build()` 方法创建一个 tracer。
```
private static AliyunLogSender buildAliyunLogSender() {
    String projectName = System.getenv("PROJECT");
    String logStore = System.getenv("LOG_STORE");
    String endpoint = System.getenv("ENDPOINT");
    String accessKeyId = System.getenv("ACCESS_KEY_ID");
    String accessKey = System.getenv("ACCESS_KEY_SECRET");
    return new AliyunLogSender.Builder(projectName, logStore, endpoint, accessKeyId, accessKey)
        .build();
}

public static void build() {
    AliyunLogSender aliyunLogSender = buildAliyunLogSender();
    RemoteReporter remoteReporter = new RemoteReporter.Builder()
        .withSender(aliyunLogSender)
        .build();
    TracerHolder.register(new Tracer.Builder("simple-opentracing-demo")
        .withReporter(remoteReporter)
        .withSampler(new ConstSampler(true))
        .build());
}
```

参阅 [TracerManager.java](../TracerManager.java)

## TracerHelper
`aliyun-log-jaeger-sender` 提供了一个工具类 `com.aliyun.openservices.log.jaeger.sender.util.TracerHelper` 用来全局保存 tracer。之后您在程序的任意位置可以通过 `TracerHelper.buildSpan()` 方法创建 span 或者通过 `TracerHelper.traceLatency()` 开启追踪。

### 创建 tracer
您可以通过 `TracerManager.buildTracer(String serviceName, AliyunLogSender aliyunLogSender,
      Sampler sampler)` 方法创建 tracer，该方法会把创建好的 tracer 保存在 TracerManager 类静态变量中。
```
// 构建 AliyunLogSender
AliyunLogSender aliyunLogSender = buildAliyunLogSender();

// 创建 Tracer，同时需要传入创建好的 AliyunLogSender 和 Sampler
TracerHelper.buildTracer("xxx", aliyunLogSender, new ConstSampler(true));
```

### 传入构建好的 tracer
如果您需要更加灵活地控制构建 tracer 的过程，您可以在外部先构建好一个 tracer 对象，然后通过 `TracerManager.registerTracer(final com.uber.jaeger.Tracer tracer)` 方法将这个 tracer 注册到 TracerManager 中。

### 关闭 tracer
进程退出前，需要调用 `TracerManager.close()` 方法关闭 tracer。数据是异步发送的，这么做是为了防止缓存在内存中的追踪数据丢失。


参阅 [TracerHelper.java](https://github.com/aliyun/aliyun-log-jaeger-sender/blob/master/src/main/java/com/aliyun/openservices/log/jaeger/sender/util/TracerHelper.java)

## 使用步骤
创建/销毁 tracer
```
// 构建 AliyunLogSender
AliyunLogSender aliyunLogSender = buildAliyunLogSender();

// 创建 Tracer，同时需要传入创建好的 AliyunLogSender 和 Sampler
TracerHelper.buildTracer("xxx", aliyunLogSender, new ConstSampler(true));

// 执行业务逻辑
new Hello().sayHello(helloTo);

// 调用 TracerHelper.closeTracer() 方法关闭 tracer
TracerHelper.closeTracer();
```

记录追踪数据
```
private void sayHello(String helloTo) {
    Span span = tracer.buildSpan("say-hello").start();
    span.setTag("hello-to", helloTo);

    String helloStr = String.format("Hello, %s!", helloTo);
    span.log(ImmutableMap.of("event", "string-format", "value", helloStr));

    System.out.println(helloStr);
    span.log(ImmutableMap.of("event", "println"));
    span.log("log_event");

    span.finish();
}
```
使用方式

1. 通过 `tracer.buildSpan()` 方法创建一个 span
2. 通过 `span.setTag()` 方法为 span 添加 tag
3. 通过 `span.log()` 方法为 span 添加 log
4. 通过 `span.finish()` 方法结束一个 span

参阅 [Hello.java](./Hello.java)
