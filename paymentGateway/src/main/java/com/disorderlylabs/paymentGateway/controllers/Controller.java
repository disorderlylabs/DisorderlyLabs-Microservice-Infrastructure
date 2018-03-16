package com.disorderlylabs.paymentGateway.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Random;

@RestController
public class Controller {

  @RequestMapping("/pg")
  public String index() {
      return "Greetings from Payment Gateway!";
  }

  @RequestMapping("/pg/makePayment")
  public String makePayment(@RequestParam(value="total_price", required=true) double total_price) 
  {
    try
    {
      Random ran = new Random();
      int x = ran.nextInt(5);

      // Thread.sleep(2000); //Intentional sleep to payment processing
      Thread.sleep(100);

      // if (x == 0)
      //   return "{\"status\":\"Payment failure\"}";
      //Commenting the above two lines to make sure payment returns consistent result

      return "{\"status\":\"success\", \"message\":\"A payment of $" + total_price + " was successful\"}";
    }

    catch(Exception e)
    {
      return "{\"status\":\"Payment failure because of " + e.toString() + " \"}";
    } 
  }
  
}  