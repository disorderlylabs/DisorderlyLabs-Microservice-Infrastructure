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
      requests.put(url+'/invoice/umang/clearInvoice')
      res = requests.get(url+'/invoice/umang/getInvoice')
      self.assertEqual(res.text, 'Invoice not generated yet')

  def test_randomGetInvoice(self):
      res = requests.get(url+'/invoice/qwerty/getInvoice')
      self.assertEqual(res.text, 'Invoice not generated yet')

  def test_randomClearInvoice(self):
      res = requests.get(url+'/invoice/asdf/clearInvoice')
      self.assertEqual(res.status_code,200)            

if __name__ == '__main__':
    unittest.main()