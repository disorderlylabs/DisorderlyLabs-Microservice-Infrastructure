package com.disorderlylabs.cart.repositories;

public class Cart
{

  private int itemID;
  private int quantity;
  private double total_price;

  public Cart()
  {

  }
  
  public Cart(int itemID, int quantity, double total_price)
  {
    this.itemID = itemID;
    this.quantity = quantity;
    this.total_price = total_price;
  }

  public void setItemID(int itemID)
  {
    this.itemID = itemID;
  }

  public void setQuantity(int quantity)
  {
    this.quantity = quantity;
  }

  public void setTotalPrice(Double total_price)
  {
    this.total_price = total_price;
  }

  public int getItemID()
  {
     return this.itemID;
  }

  public int getQuantity()
  {
    return this.quantity;
  }

  public double getTotalPrice()
  {
    return this.total_price;
  }

  public String toString()
  {
    return "ItemID: "+ itemID + "\nQuantity: " + quantity + "\nTotal Price: " + total_price + "\n"; 
  }         
}