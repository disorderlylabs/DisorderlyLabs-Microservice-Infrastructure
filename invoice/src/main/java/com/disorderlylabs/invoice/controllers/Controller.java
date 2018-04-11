package com.disorderlylabs.invoice.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.HashMap;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;


@RestController
public class Controller {

// private static final String cart_p_ip = System.getenv("cart_ip");
// private static final String inventory_p_ip = System.getenv("inventory_ip");
private static HashMap<String, String> invoices = new HashMap<String, String>();

  @Autowired
  @Lazy
  RestTemplate restTemplate;

  @Value("${message:Hello default}")
    private String message;

  @Value("${cart_p_ip}")
    private String cart_p_ip;

  @Value("${cart_b_ip}")
    private String cart_b_ip;

  @Value("${inventory_p_ip}")
    private String inventory_p_ip;

  @Value("${inventory_b_ip}")
    private String inventory_b_ip;

  @Value("${pg_p_ip}")
    private String pg_p_ip;

  @Value("${pg_b_ip}")
    private String pg_b_ip;

  @RequestMapping("/invoice")
  public String index() {
      return "Greetings from Invoice Microservice!";
  }

  @RequestMapping("/invoice/checkConfig")
  public String checkConfig() {
      return message;
  }  


  /*
    --------------------------------
    /invoice/test
    This command tests connectivity to payment gateway.
    --------------------------------
  */
  @RequestMapping("/invoice/test")
  @HystrixCommand(groupKey="InvoiceServiceGroup", commandKey = "testCommand", fallbackMethod = "testFallback")
  public String test()
  {
    return testFunc(pg_p_ip);
  }

  public String testFallback()
  {
    return testFunc(pg_b_ip);
  }

  private String testFunc(String pg_ip)
  {
    String response;
    String pg = "http://" + pg_ip + "/pg";
    response = restTemplate.getForObject(pg, String.class);
    System.out.println("pg response: " + response);
    return response;
  }


  /*
    --------------------------------
    /invoice/generateInvoice
    This command tests gets the cart items for a specific user
    and calls inventory to generate an invoice for the items in
    the user's cart.
    --------------------------------
  */
  @RequestMapping("/invoice/{userID}/generateInvoice")
  @HystrixCommand(groupKey="InvoiceServiceGroup", commandKey = "generateInvoiceCommand", fallbackMethod = "generateInvoiceFallback")
  public String generateInvoice(@PathVariable String userID) 
  {
    // return generateInvoiceFunc(userID, cart_p_ip, inventory_p_ip);
    return generateInvoiceFunc(userID, cart_b_ip, inventory_b_ip);
  }

  public String generateInvoiceFallback(String userID)
  {
    return generateInvoiceFunc(userID, cart_b_ip, inventory_b_ip);
  }

  private String generateInvoiceFunc(String userID, String cart_ip, String inventory_ip)
  {
    String url = "http://" + cart_ip + "/cart/" + userID + "/getCartItems";
    String response = restTemplate.getForObject(url, String.class);

    JsonParser parser = new JsonParser();
    JsonElement e = parser.parse(response);
    double final_price = 0;
    String invoice_data = "";
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
        url = "http://" + inventory_ip + "/inventory/getName?ItemID="+ItemID;
        response = restTemplate.getForObject(url, String.class);
        o = parser.parse(response).getAsJsonObject();
        String name = o.get("name").toString();
        final_price = final_price + total_price;
        invoice_data = invoice_data + name +"\t|"+ quantity +"\t|"+ total_price + "\n";                      
      }
      invoice_data = invoice_data + "--------------------------------------------\n";
      invoice_data = invoice_data + "Total Price  =  "+ final_price + "\n";
      invoices.put(userID + "", invoice_data);
      return "{\"status\":\"success\"}";
    }
    else
     return "{\"status\":\"failure: Get Cart Items wasn't a JSON Array\"}";


    // String url = "http://" + cart_ip + "/cart/" + userID + "/getCartItems";
    // String response = restTemplate.getForObject(url, String.class);

    // JsonParser parser = new JsonParser();
    // JsonElement e = parser.parse(response);
    // double final_price = 0;
    // String invoice_data = "";
    // invoice_data = "Name\t|\tQuantity\t|\tPrice\n";
    // invoice_data = invoice_data + "--------------------------------------------\n";
    // if (e.isJsonArray())
    // {
    //   JsonArray ja = e.getAsJsonArray();
    //   for(int i = 0; i<ja.size(); i++)
    //   {
    //     JsonObject o = ja.get(i).getAsJsonObject();
    //     int ItemID = Integer.parseInt(o.get("itemID").toString());
    //     int quantity = Integer.parseInt(o.get("quantity").toString());
    //     double total_price = Double.parseDouble(o.get("totalPrice").toString());  
    //     url = "http://" + inventory_ip + "/inventory/getName?ItemID="+ItemID;
    //     response = restTemplate.getForObject(url, String.class);
    //     o = parser.parse(response).getAsJsonObject();
    //     String name = o.get("name").toString();
    //     final_price = final_price + total_price;
    //     invoice_data = invoice_data + name +"\t|"+ quantity +"\t|"+ total_price + "\n";                      
    //   }
    //   invoice_data = invoice_data + "--------------------------------------------\n";
    //   invoice_data = invoice_data + "Total Price  =  "+ final_price + "\n";
    //   invoices.put(userID + "", invoice_data);
    // }
    // return "{\"status\":\"success\"}";
  }

  @RequestMapping("/invoice/{userID}/getInvoice")
  public String getInvoice(@PathVariable String userID)
  {
    if (invoices.containsKey(userID + ""))
      return invoices.get(userID + "");
    return "Invoice not generated yet";
      
  }

  @RequestMapping("/invoice/{userID}/clearInvoice")
  public void clearInvoice(@PathVariable String userID) 
  {
    if (invoices.containsKey(userID + ""))
      invoices.remove(userID + "");
  }  

  String convertToString(HttpResponse response) throws IOException
  {
    if(response!=null)
    {
      BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
      String line = "";
      String line2 = "";
      while ((line = rd.readLine()) != null) 
      {
        line2+=line+"\n";
      }
      return line2;
    }
    return "";
  }   
  
}  
