#get correct arguments
jenkinsJobName=$1
destinationAddress=$2
stageLocation=$3
#can grap any zip in dist as there is only every one in there
packageLocation=scp' '/var/lib/jenkins/jobs/$jenkinsJobName/workspace/dist/*.zip 
#add url

destination=$destinationAddress:$stageLocation
cmd=$packageLocation' '$destination
$cmd