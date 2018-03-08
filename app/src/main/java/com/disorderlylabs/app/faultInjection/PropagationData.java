package com.disorderlylabs.app.faultInjection;


import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class will contain data structures for logging data to be propagated from
 * child to parent and also for fault injection logic
 *
 * */
public class PropagationData {

    //map of <traceID, set<services urls>> to keep track of the services that have been called
    private ConcurrentHashMap<String, HashSet<String>> calledServices;


    public PropagationData() {
        calledServices = new ConcurrentHashMap<>();
    }


    public ConcurrentHashMap<String, HashSet<String>> getCalledServices() {
        return calledServices;
    }
}
