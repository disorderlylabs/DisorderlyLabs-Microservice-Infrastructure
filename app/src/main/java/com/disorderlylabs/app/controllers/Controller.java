package com.disorderlylabs.app.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.context.annotation.Lazy;

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
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@RestController
public class Controller {

  @Autowired
  JdbcTemplate jdbcTemplate;
  private static final String inventory_primary_ip = System.getenv("inventory_ip");
  private static final String cart_primary_ip = System.getenv("cart_ip");
  private static final String invoice_primary_ip = System.getenv("invoice_ip");
  private static final String inventory_backup_ip = System.getenv("inventory_b_ip");
  private static final String cart_backup_ip = System.getenv("cart_b_ip");
  private static final String invoice_backup_ip = System.getenv("invoice_b_ip");

  @Autowired
  @Lazy
  RestTemplate restTemplate;

  @RequestMapping("/app")
  public String index() {
    String result = GenericHystrixCommand.execute("AppCommandGroup", "IndexCommand", () -> {
      // maps to initial run()
      throw new IllegalArgumentException();
      // return "This is the GenericHystrixCommand for App.  Hello!";
    }, (t) -> {
      // maps to getFallback()
      return "This is the GenericHystrixCommand fallback for App.";
    });

    return result;
  }

  @RequestMapping("/app/test")
  public String test() {
    String result = GenericHystrixCommand.execute("AppCommandGroup", "TestCommand", () -> {
      return testFunc(inventory_primary_ip, cart_primary_ip);
    }, (t) -> {
      // for now, just call test again without the backups
      return testFunc(inventory_backup_ip, cart_backup_ip);
    });

    return result;
  }

  private String testFunc(String inventory_ip, String cart_ip) {
    String response;
    String inventory_URL = "http://" + inventory_ip + "/inventory";
    String cart_URL = "http://" + cart_ip + "/cart/test";

    response = restTemplate.getForObject(inventory_URL, String.class);
    System.out.println("Inventory response: " + response);

    response = restTemplate.getForObject(cart_URL, String.class);
    System.out.println("cart response: " + response);

    return response;
  }

  @RequestMapping("/app/checkEnv")
  public String checkEnv() 
  {
    String result = GenericHystrixCommand.execute("AppCommandGroup", "CeckEnvCommand", () -> {
      String ans = "";
      for (String key : System.getenv().keySet())
        ans = ans + key + " : " + System.getenv(key) + "\n";
      return ans;
    }, (t) -> {
      return "{\"status\":\"failure at app/checkEnv: Could return environment status." + "\"}";
    });

    return result;
  }

  //******--------For this particular request 'App' is acting like a forwarding node to 'Inventory'--------******.
  @RequestMapping(value = "/app/takeFromInventory", method = {RequestMethod.PUT, RequestMethod.POST})
  public String takeFromInventory(@RequestParam(value="name", required=true) String name, @RequestParam(value="quantity", required=true) int quantity) 
  {
    String result = GenericHystrixCommand.execute("AppCommandGroup", "TakeFromInventoryCommand", () -> {
      return takeFromInventoryFunc(name, quantity, inventory_primary_ip);
    }, (t) -> {
      return takeFromInventoryFunc(name, quantity, inventory_backup_ip);
      // return "{\"status\":\"failure at app/takeFromInventory." + "\"}";
    });

    return result;
  }

  private String takeFromInventoryFunc(String name, int quantity, String inventory_ip) {
    String inventory_URL = "http://" + inventory_ip + "/inventory/takeFromInventory";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
    map.add("name", name);
    map.add("quantity", quantity + "");

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);
    ResponseEntity<String> response = restTemplate.postForEntity(inventory_URL, request, String.class);

