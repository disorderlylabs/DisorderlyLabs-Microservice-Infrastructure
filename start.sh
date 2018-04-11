echo "Start config server"
docker run -d -p 8888:8888 --net=mynet --ip=10.0.0.29 -v /Users/Ryan/config/:/config com.disorderlylabs/config
sleep 10

echo "Start primary instances"
docker run -d -p 7001:8080 --net=mynet --ip=10.0.0.21 -e zipkin_ip=172.17.0.1:9411 -e config_ip=10.0.0.29 com.disorderlylabs/inventory
docker run -d -p 7002:8080 --net=mynet --ip=10.0.0.22 -e zipkin_ip=172.17.0.1:9411 -e config_ip=10.0.0.29 com.disorderlylabs/cart
docker run -d -p 7003:8080 --net=mynet --ip=10.0.0.23 -e zipkin_ip=172.17.0.1:9411 -e config_ip=10.0.0.29 com.disorderlylabs/invoice
docker run -d -p 7004:8080 --net=mynet --ip=10.0.0.24 -e zipkin_ip=172.17.0.1:9411 -e config_ip=10.0.0.29 com.disorderlylabs/paymentgateway

echo "Start backups"
docker run -d -p 7005:8080 --net=mynet --ip=10.0.0.25 -e zipkin_ip=172.17.0.1:9411 -e config_ip=10.0.0.29 com.disorderlylabs/inventory
docker run -d -p 7006:8080 --net=mynet --ip=10.0.0.26 -e zipkin_ip=172.17.0.1:9411 -e config_ip=10.0.0.29 com.disorderlylabs/cart
docker run -d -p 7007:8080 --net=mynet --ip=10.0.0.27 -e zipkin_ip=172.17.0.1:9411 -e config_ip=10.0.0.29 com.disorderlylabs/invoice
docker run -d -p 7008:8080 --net=mynet --ip=10.0.0.28 -e zipkin_ip=172.17.0.1:9411 -e config_ip=10.0.0.29 com.disorderlylabs/paymentgateway

echo "Starting App"
docker run -d -p 7000:8080 --net=mynet --ip=10.0.0.20 -e zipkin_ip=172.17.0.1:9411 -e config_ip=10.0.0.29 com.disorderlylabs/app

echo "Forward zipkin access"
docker run -d -p 9411:9411 --net=mynet openzipkin/zipkin

echo "Done"