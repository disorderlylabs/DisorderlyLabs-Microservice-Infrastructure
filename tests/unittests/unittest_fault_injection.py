import unittest
import requests
import sys

if len(sys.argv)>2:
  cart_ip = sys.argv[1] 
  port=sys.argv[2]
else:
  app_ip = "localhost" 
  port="7000"

url = "http://" + app_ip + ":" + port  

class unittest_cart(unittest.TestCase):

  def test_startup(self):
      print "calling endpoint: /app"
      res = requests.get(url+'/app')
      print "result: " + res
      

  def test_testEndpoint(self):
      print "calling test endpoint: /app/test with delay of 2s at cart"
      headers = {'InjectFault': '/cart/test=DELAY:2000'}
      res = requests.get(url+'/app/test', headers=headers)
      print "result: " + res
      
  def test_placeorder(self):
      print "full app test with placeorder request. dropping message to inventory, request should fail"
      headers = {'InjectFault': '/inventory/takeFromInventory=DROP_PACKET'}

      res = requests.post(url+'/app/instantPlaceOrder?name=Chamber&quantity=7')

      d = res.json()
      self.assertEqual(d['status'],"success")
 

if __name__ == '__main__':
    unittest.main()
