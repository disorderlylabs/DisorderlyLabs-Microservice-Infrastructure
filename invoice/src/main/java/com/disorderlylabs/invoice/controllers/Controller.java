package com.disorderlylabs.invoice.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.context.annotation.Lazy;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.http.util.EntityUtils; 
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;


@RestController
public class Controller {

private static final String cart_primary_ip = System.getenv("cart_ip");
private static final String inventory_primary_ip = System.getenv("inventory_ip");
private static final String pg_primary_ip = System.getenv("pg_ip");
private static final String cart_backup_ip = System.getenv("cart_b_ip");
private static final String inventory_backup_ip = System.getenv("inventory_b_ip");
private static final String pg_backup_ip = System.getenv("pg_b_ip");
private static String invoice_data = "";

  @Autowired
  @Lazy
  RestTemplate restTemplate;

  @RequestMapping("/invoice")
  public String index() {
    String result = GenericHystrixCommand.execute("InvoiceCommandGroup", "IndexCommand", () -> {
      // maps to initial run()
      throw new IllegalArgumentException();
      // return "This is the GenericHystrixCommand Invoice App!";
    }, (t) -> {
      // maps to getFallback()
      return "This is the GenericHystrixCommand fallback Invoice.";
    });

    return result;
  }

  @RequestMapping("/invoice/test")
  public String test() {
    String result = GenericHystrixCommand.execute("InvoiceCommandGroup", "TestCommand", () -> {
      return testFunc(pg_primary_ip);
    }, (t) -> {
      return testFunc(pg_backup_ip);
      // return "{\"status\":\"failure\",\"message\":\"/invoice/test has failed.\"}";
    });

    return result;
  }

  private String testFunc(String pg_ip) {
    String response;
    String pg_URL = "http://" + pg_ip + "/pg";

    response = restTemplate.getForObject(pg_URL, String.class);
    System.out.println("pg response: " + response);

    return response;
  }

  @RequestMapping("/invoice/generateInvoice")
  public String generateInvoice() 
  {
    String result = GenericHystrixCommand.execute("InvoiceCommandGroup", "GenerateInvoiceCommand", () -> {
      return generateInvoiceFunc(cart_primary_ip, inventory_primary_ip);
    }, (t) -> {
      return generateInvoiceFunc(cart_backup_ip, inventory_backup_ip);
      // return "{\"status\":\"failure\",\"message\":\"/invoice/test has failed.\"}";
    });

    return result;
  }

  private String generateInvoiceFunc(String cart_ip, String inventory_ip) {
    String cart_URL = "http://" + cart_ip + "/cart/getCartItems";
    String response = restTemplate.getForObject(cart_URL, String.class);

    JsonParser parser = new JsonParser();
    JsonElement e = parser.parse(response);
    double final_price = 0;
    invoice_data = "Name\t|\tQuantity\t|\tPrice\n";
    invoice_data = invoice_data + "--------------------------------------------\n";
    if (e.isJsonArray())
    {
      JsonArray ja = e.getAsJsonArray();
      for(int i = 0; i<ja.size(); i++)
      {
        JsonObject o = ja.get(i).getAsJsonObject();
        int ItemID = Integer.parseInt(o.get("itemID").toString());
        int quantity = Integer.parseInt(o.get("quantity").toString());
        double total_price = Double.parseDouble(o.get("totalPrice").toString());  
        String inventory_URL = "http://" + inventory_ip + "/inventory/getName?ItemID="+ItemID;
        response = restTemplate.getForObject(inventory_URL, String.class);
        o = parser.parse(response).getAsJsonObject();
        String name = o.get("name").toString();
        final_price = final_price + total_price;
        invoice_data = invoice_data + name +"\t|"+ quantity +"\t|"+ total_price + "\n";                      
      }
      invoice_data = invoice_data + "--------------------------------------------\n";
      invoice_data = invoice_data + "Total Price  =  "+ final_price + "\n";
      return "{\"status\":\"success\"}";
    }
    else return "{\"status\":\"failure: Get Cart Items wasn't a JSON Array\"}";
  }

  @RequestMapping("/invoice/getInvoice")
  public String getInvoice() 
  {
    if (invoice_data.length() == 0)
      return "Invoice not generated yet";
    else
      return invoice_data;
  }

  @RequestMapping("/invoice/clearInvoice")
  public void clearInvoice() 
  {
    invoice_data = "";
  }  

  // String convertToString(HttpResponse response) throws IOException
  // {
  //   if(response!=null)
  //   {
  //     BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
  //     String line = "";
  //     String line2 = "";
  //     while ((line = rd.readLine()) != null) 
  //     {
  //       line2+=line+"\n";
  //     }
  //     return line2;
  //   }
  //   return "";
  // }   
  
}  
