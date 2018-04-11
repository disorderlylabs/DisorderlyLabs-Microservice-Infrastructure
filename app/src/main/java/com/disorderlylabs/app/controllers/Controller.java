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

  @Value("${cart_p_ip}")
    private String cart_p_ip;

  @Value("${cart_b_ip}")
    private String cart_b_ip;

  @Value("${inventory_p_ip}")
    private String inventory_p_ip;

  @Value("${inventory_b_ip}")
    private String inventory_b_ip;

  @Value("${invoice_p_ip}")
    private String invoice_p_ip;

  @Value("${invoice_b_ip}")
    private String invoice_b_ip;

  @RequestMapping("/app")
  public String index()
  {
      return "Greetings from App Microservice!";
  }

  @RequestMapping("/app/checkConfig")
  public String checkConfig()
  {
      return message;
  }


  /*  
    --------------------------------
    /app/test
    This command tests connectivity to both inventory and cart.
    Inventory and Cart will then execute their own test functions
    to verify that everything works down the service tree.
    --------------------------------
  */
  @RequestMapping("/app/test")
  @HystrixCommand(groupKey="AppServiceGroup", commandKey = "testCommand", fallbackMethod = "testFallback")
  public String test()
  {
    if (2 != 3) throw new IllegalArgumentException();
    return testFunc(inventory_p_ip,cart_p_ip);
  }

  public String testFallback()
  {
    return testFunc(inventory_b_ip,cart_b_ip);
  }

  private String testFunc(String inventory_ip, String cart_ip)
  {
    String response;
    String inventory_url = "http://" + inventory_ip + "/inventory";
    String cart_url = "http://" + cart_ip + "/cart/test";

    response = restTemplate.getForObject(inventory_url, String.class);
    response = restTemplate.getForObject(cart_url, String.class);

    return response;
  }


  /*  
    --------------------------------
    /app/checkEnv
    This command tests that the service can retrieve its environment
    variables.  With the integration of SpringCloud, this function
    appears to be irrelevant.  It has been left here in case we return
    to an older verison of the code and it is needed.
    --------------------------------
  */
  @RequestMapping("/app/checkEnv")
  @HystrixCommand(groupKey="AppServiceGroup", commandKey = "checkEnvCommand", fallbackMethod = "checkEnvFallback")
  public String checkEnv() 
  {
    String ans = "";
    for (String key : System.getenv().keySet())
      ans = ans + key + " : " + System.getenv(key) + "\n";
    return ans;
  }

  public String checkEnvFallback()
  {
    return "This is /app/checkEnv Hystrix fallbackMethod";
  }


  /*  
    --------------------------------
    /app/takeFromInventory
    This command acts as a forwarding node to Inventory.  It is mostly
    concerned with formatting the request properly.  It should remove
    the desired quantity of the specified item from the inventory.
    Note that there is no userID associated with Inventory requests.
    --------------------------------
  */
  @RequestMapping(value = "/app/takeFromInventory", method = {RequestMethod.PUT, RequestMethod.POST})
  @HystrixCommand(groupKey="AppServiceGroup", commandKey = "takeFromInventoryCommand", fallbackMethod = "takeFromInventoryFallback")
  public String takeFromInventory(@RequestParam(value="name", required=true) String name, @RequestParam(value="quantity", required=true) int quantity) 
  {
    String url = "http://" + inventory_p_ip + "/inventory/takeFromInventory";
    return takeFromInventoryFunc(name, quantity, url);
  }

  public String takeFromInventoryFallback(String name, int quantity)
  {
    String url = "http://" + inventory_b_ip + "/inventory/takeFromInventory";
    return takeFromInventoryFunc(name, quantity, url);
  }

  private String takeFromInventoryFunc(String name, int quantity, String inventory_url)
  {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
    map.add("name", name);
    map.add("quantity", quantity + "");

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);

    ResponseEntity<String> response = restTemplate.postForEntity(inventory_url, request, String.class);

    return response.getBody();
  }


  /*  
    --------------------------------
    /app/addToCart
    This command takes the requested number of items from Inventory
    and then adds them to the cart.  It requires a userID to specify
    which virtual cart to put the items into.
    --------------------------------
  */
  @RequestMapping(value = "/app/{userID}/addToCart", method = {RequestMethod.PUT, RequestMethod.POST})
  @HystrixCommand(groupKey="AppServiceGroup", commandKey = "addToCartCommand", fallbackMethod = "addToCartFallback")
  public String addToCart(@PathVariable String userID, @RequestParam(value="name", required=true) String name, @RequestParam(value="quantity", required=true) int quantity) 
  {
    return addToCartFunc(userID, name, quantity, inventory_p_ip, cart_p_ip);
  }

  public String addToCartFallback(String userID, String name, int quantity)
  {
    return addToCartFunc(userID, name, quantity, inventory_b_ip, cart_b_ip);
  }

  private String addToCartFunc(String userID, String name, int quantity, String inventory_ip, String cart_ip)
  {
    String inventory_url = "http://" + inventory_ip + "/inventory/takeFromInventory";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
    map.add("name", name);
    map.add("quantity", quantity + "");

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);
    ResponseEntity<String> response = restTemplate.postForEntity(inventory_url, request, String.class);

    JsonParser parser = new JsonParser();
    String res = response.getBody();

    JsonObject o = parser.parse(res).getAsJsonObject();
    if(o.get("status").toString().contains("failure"))
      return res;
    int ItemID = Integer.parseInt(o.get("ItemID").toString());
    double total_price = Double.parseDouble(o.get("total_price").toString());

    String cart_url = "http://" + cart_ip + "/cart/"+ userID +"/addToCart";

    headers.clear(); //clearing the headers
    map.clear();  //clearing the map for new parameters

    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    map.add("ItemID", ItemID + "");
    map.add("quantity", quantity + "");
    map.add("total_price", total_price + "");

    request = new HttpEntity<MultiValueMap<String, String>>(map, headers);
    response = restTemplate.postForEntity( cart_url, request , String.class );

    return response.getBody();
  }


  /*  
    --------------------------------
    /app/instantPlaceOrder
    This command does the entire workflow in one command.  It will
    contact inventory, then add to cart, then place the order.  This
    sequence interacts with every service at least once.  See the tree
    on the github README for an illustration.  Hystrix is not implemented
    here because instantPlaceOrder only calls App functions - all network
    calls take place in other App functions that have been instrumented
    with Hystrix.
    --------------------------------
  */
  @RequestMapping(value = "/app/{userID}/instantPlaceOrder", method = {RequestMethod.PUT, RequestMethod.POST})
  @HystrixCommand(groupKey="AppServiceGroup", commandKey = "instantPlaceOrderCommand", fallbackMethod = "instantPlaceOrderFallback")
  public String instantPlaceOrder(@PathVariable String userID, @RequestParam(value="name") String name, @RequestParam(value="quantity") int quantity) 
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

  public String instantPlaceOrderFallback(String userID, String name, int quantity)
  {
    if (name != null)
      {
        String res = addToCartFallback(userID+"", name, quantity);
        JsonParser parser = new JsonParser();
        JsonObject o = parser.parse(res).getAsJsonObject();
        if(o.get("status").toString().contains("failure"))
        return res;        
      }  

      return placeOrderFallback(userID+"");
  }


  /*  
    --------------------------------
    /app/placeOrder
    This command implements the later half of the instantPlaceOrder workflow.
    --------------------------------
  */
  @RequestMapping(value = "/app/{userID}/placeOrder", method = {RequestMethod.PUT, RequestMethod.POST})
  @HystrixCommand(groupKey="AppServiceGroup", commandKey = "placeOrderCommand", fallbackMethod = "placeOrderFallback")
  public String placeOrder(@PathVariable String userID) 
  {
    return placeOrderFunc(userID, cart_p_ip);
  }

  public String placeOrderFallback(String userID)
  {
    return placeOrderFunc(userID, cart_b_ip);
  }

  private String placeOrderFunc(String userID, String cart_ip)
  {
    String cart_url = "http://" + cart_ip + "/cart/" + userID + "/placeOrder";
    HttpEntity<String> request = new HttpEntity<>("");
    ResponseEntity<String> response = restTemplate.postForEntity(cart_url, request, String.class);
    return response.getBody();
  }


  /*  
    --------------------------------
    /app/undoCart
    This command empties the cart for the specified userID.
    --------------------------------
  */
  @RequestMapping(value = "/app/{userID}/undoCart", method = {RequestMethod.PUT, RequestMethod.POST})
  @HystrixCommand(groupKey="AppServiceGroup", commandKey = "undoCartCommand", fallbackMethod = "undoCartFallback")
  public String undoCart(@PathVariable String userID) 
  {
    return undoCartFunc(userID,cart_p_ip);
  }

  public String undoCartFallback(String userID)
  {
    return undoCartFunc(userID, cart_b_ip);
  }

  private String undoCartFunc(String userID, String cart_ip)
  {
    String cart_url = "http://" + cart_ip + "/cart/" + userID + "/undoCart";
    HttpEntity<String> request = new HttpEntity<>("");
    ResponseEntity<String> response = restTemplate.postForEntity(cart_url, request, String.class);
    return response.getBody();
  }


  /*  
    --------------------------------
    /app/test
    This command generates the Invoice (receipt) for the current user's cart.
    --------------------------------
  */
  @RequestMapping(value = "/app/{userID}/getInvoice")
  @HystrixCommand(groupKey="AppServiceGroup", commandKey = "getInvoiceCommand", fallbackMethod = "getInvoiceFallback")
  public String getInvoice(@PathVariable String userID) 
  {
    return getInvoiceFunc(userID, invoice_p_ip);
  }

  public String getInvoiceFallback(String userID)
  {
    return getInvoiceFunc(userID, invoice_b_ip);
  }

  private String getInvoiceFunc(String userID, String invoice_ip)
  {
    String invoice_url = "http://" + invoice_ip + "/invoice/" + userID + "/getInvoice";
    String response = restTemplate.getForObject(invoice_url, String.class);

    if (response.contains("not generated"))
      return response.replace("\n","");
    else
      return response;
  }


  /*  
    --------------------------------
    convertToString
    This function seems to be another obsolete function to convert
    a HTTP Response to a String object.  It has been left here if reversions
    to legacy code require it.
    --------------------------------
  */
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