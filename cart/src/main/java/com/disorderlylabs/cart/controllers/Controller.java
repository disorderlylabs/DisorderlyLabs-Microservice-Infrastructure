package com.disorderlylabs.cart.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import org.springframework.jdbc.core.JdbcTemplate;
import com.disorderlylabs.cart.mappers.CartMapper;
import com.disorderlylabs.cart.repositories.Cart;
import java.util.ArrayList;
import java.util.Arrays;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.util.EntityUtils; 
import org.apache.http.HttpResponse;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@RestController
public class Controller {


  @Autowired
  @Lazy
  RestTemplate restTemplate;

  @Autowired
  JdbcTemplate jdbcTemplate;
  private static final String pg_URL = System.getenv("pg_ip");
  private static final String invoice_URL = System.getenv("invoice_ip");
  private static final String inventory_URL = System.getenv("inventory_ip");

  @RequestMapping("/cart")
  public String index() {
    String result = GenericHystrixCommand.execute("mycommandgroup", "mycommand", () -> {
        // maps to initial run()
        throw new IllegalArgumentException();
        // return "This is the GenericHystrixCommand Cart App!";
    }, (t) -> {
      // maps to getFallback()
        return "This is the GenericHystrixCommand fallback (sad)";
    });

    return result;
  }

  @RequestMapping("/cart/test")
  public String test() {
    String response;

    String invoice = "http://" + System.getenv("invoice_ip") + "/invoice/test";

    response = restTemplate.getForObject(invoice, String.class);
    System.out.println("Inventory response: " + response);

    return response;
  }

  @RequestMapping(value = "/cart/addToCart", method = RequestMethod.POST)
  public String addToCart(@RequestParam(value="ItemID", required=true) int ItemID, @RequestParam(value="quantity", required=true) int quantity, @RequestParam(value="total_price", required=true) double total_price)
  {
    try
    {
      String sql = "insert into cart values ("+ ItemID +", "+ quantity+", "+ total_price+")";
      jdbcTemplate.execute(sql);
      return "{\"status\":\"success\"}";      
    }
    catch (Exception e)
    {
      return "{\"status\":\"failure\"}";
    }
  }  

  @RequestMapping(value = "/cart/emptyCart", method = RequestMethod.DELETE)
  public String emptyCart()
  {
    try
    {
      String sql = "DELETE FROM cart";
      jdbcTemplate.execute(sql);
      return "{\"status\":\"success\"}";      
    }
    catch (Exception e)
    {
      return "{\"status\":\"failure "+ e.toString()+" \"}";
    }
  }

  @RequestMapping(value = "/cart/getCartItems", method = RequestMethod.GET)
  public ArrayList<Cart> getCartItems()
  {
    try
    {
      String sql = "select * from cart";
      ArrayList<Cart> cartItems = new ArrayList<Cart>(jdbcTemplate.query(sql, new CartMapper()));
      return cartItems;
    }
    catch(Exception e)
    {
      return null;
    }
  }

  @RequestMapping(value = "/cart/undoCart", method = {RequestMethod.PUT, RequestMethod.POST})
  public String undoCart()
  {
    try
    {
      ArrayList<Cart> cartItems = getCartItems();

      if (cartItems.size() == 0)
        return "{\"status\":\"failure\",\"message\":\"No items in cart\"}";

      for (Cart cart: cartItems)
      {  
        int quantity = cart.getQuantity();
        int ItemID = cart.getItemID();

        String url = "http://" + inventory_URL + "/inventory/addBackToInventory?ItemID="+ItemID+"&quantity="+quantity;
        HttpEntity<String> request = new HttpEntity<>("");
        String res = restTemplate.postForObject(url, request, String.class);

        JsonParser parser = new JsonParser();
        JsonObject o = parser.parse(res).getAsJsonObject();
        if((o.get("status").toString()).contains("failure"))
          return res;                
      }

      String res2 = emptyCart();
      return res2;      
    }
    catch (Exception e)
    {
      return "{\"status\":\"failure\"}";
    }
  }  

  @RequestMapping(value = "/cart/placeOrder", method = {RequestMethod.PUT, RequestMethod.POST})
  public String placeOrder()
  {
    try
    {
      ArrayList<Cart> cartItems = getCartItems();
      double final_price = 0;

      if (cartItems.size() == 0)
        return "{\"status\":\"failure\",\"message\":\"No items in cart\"}";  
              
      for (Cart cart: cartItems)
        final_price = final_price + cart.getTotalPrice();

      String url = "http://" + pg_URL + "/pg/makePayment?total_price=" + final_price;
//      HttpClient client = new DefaultHttpClient();
//      HttpGet get = new HttpGet(url);
//      HttpResponse response = client.execute(get);
//      String res = convertToString(response);
      String response = restTemplate.getForObject(url, String.class);

      
      JsonParser parser = new JsonParser();
      JsonObject o = parser.parse(response).getAsJsonObject();
      if((o.get("status").toString()).contains("failure"))
        return response;

      url = "http://" + invoice_URL + "/invoice/generateInvoice";
      response = restTemplate.getForObject(url, String.class);

      String res2 = emptyCart();
      o = parser.parse(res2).getAsJsonObject();
      if(o.get("status").toString().contains("failure"))
        return res2;

      return response;

    }
    catch (Exception e)
    {
      return "{\"status\":\"Failure: Could not place order at cart because of " + e.toString() + " \"}";
    }
  }

  JsonArray convertToJsonArray(HttpResponse response) throws IOException
  {
    if (response!=null)
    {
      String json = EntityUtils.toString(response.getEntity(), "UTF-8");
      Gson gson = new Gson();
      JsonObject body = gson.fromJson(json, JsonObject.class);
      JsonArray results = body.get("results").getAsJsonArray();
      return results;
    }
    return null;
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
