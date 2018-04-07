docker run -d -p 8888:8888 --net=mynet --ip=10.0.0.25 -v ~/config/:/config com.disorderlylabs/config
sleep 10
docker run -d -p 7001:8080 --net=mynet --ip=10.0.0.21 -e zipkin_ip=172.17.0.1:9411 -e config_ip=10.0.0.25 com.disorderlylabs/inventory
docker run -d -p 7002:8080 --net=mynet --ip=10.0.0.22 -e zipkin_ip=172.17.0.1:9411 -e config_ip=10.0.0.25 com.disorderlylabs/cart
docker run -d -p 7003:8080 --net=mynet --ip=10.0.0.23 -e zipkin_ip=172.17.0.1:9411 -e config_ip=10.0.0.25 com.disorderlylabs/invoice
docker run -d -p 7004:8080 --net=mynet --ip=10.0.0.24 -e zipkin_ip=172.17.0.1:9411 -e config_ip=10.0.0.25 com.disorderlylabs/paymentgateway
docker run -d -p 7000:8080 --net=mynet --ip=10.0.0.20 -e zipkin_ip=172.17.0.1:9411 -e config_ip=10.0.0.25 com.disorderlylabs/app
docker run -d -p 9411:9411 --net=mynet openzipkin/zipkin