    return response.getBody();
  }

  @RequestMapping(value = "/app/addToCart", method = {RequestMethod.PUT, RequestMethod.POST})
  public String addToCart(@RequestParam(value="name", required=true) String name, @RequestParam(value="quantity", required=true) int quantity) 
  {
    String result = GenericHystrixCommand.execute("AppCommandGroup", "AddToCartCommand", () -> {
      return addToCartFunc(name, quantity, inventory_primary_ip, cart_primary_ip);
    }, (t) -> {
      return addToCartFunc(name, quantity, inventory_backup_ip, cart_backup_ip);
      // return "{\"status\":\"failure at app/addToCart." + "\"}";
    });

    return result;
  }

  private String addToCartFunc(String name, int quantity, String inventory_ip, String cart_ip) {
    String inventory_URL = "http://" + inventory_ip + "/inventory/takeFromInventory";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
    map.add("name", name);
    map.add("quantity", quantity + "");

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);
    ResponseEntity<String> response = restTemplate.postForEntity(inventory_URL, request, String.class);

    JsonParser parser = new JsonParser();
    String res = response.getBody();

    JsonObject o = parser.parse(res).getAsJsonObject();
    if(o.get("status").toString().contains("failure"))
      return res;
    int ItemID = Integer.parseInt(o.get("ItemID").toString());
    double total_price = Double.parseDouble(o.get("total_price").toString());

    String cart_URL = "http://" + cart_ip + "/cart/addToCart";

    headers.clear(); //clearing the headers
    map.clear();  //clearing the map for new parameters

    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    map.add("ItemID", ItemID + "");
    map.add("quantity", quantity + "");
    map.add("total_price", total_price + "");

    request = new HttpEntity<MultiValueMap<String, String>>(map, headers);
    response = restTemplate.postForEntity( cart_URL, request , String.class );

    return response.getBody();
  }

  @RequestMapping(value = "/app/instantPlaceOrder", method = {RequestMethod.PUT, RequestMethod.POST})
  public String instantPlaceOrder(@RequestParam(value="name") String name, @RequestParam(value="quantity") int quantity) 
  {
    String result = GenericHystrixCommand.execute("AppCommandGroup", "InstantPlaceOrderCommand", () -> {
      if (name != null) {
        String res = addToCart(name, quantity);
        JsonParser parser = new JsonParser();
        JsonObject o = parser.parse(res).getAsJsonObject();
        if(o.get("status").toString().contains("failure"))
          return res;        
      }  

      return placeOrder(); 
    }, (t) -> {
      return "{\"status\":\"failure at app/instantPlaceOrder." + "\"}";
    });

    return result;
  }

  @RequestMapping(value = "/app/placeOrder", method = {RequestMethod.PUT, RequestMethod.POST})
  public String placeOrder() 
  {
    String result = GenericHystrixCommand.execute("AppCommandGroup", "PlaceOrderCommand", () -> {
      return placeOrderFunc(cart_primary_ip);
    }, (t) -> {
      return placeOrderFunc(cart_backup_ip);
      // return "{\"status\":\"failure at app/placeOrder." + "\"}";
    });

    return result;
  }

  private String placeOrderFunc(String cart_ip) {
    String cart_URL = "http://" + cart_ip + "/cart/placeOrder";
    HttpEntity<String> request = new HttpEntity<>("");
    ResponseEntity<String> response = restTemplate.postForEntity(cart_URL, request, String.class);
    return response.getBody();
  }

  @RequestMapping(value = "/app/undoCart", method = {RequestMethod.PUT, RequestMethod.POST})
  public String undoCart() 
  {
    String result = GenericHystrixCommand.execute("AppCommandGroup", "UndoCartCommand", () -> {
      return undoCartFunc(cart_primary_ip);
    }, (t) -> {
      return undoCartFunc(cart_backup_ip);
      // return "{\"status\":\"failure at app/undoCart." + "\"}";
    });

    return result;
  }

  private String undoCartFunc(String cart_ip) {
    String cart_URL = "http://" + cart_ip + "/cart/undoCart";
    HttpEntity<String> request = new HttpEntity<>("");
    ResponseEntity<String> response = restTemplate.postForEntity(cart_URL, request, String.class);
    return response.getBody();
  }

  @RequestMapping(value = "/app/getInvoice")
  public String getInvoice() 
  {
    String result = GenericHystrixCommand.execute("AppCommandGroup", "GetInvoiceCommand", () -> {
      return getInvoiceFunc(invoice_primary_ip);
    }, (t) -> {
      return getInvoiceFunc(invoice_backup_ip);
      // return "{\"status\":\"failure at app/getInvoice." + "\"}";
    });

    return result;
  }

  private String getInvoiceFunc(String invoice_ip) {
    String invoice_URL = "http://" + invoice_ip + "/invoice/getInvoice";
    String response = restTemplate.getForObject(invoice_URL, String.class);

    if (response.contains("not generated")) return response.replace("\n","");
    else return response;
  }
}
