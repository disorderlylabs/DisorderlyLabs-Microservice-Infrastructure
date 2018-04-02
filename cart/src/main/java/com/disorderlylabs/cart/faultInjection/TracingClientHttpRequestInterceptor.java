package com.disorderlylabs.cart.faultInjection;

import brave.Span;
import brave.Tracer;
import brave.http.HttpClientHandler;
import brave.http.HttpTracing;
import brave.propagation.Propagation.Setter;
import brave.propagation.TraceContext;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
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

  final Tracer tracer;
  final HttpClientHandler<HttpRequest, ClientHttpResponse> handler;
  final TraceContext.Injector<HttpHeaders> injector;

  //map of <traceID, set<services urls>> to keep track of the services that have been called
  Propagation propagation;
  //private ConcurrentHashMap<String, HashSet<String>> calledServices;
  private ConcurrentHashMap<String, Propagation.propagationData> traceMap;

  @Autowired TracingClientHttpRequestInterceptor(HttpTracing httpTracing, Propagation propagation) {
    System.out.println("[LOG] Autowired TracingClientHttpRequestInterceptor");
    tracer = httpTracing.tracing().tracer();
    handler = HttpClientHandler.create(httpTracing, new HttpAdapter());
    injector = httpTracing.tracing().propagation().injector(SETTER);
    this.propagation = propagation;
    //calledServices = propagation.getCalledServices();
    traceMap = propagation.getTraceMap();
  }

  @Override public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                                ClientHttpRequestExecution execution) throws IOException {
    System.out.println("[LOG] ClientInterceptor - Intercepting");

    Span span = handler.handleSend(injector, request.getHeaders(), request);

    //injecting called services into headers
    String traceid = propagation.extractTraceID(span);





    Propagation.propagationData traceData = traceMap.get(traceid);
    if(traceData == null) {
      System.out.println("[ERROR] traceData null for entry: " + traceid);
      System.exit(1);
    }


    String services = "";
    HashSet<String> servicesSet = null;
    servicesSet = traceData.getCalledServices();

    for(String s : servicesSet) {
      services += s;
      services += Propagation.SERVICES_DELIM; //DELIMITER, need to move to another class
    }

    System.out.println("[LOG] Injecting Services: " + services);

    //inject services into header
    request.getHeaders().add(traceid, services);

    if(traceData.getFault() != null) {
      request.getHeaders().add("InjectFault", traceData.getFault());
    }


    ClientHttpResponse response = null;
    Throwable error = null;
    try (Tracer.SpanInScope ws = tracer.withSpanInScope(span)) {
      response = execution.execute(request, body);
      System.out.println("[LOG] ClientInterceptor - After calling: " + request.getURI());

      System.out.println("[LOG] Parsing called services from child");
      List<String> calledServices = response.getHeaders().get(Propagation.SERVICES);
      if(calledServices != null) {
        System.out.println("[LOG] services list: " + calledServices);
      }


    } catch (IOException | RuntimeException | Error e) {
      error = e;
      throw e;
    } finally {
      handler.handleReceive(response, error, span);
    }


    //logging the called service
    if(response.getStatusCode().is2xxSuccessful() && servicesSet != null) {
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
