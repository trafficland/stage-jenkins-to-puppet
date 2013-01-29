#get correct arguments
jenkinsJobName=$1 #example proj1-stage
destinationAddress=$2 #
stageHome=$3 #example ~/stage/
applicationName=$4 #example proj1

applicationNameNew=$applicationName'.new'

#create the package location by replacing the jenkinsJobName variable with the actual jobName
packageLocation=/var/lib/jenkins/jobs/$jenkinsJobName/workspace/dist/*.zip

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
cmd=scp' '$packageLocation' '$destination
#execute scp
$cmd

#ssh into puppet machine
#http://stackoverflow.com/questions/305035/how-to-use-ssh-to-run-shell-script-on-a-remote-machine
ssh $destinationAddress applicationName=$applicationName 'bash -s' <<'ENDSSH'
  newAppToBecomeCurrentApp=$stagePath$applicationName'.new'
  currentApp=$stagePath$applicationName
  originalBackupApp=$stagePath$applicationName'.last'
  rollBackApp=$stagePath$applicationName'.last.bak'
  # commands to run on remote host
  echo 'stagePath: '$stagePath
  #cd $stagePath
  pwd
  #if [ -b "$device0" ]
  #then
  #	echo "$device0 is a block device.
  #fi
  mv "$originalBackupApp" "$rollBackApp";
  mv "$currentApp" "$originalBackupApp";
  mv "$newAppToBecomeCurrentApp" "$currentApp";
  echo $currentApp
  echo $originalBackupApp
  echo $rollBackApp
ENDSSH