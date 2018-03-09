package com.disorderlylabs.app.faultInjection;


import brave.Span;

import javax.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class will contain data structures for logging data to be propagated from child to parent
 *
 * */
public class Propagation {

    //delimiters
    public static String SERVICES_DELIM = "#"; //called services


    //response header keys
    public static String SERVICES = "CALLED_SERVICES";
    public static String TIMESTAMP = "TIMESTAMP";      //global timestamp incrementing once per service

    public static class propagationData {
        //map of <traceID, set<services urls>> to keep track of the services that have been called
        HashSet<String> calledServices;

        //global timestamp for the trace
        int timestamp;

        public propagationData() {
            calledServices = new HashSet<>();
            timestamp = 1;
        }

        public HashSet<String> getCalledServices() {
            return calledServices;
        }

        public int getTimestamp() {
            return timestamp;
        }
    }


    //map of <traceID, set<services urls>> to keep track of the services that have been called
    private ConcurrentHashMap<String, HashSet<String>> calledServices;

    //map of <traceID, traceData>
    private ConcurrentHashMap<String, propagationData> traceMap;



    public Propagation() {
        traceMap = new ConcurrentHashMap<>();
    }


    public String extractTraceID(Span span) {
        //span info will be in the form "RealSpan (traceID/spanID)", we need to extract trace and span IDs
        String spanInfo = span.toString();
        String ids = spanInfo.substring(spanInfo.indexOf("(")+1, spanInfo.indexOf(")"));
        return ids.split("/")[0];
    }

//    public void incrementTimestamp(HttpServletResponse response) {
//        int timestamp;
//        if(!response.containsHeader(TIMESTAMP)) {
//            timestamp = 1;
//        }else{
//            String timestr = response.get
//        }
//
//
//    }



    public ConcurrentHashMap<String, HashSet<String>> getCalledServices() {
        return calledServices;
    }

    public ConcurrentHashMap<String, propagationData> getTraceMap() {
        return traceMap;
    }
}
