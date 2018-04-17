# Demo 5 - 跨进程追踪

## 目标
* 了解如何追踪一个跨进程调用
* 使用 `Inject` 和 `Extract` 传递 context

## 样例说明

### Formatter
`Formatter` 是一个 HTTP 应用，当您发送一个请求 `curl http://localhost:8081/format?helloTo=Bruce`，它会返回字符串 `Hello, Bruce!`。

可通过如下方式运行 `Formatter` 应用
```
export PROJECT=<your_project> \
ENDPOINT=<your_endpoint> \
ACCESS_KEY_ID=<your_access_key_id> \
ACCESS_KEY_SECRET=<your_access_key_secret> \
LOG_STORE=<your_log_store> && \
mvn exec:java -Dexec.mainClass="com.aliyun.opentracingdemo.demo05.Formatter" -Dexec.args="server"
```

### Publisher
`Publisher` 是另外一个 HTTP 应用，当您发送一个请求 `curl http://localhost:8082/publish?helloStr=hi%20there`，它会返回字符串 `published`。

可通过如下方式运行 `Publisher` 应用
```
export PROJECT=<your_project> \
ENDPOINT=<your_endpoint> \
ACCESS_KEY_ID=<your_access_key_id> \
ACCESS_KEY_SECRET=<your_access_key_secret> \
LOG_STORE=<your_log_store> && \
mvn exec:java -Dexec.mainClass="com.aliyun.opentracingdemo.demo05.Publisher" -Dexec.args="server"
```

### Hello
`Hello` 中包含三个方法 `sayHello`，`formatString`，`printHello`。方法 `sayHello` 位于最外层，它会依次调用 `formatString` 和 `printHello`。`formatString` 会向 Formatter 发起一个远程方法调用。`printHello` 会向 `Publisher`  发起一个远程方法调用。

可通过如下方式运行 `Hello` 应用
```
export PROJECT=<your_project> \
ENDPOINT=<your_endpoint> \
ACCESS_KEY_ID=<your_access_key_id> \
ACCESS_KEY_SECRET=<your_access_key_secret> \
LOG_STORE=<your_log_store> && \
mvn exec:java -Dexec.mainClass="com.aliyun.opentracingdemo.demo05.Hello" -Dexec.args="world"
```

## 跨进程传递

主要通过 inject 方法将 spanContext 注入到传输协议，然后通过 extract 方法从传输协议中抽取 spanContext。详细流程如下。

### sayHello
`Hello` 中的方法 `sayHello` 作为程序的入口通过调用 TracerHelper.traceLatency() 方法创建 root span。
```
private void sayHello(String helloTo) {
  try (Scope scope = TracerHelper.traceLatency("say-hello", true)) {
    scope.span().setTag("hello-to", helloTo);

    String helloStr = formatString(helloTo);
    printHello(helloStr);
  }
}
```

### formatString
方法 `formatString` 中创建的 span 作为 `sayHello` 的子 span。
```
private String formatString(String helloTo) {
  try (Scope scope = TracerHelper.traceLatency("formatString", true)) {
    String helloStr = getHttp(8081, "format", "helloTo", helloTo);
    scope.span().log(ImmutableMap.of("event", "string-format", "value", helloStr));
    return helloStr;
  }
}
```

### printHello
方法 `printHello` 中创建的 span 作为 `sayHello` 的子 span。
```
private void printHello(String helloStr) {
  try (Scope scope = TracerHelper.traceLatency("printHello", true)) {
    getHttp(8082, "publish", "helloStr", helloStr);
    scope.span().log(ImmutableMap.of("event", "println"));
  }
}
```

### inject
`formatString` 和 `printHello` 分布调用 `getHttp` 方法向 `Formatter` 和 `Publisher` 发送 HTTP 请求。其中会通过  `TracerHelper.inject(SpanContext spanContext, Format<C> format, C carrier)` 方法将当前 context 注册到 HTTP Header 中去，并随着 HTTP 请求进行传输。

```
private String getHttp(int port, String path, String param, String value) {
  try {
    HttpUrl url = new HttpUrl.Builder().scheme("http").host("localhost").port(port)
        .addPathSegment(path)
        .addQueryParameter(param, value).build();
    Request.Builder requestBuilder = new Request.Builder().url(url);

    Tags.SPAN_KIND.set(TracerHelper.activeSpan(), Tags.SPAN_KIND_CLIENT);
    Tags.HTTP_METHOD.set(TracerHelper.activeSpan(), "GET");
    Tags.HTTP_URL.set(TracerHelper.activeSpan(), url.toString());
    TracerHelper.inject(TracerHelper.activeSpan().context(), Builtin.HTTP_HEADERS,
        new RequestBuilderCarrier(requestBuilder));

    Request request = requestBuilder.build();
    Response response = client.newCall(request).execute();
    if (response.code() != 200) {
      throw new RuntimeException("Bad HTTP result: " + response);
    }
    return response.body().string();
  } catch (IOException e) {
    throw new RuntimeException(e);
  }
}
```

### extract
`Formatter` 和 `Publisher` 会通过 `TracerHelper.extract(Format<C> format, C carrier)` 方法从 Header 中抽取 context。

如果成功抽取到 parentSpanCtx，会以 parentSpanCtx 作为父 span 创建新的 span；否则，直接创建一个新的 Span。
```
Tracer.SpanBuilder spanBuilder;
try {
  SpanContext parentSpanCtx = TracerHelper.extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headers));
  if (parentSpanCtx == null) {
    spanBuilder = TracerHelper.buildSpan(operationName);
  } else {
    spanBuilder = TracerHelper.buildSpan(operationName).asChildOf(parentSpanCtx);
  }
} catch (IllegalArgumentException e) {
  spanBuilder = TracerHelper.buildSpan(operationName);
}
```
