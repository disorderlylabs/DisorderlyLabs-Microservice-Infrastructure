import unittest
import requests
import sys

if len(sys.argv)>2:
  inventory_ip = sys.argv[1] 
  port=sys.argv[2]
else:
  inventory_ip = "localhost" 
  port="7001"

url = "http://" + inventory_ip + ":" + port  

class unittest_inventory(unittest.TestCase):

  def test_startup(self):
      res = requests.get(url+'/inventory')
      self.assertEqual(res.text, 'Greetings from Inventory App!')

  def test_checkPrice(self):
      res = requests.get(url+'/inventory/checkPrice?name=Chamber')
      d = res.json()
      self.assertEqual(d['price'],6.5)   

  def test_getItemID(self):
      res = requests.get(url+'/inventory/getItemID?name=Order')
      d = res.json()
      self.assertEqual(d['itemid'],5)

  def test_getName(self):
      res = requests.get(url+'/inventory/getName?ItemID=6')
      d = res.json()
      self.assertEqual(d['name'],"HP - Half Blood Prince")      

  def test_checkAvailibilityAndTakeFromInventory(self):
      res = requests.get(url+'/inventory/checkAvailibility?name=Aska')
      d = res.json()
      available = d['quantity']

      if available > 2: 
        res = requests.put(url+'/inventory/takeFromInventory?name=Aska&quantity=2')
        d = res.json()
        self.assertEqual(d['status'],"success")
        self.assertEqual(d['total_price'],15)
        self.assertEqual(d['ItemID'],3)
        res = requests.get(url+'/inventory/checkAvailibility?name=Aska')
        d = res.json() 
        self.assertEqual(d['quantity'],available-2)   

  def test_notEnoughInInventory(self):
      res = requests.get(url+'/inventory/checkAvailibility?name=Aska')
      d = res.json()
      available = d['quantity']

      res = requests.put(url+'/inventory/takeFromInventory?name=Aska&quantity='+ str(available+1))
      d = res.json()
      self.assertEqual(d['status'],"failure")
      self.assertEqual(d['message'],"Not enough in inventory")

  def test_refreshCatalog(self):
      res = requests.put(url+'/inventory/refreshCatalog')
      d = res.json()
      self.assertEqual(d['status'],"success")

      res = requests.get(url+'/inventory/checkAvailibility?name=Aska')
      d = res.json()
      self.assertEqual(d['quantity'], 10)           

if __name__ == '__main__':
    unittest.main()