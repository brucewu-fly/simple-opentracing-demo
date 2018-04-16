package com.aliyun.opentracingdemo.demo01;

import com.aliyun.openservices.log.jaeger.sender.AliyunLogSender;
import com.aliyun.openservices.log.jaeger.sender.util.TracerHelper;
import com.google.common.collect.ImmutableMap;

import com.uber.jaeger.samplers.ConstSampler;
import io.opentracing.Span;

public class Hello {

  private void sayHello(String helloTo) {
    Span span = TracerHelper.buildSpan("say-hello").start();
    span.setTag("hello-to", helloTo);

    String helloStr = String.format("Hello, %s!", helloTo);
    span.log(ImmutableMap.of("event", "string-format", "value", helloStr));

    System.out.println(helloStr);
    span.log(ImmutableMap.of("event", "println"));

    span.log("log_event");

    span.finish();
  }

  private static AliyunLogSender buildAliyunLogSender() {
    String projectName = System.getenv("PROJECT");
    String logStore = System.getenv("LOG_STORE");
    String endpoint = System.getenv("ENDPOINT");
    String accessKeyId = System.getenv("ACCESS_KEY_ID");
    String accessKey = System.getenv("ACCESS_KEY_SECRET");
    return new AliyunLogSender.Builder(projectName, logStore, endpoint, accessKeyId, accessKey)
        .build();
  }

  public static void main(String[] args) {
    if (args.length != 1) {
      throw new IllegalArgumentException("Expecting one argument");
    }
    String helloTo = args[0];
    TracerHelper
        .buildTracer("simple-opentracing-demo", buildAliyunLogSender(), new ConstSampler(true));
    new Hello().sayHello(helloTo);
    TracerHelper.closeTracer();
  }

}
