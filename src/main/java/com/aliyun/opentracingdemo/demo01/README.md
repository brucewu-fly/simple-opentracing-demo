# Demo 1 - Hello World

## 目标
* 如何初始化一个 tracer
* 如何创建一个简单的 trace

## TracerBuilder
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

参阅 [TracerManager.java](https://github.com/brucewu-fly/simple-opentracing-demo/blob/master/src/main/java/com/aliyun/opentracingdemo/TracerManager.java)

## TracerHolder
提供了一个工具类 `TracerHolder ` 用来全局保存创建好的 tracer。

`TracerManager.build()` 方法会将创建好的 tracer 注册到 `TracerHolder` 中。您在程序的任意位置可以通过 `TracerHolder.get()` 获取 tracer 实例。

参阅 [TracerHolder.java](https://github.com/brucewu-fly/simple-opentracing-demo/blob/master/src/main/java/com/aliyun/opentracingdemo/TracerHolder.java)

## 使用步骤
```
TracerManager.build();
new Hello().sayHello(helloTo);
TracerManager.close();
```
* 程序开始时，通过 `TracerManager.build()` 创建 tracer。
* 执行业务逻辑。
* 程序结束前，通过 `TracerManager.close()` 关闭 tracer。

```
private void sayHello(String helloTo) {
	Span span = tracer.buildSpan("say-hello").start();
	span.setTag("hello-to", helloTo);

	String helloStr = String.format("Hello, %s!", helloTo);
	span.log(ImmutableMap.of("event", "string-format", "value", helloStr));

	System.out.println(helloStr);
	span.log(ImmutableMap.of("event", "println"));

	span.finish();
}
```
* 通过 `tracer.buildSpan()` 方法创建一个 span。
* 通过 `span.setTag()` 方法为 span 添加 tag。
* 通过 `span.log()` 方法为 span 添加 log。
* 通过 `span.finish()` 方法结束一个 span。

参阅 [Hello.java](https://github.com/brucewu-fly/simple-opentracing-demo/blob/master/src/main/java/com/aliyun/opentracingdemo/demo1/Hello.java)