package com.disorderlylabs.cart.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.beans.factory.annotation.Value;
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
  JdbcTemplate jdbcTemplate;
  // private static final String pg_URL = System.getenv("pg_ip");
  // private static final String invoice_URL = System.getenv("invoice_ip");
  // private static final String inventory_URL = System.getenv("inventory_ip");

  @Autowired
  @Lazy
  RestTemplate restTemplate;

  @Value("${message:Hello default}")
    private String message;

  @Value("${pg_ip}")
    private String pg_URL;

  @Value("${inventory_ip}")
    private String inventory_URL;

  @Value("${invoice_ip}")
    private String invoice_URL;

  @RequestMapping("/cart")
  public String index() {
      return "Greetings from Cart App!";
  }

  @RequestMapping("/cart/checkConfig")
  public String checkConfig() {
      return message;
  }  

  @RequestMapping("/cart/test")
  public String test() {
    String response;

    String invoice = "http://" + invoice_URL + "/invoice/test";

    response = restTemplate.getForObject(invoice, String.class);
    System.out.println("Inventory response: " + response);

    return response;
  }

  @RequestMapping(value = "/cart/{userID}/addToCart", method = RequestMethod.POST)
  public String addToCart(@PathVariable String userID, @RequestParam(value="ItemID", required=true) int ItemID, @RequestParam(value="quantity", required=true) int quantity, @RequestParam(value="total_price", required=true) double total_price)
  {
    try
    {
      String sql = "insert into cart(ItemID, quantity, total_price, userID) values ("+ ItemID +", "+ quantity+", "+ total_price+", \'"+ userID+"\')";
      jdbcTemplate.execute(sql);
      return "{\"status\":\"success\"}";      
    }
    catch (Exception e)
    {
      return "{\"status\":\"failure "+ e.toString()+" \"}";
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

  @RequestMapping(value = "/cart/{userID}/emptyCart", method = RequestMethod.DELETE)
  public String emptyCart(@PathVariable String userID)
  {
    try
    {
      String sql = "DELETE FROM cart where userID=\'"+ userID +"\'";
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

  @RequestMapping(value = "/cart/{userID}/getCartItems", method = RequestMethod.GET)
  public ArrayList<Cart> getCartItems(@PathVariable String userID)
  {
    try
    {
      String sql = "select * from cart where userID=\'"+ userID +"\'";
      ArrayList<Cart> cartItems = new ArrayList<Cart>(jdbcTemplate.query(sql, new CartMapper()));
      return cartItems;
    }
    catch(Exception e)
    {
      return null;
    }
  }

  @RequestMapping(value = "/cart/{userID}/undoCart", method = {RequestMethod.PUT, RequestMethod.POST})
  public String undoCart(@PathVariable String userID)
  {
    try
    {
      ArrayList<Cart> cartItems = getCartItems(userID);

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

      String res2 = emptyCart(userID);
      return res2;      
    }
    catch (Exception e)
    {
      return "{\"status\":\"failure\"}";
    }
  }  

  @RequestMapping(value = "/cart/{userID}/placeOrder", method = {RequestMethod.PUT, RequestMethod.POST})
  public String placeOrder(@PathVariable String userID)
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
      String response = restTemplate.getForObject(url, String.class);

      
      JsonParser parser = new JsonParser();
      JsonObject o = parser.parse(response).getAsJsonObject();
      if((o.get("status").toString()).contains("failure"))
        return response;

      url = "http://" + invoice_URL + "/invoice/" + userID + "/generateInvoice";
      response = restTemplate.getForObject(url, String.class);

      String res2 = emptyCart(userID);
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
