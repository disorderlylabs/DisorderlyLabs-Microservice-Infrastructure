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


@RestController
public class Controller {

private static final String cart_URL = System.getenv("cart_ip");
private static final String inventory_URL = System.getenv("inventory_ip");
private static String invoice_data = "";

  @Autowired
  @Lazy
  RestTemplate restTemplate;

  @RequestMapping("/invoice")
  public String index() {
      return "Greetings from Invoice Microservice!";
  }

  @RequestMapping("/invoice/test")
  public String test() {
    String response;

    String pg = "http://" + System.getenv("pg_ip") + "/pg";

    response = restTemplate.getForObject(pg, String.class);
    System.out.println("pg response: " + response);

    return response;
  }

  @RequestMapping("/invoice/generateInvoice")
  public String generateInvoice() 
  {
    try
    {
      String url = "http://" + cart_URL + "/cart/getCartItems";
      String response = restTemplate.getForObject(url, String.class);

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
          url = "http://" + inventory_URL + "/inventory/getName?ItemID="+ItemID;
          response = restTemplate.getForObject(url, String.class);
          o = parser.parse(response).getAsJsonObject();
          String name = o.get("name").toString();
          final_price = final_price + total_price;
          invoice_data = invoice_data + name +"\t|"+ quantity +"\t|"+ total_price + "\n";                      
        }
        invoice_data = invoice_data + "--------------------------------------------\n";
        invoice_data = invoice_data + "Total Price  =  "+ final_price + "\n";
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
