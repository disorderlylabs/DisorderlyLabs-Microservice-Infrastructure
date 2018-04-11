#!/bin/sh

build() {
	cd $1
	./gradlew build docker
	cd ..
}

docker ps | grep -v zipkin | cut -d ' ' -f1 | xargs docker kill
docker ps -a | awk '{if(NR>1) print}' | grep -v zipkin | awk '{print $1}' | xargs docker rm

build config
build app
build inventory
build cart
build paymentGateway
build invoice

./start.sh
docker ps
