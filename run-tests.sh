cd tests/unittests
echo "Running unit-tests for inventory:" 
python unittest_inventory.py

echo "Running unit-tests for app:"
python unittest_app.py       

echo "Running unit-tests for cart:"
python unittest_cart.py        

echo "Running unit-tests for paymentGateway:"
python unittest_pg.py

echo "Running unit-tests for invoice:"
python unittest_invoice.py

cd ../integrationtests
echo "Running Integration Tests:"
python integration-tests.py  