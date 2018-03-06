package com.disorderlylabs.app;

import org.springframework.web.SpringServletContainerInitializer;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

/** Indirectly invoked by {@link SpringServletContainerInitializer} in a Servlet 3+ container */
public class Initializer extends AbstractAnnotationConfigDispatcherServletInitializer {

  @Override protected String[] getServletMappings() {
    return new String[] {"/app", "/app/checkEnv", "/app/takeFromInventory", "/app/addToCart", "/app/instantPlaceOrder", "/app/placeOrder", "/app/getInvoice"};
  }

  @Override protected Class<?>[] getRootConfigClasses() {
    return null;
  }

  @Override protected Class<?>[] getServletConfigClasses() {
    return new Class[] {AppApplication.class, TracingConfiguration.class};
  }
}
