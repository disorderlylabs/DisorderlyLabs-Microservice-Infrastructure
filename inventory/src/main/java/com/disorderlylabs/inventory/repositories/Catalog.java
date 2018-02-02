package com.disorderlylabs.inventory.repositories;

public class Catalog
{

  private int itemID;
  private String name;
  private int quantity;
  private double price;

  public Catalog()
  {

  }
  
  public Catalog(int itemID, String name, int quantity, double price)
  {
    this.itemID = itemID;
    this.name = name;
    this.quantity = quantity;
    this.price = price;
  }

  public void setItemID(int itemID)
  {
    this.itemID = itemID;
  }

  public void setName(String name)
  {
    this.name = name;
  }

  public void setQuantity(int quantity)
  {
    this.quantity = quantity;
  }

  public void setPrice(Double price)
  {
    this.price = price;
  }

  public int getItemID()
  {
     return this.itemID;
  }

  public String getName()
  {
    return this.name;
  }

  public int getQuantity()
  {
    return this.quantity;
  }

  public double getPrice()
  {
    return this.price;
  }

  public String toString()
  {
    return "ItemID: "+ itemID + "\nName: " + name + "\nQuantity: " + quantity + "\nPrice: " + price + "\n"; 
  }         
}