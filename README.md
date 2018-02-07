# DisorderlyLabs-Microservice-Infrastructure
In-house MicroService Infrastructure for Disorderly Labs

## Build the images
Go to individual folders and execute `./gradlew build docker`. You can configure the name of your docker image in the `build.gradle` of each microservice.

## Create Network
`docker network create --subnet=10.0.0.0/16 mynet`

## Start the services
```
docker run -p 7001:8080 --net=mynet --ip=10.0.0.21 -d com.disorderlylabs/inventory
docker run -p 7002:8080 --net=mynet --ip=10.0.0.22 -d com.disorderlylabs/cart
docker run -p 7000:8080 --net=mynet -e inventory_ip=10.0.0.21:8080 -e cart_ip=10.0.0.22:8080 -d com.disorderlylabs/app
```

## Run a sample command
`curl -X PUT "http://localhost:7000/app/addToCart?quantity=3&name=Chamber"`

The above call involves all three microservices. First _'App'_ makes a call to _'Inventory'_, to take the item out of inventory (Inventory Database). Then _'App'_ makes a call to _'Cart'_, to add the item to the Cart (Cart Database).         

