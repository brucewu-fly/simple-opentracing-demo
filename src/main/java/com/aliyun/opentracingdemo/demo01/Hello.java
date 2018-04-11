package com.aliyun.opentracingdemo.demo01;

import com.aliyun.opentracingdemo.TracerHolder;
import com.aliyun.opentracingdemo.TracerManager;
import com.google.common.collect.ImmutableMap;

import io.opentracing.Span;
import io.opentracing.Tracer;

public class Hello {

  private Tracer tracer;

  public Hello() {
    this(TracerHolder.get());
  }

  public Hello(Tracer tracer) {
    this.tracer = tracer;
  }

  private void sayHello(String helloTo) {
    Span span = tracer.buildSpan("say-hello").start();
    span.setTag("hello-to", helloTo);

    String helloStr = String.format("Hello, %s!", helloTo);
    span.log(ImmutableMap.of("event", "string-format", "value", helloStr));

    System.out.println(helloStr);
    span.log(ImmutableMap.of("event", "println"));

    span.finish();
  }

  public static void main(String[] args) {
    if (args.length != 1) {
      throw new IllegalArgumentException("Expecting one argument");
    }
    String helloTo = args[0];
    TracerManager.build();
    new Hello().sayHello(helloTo);
    TracerManager.close();
  }

}
