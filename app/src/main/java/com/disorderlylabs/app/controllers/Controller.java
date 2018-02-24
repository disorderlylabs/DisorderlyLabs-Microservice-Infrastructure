package com.disorderlylabs.app.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMethod;

import org.springframework.jdbc.core.JdbcTemplate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.http.util.EntityUtils; 
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@RestController
public class Controller {

  @Autowired
  JdbcTemplate jdbcTemplate;
  private static final String inventory_URL = System.getenv("inventory_ip");
  private static final String cart_URL = System.getenv("cart_ip");
  private static final String invoice_URL = System.getenv("invoice_ip");

  @RequestMapping("/app")
  public String index() {
      return "Greetings from App Microservice!";
  }

  @RequestMapping("/app/checkEnv")
  public String checkEnv() 
  {
    String ans = "";
    for (String key : System.getenv().keySet())
      ans = ans + key + " : " + System.getenv(key) + "\n";
    return ans;
    // return System.getenv("inventory_ip") + "";
  }

  //******--------For this particular request 'App' is acting like a forwarding node to 'Inventory'--------******.
  @RequestMapping(value = "/app/takeFromInventory", method = RequestMethod.PUT)
  public String takeFromInventory(@RequestParam(value="name", required=true) String name, @RequestParam(value="quantity", required=true) int quantity) 
  {
    try
    {
      String url = "http://" + inventory_URL + "/inventory/takeFromInventory";
      HttpClient client = new DefaultHttpClient();
      HttpPut put = new HttpPut(url);

      List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
      urlParameters.add(new BasicNameValuePair("name", name));
      urlParameters.add(new BasicNameValuePair("quantity", quantity+""));

      put.setEntity(new UrlEncodedFormEntity(urlParameters));
      HttpResponse response = client.execute(put);

      return convertToString(response);   
    }
    catch(Exception e)
    {
      return e.toString();
    }
  }

  @RequestMapping(value = "/app/addToCart", method = RequestMethod.PUT)
  public String addToCart(@RequestParam(value="name", required=true) String name, @RequestParam(value="quantity", required=true) int quantity) 
  {
    try
    {
      String url = "http://" + inventory_URL + "/inventory/takeFromInventory";
      HttpClient client = new DefaultHttpClient();
      HttpPut put = new HttpPut(url);

      List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
      urlParameters.add(new BasicNameValuePair("name", name));
      urlParameters.add(new BasicNameValuePair("quantity", quantity+""));

      put.setEntity(new UrlEncodedFormEntity(urlParameters));
      HttpResponse response = client.execute(put);
      JsonParser parser = new JsonParser();
      String res = convertToString(response);
      JsonObject o = parser.parse(res).getAsJsonObject();

      if(o.get("status").toString().contains("failure"))
        return res;
      int ItemID = Integer.parseInt(o.get("ItemID").toString());
      double total_price = Double.parseDouble(o.get("total_price").toString());

      url = "http://" + cart_URL + "/cart/addToCart";
      put = new HttpPut(url);
      urlParameters = new ArrayList<NameValuePair>();
      urlParameters.add(new BasicNameValuePair("ItemID", ItemID + ""));
      urlParameters.add(new BasicNameValuePair("quantity", quantity+""));
      urlParameters.add(new BasicNameValuePair("total_price", total_price + ""));
      put.setEntity(new UrlEncodedFormEntity(urlParameters));
      response = client.execute(put);   
      return convertToString(response);   
    }
    catch(Exception e)
    {
      return "{\"status\":\"failure at app: Could not add to cart because of " + e.toString() + "\"}";
    }    
  }

  @RequestMapping(value = "/app/instantPlaceOrder", method = RequestMethod.PUT)
  public String instantPlaceOrder(@RequestParam(value="name") String name, @RequestParam(value="quantity") int quantity) 
  {
    try
    {
      if (name != null)
      {
        String res = addToCart(name, quantity);
        JsonParser parser = new JsonParser();
        JsonObject o = parser.parse(res).getAsJsonObject();
        if(o.get("status").toString().contains("failure"))
        return res;        
      }  

      return placeOrder();   
    }
    catch(Exception e)
    {
      return "{\"status\":\"failure at app: Could not place instant order because of " + e.toString() + "\"}";
    }
  }

  @RequestMapping(value = "/app/placeOrder", method = RequestMethod.PUT)
  public String placeOrder() 
  {
    try
    {
      String url = "http://" + cart_URL + "/cart/placeOrder";
      HttpClient client = new DefaultHttpClient();
      HttpPut put = new HttpPut(url);
      HttpResponse response = client.execute(put);
      return convertToString(response);   
    }
    catch(Exception e)
    {
      return "{\"status\":\"failure at app: Could not place order because of " + e.toString() + "\"}";
    }
  }     

  @RequestMapping(value = "/app/getInvoice")
  public String getInvoice() 
  {
    try
    {
      String url = "http://" + invoice_URL + "/invoice/getInvoice";
      HttpClient client = new DefaultHttpClient();
      HttpGet get = new HttpGet(url);
      HttpResponse response = client.execute(get);
      return convertToString(response);   
    }
    catch(Exception e)
    {
      return "{\"status\":\"failure at app: Could not invoice because of " + e.toString() + "\"}";
    }
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