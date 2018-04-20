# DisorderlyLabs-Microservice-Infrastructure
In-house MicroService Infrastructure for Disorderly Labs

## Architecture
![Architecture](files/Infra.png)

## Prerequisites 
1. JDK 6+
2. Gradle
3. Docker

## Create Network
`docker network create --subnet=10.0.0.0/16 mynet`

## Build the images
build.sh in scripts directory will set up the config server for Spring Cloud and also build all of the images.
After building, the script will start all the images in a subnet, you should see all the docker images thereafter. 

To view the build logs of each image, you can do "$docker logs $imageid"
 

## Run a sample command
`curl -X PUT "http://localhost:7000/app/instantPlaceOrder?name=Chamber&quantity=7"`

The above call involves all five microservices. First _'App'_ makes a call to _'Inventory'_, to take the item out of inventory (Inventory Database). Then _'App'_ makes a call to _'Cart'_, to add the item to the Cart (Cart Database). Once successful, _'Cart'_ makes a call to _'paymentGateway'_ to process the payment. On successful payment, _'Cart'_ makes a call to _'Invoice'_ to generate invoice.         



# Fault Injection
Faults are injected into a request through headers and are propagated downstream through the opentracing framework.
Each service will listen for a specific header representing a fault flag. If there is a flag, it will parse the value associated with
the key to determine the services to fail. When looping through the services, if one of the targeted services is the current service,
then it will trigger the fault specified inside. Fault flags will be in the form

<string,string> => <InjectFault, serviceName=fault1;fault2;fault3>

where fault is in the form: 

faulttype:param

Example: <InjectFault, service1=DELAY:1000;DROP_PACKET:service3>

services here represent endpoints that we are targeting.





