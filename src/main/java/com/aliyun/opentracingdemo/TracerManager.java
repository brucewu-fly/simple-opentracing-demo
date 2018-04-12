package com.aliyun.opentracingdemo;

import com.aliyun.openservices.log.jaeger.sender.AliyunLogSender;
import com.aliyun.openservices.log.jaeger.sender.util.TracerHolder;
import com.uber.jaeger.Tracer;
import com.uber.jaeger.reporters.RemoteReporter;
import com.uber.jaeger.samplers.ConstSampler;

public abstract class TracerManager {

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

  public static void close() {
    TracerHolder.get().close();
  }

}
