package com.aliyun.opentracingdemo.demo03;

import com.aliyun.openservices.log.jaeger.sender.AliyunLogSender;
import com.aliyun.openservices.log.jaeger.sender.util.TracerHelper;
import com.uber.jaeger.samplers.ConstSampler;
import io.opentracing.Scope;
import io.opentracing.Span;

public class HelloException {

  private static void func1(boolean invokeException) {
    Span span = TracerHelper.buildSpan("func1").start();

    System.out.println("in func1");
    if (invokeException) {
      throw new RuntimeException("func1 RuntimeException");
    }

    span.finish();
  }

  private static void func2(boolean invokeException) {
    Span span = TracerHelper.buildSpan("func2").start();

    try {
      System.out.println("in func2");
      if (invokeException) {
        throw new RuntimeException("func2 RuntimeException");
      }
    } finally {
      span.finish();
    }

  }

  private static void func3(boolean invokeException) {
    try (Scope scope = TracerHelper.buildSpan("func3").startActive(true)) {
      System.out.println("in func3");
      if (invokeException) {
        throw new RuntimeException("func3 RuntimeException");
      }
    }
  }

  private static void func4(boolean invokeException) {
    try (Scope scope = TracerHelper.buildSpan("func4").startActive(false)) {
      System.out.println("in func4");
      if (invokeException) {
        throw new RuntimeException("func4 RuntimeException");
      }
    }
  }

  private static void func5(boolean invokeException) {
    Scope scope = TracerHelper.buildSpan("func5").startActive(true);

    try {
      System.out.println("in func5");
      if (invokeException) {
        throw new RuntimeException("func5 RuntimeException");
      }
    } catch (Throwable ex) {
      scope.span().setTag("error", true);
    } finally {
      scope.close();
    }
  }

  private static void func6(boolean invokeException) {
    Scope scope = TracerHelper.traceLatency("func6");

    try {
      System.out.println("in func6");
      if (invokeException) {
        throw new RuntimeException("func6 RuntimeException");
      }
    } catch (Throwable ex) {
      scope.span().setTag("error", true);
    } finally {
      scope.close();
    }
  }

  private static void handleFunc1() {
    try {
      func1(true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void handleFunc2() {
    try {
      func2(true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void handleFunc3() {
    try {
      func3(true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void handleFunc4() {
    try {
      func4(true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void handleFunc5() {
    try {
      func5(true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void handleFunc6() {
    try {
      func6(true);
    } catch (Exception e) {
      e.printStackTrace();
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
    TracerHelper
        .buildTracer("simple-opentracing-demo", buildAliyunLogSender(), new ConstSampler(true));
    handleFunc1();
    handleFunc2();
    handleFunc3();
    handleFunc4();
    handleFunc5();
    handleFunc6();
    TracerHelper.closeTracer();
  }

}
