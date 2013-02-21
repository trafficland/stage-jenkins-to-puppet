#!/bin/sh
#get correct arguments
jenkinsJobName=${1?missing jenkins job name} #example proj1-stage
destinationAddress=${2?missing destination ip or hostname}
applicationName=${3?missing application name} #example proj1
applicationPortNumber=${4:-} #if this is a play app the default will be empty and the port # will be 9000
stageHome=${5:-'~/stage/'} #example ~/stage/
extension=${6:-.zip} #default extension to zip
extractCmd=${7:-unzip} #argument is used for extraction, if this was tar then "tar -xvf" would be here

# echo "$jenkinsJobName"
# echo "$destinationAddress"
# echo "$applicationName"
# echo "$stageHome"
# echo "$extension"
# echo "$extractCmd"

echo 'Your extension for '"$applicationName"' is '"$extension"' . Your extraction cmd is '"$extractCmd"'.'
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

#echo "$destination"

ssh $destinationAddress applicationName=$applicationName stagePath=$stagePath applicationPortNumber=$applicationPortNumber 'bash -s' <<'ENDSSH'
  # commands to run on remote host
  mkdir -p "$stagePath"
ENDSSH
#pack full scp line
cmd=scp' '$packageLocation' '$destination
#execute scp
echo $cmd
$cmd

sh ~/stageRemote.sh $applicationName $stagePath $extension $destinationAddress $extractCmd