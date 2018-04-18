# Demo 5 - 跨进程追踪

## 目标
* 了解如何追踪一个跨进程调用
* 如何跨进程传递 SpanContext

## 样例说明

`Hello` 中包含三个方法 `sayHello`，`formatString`，`printHello`。方法 `sayHello` 位于最外层，它会依次调用 `formatString` 和 `printHello`。`formatString` 会向 `Formatter` 发起一个远程方法调用。`printHello` 会向 `Publisher`  发起一个远程方法调用。

~~~
––|–––––––|–––––––|–––––––|–––––––|–––––––|–––––––|–––––––|–> time

 [sayHello ···············································]
  [formatString ············]
       [format ···········]
                              [printHello ···············]
                                  [publish ···········]
~~~

## 跨进程传递

### 客户端
可以通过 `TracerHelper.getActiveSpanContextString()` 方法获取 spanContextString，然后将 spanContextString 作为网络协议里的字段发往服务端。比如，可以将 spanContextString 放入 HTTP Header 里。
```
String spanContextString = TracerHelper.getActiveSpanContextString();
requestBuilder.addHeader("trace-id", spanContextString);
```

参阅 [Hello.java](./Hello.java#L30)

### 服务端
将 spanContextString 从网络协议的字段中提取出来，如 HTTP Header，然后通过 `TracerHelper.traceLatency(String operationName, boolean finishSpanOnClose, String spanContextString)` 方法创建 scope。
```
String spanContextString = rawHeaders.get("trace-id").get(0);
try (Scope scope = TracerHelper.traceLatency("format", true, spanContextString)) {
  ...
}
```

参阅 [Formatter.java](./Formatter.java#L32)

## 运行
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

可通过如下方式运行 `Hello` 应用
```
export PROJECT=<your_project> \
ENDPOINT=<your_endpoint> \
ACCESS_KEY_ID=<your_access_key_id> \
ACCESS_KEY_SECRET=<your_access_key_secret> \
LOG_STORE=<your_log_store> && \
mvn exec:java -Dexec.mainClass="com.aliyun.opentracingdemo.demo05.Hello" -Dexec.args="world"
```
