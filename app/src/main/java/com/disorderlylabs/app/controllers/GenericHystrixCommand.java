package com.disorderlylabs.app.controllers;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.exception.HystrixBadRequestException;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.config.ConfigurationManager;
import rx.Observable;

import java.util.function.Function;
import java.util.function.Supplier;


/*
GenericHystrixCommand.java
Source: https://dzone.com/articles/hystrix-command-java-8-helpers
None of this comes from me, entirely stolen from this guy

Idea: nested HystrixCommand wrappers are awful -> use lambda expressions

Works with the microservices.

Problem: the first call errors because of a timeout.  Still trying to figure
out whether this is because of Hystrix's own timeout code or because of the
error Spring Boot/restTemplate throws.  If it is the restTemplate error, Hystrix
may be catching that and causing the entire thing to error rather than enforcing
its own timeout - which should be fixed to 4000ms in this file.
*/


class CustomSystemException extends RuntimeException {
    
    public CustomSystemException(Throwable root) {
        super(root);
    }
}


class CustomBusinessException extends RuntimeException {
    
    public CustomBusinessException(String message) {
        super(message);
    }
    public CustomBusinessException(Throwable root) {
        super(root);
    }
}


public class GenericHystrixCommand<T> extends HystrixCommand<T> {

    private Supplier<T> toRun;

    private Function<Throwable, T> fallback;


    public static <T> T execute(String groupKey, String commandkey, Supplier<T> toRun) {
        return execute(groupKey, commandkey, toRun, null);
    }

    public static <T> T execute(String groupKey, String commandkey, Supplier<T> toRun, Function<Throwable, T> fallback) {
        try {
            return new GenericHystrixCommand<>(groupKey, commandkey, toRun, fallback).execute();
        } catch (Exception e) {
            throw wrappedException(e);
        }
    }

    public static <T> Observable<T> executeObservable(String groupKey, String commandkey, Supplier<T> toRun) {
        return executeObservable(groupKey, commandkey, toRun, null);
    }

    public static <T> Observable<T> executeObservable(String groupKey, String commandkey, Supplier<T> toRun, Function<Throwable, T> fallback) {
        return new GenericHystrixCommand<>(groupKey, commandkey, toRun, fallback)
                .toObservable()
                .onErrorReturn(t -> {throw wrappedException(t);});
    }

    public GenericHystrixCommand(String groupKey, String commandkey, Supplier<T> toRun, Function<Throwable, T> fallback) {
        super(Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
                .andCommandKey(HystrixCommandKey.Factory.asKey(commandkey)));

        String groupConfig = "hystrix.command." + groupKey + ".execution.isolation.thread.timeoutInMilliseconds";
        ConfigurationManager.getConfigInstance().setProperty(groupConfig,4000);
        this.toRun = toRun;
        this.fallback = fallback;
    }

    protected T run() throws Exception {
        return this.toRun.get();
    }

    @Override
    protected T getFallback() {
        System.out.println(getFailedExecutionException().getMessage());
        return (this.fallback != null)
                ? this.fallback.apply(getExecutionException())
                : super.getFallback();
    }
    
    private static RuntimeException wrappedException(Throwable t) {
        if (t instanceof HystrixRuntimeException) {
            return new CustomSystemException(t.getCause());
        } else if (t instanceof HystrixBadRequestException) {
            return new CustomBusinessException(t.getMessage());
        }
        throw new RuntimeException(t);
    }
}