import unittest
import requests
import sys

if len(sys.argv)>2:
  pg_ip = sys.argv[1] 
  port=sys.argv[2]
else:
  pg_ip = "localhost" 
  port="7004"

url = "http://" + pg_ip + ":" + port  

class unittest_pg(unittest.TestCase):

  def test_startup(self):
      res = requests.get(url+'/pg')
      self.assertEqual(res.text, 'Greetings from Payment Gateway!')      

  def test_makePayment(self):
      res = requests.put(url+'/pg/makePayment?total_price=10')
      d = res.json()
      self.assertEqual(d['status'],"success")
      self.assertEqual(d['message'],"A payment of $10.0 was successful")

if __name__ == '__main__':
    unittest.main()