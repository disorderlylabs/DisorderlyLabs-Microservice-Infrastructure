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

// import com.netflix.hystrix.HystrixCommand;
// import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.*;


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
    String result = GenericHystrixCommand.execute("CartCommandGroup", "IndexCommand", () -> {
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
    String result = GenericHystrixCommand.execute("CartCommandGroup", "TestCommand", () -> {
      String response;
      String invoice = "http://" + System.getenv("invoice_ip") + "/invoice/test";

      response = restTemplate.getForObject(invoice, String.class);
      System.out.println("Inventory response: " + response);

      return response;
    }, (t) -> {
      return "{\"status\":\"failure\",\"message\":\"/cart/test has failed.\"}";
    });

    return result;
  }

  @RequestMapping(value = "/cart/addToCart", method = RequestMethod.POST)
  public String addToCart(@RequestParam(value="ItemID", required=true) int ItemID, @RequestParam(value="quantity", required=true) int quantity, @RequestParam(value="total_price", required=true) double total_price)
  {
    String result = GenericHystrixCommand.execute("CartCommandGroup", "AddToCartCommand", () -> {
      String sql = "insert into cart values ("+ ItemID +", "+ quantity+", "+ total_price+")";
      jdbcTemplate.execute(sql);
      return "{\"status\":\"success\"}";
    }, (t) -> {
      return "{\"status\":\"failure\",\"message\":\"/cart/addToCart has failed.\"}";
    });

    return result;
  }  

  @RequestMapping(value = "/cart/emptyCart", method = RequestMethod.DELETE)
  public String emptyCart()
  {
    String result = GenericHystrixCommand.execute("CartCommandGroup", "EmptyCartCommand", () -> {
      String sql = "DELETE FROM cart";
      jdbcTemplate.execute(sql);
      return "{\"status\":\"success\"}";
    }, (t) -> {
      return "{\"status\":\"failure\",\"message\":\"/cart/emptyCart has failed.\"}";
    });

    return result;
  }

  @RequestMapping(value = "/cart/getCartItems", method = RequestMethod.GET)
  public ArrayList<Cart> getCartItems()
  {
    ArrayList<Cart> result = GenericHystrixCommand.execute("CartCommandGroup", "GetCartItemsCommand", () -> {
      String sql = "select * from cart";
      ArrayList<Cart> cartItems = new ArrayList<Cart>(jdbcTemplate.query(sql, new CartMapper()));
      return cartItems;
    }, (t) -> {
      return null;
    });

    return result;
  }

  @RequestMapping(value = "/cart/undoCart", method = {RequestMethod.PUT, RequestMethod.POST})
  public String undoCart()
  {
    String result = GenericHystrixCommand.execute("CartCommandGroup", "UndoCartCommand", () -> {
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
    }, (t) -> {
      return "{\"status\":\"failure\",\"message\":\"/cart/undoCart has failed.\"}";
    });

    return result;
  }  

  @RequestMapping(value = "/cart/placeOrder", method = {RequestMethod.PUT, RequestMethod.POST})
  public String placeOrder()
  {
    String result = GenericHystrixCommand.execute("CartCommandGroup", "PlaceOrderCommand", () -> {
      System.out.println("Started placeOrder command");
      ArrayList<Cart> cartItems = getCartItems();
      double final_price = 0;

      if (cartItems.size() == 0)
        return "{\"status\":\"failure\",\"message\":\"No items in cart\"}";
      System.out.println("Checked if items are in cart");

      for (Cart cart: cartItems)
        final_price = final_price + cart.getTotalPrice();
      System.out.println("Got total price of items");

      /* OLD HTTP CONNECTION STUFF
      HttpClient client = new DefaultHttpClient();
      HttpGet get = new HttpGet(url);
      HttpResponse response = client.execute(get);
      String res = convertToString(response);
      */
      String url = "http://" + pg_URL + "/pg/makePayment?total_price=" + final_price;
      String response = restTemplate.getForObject(url, String.class);
      System.out.println("called out to payment gateway");

      JsonParser parser = new JsonParser();
      JsonObject o = parser.parse(response).getAsJsonObject();
      if(o.get("status").toString().contains("failure"))
        return response;
      System.out.println("parsed payment gateway response, it checks out");

      url = "http://" + invoice_URL + "/invoice/generateInvoice";
      response = restTemplate.getForObject(url, String.class);
      System.out.println("called out to invoice, response: " + response);

      String res2 = emptyCart();
      System.out.println("emptyed cart, res2: " + res2);

      o = parser.parse(res2).getAsJsonObject();
      if(o.get("status").toString().contains("failure"))
        return res2;
      System.out.println("parsed empty cart response, returning last response");
      
      System.out.println("first try return: " + response);
      return response;
    }, (t) -> {
      System.out.println("inside placeOrder fallback");
      return "{\"status\":\"failure\",\"message\":\"/cart/placeOrder has failed.\"}";
    });

    System.out.println("final result return: " + result);
    return result;
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
