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

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;


@RestController
public class Controller {

  @Autowired
  JdbcTemplate jdbcTemplate;

  @Autowired
  @Lazy
  RestTemplate restTemplate;

  @Value("${message:Hello default}")
    private String message;

  @Value("${pg_p_ip}")
    private String pg_p_ip;

  @Value("${pg_b_ip}")
    private String pg_b_ip;

  @Value("${inventory_p_ip}")
    private String inventory_p_ip;

  @Value("${inventory_b_ip}")
    private String inventory_b_ip;

  @Value("${invoice_p_ip}")
    private String invoice_p_ip;

  @Value("${invoice_b_ip}")
    private String invoice_b_ip;

  @RequestMapping("/cart")
  public String index() {
      return "Greetings from Cart App!";
  }

  @RequestMapping("/cart/checkConfig")
  public String checkConfig() {
      return message;
  }  


  /*
    --------------------------------
    /cart/test
    This command tests connectivity to both inventory and cart.
    Inventory and Cart will then execute their own test functions
    to verify that everything works down the service tree.
    --------------------------------
  */
  @RequestMapping("/cart/test")
  @HystrixCommand(groupKey="CartServiceGroup", commandKey = "testCommand", fallbackMethod = "testFallback")
  public String test()
  {
    return testFunc(invoice_p_ip);
  }

  public String testFallback()
  {
    return testFunc(invoice_b_ip);
  }

  private String testFunc(String invoice_ip)
  {
    String response;
    String invoice = "http://" + invoice_ip + "/invoice/test";

    response = restTemplate.getForObject(invoice, String.class);
    System.out.println("Inventory response: " + response);

    return response;
  }


  /*  
    --------------------------------
    /cart/addToCart
    This command takes in an itemID, an integer, and a total price
    and adds that data to the cart of the specified user.
    --------------------------------
  */
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


  /*  
    --------------------------------
    /cart/emptyCart
    This command deletes the cart for all users.
    --------------------------------
  */
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


  /*  
    --------------------------------
    /cart/emptyCart
    This command deletes the cart for the specified user.
    --------------------------------
  */
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


  /*  
    --------------------------------
    /cart/getCartItems
    This command returns all the items in the cart, regardless of user.
    --------------------------------
  */
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


  /*  
    --------------------------------
    /cart/getCartItems
    This command returns the items in the cart for a specified user.
    --------------------------------
  */
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


  /*  
    --------------------------------
    /cart/undoCart
    This command adds returns all the items in a user's cart to the inventory.
    --------------------------------
  */
  @RequestMapping(value = "/cart/{userID}/undoCart", method = {RequestMethod.PUT, RequestMethod.POST})
  @HystrixCommand(groupKey="CartServiceGroup", commandKey = "undoCartCommand", fallbackMethod = "undoCartFallback")
  public String undoCart(@PathVariable String userID)
  {
    return undoCartFunc(userID, inventory_p_ip);
  }

  public String undoCartFallback(String userID)
  {
    return undoCartFunc(userID, inventory_b_ip);
  }

  private String undoCartFunc(String userID, String inventory_ip)
  {
    ArrayList<Cart> cartItems = getCartItems(userID);

    if (cartItems.size() == 0)
      return "{\"status\":\"failure\",\"message\":\"No items in cart\"}";

    for (Cart cart: cartItems)
    {  
      int quantity = cart.getQuantity();
      int ItemID = cart.getItemID();

      String url = "http://" + inventory_ip + "/inventory/addBackToInventory?ItemID="+ItemID+"&quantity="+quantity;
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


  /*  
    --------------------------------
    /cart/placeOrder
    This command places an order by retrieving the cart items for a specific
    user, then making a payment for the total price of the items, then 
    generating an invoice for the payment.
    --------------------------------
  */
  @RequestMapping(value = "/cart/{userID}/placeOrder", method = {RequestMethod.PUT, RequestMethod.POST})
  @HystrixCommand(groupKey="CartServiceGroup", commandKey = "placeOrderCommand", fallbackMethod = "placeOrderFallback")
  public String placeOrder(@PathVariable String userID)
  {
    return placeOrderFunc(userID, pg_p_ip, invoice_p_ip);
  }

  public String placeOrderFallback(String userID)
  {
    return placeOrderFunc(userID, pg_b_ip, invoice_b_ip);
  }

  private String placeOrderFunc(String userID, String pg_ip, String invoice_ip)
  {
    ArrayList<Cart> cartItems = getCartItems();
    double final_price = 0;

    if (cartItems.size() == 0)
      return "{\"status\":\"failure\",\"message\":\"No items in cart\"}";  
            
    for (Cart cart: cartItems)
      final_price = final_price + cart.getTotalPrice();

    String url = "http://" + pg_ip + "/pg/makePayment?total_price=" + final_price; 
    String response = restTemplate.getForObject(url, String.class);

    
    JsonParser parser = new JsonParser();
    JsonObject o = parser.parse(response).getAsJsonObject();
    if((o.get("status").toString()).contains("failure"))
      return response;

    url = "http://" + invoice_ip + "/invoice/" + userID + "/generateInvoice";
    response = restTemplate.getForObject(url, String.class);

    String res2 = emptyCart(userID);
    o = parser.parse(res2).getAsJsonObject();
    if(o.get("status").toString().contains("failure"))
      return res2;

    return response;
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
