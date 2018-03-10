import unittest
import requests
import sys

if len(sys.argv)>5:
  app_url = sys.argv[1] 
  inventory_url=sys.argv[2]
  cart_url = sys.argv[3] 
  invoice_url=sys.argv[4]
  pg_url=sys.argv[5]  
else:
  app_url = "localhost:7000" 
  inventory_url = "localhost:7001"
  cart_url = "localhost:7002" 
  invoice_url = "localhost:7003"
  pg_url = "localhost:7004"
  
app_url = "http://" + app_url
inventory_url = "http://" + inventory_url
cart_url = "http://" + cart_url
invoice_url = "http://" + invoice_url
pg_url = "http://" + pg_url

class integrationtests(unittest.TestCase):

  def test_takeFromInventory(self):
      res = requests.put(inventory_url+'/inventory/refreshCatalog')
      d = res.json()
      self.assertEqual(d['status'],"success")

      res = requests.put(app_url+'/app/takeFromInventory?name=Philo&quantity=3')
      d = res.json()
      self.assertEqual(d['status'],"success")
      self.assertEqual(d['total_price'],16.5)
      self.assertEqual(d['ItemID'],1)

  def test_notEnoughInInventory(self):
      res = requests.put(inventory_url+'/inventory/refreshCatalog')
      d = res.json()
      self.assertEqual(d['status'],"success")

      res = requests.get(inventory_url+'/inventory/checkAvailibility?name=Chamber')
      d = res.json()
      available = d['quantity']

      res = requests.put(app_url+'/app/takeFromInventory?name=Chamber&quantity='+ str(available+1))
      d = res.json()
      self.assertEqual(d['status'],"failure")
      self.assertEqual(d['message'],"Not enough in inventory")

  def test_addToCart_notEnoughInInventory(self):
      res = requests.put(inventory_url+'/inventory/refreshCatalog')
      d = res.json()
      self.assertEqual(d['status'],"success")

      res = requests.get(inventory_url+'/inventory/checkAvailibility?name=Chamber')
      d = res.json()
      available = d['quantity']

      res = requests.put(app_url+'/app/addToCart?name=Chamber&quantity='+ str(available+1))
      d = res.json()
      self.assertEqual(d['status'],"failure")
      self.assertEqual(d['message'],"Not enough in inventory")  

  def test_addToCart(self):
      res = requests.put(inventory_url+'/inventory/refreshCatalog')
      d = res.json()
      self.assertEqual(d['status'],"success")

      res = requests.delete(cart_url+'/cart/emptyCart')
      d = res.json()
      self.assertEqual(d['status'],"success")

      res = requests.put(app_url+'/app/addToCart?name=Philo&quantity=3')
      d = res.json()
      self.assertEqual(d['status'],"success")

      res = requests.put(app_url+'/app/addToCart?name=Goblet&quantity=4')
      d = res.json()
      self.assertEqual(d['status'],"success")        

      res = requests.get(cart_url+'/cart/getCartItems')
      d = res.json()
      self.assertEqual(d[0]['itemID'],1)
      self.assertEqual(d[0]['quantity'],3)
      self.assertEqual(d[0]['totalPrice'],16.5)
      self.assertEqual(d[1]['itemID'],4)
      self.assertEqual(d[1]['quantity'],4)
      self.assertEqual(d[1]['totalPrice'],34.0)                          

  def test_instantPlaceOrder_notEnoughInInventory(self):
      res = requests.put(inventory_url+'/inventory/refreshCatalog')
      d = res.json()
      self.assertEqual(d['status'],"success")

      res = requests.get(inventory_url+'/inventory/checkAvailibility?name=Chamber')
      d = res.json()
      available = d['quantity']

      res = requests.put(app_url+'/app/instantPlaceOrder?name=Chamber&quantity='+ str(available+1))
      d = res.json()
      self.assertEqual(d['status'],"failure")
      self.assertEqual(d['message'],"Not enough in inventory")  

  def test_placeOrder(self):
      res = requests.put(inventory_url+'/inventory/refreshCatalog')
      d = res.json()
      self.assertEqual(d['status'],"success")

      res = requests.delete(cart_url+'/cart/emptyCart')
      d = res.json()
      self.assertEqual(d['status'],"success")

      res = requests.put(app_url+'/app/addToCart?name=Half&quantity=2')
      d = res.json()
      self.assertEqual(d['status'],"success")      

      res = requests.put(app_url+'/app/addToCart?name=Deathly&quantity=2')
      d = res.json()
      self.assertEqual(d['status'],"success") 

      res = requests.put(app_url+'/app/placeOrder')
      d = res.json()
      self.assertEqual(d['status'],"success")

      res = requests.get(cart_url+'/cart/getCartItems')
      d = res.json()
      self.assertEqual(len(d),0)

      res = requests.get(app_url+'/app/getInvoice')
      self.assertEqual(res.text.split("\n")[5], "Total Price  =  44.0")

  def test_instantPlaceOrder(self):
      res = requests.put(inventory_url+'/inventory/refreshCatalog')
      d = res.json()
      self.assertEqual(d['status'],"success")

      res = requests.delete(cart_url+'/cart/emptyCart')
      d = res.json()
      self.assertEqual(d['status'],"success")

      res = requests.put(app_url+'/app/instantPlaceOrder?name=Order&quantity=3')
      d = res.json()
      self.assertEqual(d['status'],"success")      

      res = requests.get(cart_url+'/cart/getCartItems')
      d = res.json()
      self.assertEqual(len(d),0)

      res = requests.get(app_url+'/app/getInvoice')
      self.assertEqual(res.text.split("\n")[4], "Total Price  =  28.5")

  def test_instantPlaceOrder_withIteminCart(self):
      res = requests.put(inventory_url+'/inventory/refreshCatalog')
      d = res.json()
      self.assertEqual(d['status'],"success")

      res = requests.delete(cart_url+'/cart/emptyCart')
      d = res.json()
      self.assertEqual(d['status'],"success")

      res = requests.put(app_url+'/app/addToCart?name=Half&quantity=2')
      d = res.json()
      self.assertEqual(d['status'],"success")       

      res = requests.put(app_url+'/app/instantPlaceOrder?name=Order&quantity=3')
      d = res.json()
      self.assertEqual(d['status'],"success")      

      res = requests.get(cart_url+'/cart/getCartItems')
      d = res.json()
      self.assertEqual(len(d),0)

      res = requests.get(app_url+'/app/getInvoice')
      self.assertEqual(res.text.split("\n")[5], "Total Price  =  49.5")

  def test_placeOrder_nothinginCart_viaApp(self):
      res = requests.delete(cart_url+'/cart/emptyCart')
      d = res.json()
      self.assertEqual(d['status'],"success")      

      res = requests.put(app_url+'/app/placeOrder')
      d = res.json()
      self.assertEqual(d['status'],"failure")
      self.assertEqual(d['message'],"No items in cart")      

  def test_test_noInvoiceGenerated_viaApp(self):
      requests.put(invoice_url+'/invoice/clearInvoice')

      res = requests.get(app_url+'/app/getInvoice')
      self.assertEqual(res.text, 'Invoice not generated yet')

  def test_undoCart_nothingCart(self):
      res = requests.put(inventory_url+'/inventory/refreshCatalog')
      d = res.json()
      self.assertEqual(d['status'],"success")

      res = requests.delete(cart_url+'/cart/emptyCart')
      d = res.json()
      self.assertEqual(d['status'],"success")

      res = requests.put(app_url+'/app/undoCart')
      d = res.json()
      self.assertEqual(d['status'],"failure")
      self.assertEqual(d['message'],"No items in cart")


  def test_undoCart(self):
      res = requests.put(inventory_url+'/inventory/refreshCatalog')
      d = res.json()
      self.assertEqual(d['status'],"success")

      res = requests.delete(cart_url+'/cart/emptyCart')
      d = res.json()
      self.assertEqual(d['status'],"success")

      res = requests.put(app_url+'/app/addToCart?name=Aska&quantity=3')
      d = res.json()
      self.assertEqual(d['status'],"success")

      res = requests.put(app_url+'/app/addToCart?name=Chamber&quantity=4')
      d = res.json()
      self.assertEqual(d['status'],"success")

      res = requests.get(inventory_url+'/inventory/checkAvailibility?name=Chamber')
      d = res.json()
      available_C = d['quantity']

      res = requests.get(inventory_url+'/inventory/checkAvailibility?name=Aska')
      d = res.json()
      available_A = d['quantity']

      res = requests.put(app_url+'/app/undoCart')
      d = res.json()
      self.assertEqual(d['status'],"success")

      res = requests.get(inventory_url+'/inventory/checkAvailibility?name=Chamber')
      d = res.json()
      self.assertEqual(d['quantity'],available_C+4)

      res = requests.get(inventory_url+'/inventory/checkAvailibility?name=Aska')
      d = res.json()
      self.assertEqual(d['quantity'],available_A+3)

      res = requests.get(cart_url+'/cart/getCartItems')
      d = res.json()
      self.assertEqual(len(d),0)      

if __name__ == '__main__':
    unittest.main()      