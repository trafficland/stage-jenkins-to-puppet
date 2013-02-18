#!/bin/sh
#curl to add an application and a machine set
appName=$1
versionExpected=$2
commaDelimitedMachineNameList=$3
currentVersion=${4:-} #default none
serviceUrl=$5

machines=(${commaDelimitedMachineNameList//,/ })

declare -a jsonReadyMachines
for i in "${!machines[@]}"
do
    if [ "$currentVersion" ]
    then
    	jsonReadyMachines["$i"]="{\"machineName\":\""${machines["$i"]}"\",\"actual\":\""$currentVersion"\"},"
    else

    	jsonReadyMachines["$i"]="{\"machineName\":\""${machines["$i"]}"\"},"
    fi
done
machineVersionsPaired="${jsonReadyMachines[*]}"
machinePairLastCommaRemoved="${machineVersionsPaired%?}"

echo "curl -v -H \"Content-type: application/json\" -X POST -d '{\"name\"":\"$appName\"",\"expected\"":\"$versionExpected\"",\"actualCluster\":[ $machinePairLastCommaRemoved ]}' http://"$serviceUrl"/apps/save"