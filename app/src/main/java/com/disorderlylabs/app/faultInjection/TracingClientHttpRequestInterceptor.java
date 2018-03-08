package com.disorderlylabs.app.faultInjection;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.http.HttpClientHandler;
import brave.http.HttpTracing;
import brave.propagation.Propagation.Setter;
import brave.propagation.TraceContext;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public final class TracingClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {
  static final Setter<HttpHeaders, String> SETTER = new Setter<HttpHeaders, String>() {
    @Override public void put(HttpHeaders carrier, String key, String value) {
      carrier.set(key, value);
    }

    @Override public String toString() {
      return "HttpHeaders::set";
    }
  };

//  public static ClientHttpRequestInterceptor create(Tracing tracing) {
//    System.out.println("[LOG] ClientHttpRequestInterceptor create");
//    return create(HttpTracing.create(tracing));
//  }
//
//  public static ClientHttpRequestInterceptor create(HttpTracing httpTracing, PropagationData propagationData) {
//    System.out.println("[LOG] ClientHttpRequestInterceptor create - HttpTracing");
//    System.out.println("HttpTracing variable: " + httpTracing.hashCode());
//    return new TracingClientHttpRequestInterceptor(httpTracing);
//  }

  final Tracer tracer;
  final HttpClientHandler<HttpRequest, ClientHttpResponse> handler;
  final TraceContext.Injector<HttpHeaders> injector;

  //map of <traceID, set<services urls>> to keep track of the services that have been called
  private ConcurrentHashMap<String, HashSet<String>> calledServices;

  @Autowired TracingClientHttpRequestInterceptor(HttpTracing httpTracing, PropagationData propagationData) {
    System.out.println("[LOG] Autowired TracingClientHttpRequestInterceptor");
    System.out.println("HttpTracing variable: " + httpTracing.hashCode());
    System.out.println("propagationData variable: " + propagationData.hashCode());
    tracer = httpTracing.tracing().tracer();
    handler = HttpClientHandler.create(httpTracing, new HttpAdapter());
    injector = httpTracing.tracing().propagation().injector(SETTER);
    calledServices = propagationData.getCalledServices();
  }

  @Override public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                                ClientHttpRequestExecution execution) throws IOException {
    System.out.println("[LOG] ClientInterceptor - Intercepting");


    Span span = handler.handleSend(injector, request.getHeaders(), request);

    //injecting called services into headers
    //span info will be in the form "RealSpan (traceID/spanID)", we need to extract trace and span IDs
    String spanInfo = span.toString();
    String ids = spanInfo.substring(spanInfo.indexOf("(")+1, spanInfo.indexOf(")"));
    String traceid = ids.split("/")[0];
    System.out.println("[LOG] traceID: " + traceid);

    HashSet<String> servicesSet;
    if(!calledServices.containsKey(traceid)) {
      System.out.println("[DEBUG] calledservices->size before: " + calledServices.size());
      System.out.println("[DEBUG] trace not in calledservices map");
      servicesSet = new HashSet<>();
      calledServices.put(traceid, servicesSet);
      System.out.println("[DEBUG] calledservices->size after: " + calledServices.size());
    }else {
      servicesSet = calledServices.get(traceid);
    }

//    String services = "";
//    for(String s : servicesSet) {
//      services += s;
//      services += "_"; //DELIMITER, need to move to another class
//    }
//    System.out.println("Services: " + services);
//
//    //inject services into header
//    request.getHeaders().add(traceid, services);
//
//    System.out.println("[LOG] ClientInterceptor - Constructing span. ID: " + span.toString());

    ClientHttpResponse response = null;
    Throwable error = null;
    try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
      response = execution.execute(request, body);
      System.out.println("[LOG] ClientInterceptor - After calling: " + request.getURI());
    } catch (IOException | RuntimeException | Error e) {
      error = e;
      throw e;
    } finally {
      handler.handleReceive(response, error, span);
    }


    //logging the called service
    if(response.getStatusCode().is2xxSuccessful()) {
      String endpoint = request.getURI().toString();
      System.out.println("[DEBUG] call succcessful, adding endpoint to services set: " + endpoint);
      servicesSet.add(endpoint);
    }

    return response;
  }

  static final class HttpAdapter
          extends brave.http.HttpClientAdapter<HttpRequest, ClientHttpResponse> {

    @Override public String method(HttpRequest request) {
      return request.getMethod().name();
    }

    @Override public String url(HttpRequest request) {
      return request.getURI().toString();
    }

    @Override public String requestHeader(HttpRequest request, String name) {
      Object result = request.getHeaders().getFirst(name);
      return result != null ? result.toString() : "";
    }

    @Override public Integer statusCode(ClientHttpResponse response) {
      try {
        return response.getRawStatusCode();
      } catch (IOException e) {
        return null;
      }
    }
  }
}
