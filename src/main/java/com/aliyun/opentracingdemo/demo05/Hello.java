package com.aliyun.opentracingdemo.demo05;

import com.aliyun.openservices.log.jaeger.sender.AliyunLogSender;
import com.aliyun.openservices.log.jaeger.sender.util.TracerHelper;
import com.google.common.collect.ImmutableMap;
import com.uber.jaeger.samplers.ConstSampler;
import io.opentracing.Scope;
import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Hello {

  private final OkHttpClient client;

  private Hello() {
    this.client = new OkHttpClient();
  }

  private String getHttp(int port, String path, String param, String value) {
    try {
      HttpUrl url = new HttpUrl.Builder().scheme("http").host("localhost").port(port)
          .addPathSegment(path)
          .addQueryParameter(param, value).build();
      Request.Builder requestBuilder = new Request.Builder().url(url);

      String spanContextString = TracerHelper.getActiveSpanContextString();
      requestBuilder.addHeader("trace-id", spanContextString);

      Request request = requestBuilder.build();
      Response response = client.newCall(request).execute();
      if (response.code() != 200) {
        throw new RuntimeException("Bad HTTP result: " + response);
      }
      return response.body().string();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void sayHello(String helloTo) {
    try (Scope scope = TracerHelper.traceLatency("say-hello", true)) {
      scope.span().setTag("hello-to", helloTo);

      String helloStr = formatString(helloTo);
      printHello(helloStr);
    }
  }

  private String formatString(String helloTo) {
    try (Scope scope = TracerHelper.traceLatency("formatString", true)) {
      String helloStr = getHttp(8081, "format", "helloTo", helloTo);
      scope.span().log(ImmutableMap.of("event", "string-format", "value", helloStr));
      return helloStr;
    }
  }

  private void printHello(String helloStr) {
    try (Scope scope = TracerHelper.traceLatency("printHello", true)) {
      getHttp(8082, "publish", "helloStr", helloStr);
      scope.span().log(ImmutableMap.of("event", "println"));
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
    TracerHelper
        .buildTracer("hello-world-client", buildAliyunLogSender(), new ConstSampler(true));
    new Hello().sayHello(helloTo);
    TracerHelper.closeTracer();
    System.exit(0); // okhttpclient sometimes hangs maven otherwise
  }

}
