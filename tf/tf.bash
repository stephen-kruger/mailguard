#!/bin/bash

function deep_clean {
	echo "Cleaning terraform artifacts"
	terraform destroy --force
	
	echo "Cleaning docker artifacts"
	docker system prune --force
	docker system prune -af
	
	echo "Cleaning images"
	docker images purge
	
	echo "Cleaning volumes"
	docker volume prune --force
}

function clean {
	echo "Cleaning terraform artifacts"
	terraform destroy --force
	terraform apply
}

function build {
	echo "Building docker image"
	cd ..
	mvn clean package -Dmaven.test.skip=true
	cd docker
	docker build -t mailguard:latest .
	cd ../tf
}

function deploy {
	terraform apply
}

function help() {
	echo "Please use one of the following options :"
	echo "	--build  Build the docker image"
	echo "	--clean  Clean the docker image"
	echo "	--deploy Deploy the docker image"
}

for arg in "$@"
do
    if [ "$arg" == "--clean" ] || [ "$arg" == "-c" ]
    then
        clean
    else 
    	if [ "$arg" == "--build" ] || [ "$arg" == "-b" ]
    	then
        	build
    	else
			if [ "$arg" == "--deploy" ] || [ "$arg" == "-d" ]
			then
				build
    		else
    			if [ "$arg" == "--help" ] || [ "$arg" == "-h" ]
				then
					help
	    		else
	    			help
	    		fi
    		fi
    	fi
    fi
done