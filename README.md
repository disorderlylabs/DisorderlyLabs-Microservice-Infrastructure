# DisorderlyLabs-Microservice-Infrastructure
In-house MicroService Infrastructure for Disorderly Labs

## Prerequisites 
1. JDK 6+
2. Gradle
3. Docker

## Build the images
Go to individual folders and execute `./gradlew build docker`. You can configure the name of your docker image in the `build.gradle` of each microservice. 

To build all run `sh build.sh`.


## Create Network
`docker network create --subnet=10.0.0.0/16 mynet`

## Start the services
`sh start.sh`

You can also run each container separately without the `-d` parameter, to view the logs for each Microservice. 

## Run a sample command
`curl -X PUT "http://localhost:7000/app/instantPlaceOrder?name=Chamber&quantity=7"`

The above call involves all five microservices. First _'App'_ makes a call to _'Inventory'_, to take the item out of inventory (Inventory Database). Then _'App'_ makes a call to _'Cart'_, to add the item to the Cart (Cart Database). Once successful, _'Cart'_ makes a call to _'paymentGateway'_ to process the payment. On successful payment, _'Cart'_ makes a call to _'Invoice'_ to generate invoice.         

