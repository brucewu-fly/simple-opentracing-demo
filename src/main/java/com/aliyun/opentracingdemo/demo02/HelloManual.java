package com.aliyun.opentracingdemo.demo02;

import com.aliyun.opentracingdemo.TracerHolder;
import com.aliyun.opentracingdemo.TracerManager;
import com.google.common.collect.ImmutableMap;
import io.opentracing.Span;

public class HelloManual {

  private void sayHello(String helloTo) {
    Span span = TracerHolder.get().buildSpan("say-hello").start();

    span.setTag("hello-to", helloTo);

    String helloStr = formatString(span, helloTo);
    printHello(span, helloStr);

    span.finish();
  }

  private String formatString(Span rootSpan, String helloTo) {
    Span span = TracerHolder.get().buildSpan("formatString").asChildOf(rootSpan).start();
    try {
      String helloStr = String.format("Hello, %s!", helloTo);
      span.log(ImmutableMap.of("event", "string-format", "value", helloStr));
      return helloStr;
    } finally {
      span.finish();
    }
  }

  private void printHello(Span rootSpan, String helloStr) {
    Span span = TracerHolder.get().buildSpan("printHello").asChildOf(rootSpan).start();
    try {
      System.out.println(helloStr);
      span.log(ImmutableMap.of("event", "println"));
    } finally {
      span.finish();
    }
  }

  public static void main(String[] args) {
    if (args.length != 1) {
      throw new IllegalArgumentException("Expecting one argument");
    }
    String helloTo = args[0];
    TracerManager.build();
    new HelloManual().sayHello(helloTo);
    TracerManager.close();
  }
}
