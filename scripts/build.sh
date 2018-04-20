#!/bin/sh

build() {
	cd $1
	./gradlew build docker
	cd ..
}

setup_config() {
  working_dir=`pwd`
  config_dir="$HOME/config"
  files_dir="$working_dir/files"
	config_file="application.yml"

	echo "Making directory: $config_dir"
	mkdir $config_dir 

	echo "copying config file: $config_file"
	cp $files_dir/$config_file $config_dir/

	echo "Changing directory to: $config"
	cd $config_dir 

	echo "initializing local git repo"
	git init
	git add $config_file
	git commit -m "adding configuration file" 

  cd $working_dir
}


setup_config


docker ps | grep -v zipkin | cut -d ' ' -f1 | xargs docker kill
docker ps -a | awk '{if(NR>1) print}' | grep -v zipkin | awk '{print $1}' | xargs docker rm

build app
build inventory
build cart
build paymentGateway
build invoice

scripts/start.sh

docker ps
