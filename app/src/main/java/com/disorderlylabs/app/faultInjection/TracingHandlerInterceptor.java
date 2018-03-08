package com.disorderlylabs.app.faultInjection;

import brave.Span;
import brave.Tracer;
import brave.Tracing;
import brave.Tracer.SpanInScope;
import brave.http.HttpServerHandler;
import brave.http.HttpTracing;
import brave.propagation.Propagation.Getter;
import brave.propagation.TraceContext.Extractor;
import brave.servlet.HttpServletAdapter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public final class TracingHandlerInterceptor implements HandlerInterceptor {
    static final Getter<HttpServletRequest, String> GETTER = new Getter<HttpServletRequest, String>() {
        public String get(HttpServletRequest carrier, String key) {
            return carrier.getHeader(key);
        }

        public String toString() {
            return "HttpServletRequest::getHeader";
        }
    };
    final Tracer tracer;
    final HttpServerHandler<HttpServletRequest, HttpServletResponse> handler;
    final Extractor<HttpServletRequest> extractor;
    //map of <traceID, set<services urls>> to keep track of the services that have been called
    private ConcurrentHashMap<String, HashSet<String>> calledServices;

//    public static HandlerInterceptor create(Tracing tracing) {
//        return new TracingHandlerInterceptor(HttpTracing.create(tracing));
//    }
//
//    public static HandlerInterceptor create(HttpTracing httpTracing) {
//        return new TracingHandlerInterceptor(httpTracing);
//    }

    @Autowired
    TracingHandlerInterceptor(HttpTracing httpTracing, PropagationData propagationData) {
        System.out.println("[LOG] Autowired TracingHandlerInterceptor");
        System.out.println("HttpTracing variable: " + httpTracing.hashCode());
        System.out.println("propagationData variable: " + propagationData.hashCode());
        this.tracer = httpTracing.tracing().tracer();
        this.handler = HttpServerHandler.create(httpTracing, new HttpServletAdapter());
        this.extractor = httpTracing.tracing().propagation().extractor(GETTER);
        calledServices = propagationData.getCalledServices();
    }

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object o) {
        System.out.println("[LOG] ServerInterceptor - Prehandle. URL = " + request.getRequestURI());
        if (request.getAttribute(SpanInScope.class.getName()) != null) {
            return true;
        } else {
            Span span = this.handler.handleReceive(this.extractor, request);
            System.out.println("[LOG] ServerInterceptor Constructing span. ID: " + span.toString());
            request.setAttribute(SpanInScope.class.getName(), this.tracer.withSpanInScope(span));
            return true;
        }
    }

    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }

    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object o, Exception ex) {
        System.out.println("[LOG] SErverInterceptor - afterCompletion. URL = " + request.getRequestURI());
        Span span = this.tracer.currentSpan();
        System.out.println("[LOG] ServerInterceptor, current span: " + span.toString());
        if (span != null) {
            ((SpanInScope)request.getAttribute(SpanInScope.class.getName())).close();
            this.handler.handleSend(response, ex, span);
        }
        System.out.println("[LOG] getting the called services for this trace");
        String spanInfo = span.toString();
        String ids = spanInfo.substring(spanInfo.indexOf("(")+1, spanInfo.indexOf(")"));
        String traceid = ids.split("/")[0];
        System.out.println("[LOG] traceID: " + traceid);
        System.out.println("[DEBUG] called services->size: " + calledServices.size());

        HashSet<String> servicesSet;
        if(!calledServices.containsKey(traceid)) {
            System.out.println("[DEBUG] trace not in calledservices map");
        }else {
            servicesSet = calledServices.get(traceid);
            for(String s : servicesSet) {
                System.out.println("service: " + s);
            }
        }


    }
}
