package com.disorderlylabs.inventory.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMethod;

import org.springframework.jdbc.core.JdbcTemplate;
import com.disorderlylabs.inventory.mappers.CatalogMapper;
import com.disorderlylabs.inventory.repositories.Catalog;
import java.util.ArrayList;
import java.util.Arrays;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.util.EntityUtils; 
import org.apache.http.HttpResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@RestController
public class Controller {

  @Autowired
  JdbcTemplate jdbcTemplate;

  @RequestMapping("/")
  public String index() {
      return "Greetings from Inventory App!";
  }

  @RequestMapping(value = "/checkAvailibility", method = RequestMethod.GET)
  public String checkAvailibility(@RequestParam(value="name", required=true) String name) 
  {
    try
    {
      //Approach 2
      String sql = "select * from Catalog where name like '%" + name + "%'";
      Catalog c = (Catalog)jdbcTemplate.queryForObject(sql, new CatalogMapper());
      return "{\"quantity\": "+ c.getQuantity()+"} ";

    }
    catch(Exception e)
    {
      return e.toString();
    }
  }

  @RequestMapping(value = "/checkPrice", method = RequestMethod.GET)
  public String checkPrice(@RequestParam(value="name", required=true) String name) 
  {
    try
    {
      String sql = "select * from Catalog where name like '%" + name + "%'";
      Catalog c = (Catalog)jdbcTemplate.queryForObject(sql, new CatalogMapper());
      return "{\"price\": "+ c.getPrice()+"} ";
    }
    catch(Exception e)
    {
      return e.toString();
    }
  }

  @RequestMapping(value = "/getItemID", method = RequestMethod.GET)
  public String getItemID(@RequestParam(value="name", required=true) String name) 
  {
    try
    {
      String sql = "select * from Catalog where name like '%" + name + "%'";
      Catalog c = (Catalog)jdbcTemplate.queryForObject(sql, new CatalogMapper());
      return "{\"ItemID\": "+ c.getItemID()+"} ";
    }
    catch(Exception e)
    {
      return e.toString();
    }
  }    

  @RequestMapping(value = "/getName", method = RequestMethod.GET)
  public String getName(@RequestParam(value="ItemID", required=true) String ItemID) 
  {
    try
    {
      String sql = "select * from Catalog where ItemID=" + ItemID;
      Catalog c = (Catalog)jdbcTemplate.queryForObject(sql, new CatalogMapper());
      return "{\"name\": \""+ c.getName()+"\"} ";
    }
    catch(Exception e)
    {
      return e.toString();
    }
  }

  @RequestMapping(value = "/takeFromInventory", method = RequestMethod.PUT)
  public String takeFromInventory(@RequestParam(value="name", required=true) String name, @RequestParam(value="quantity", required=true) int quantity) 
  {
    try
    {
      JsonParser parser = new JsonParser();
      JsonObject o = parser.parse(checkAvailibility(name)).getAsJsonObject();
      int available = Integer.parseInt(o.get("quantity").toString());

      o = parser.parse(checkPrice(name)).getAsJsonObject();
      double price = Double.parseDouble(o.get("price").toString());

      o = parser.parse(getItemID(name)).getAsJsonObject();
      int ItemID = Integer.parseInt(o.get("ItemID").toString());      

      if (available>=quantity)
      {
        int remaining = available - quantity;
        String sql = "update Catalog set quantity = " + remaining + " where name like '%" + name + "%'";
        jdbcTemplate.execute(sql);
        return "{\"status\":\"success\",\"total_price\": "+ (price*quantity) +", \"ItemID\":"+ItemID+"} ";
      }
      else
      {
        return "{\"status\":\"error\",\"reason\":\"Not enough in inventory\"}";
      }
    }
    catch(Exception e)
    {
      return e.toString();
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
}