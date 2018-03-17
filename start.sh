docker run -d -p 7001:8080 --net=mynet --ip=10.0.0.21 -e zipkin_ip=172.17.0.1:9411  com.disorderlylabs/inventory
docker run -d -p 7002:8080 --net=mynet --ip=10.0.0.22 -e zipkin_ip=172.17.0.1:9411 -e inventory_ip=10.0.0.21:8080 -e inventory_b_ip=10.0.0.25:8080 -e invoice_ip=10.0.0.23:8080 -e invoice_b_ip=10.0.0.26:8080 -e pg_ip=10.0.0.24:8080 -e pg_b_ip=10.0.0.27:8080 com.disorderlylabs/cart
docker run -d -p 7003:8080 --net=mynet --ip=10.0.0.23 -e zipkin_ip=172.17.0.1:9411 -e cart_ip=10.0.0.22:8080 -e cart_b_ip=10.0.0.28:8080 -e inventory_ip=10.0.0.21:8080 -e inventory_b_ip=10.0.0.25:8080 -e pg_ip=10.0.0.24:8080 -e pg_b_ip=10.0.0.27:8080 com.disorderlylabs/invoice
docker run -d -p 7004:8080 --net=mynet --ip=10.0.0.24 -e zipkin_ip=172.17.0.1:9411 com.disorderlylabs/paymentgateway
docker run -d -p 7000:8080 --net=mynet --ip=10.0.0.20 -e zipkin_ip=172.17.0.1:9411 -e inventory_ip=10.0.0.21:8080 -e inventory_b_ip=10.0.0.25:8080 -e cart_ip=10.0.0.22:8080 -e cart_b_ip=10.0.0.28:8080 -e invoice_ip=10.0.0.23:8080 -e invoice_b_ip=10.0.0.26:8080 com.disorderlylabs/app

# docker run -d -p 7005:8080 --net=mynet --ip=10.0.0.28 -e zipkin_ip=172.17.0.1:9411 -e inventory_ip=10.0.0.21:8080 -e inventory_b_ip=10.0.0.25:8080 -e invoice_ip=10.0.0.23:8080 -e invoice_b_ip=10.0.0.26:8080 -e pg_ip=10.0.0.24:8080 -e pg_b_ip=10.0.0.27:8080 com.disorderlylabs/cart
# docker run -d -p 7006:8080 --net=mynet --ip=10.0.0.26 -e zipkin_ip=172.17.0.1:9411 -e cart_ip=10.0.0.22:8080 -e cart_b_ip=10.0.0.28:8080 -e inventory_ip=10.0.0.21:8080 -e inventory_b_ip=10.0.0.25:8080 -e pg_ip=10.0.0.24:8080 -e pg_b_ip=10.0.0.27:8080 com.disorderlylabs/invoice
# docker run -d -p 7007:8080 --net=mynet --ip=10.0.0.27 -e zipkin_ip=172.17.0.1:9411 com.disorderlylabs/paymentgateway
# docker run -d -p 7008:8080 --net=mynet --ip=10.0.0.29 -e zipkin_ip=172.17.0.1:9411 -e inventory_ip=10.0.0.21:8080 -e inventory_b_ip=10.0.0.25:8080 -e cart_ip=10.0.0.22:8080 -e cart_b_ip=10.0.0.28:8080 -e invoice_ip=10.0.0.23:8080 -e invoice_b_ip=10.0.0.26:8080 com.disorderlylabs/app
docker run -d -p 7009:8080 --net=mynet --ip=10.0.0.25 -e zipkin_ip=172.17.0.1:9411  com.disorderlylabs/inventory

docker run -d -p 9411:9411 --net=mynet openzipkin/zipkin