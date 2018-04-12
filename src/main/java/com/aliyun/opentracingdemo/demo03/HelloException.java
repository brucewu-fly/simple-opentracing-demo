package com.aliyun.opentracingdemo.demo03;

import com.aliyun.openservices.log.jaeger.sender.util.TracerHolder;
import com.aliyun.opentracingdemo.TracerManager;
import io.opentracing.Scope;
import io.opentracing.Span;

public class HelloException {

  private static void func1(boolean invokeException) {
    Span span = TracerHolder.get().buildSpan("func1").start();

    System.out.println("in func1");
    if (invokeException) {
      throw new RuntimeException("func1 RuntimeException");
    }

    span.finish();
  }

  private static void func2(boolean invokeException) {
    Span span = TracerHolder.get().buildSpan("func2").start();

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
    try (Scope scope = TracerHolder.get().buildSpan("func3").startActive(true)) {
      System.out.println("in func3");
      if (invokeException) {
        throw new RuntimeException("func3 RuntimeException");
      }
    }
  }

  private static void func4(boolean invokeException) {
    try (Scope scope = TracerHolder.get().buildSpan("func4").startActive(false)) {
      System.out.println("in func4");
      if (invokeException) {
        throw new RuntimeException("func4 RuntimeException");
      }
    }
  }

  private static void func5(boolean invokeException) {
    Scope scope = TracerHolder.get().buildSpan("func5").startActive(true);

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

  public static void main(String[] args) {
    TracerManager.build();
    handleFunc1();
    handleFunc2();
    handleFunc3();
    handleFunc4();
    handleFunc5();
    TracerManager.close();
  }

}
