import unittest
import requests
import sys

if len(sys.argv)>2:
  cart_ip = sys.argv[1] 
  port=sys.argv[2]
else:
  cart_ip = "localhost" 
  port="7002"

url = "http://" + cart_ip + ":" + port  

class unittest_cart(unittest.TestCase):

  def test_startup(self):
      res = requests.get(url+'/cart')
      self.assertEqual(res.text, 'Greetings from Cart App!') 

  def test_addToCart(self):
      res = requests.put(url+'/cart/addToCart?ItemID=4&quantity=4&total_price=34')
      d = res.json()
      self.assertEqual(d['status'],"success")
 
      res = requests.get(url+'/cart/getCartItems')
      d = res.json()
      self.assertEqual(d[0]['itemID'],4)
      self.assertEqual(d[0]['quantity'],4)
      self.assertEqual(d[0]['totalPrice'],34.0)

  def test_emptyCart(self):
      res = requests.delete(url+'/cart/emptyCart')
      d = res.json()
      self.assertEqual(d['status'],"success")

      res = requests.get(url+'/cart/getCartItems')
      d = res.json()
      self.assertEqual(len(d),0)

  def test_placeOrder_nothinginCart(self):
      res = requests.delete(url+'/cart/emptyCart')
      d = res.json()
      self.assertEqual(d['status'],"success")      

      res = requests.put(url+'/cart/placeOrder')
      d = res.json()
      self.assertEqual(d['status'],"failure")
      self.assertEqual(d['message'],"No items in cart")

if __name__ == '__main__':
    unittest.main()