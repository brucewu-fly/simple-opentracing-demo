package com.aliyun.opentracingdemo.demo05;

import com.aliyun.openservices.log.jaeger.sender.AliyunLogSender;
import com.aliyun.openservices.log.jaeger.sender.util.TracerHelper;
import com.google.common.collect.ImmutableMap;

import com.uber.jaeger.samplers.ConstSampler;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;
import io.opentracing.Scope;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

public class Formatter extends Application<Configuration> {

  @Path("/format")
  @Produces(MediaType.TEXT_PLAIN)
  public class FormatterResource {

    @GET
    public String format(@QueryParam("helloTo") String helloTo, @Context HttpHeaders httpHeaders) {
      MultivaluedMap<String, String> rawHeaders = httpHeaders.getRequestHeaders();
      if (rawHeaders.get("trace-id") != null) {
        String spanContextString = rawHeaders.get("trace-id").get(0);
        try (Scope scope = TracerHelper.traceLatency("format", true, spanContextString)) {
          return doFormat(helloTo, scope);
        }
      } else {
        try (Scope scope = TracerHelper.traceLatency("format", true)) {
          return doFormat(helloTo, scope);
        }
      }
    }
  }

  private String doFormat(String helloTo, Scope scope) {
    String helloStr = String.format("Hello, %s!", helloTo);
    scope.span().log(ImmutableMap.of("event", "string-format", "value", helloStr));
    return helloStr;
  }

  @Override
  public void run(Configuration configuration, Environment environment) throws Exception {
    environment.jersey().register(new FormatterResource());
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

  public static void main(String[] args) throws Exception {
    System.setProperty("dw.server.applicationConnectors[0].port", "8081");
    System.setProperty("dw.server.adminConnectors[0].port", "9081");
    TracerHelper
        .buildTracer("formatter", buildAliyunLogSender(), new ConstSampler(true));
    new Formatter().run(args);
  }
}
