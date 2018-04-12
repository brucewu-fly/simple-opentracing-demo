# Simple OpenTracing Demo

## 安装 & 配置

样例代码使用阿里云[日志服务](https://sls.console.aliyun.com)作为追踪数据的后端存储。当您通过日志服务控制台创建好 project，logstore 后便可通过如下方式运行样例。
```
git clone https://github.com/brucewu-fly/simple-opentracing-demo.git
cd simple-opentracing-demo
mvn clean compile

## 运行 demo01
export PROJECT=<your_project> \
ENDPOINT=<your_endpoint> \
ACCESS_KEY_ID=<your_access_key_id> \
ACCESS_KEY_SECRET=<your_access_key_secret> \
LOG_STORE=<your_log_store> && \
mvn exec:java -Dexec.mainClass="com.aliyun.opentracingdemo.demo01.Hello" -Dexec.args="world"

## 运行 demo02
export PROJECT=<your_project> \
ENDPOINT=<your_endpoint> \
ACCESS_KEY_ID=<your_access_key_id> \
ACCESS_KEY_SECRET=<your_access_key_secret> \
LOG_STORE=<your_log_store> && \
mvn exec:java -Dexec.mainClass="com.aliyun.opentracingdemo.demo02.HelloManual" -Dexec.args="world"

export PROJECT=<your_project> \
ENDPOINT=<your_endpoint> \
ACCESS_KEY_ID=<your_access_key_id> \
ACCESS_KEY_SECRET=<your_access_key_secret> \
LOG_STORE=<your_log_store> && \
mvn exec:java -Dexec.mainClass="com.aliyun.opentracingdemo.demo02.HelloActive" -Dexec.args="world"

## 运行 demo03
export PROJECT=<your_project> \
ENDPOINT=<your_endpoint> \
ACCESS_KEY_ID=<your_access_key_id> \
ACCESS_KEY_SECRET=<your_access_key_secret> \
LOG_STORE=<your_log_store> && \
mvn exec:java -Dexec.mainClass="com.aliyun.opentracingdemo.demo03.HelloException"
```

## 样例
* [Demo 1 - Hello World](./src/main/java/com/aliyun/opentracingdemo/demo01)
  * 如何初始化一个 tracer
  * 如何创建一个简单的 trace
* [Demo 2 - 嵌套 Span](./src/main/java/com/aliyun/opentracingdemo/demo02)
  * 了解如何在单个 trace 中整合多个 span
  * 如何在进程内传递 context
* [Demo 3 - 异常处理](./src/main/java/com/aliyun/opentracingdemo/demo03)
  * 了解如果待追踪的方法在执行过程中抛出异常该如何处理
  * 不同创建 span 的方法在遇到未捕获异常时所表现出的行为

## 其他资源
[spring-boot-opentracing-demo](https://github.com/brucewu-fly/spring-boot-opentracing-demo)
