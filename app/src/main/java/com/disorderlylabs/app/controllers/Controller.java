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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.beans.factory.annotation.Value;
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
  // private static final String inventory_URL = System.getenv("inventory_ip");
  // private static final String cart_URL = System.getenv("cart_ip");
  // private static final String invoice_URL = System.getenv("invoice_ip");

  @Autowired
  @Lazy
  RestTemplate restTemplate;

  @Value("${message:Hello default}")
    private String message;

  @Value("${cart_ip}")
    private String cart_URL;

  @Value("${inventory_ip}")
    private String inventory_URL;

  @Value("${invoice_ip}")
    private String invoice_URL;    

  @RequestMapping("/app")
  public String index() {
      return "Greetings from App Microservice!";
  }

  @RequestMapping("/app/checkConfig")
  public String checkConfig() {
      return message;
  }  

  @RequestMapping("/app/test")
  public String test() {
      String response;

      String inventory = "http://" + System.getenv("inventory_ip") + "/inventory";

      String cart = "http://" + System.getenv("cart_ip") + "/cart/test";

      response = restTemplate.getForObject(inventory, String.class);
      System.out.println("Inventory response: " + response);

      response = restTemplate.getForObject(cart, String.class);
      System.out.println("cart response: " + response);

      return response;
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
  @RequestMapping(value = "/app/takeFromInventory", method = {RequestMethod.PUT, RequestMethod.POST})
  public String takeFromInventory(@RequestParam(value="name", required=true) String name, @RequestParam(value="quantity", required=true) int quantity) 
  {
    try
    {

      //[NEW REQUEST CODE]
      String url = "http://" + inventory_URL + "/inventory/takeFromInventory";
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
      map.add("name", name);
      map.add("quantity", quantity + "");

      HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);

      ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

      return response.getBody();
    }
    catch(Exception e)
    {
      return e.toString();
    }
  }

  @RequestMapping(value = "/app/{userID}/addToCart", method = {RequestMethod.PUT, RequestMethod.POST})
  public String addToCart(@PathVariable String userID, @RequestParam(value="name", required=true) String name, @RequestParam(value="quantity", required=true) int quantity) 
  {
    try
    {
      String url = "http://" + inventory_URL + "/inventory/takeFromInventory";

      //[NEW REQUEST CODE]
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
      map.add("name", name);
      map.add("quantity", quantity + "");

      HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);
      ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

      JsonParser parser = new JsonParser();
      String res = response.getBody();

      JsonObject o = parser.parse(res).getAsJsonObject();
      if(o.get("status").toString().contains("failure"))
        return res;
      int ItemID = Integer.parseInt(o.get("ItemID").toString());
      double total_price = Double.parseDouble(o.get("total_price").toString());

      url = "http://" + cart_URL + "/cart/"+ userID +"/addToCart";

      //[NEW REQUEST CODE]
      headers.clear(); //clearing the headers
      map.clear();  //clearing the map for new parameters

      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

      map.add("ItemID", ItemID + "");
      map.add("quantity", quantity + "");
      map.add("total_price", total_price + "");

      request = new HttpEntity<MultiValueMap<String, String>>(map, headers);
      response = restTemplate.postForEntity( url, request , String.class );

      return response.getBody();
    }
    catch(Exception e)
    {
      return "{\"status\":\"failure at app: Could not add to cart because of " + e.toString() + "\"}";
    }    
  }

  @RequestMapping(value = "/app/{userID}/instantPlaceOrder", method = {RequestMethod.PUT, RequestMethod.POST})
  public String instantPlaceOrder(@PathVariable String userID, @RequestParam(value="name") String name, @RequestParam(value="quantity") int quantity) 
  {
    try
    {
      if (name != null)
      {
        String res = addToCart(userID+"", name, quantity);
        JsonParser parser = new JsonParser();
        JsonObject o = parser.parse(res).getAsJsonObject();
        if(o.get("status").toString().contains("failure"))
        return res;        
      }  

      return placeOrder(userID+"");   
    }
    catch(Exception e)
    {
      return "{\"status\":\"failure at app: Could not place instant order because of " + e.toString() + "\"}";
    }
  }

  @RequestMapping(value = "/app/{userID}/placeOrder", method = {RequestMethod.PUT, RequestMethod.POST})
  public String placeOrder(@PathVariable String userID) 
  {
    try
    {
      String url = "http://" + cart_URL + "/cart/" + userID + "/placeOrder";

      //[NEW REQUEST CODE]
      HttpEntity<String> request = new HttpEntity<>("");
      ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
      return response.getBody();
    }
    catch(Exception e)
    {
      return "{\"status\":\"failure at app: Could not place order because of " + e.toString() + "\"}";
    }
  }     

  @RequestMapping(value = "/app/{userID}/undoCart", method = {RequestMethod.PUT, RequestMethod.POST})
  public String undoCart(@PathVariable String userID) 
  {
    try
    {
      String url = "http://" + cart_URL + "/cart/" + userID + "/undoCart";
      HttpEntity<String> request = new HttpEntity<>("");
      ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
      return response.getBody();
    }
    catch(Exception e)
    {
      return "{\"status\":\"failure at app: Could not undo cart because of " + e.toString() + "\"}";
    }
  }

  @RequestMapping(value = "/app/{userID}/getInvoice")
  public String getInvoice(@PathVariable String userID) 
  {
    try
    {
      String url = "http://" + invoice_URL + "/invoice/" + userID + "/getInvoice";
      //[NEW REQUEST CODE]
      String response = restTemplate.getForObject(url, String.class);

      if (response.contains("not generated"))
        return response.replace("\n","");
      else
        return response;
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