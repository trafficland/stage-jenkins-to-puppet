#!/bin/sh
#get correct arguments
jenkinsJobName=$1 #example proj1-stage
destinationAddress=$2 #
stageHome=$3 #example ~/stage/
applicationName=$4 #example proj1
extension=${5-.zip} #default extension to zip
extractCmd=${6-unzip} #argument is used for extraction, if this was tar then "tar -xvf" would be here

echo 'Your extension for '$applicationName' is '$extension' . Your extraction cmd is '$extractCmd'.'

applicationNameNew=$applicationName'.new'$extension

#create the package location by replacing the jenkinsJobName variable with the actual jobName
packageLocation=/var/lib/jenkins/jobs/$jenkinsJobName/workspace/dist/*$extension

#stagePath will combine the stageHome location to the subdirectory structure we want
#ie: if stageHome = '~/stage/'' and applicationName = 'proj1'
#then stagePath = ~/stage/proj1/ 
stagePath=$stageHome''$applicationName'/'
#append appNew, #then stagePath = ~/stage/proj1/proj1.new
stagePathWithNewApplication=$stagePath''$applicationNameNew

#destination will combine address/hostname to a full stagePath, this will fulfill the scp destination
#ie: 127.0.0.1:~/stage/proj1/proj1New
destination=$destinationAddress:$stagePathWithNewApplication
#pack full scp line
cmd=scp' -r '$packageLocation' '$destination
#execute scp
$cmd


sh ~/stageRemote.sh $applicationName $stagePath $extension $destinationAddress $extractCmd