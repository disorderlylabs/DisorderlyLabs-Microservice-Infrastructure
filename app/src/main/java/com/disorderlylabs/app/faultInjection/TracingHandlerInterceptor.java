package com.disorderlylabs.app.faultInjection;

import brave.Span;
import brave.Tracer;
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
    Propagation propagation;
    //private ConcurrentHashMap<String, HashSet<String>> calledServices;
    private ConcurrentHashMap<String, Propagation.propagationData> traceMap;

    @Autowired
    TracingHandlerInterceptor(HttpTracing httpTracing, Propagation propagation) {
        System.out.println("[LOG] Autowired TracingHandlerInterceptor");
        this.tracer = httpTracing.tracing().tracer();
        this.handler = HttpServerHandler.create(httpTracing, new HttpServletAdapter());
        this.extractor = httpTracing.tracing().propagation().extractor(GETTER);
        this.propagation = propagation;
        //calledServices = propagation.getCalledServices();
        traceMap = propagation.getTraceMap();
    }

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object o) {
        System.out.println("[LOG] ServerInterceptor - Prehandle. URL = " + request.getRequestURI());
        if (request.getAttribute(SpanInScope.class.getName()) != null) {
            return true;
        } else {
            Span span = this.handler.handleReceive(this.extractor, request);
            System.out.println("[LOG] ServerInterceptor Constructing span. ID: " + span.toString());
            request.setAttribute(SpanInScope.class.getName(), this.tracer.withSpanInScope(span));

            String traceid = propagation.extractTraceID(span);
            if(!traceMap.containsKey(traceid)) {
                System.out.println("[LOG] creating new traceMap entry for traceID: " + traceid);
                 Propagation.propagationData traceData = new Propagation.propagationData();
                 traceMap.put(traceid, traceData);
            }

            return true;
        }
    }

    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }

    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object o, Exception ex) {
        System.out.println("[LOG] ServerInterceptor - afterCompletion. URL = " + request.getRequestURI());
        Span span = this.tracer.currentSpan();
        if (span != null) {
            ((SpanInScope)request.getAttribute(SpanInScope.class.getName())).close();
            this.handler.handleSend(response, ex, span);
        }

        String traceid = propagation.extractTraceID(span);
        System.out.println("[LOG] traceID: " + traceid);

        Propagation.propagationData traceData = traceMap.get(traceid);


        HashSet<String> servicesSet = traceData.getCalledServices();
        String services = "";
        if(servicesSet == null) {
            System.out.println("[ERROR] servicesSet null for trace: " + traceid);
        }else {
            for(String s : servicesSet) {
                System.out.println("service: " + s);
                services += s;
                services += propagation.SERVICES_DELIM;
            }
            //check if string empty
            System.out.println("[LOG] propagating services set back to parent: " + services);
            response.addHeader(propagation.SERVICES, services);
        }


        //Increment timestamp


    }
}
