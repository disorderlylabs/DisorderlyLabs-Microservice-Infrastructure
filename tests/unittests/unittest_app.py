import unittest
import requests
import sys

if len(sys.argv)>2:
  app_ip = sys.argv[1] 
  port=sys.argv[2]
else:
  app_ip = "localhost" 
  port="7000"

url = "http://" + app_ip + ":" + port  

class unittest_app(unittest.TestCase):

  def test_startup(self):
      res = requests.get(url+'/app')
      self.assertEqual(res.text, 'Greetings from App Microservice!')      

if __name__ == '__main__':
    unittest.main()