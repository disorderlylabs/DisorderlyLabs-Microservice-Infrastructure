package com.disorderlylabs.paymentGateway.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMethod;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;

import java.util.Random;

@RestController
public class Controller {

  @RequestMapping("/pg")
  public String index() {
    String result = GenericHystrixCommand.execute("PaymentGatewayCommandGroup", "IndexCommand", () -> {
      // maps to initial run()
      throw new IllegalArgumentException();
      // return "This is the GenericHystrixCommand PaymentGateway App!";
    }, (t) -> {
      // maps to getFallback()
      return "This is the GenericHystrixCommand fallback for PaymentGateway.";
    });

    return result;
  }

  @RequestMapping("/pg/makePayment")
  public String makePayment(@RequestParam(value="total_price", required=true) double total_price) 
  {
    String result = GenericHystrixCommand.execute("PaymentGatewayCommandGroup", "IndexCommand", () -> {
      Random ran = new Random();
      int x = ran.nextInt(5);

      try {
        // Thread.sleep(2000); //Intentional sleep to payment processing
        Thread.sleep(100);
      } catch (Exception e) {
        return "{\"status\":\"Payment failure because of " + e.toString() + " \"}";
      }

      // if (x == 0)
      //   return "{\"status\":\"Payment failure\"}";
      //Commenting the above two lines to make sure payment returns consistent result

      return "{\"status\":\"success\", \"message\":\"A payment of $" + total_price + " was successful\"}";
    }, (t) -> {
      // maps to getFallback()
      return "{\"status\":\"success\", \"message\":\"A payment of $" + total_price + " was successful\"}";
    });

    return result;
  }
}
