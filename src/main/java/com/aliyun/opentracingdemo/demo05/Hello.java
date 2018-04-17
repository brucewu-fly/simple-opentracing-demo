package com.aliyun.opentracingdemo.demo05;

import com.aliyun.openservices.log.jaeger.sender.AliyunLogSender;
import com.aliyun.openservices.log.jaeger.sender.util.TracerHelper;
import com.google.common.collect.ImmutableMap;
import com.uber.jaeger.samplers.ConstSampler;
import io.opentracing.Scope;
import io.opentracing.propagation.Format.Builtin;
import io.opentracing.tag.Tags;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Hello {

  private final OkHttpClient client;

  private Hello() {
    this.client = new OkHttpClient();
  }

  private static class RequestBuilderCarrier implements io.opentracing.propagation.TextMap {

    private final Request.Builder builder;

    RequestBuilderCarrier(Request.Builder builder) {
      this.builder = builder;
    }

    @Override
    public Iterator<Entry<String, String>> iterator() {
      throw new UnsupportedOperationException("carrier is write-only");
    }

    @Override
    public void put(String key, String value) {
      builder.addHeader(key, value);
    }

  }

  private String getHttp(int port, String path, String param, String value) {
    try {
      HttpUrl url = new HttpUrl.Builder().scheme("http").host("localhost").port(port)
          .addPathSegment(path)
          .addQueryParameter(param, value).build();
      Request.Builder requestBuilder = new Request.Builder().url(url);

      Tags.SPAN_KIND.set(TracerHelper.activeSpan(), Tags.SPAN_KIND_CLIENT);
      Tags.HTTP_METHOD.set(TracerHelper.activeSpan(), "GET");
      Tags.HTTP_URL.set(TracerHelper.activeSpan(), url.toString());
      TracerHelper.inject(TracerHelper.activeSpan().context(), Builtin.HTTP_HEADERS,
          new RequestBuilderCarrier(requestBuilder));

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
