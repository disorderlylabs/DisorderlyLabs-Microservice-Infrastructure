package com.disorderlylabs.invoice.faultInjection;


import brave.Span;

import javax.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class will contain data structures for logging data to be propagated from child to parent
 *
 * */
public class Propagation {
    public enum FAULT_TYPES {DELAY, DROP_PACKET}

    //delimiters
    public static String SERVICES_DELIM = "#"; //called services


    //response header keys
    public static String SERVICES = "CALLED_SERVICES";
    public static String TIMESTAMP = "TIMESTAMP";      //global timestamp incrementing once per service

    //fault injection keys
    public static String SEQ_DELIM = ";";
    public static String FIELD_DELIM = ":";

    public static class propagationData {
        //map of <traceID, set<services urls>> to keep track of the services that have been called
        HashSet<String> calledServices;

        //global timestamp for the trace
        int timestamp;

        //for simulating fault injection
        String fault;

        public propagationData() {
            calledServices = new HashSet<>();
            timestamp = 1;
            fault = null;
        }

        public HashSet<String> getCalledServices() {
            return calledServices;
        }

        public void setFault(String f) { fault = f; }

        public int getTimestamp() {
            return timestamp;
        }

        public String getFault() {
            return fault;
        }
    }


    //map of <traceID, set<services urls>> to keep track of the services that have been called
//    private ConcurrentHashMap<String, HashSet<String>> calledServices;

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




//    public ConcurrentHashMap<String, HashSet<String>> getCalledServices() {
//        return calledServices;
//    }

    public ConcurrentHashMap<String, propagationData> getTraceMap() {
        return traceMap;
    }
}
