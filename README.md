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
* [Demo 4 - 异步回调](./src/main/java/com/aliyun/opentracingdemo/demo04)
  * 了解在异步情况下如何结束一个 Span
* [Demo 5 - 跨进程追踪](./src/main/java/com/aliyun/opentracingdemo/demo05)
  * 了解如何追踪一个跨进程调用
  * 使用 `Inject` 和 `Extract` 传递 context

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
