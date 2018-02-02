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
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@RestController
public class Controller {

  @Autowired
  JdbcTemplate jdbcTemplate;
  private static final String inventory_URL = "http://localhost:7002";

  @RequestMapping("/")
  public String index() {
      return "Greetings from App Microservice!";
  }

  @RequestMapping("/checkEnv")
  public String checkEnv() {
      return System.getenv("inventory_ip") + "";
  }

  //******--------For now 'App' is acting like a forwarding node to 'Inventory'--------******.

  @RequestMapping(value = "/takeFromInventory", method = RequestMethod.PUT)
  public String takeFromInventory(@RequestParam(value="name", required=true) String name, @RequestParam(value="quantity", required=true) int quantity) 
  {
    try
    {
      String url = "http://" + System.getenv("inventory_ip") + "/takeFromInventory";
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