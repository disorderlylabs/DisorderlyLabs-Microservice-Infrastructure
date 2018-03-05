import unittest
import requests
import sys

if len(sys.argv)>2:
  invoice_ip = sys.argv[1] 
  port=sys.argv[2]
else:
  invoice_ip = "localhost" 
  port="7003"

url = "http://" + invoice_ip + ":" + port  

class unittest_invoice(unittest.TestCase):

  def test_startup(self):
      res = requests.get(url+'/invoice')
      self.assertEqual(res.text, 'Greetings from Invoice Microservice!')      

  def test_noInvoiceGenerated(self):
      requests.put(url+'/invoice/clearInvoice')
      res = requests.get(url+'/invoice/getInvoice')
      self.assertEqual(res.text, 'Invoice not generated yet')

if __name__ == '__main__':
    unittest.main()