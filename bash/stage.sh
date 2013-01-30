#get correct arguments
jenkinsJobName=$1 #example proj1-stage
destinationAddress=$2 #
stageHome=$3 #example ~/stage/
applicationName=$4 #example proj1
extension=${5-.zip} #default extension to zip
echo 'Your extension for '$applicationName' is '$extension' .'


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
cmd=scp' '$packageLocation' '$destination
#execute scp
$cmd

#ssh into puppet machine
#http://stackoverflow.com/questions/305035/how-to-use-ssh-to-run-shell-script-on-a-remote-machine
ssh $destinationAddress applicationName=$applicationName stagePath=$stagePath extension=$extension 'bash -s' <<'ENDSSH'
  # commands to run on remote host
  newAppToBecomeCurrentApp=$applicationName'.new'$extension
  currentApp=$applicationName$extension
  originalBackupApp=$applicationName'.last'$extension
  rollBackApp=$applicationName'.last.bak'$extension
  
  echo 'stagePath: '$stagePath' on '$destinationAddress 
  #pwd
  cd $stagePath
  #move each app if it exists as a file
  if [ -f "$originalBackupApp" ]
  then
  	mv "$originalBackupApp" "$rollBackApp";
  fi
  
  if [ -f "$currentApp" ]
  then
  	mv "$currentApp" "$originalBackupApp";
  fi

  if [ -f "$newAppToBecomeCurrentApp" ]
  then
  	mv "$newAppToBecomeCurrentApp" "$currentApp";
  fi

  echo 'Your latest application should now be: '$currentApp'!'
  #echo $originalBackupApp
  #echo $rollBackApp
ENDSSH