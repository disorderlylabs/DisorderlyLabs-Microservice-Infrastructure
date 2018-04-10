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


@RestController
public class Controller {

// private static final String cart_URL = System.getenv("cart_ip");
// private static final String inventory_URL = System.getenv("inventory_ip");
private static HashMap<String, String> invoices = new HashMap<String, String>();

  @Autowired
  @Lazy
  RestTemplate restTemplate;

  @Value("${message:Hello default}")
    private String message;

  @Value("${cart_ip}")
    private String cart_URL;

  @Value("${inventory_ip}")
    private String inventory_URL;

  @RequestMapping("/invoice")
  public String index() {
      return "Greetings from Invoice Microservice!";
  }

  @RequestMapping("/invoice/checkConfig")
  public String checkConfig() {
      return message;
  }  

  @RequestMapping("/invoice/test")
  public String test() {
    String response;

    String pg = "http://" + inventory_URL + "/inventory";

    response = restTemplate.getForObject(pg, String.class);
    System.out.println("inventory response: " + response);

    return response;
  }

  @RequestMapping("/invoice/{userID}/generateInvoice")
  public String generateInvoice(@PathVariable String userID) 
  {
    try
    {
      String url = "http://" + cart_URL + "/cart/" + userID + "/getCartItems";
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
          url = "http://" + inventory_URL + "/inventory/getName?ItemID="+ItemID;
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

    }

    catch(Exception e)
    {
      return "{\"status\":\"" + e.toString() + "\"}";
    } 
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
