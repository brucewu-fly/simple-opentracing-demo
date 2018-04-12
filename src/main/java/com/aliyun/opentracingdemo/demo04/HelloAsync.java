package com.aliyun.opentracingdemo.demo04;

import com.aliyun.openservices.log.jaeger.sender.util.TracerHolder;
import com.aliyun.opentracingdemo.TracerManager;
import com.google.common.collect.ImmutableMap;
import io.opentracing.Scope;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloAsync {

  private static ExecutorService executorService = Executors.newFixedThreadPool(1);

  private void sayHello(String helloTo) {
    final Scope scope = TracerHolder.get().buildSpan("sayHello").startActive(false);

    String helloStr = String.format("Hello, %s!", helloTo);
    scope.span().log(ImmutableMap.of("event", "string-format", "value", helloStr));

    System.out.println(helloStr);
    scope.span().log(ImmutableMap.of("event", "println"));

    executorService.submit(new Runnable() {
      @Override
      public void run() {
        try (Scope asyncScope = TracerHolder.get().scopeManager().activate(scope.span(), true)) {
          asyncScope.span().setTag("async", true);
          try {
            Thread.sleep(1000 * 2);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          System.out.println("task finished");
        }
      }
    });
  }

  public static void main(String[] args) {
    if (args.length != 1) {
      throw new IllegalArgumentException("Expecting one argument");
    }
    String helloTo = args[0];
    TracerManager.build();
    new HelloAsync().sayHello(helloTo);
    try {
      Thread.sleep(1000 * 3);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    executorService.shutdown();
    TracerManager.close();
  }

}
