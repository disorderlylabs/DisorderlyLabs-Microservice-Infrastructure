package com.disorderlylabs.app.faultInjection;


import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;


import brave.Span;
import brave.Tracing;
import brave.propagation.TraceContext;

import okhttp3.Request;

import static brave.propagation.Propagation.KeyFactory.STRING;


public class Fault {
    public enum FAULT_TYPES {DELAY, DROP_PACKET}

    public static String SEQ_DELIM = ";";
    public static String FIELD_DELIM = ":";


    public static Span spanFromContext(Tracing tracing, HttpServletRequest request) {
        Map<String, String> httpHeaders = getHeaders(request);
        TraceContext.Extractor<Map<String, String>> extractor;
        extractor = tracing.propagationFactory().create(STRING).extractor(Map::get);
        Span span = tracing.tracer().nextSpan(extractor.extract(httpHeaders));

        return span;
    }



    public static Map<String, String> getHeaders(HttpServletRequest request) {
        Map<String, String> httpHeaders = new HashMap<>();
        Enumeration headerNames = request.getHeaderNames();

        while (headerNames.hasMoreElements()) {
            String key = (String) headerNames.nextElement();
            String value = request.getHeader(key);
            httpHeaders.put(key, value);
//            System.out.println("KEY: " + key);
//            System.out.println("VALUE: " + value);
        }
        return httpHeaders;
    }


    public static void injectContext(Tracing tracing, Request.Builder req, Span span) {
        tracing.propagation().injector(Request.Builder::addHeader)
                .inject(span.context(), req);
    }

}
