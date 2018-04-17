package com.aliyun.opentracingdemo.util;

import com.aliyun.openservices.log.jaeger.sender.util.TracerHelper;
import io.opentracing.Scope;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapExtractAdapter;
import io.opentracing.tag.Tags;
import java.util.HashMap;
import javax.ws.rs.core.MultivaluedMap;

public class Tracing {

  public static Scope startServerSpan(javax.ws.rs.core.HttpHeaders httpHeaders,
      String operationName) {
    // format the headers for extraction
    MultivaluedMap<String, String> rawHeaders = httpHeaders.getRequestHeaders();
    final HashMap<String, String> headers = new HashMap<String, String>();
    for (String key : rawHeaders.keySet()) {
      headers.put(key, rawHeaders.get(key).get(0));
    }

    Tracer.SpanBuilder spanBuilder;
    try {
      SpanContext parentSpanCtx = TracerHelper.extract(Format.Builtin.HTTP_HEADERS, new TextMapExtractAdapter(headers));
      if (parentSpanCtx == null) {
        spanBuilder = TracerHelper.buildSpan(operationName);
      } else {
        spanBuilder = TracerHelper.buildSpan(operationName).asChildOf(parentSpanCtx);
      }
    } catch (IllegalArgumentException e) {
      spanBuilder = TracerHelper.buildSpan(operationName);
    }

    return spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER).startActive(true);
  }

}
