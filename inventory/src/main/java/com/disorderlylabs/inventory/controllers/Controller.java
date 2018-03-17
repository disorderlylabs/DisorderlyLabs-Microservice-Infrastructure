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

  @RequestMapping("/inventory")
  public String index() {
    String result = GenericHystrixCommand.execute("InventoryCommandGroup", "IndexCommand", () -> {
      // maps to initial run()
      throw new IllegalArgumentException();
      // return "This is the GenericHystrixCommand Cart App!";
    }, (t) -> {
      // maps to getFallback()
      return "This is the GenericHystrixCommand fallback for the Inventory.";
    });

    return result;
  }

  @RequestMapping(value = "/inventory/checkAvailibility", method = RequestMethod.GET)
  public String checkAvailibility(@RequestParam(value="name", required=true) String name) 
  {
    String result = GenericHystrixCommand.execute("InventoryCommandGroup", "CheckAvailibilityCommand", () -> {
      String sql = "select * from catalog where name like '%" + name + "%'";
      Catalog c = (Catalog)jdbcTemplate.queryForObject(sql, new CatalogMapper());
      return "{\"quantity\": "+ c.getQuantity()+"} ";
    }, (t) -> {
      return "{\"status\":\"failure\",\"message\":\"/inventory/checkAvailibility has failed.\"}";
    });

    return result;
  }

  @RequestMapping(value = "/inventory/checkAvailibilityWithID", method = RequestMethod.GET)
  public String checkAvailibilityWithID(@RequestParam(value="ItemID", required=true) int ItemID) 
  {
    String result = GenericHystrixCommand.execute("InventoryCommandGroup", "CheckAvailibilityWithIDCommand", () -> {
      String sql = "select * from catalog where ItemID=" + ItemID;
      Catalog c = (Catalog)jdbcTemplate.queryForObject(sql, new CatalogMapper());
      return "{\"quantity\": "+ c.getQuantity()+"} ";
    }, (t) -> {
      return "{\"status\":\"failure\",\"message\":\"/inventory/checkAvailibilityWithID has failed.\"}";
    });

    return result;
  }  

  @RequestMapping(value = "/inventory/checkPrice", method = RequestMethod.GET)
  public String checkPrice(@RequestParam(value="name", required=true) String name) 
  {
    String result = GenericHystrixCommand.execute("InventoryCommandGroup", "CheckPriceCommand", () -> {
      String sql = "select * from catalog where name like '%" + name + "%'";
      Catalog c = (Catalog)jdbcTemplate.queryForObject(sql, new CatalogMapper());
      return "{\"price\": "+ c.getPrice()+"} ";
    }, (t) -> {
      return "{\"status\":\"failure\",\"message\":\"/inventory/checkPrice has failed.\"}";
    });

    return result;
  }

  @RequestMapping(value = "/inventory/getItemID", method = RequestMethod.GET)
  public String getItemID(@RequestParam(value="name", required=true) String name) 
  {
    String result = GenericHystrixCommand.execute("InventoryCommandGroup", "GetItemIDCommand", () -> {
      String sql = "select * from catalog where name like '%" + name + "%'";
      Catalog c = (Catalog)jdbcTemplate.queryForObject(sql, new CatalogMapper());
      return "{\"itemid\": "+ c.getItemID()+"} ";
    }, (t) -> {
      return "{\"status\":\"failure\",\"message\":\"/inventory/getItemID has failed.\"}";
    });

    return result;
  }    

  @RequestMapping(value = "/inventory/getName", method = RequestMethod.GET)
  public String getName(@RequestParam(value="ItemID", required=true) String ItemID) 
  {
    String result = GenericHystrixCommand.execute("InventoryCommandGroup", "GetNameCommand", () -> {
      String sql = "select * from catalog where ItemID=" + ItemID;
      Catalog c = (Catalog)jdbcTemplate.queryForObject(sql, new CatalogMapper());
      return "{\"name\": \""+ c.getName()+"\"} ";
    }, (t) -> {
      return "{\"status\":\"failure\",\"message\":\"/inventory/getName has failed.\"}";
    });

    return result;
  }  

  @RequestMapping(value = "/inventory/takeFromInventory", method = RequestMethod.POST)
  public String takeFromInventory(@RequestParam(value="name", required=true) String name, @RequestParam(value="quantity", required=true) int quantity) 
  {
    String result = GenericHystrixCommand.execute("InventoryCommandGroup", "TakeFromInventoryCommand", () -> {
      JsonParser parser = new JsonParser();
      JsonObject o = parser.parse(checkAvailibility(name)).getAsJsonObject();
      int available = Integer.parseInt(o.get("quantity").toString());

      o = parser.parse(checkPrice(name)).getAsJsonObject();
      double price = Double.parseDouble(o.get("price").toString());

      o = parser.parse(getItemID(name)).getAsJsonObject();
      int ItemID = Integer.parseInt(o.get("itemid").toString());      

      if (available>=quantity) {
        int remaining = available - quantity;
        String sql = "update catalog set quantity = " + remaining + " where name like '%" + name + "%'";
        jdbcTemplate.execute(sql);
        return "{\"status\":\"success\",\"total_price\": "+ (price*quantity) +", \"ItemID\":"+ItemID+"} ";
      } else return "{\"status\":\"failure\",\"message\":\"Not enough in inventory\"}";
    }, (t) -> {
      return "{\"status\":\"failure\",\"message\":\"/inventory/takeFromInventory has failed.\"}";
    });

    return result;
  }  

  @RequestMapping(value = "/inventory/addBackToInventory", method = {RequestMethod.PUT, RequestMethod.POST})
  public String addBackToInventory(@RequestParam(value="ItemID", required=true) int ItemID, @RequestParam(value="quantity", required=true) int quantity) 
  {
    String result = GenericHystrixCommand.execute("InventoryCommandGroup", "AddBackToInventoryCommand", () -> {
      JsonParser parser = new JsonParser();
      JsonObject o = parser.parse(checkAvailibilityWithID(ItemID)).getAsJsonObject();
      int available = Integer.parseInt(o.get("quantity").toString());

      int new_quantity = available + quantity;
      String sql = "update catalog set quantity = " + new_quantity + " where ItemID=" + ItemID;
      jdbcTemplate.execute(sql);
      return "{\"status\":\"success\"}";
    }, (t) -> {
      return "{\"status\":\"failure\",\"message\":\"/inventory/addBackToInventory has failed.\"}";
    });

    return result;
  }

  @RequestMapping(value = "/inventory/refreshCatalog", method = RequestMethod.PUT)
  public String refreshCatalog() 
  {
    String result = GenericHystrixCommand.execute("InventoryCommandGroup", "RefreshCatalogCommand", () -> {
      String sql = "DELETE FROM catalog";
      jdbcTemplate.execute(sql);
      
      sql = "insert into catalog values (001, \'HP - Philosophers stone\', 10, 5.5 )";
      jdbcTemplate.execute(sql);
      sql = "insert into catalog values (002, \'HP - Chamber of Secrets\', 10, 6.5 )";
      jdbcTemplate.execute(sql);
      sql = "insert into catalog values (003, \'HP - Prisoner of Askaban\', 10, 7.5 )";
      jdbcTemplate.execute(sql);
      sql = "insert into catalog values (004, \'HP - Goblet of Fire\', 10, 8.5 )";
      jdbcTemplate.execute(sql);
      sql = "insert into catalog values (005, \'HP - Order of Pheonix\', 10, 9.5 )";
      jdbcTemplate.execute(sql);
      sql = "insert into catalog values (006, \'HP - Half Blood Prince\', 10, 10.5 )";
      jdbcTemplate.execute(sql);
      sql = "insert into catalog values (007, \'HP - Deathly Hallows\', 10, 11.5 )";
      jdbcTemplate.execute(sql);
      
      return "{\"status\":\"success\"}";
    }, (t) -> {
      return "{\"status\":\"failure\",\"message\":\"/inventory/refreshCatalog has failed.\"}";
    });

    return result;
  }
}
