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
cmd=scp' '$packageLocation' '$destination
#execute scp
$cmd

#ssh into puppet machine
#http://stackoverflow.com/questions/305035/how-to-use-ssh-to-run-shell-script-on-a-remote-machine
ssh $destinationAddress applicationName=$applicationName stagePath=$stagePath extension=$extension destinationAddress=$destinationAddress extractCmd=$extractCmd 'bash -s' <<'ENDSSH'
  # commands to run on remote host
  
  ######## Begin local hive replication
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
  ######## End local hive replication

  ######## Begin Extraction
  #an extracted package is desirable for puppet management, since the extracted contents is pushed "as is"
  #creating an isolated location for unzipping the package
  extractDir=extractZone
  
  #always start with a fresh extraction zone
  if [ -f "$extractDir" ]
  then
    rm -rf $extractDir
  else  
    mkdir $extractDir
  fi

  #copy latest compressed app to extractZone and extract!
  cp $currentApp ./$extractDir
  cd $extractDir
  #extract
  $extractCmd' '$currentApp

  #find the depth of packaging
  #move into matching directory there should only be one unziped applicationName at this point
  cd $applicationName* 
  
  #see if we need to go deeper
  if test -n "$(find ./$applicationName -maxdepth 1 -print -quit)"
  then
    echo found package one level deep moving to puppet modules
    
  else
    echo package is at zero depth, backing out and moving to puppet
    cd ../
  fi
  ####### End Extraction

  ############# Actual Puppet Module Copying
  #clean up puppet module hive, and set up puppet module locations
  puppetModule=/etc/puppet/modules/"$applicationName"/files/stage
  rm -rf $puppetModule/$applicationName*
  
  cp "$applicationName" "$puppetModule";
  echo 'App should be sent to puppet module @: '$puppetModule
ENDSSH