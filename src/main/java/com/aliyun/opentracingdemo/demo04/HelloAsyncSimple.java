package com.aliyun.opentracingdemo.demo04;

import com.aliyun.openservices.log.jaeger.sender.AliyunLogSender;
import com.aliyun.openservices.log.jaeger.sender.util.TracerHelper;
import com.google.common.collect.ImmutableMap;
import com.uber.jaeger.samplers.ConstSampler;
import io.opentracing.Scope;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloAsyncSimple {

  private static ExecutorService executorService = Executors.newFixedThreadPool(1);

  private void sayHello(String helloTo) {
    final Scope scope = TracerHelper.traceLatency("sayHello", false);

    String helloStr = String.format("Hello, %s!", helloTo);
    scope.span().log(ImmutableMap.of("event", "string-format", "value", helloStr));

    System.out.println(helloStr);
    scope.span().log(ImmutableMap.of("event", "println"));

    executorService.submit(new Runnable() {
      @Override
      public void run() {
        try (Scope asyncScope = TracerHelper.asyncTraceLatency(scope, true)) {
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
    new HelloAsyncSimple().sayHello(helloTo);
    try {
      Thread.sleep(1000 * 3);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    executorService.shutdown();
    TracerHelper.closeTracer();
  }

}
