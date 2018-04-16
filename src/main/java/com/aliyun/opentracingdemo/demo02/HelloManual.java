package com.aliyun.opentracingdemo.demo02;

import com.aliyun.openservices.log.jaeger.sender.AliyunLogSender;
import com.aliyun.openservices.log.jaeger.sender.util.TracerHelper;
import com.google.common.collect.ImmutableMap;
import com.uber.jaeger.samplers.ConstSampler;
import io.opentracing.Span;

public class HelloManual {

  private void sayHello(String helloTo) {
    Span span = TracerHelper.buildSpan("say-hello").start();

    span.setTag("hello-to", helloTo);

    String helloStr = formatString(span, helloTo);
    printHello(span, helloStr);

    span.finish();
  }

  private String formatString(Span rootSpan, String helloTo) {
    Span span = TracerHelper.buildSpan("formatString").asChildOf(rootSpan).start();
    try {
      String helloStr = String.format("Hello, %s!", helloTo);
      span.log(ImmutableMap.of("event", "string-format", "value", helloStr));
      return helloStr;
    } finally {
      span.finish();
    }
  }

  private void printHello(Span rootSpan, String helloStr) {
    Span span = TracerHelper.buildSpan("printHello").asChildOf(rootSpan).start();
    try {
      System.out.println(helloStr);
      span.log(ImmutableMap.of("event", "println"));
    } finally {
      span.finish();
    }
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
    TracerHelper.buildTracer("simple-opentracing-demo", buildAliyunLogSender(), new ConstSampler(true));
    new HelloManual().sayHello(helloTo);
    TracerHelper.closeTracer();
  }
}
