package com.aliyun.opentracingdemo.demo02;

import com.aliyun.openservices.log.jaeger.sender.util.TracerHolder;
import com.aliyun.opentracingdemo.TracerManager;
import com.google.common.collect.ImmutableMap;
import io.opentracing.Scope;

public class HelloActive {

  private void sayHello(String helloTo) {
    try (Scope scope = TracerHolder.get().buildSpan("say-hello").startActive(true)) {
      scope.span().setTag("hello-to", helloTo);

      String helloStr = formatString(helloTo);
      printHello(helloStr);
    }
  }

  private String formatString(String helloTo) {
    try (Scope scope = TracerHolder.get().buildSpan("formatString").startActive(true)) {
      String helloStr = String.format("Hello, %s!", helloTo);
      scope.span().log(ImmutableMap.of("event", "string-format", "value", helloStr));
      return helloStr;
    }
  }

  private void printHello(String helloStr) {
    try (Scope scope = TracerHolder.get().buildSpan("printHello").startActive(true)) {
      System.out.println(helloStr);
      scope.span().log(ImmutableMap.of("event", "println"));
    }
  }

  public static void main(String[] args) {
    if (args.length != 1) {
      throw new IllegalArgumentException("Expecting one argument");
    }
    String helloTo = args[0];
    TracerManager.build();
    new HelloActive().sayHello(helloTo);
    TracerManager.close();
  }
}
