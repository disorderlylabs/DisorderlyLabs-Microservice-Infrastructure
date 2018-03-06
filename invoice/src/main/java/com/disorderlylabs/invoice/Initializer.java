package com.disorderlylabs.invoice;

import org.springframework.web.SpringServletContainerInitializer;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

/** Indirectly invoked by {@link SpringServletContainerInitializer} in a Servlet 3+ container */
public class Initializer extends AbstractAnnotationConfigDispatcherServletInitializer {

  @Override protected String[] getServletMappings() {
    return new String[] {"/invoice", "/invoice/generateInvoice", "/invoice/getInvoice"};
  }

  @Override protected Class<?>[] getRootConfigClasses() {
    return null;
  }

  @Override protected Class<?>[] getServletConfigClasses() {
    return new Class[] {InvoiceApplication.class, TracingConfiguration.class};
  }
}
