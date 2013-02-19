#!/bin/sh
#curl to add an application and a machine set
commaDelimitedMachineNameList=$1
isAliveAll=$2
serviceUrl=${3:-"localhost:9000"}

machines=(${commaDelimitedMachineNameList//,/ })


curl_command() {
  machineName=$1 isAlive=$2 url=$3

  curl -v -H "Content-type:application/json" -X POST -d '{"name":"'"$machineName"'","isAlive":'"$isAlive"'}' http://"$url"/machines/save
}

for i in "${!machines[@]}"
do

 curl_command ${machines["$i"]} $isAliveAll $serviceUrl
done
