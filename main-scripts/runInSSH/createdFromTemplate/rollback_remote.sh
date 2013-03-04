#!/bin/sh
#PLEASE NOTE THIS FILE WAS CREATED BY A TEMPLATE!! TO MODIFY , modify the MODULE AND TEMPLATE!
applicationName=${1?missing application name}
stagePath=${2?missing stage path}
extension=${3?missing extension}
destinationAddress=${4?missing destination address}
extractCmd=${5?missing extraction command like "unzip"}
applicationPortNumber=${6?missing port number for application hosting}

#do something in ssh land
ssh $destinationAddress applicationName=$applicationName stagePath=$stagePath extension=$extension destinationAddress=$destinationAddress extractCmd=$extractCmd applicationPortNumber=$applicationPortNumber 'bash -s' <<'ENDSSH'
# commands to run on remote host
######## Begin local hive replication #TODO - THIS IS PROBABLY being removed, to use git  as rollback
    newAppToBecomeCurrentApp=$applicationName'.new'$extension
    currentApp=$applicationName$extension
    originalBackupApp=$applicationName'.last'$extension
    rollBackApp=$applicationName'.last.bak'$extension
    
    echo 'stagePath: '$stagePath' on '$destinationAddress 
    #pwd
    cd $stagePath
    #move each app if it exists as a file
    #last.bak to .last
    #.last becomes current
    if [ -f "$originalBackupApp" ]
    then
      mv "$originalBackupApp" "$currentApp";
    fi

    if [ -f "$rollBackApp" ]
    then
    	mv "$rollBackApp" "$originalBackupApp";
    fi
  ######## End local hive replication

  ######## Begin Extraction
    #an extracted package is desirable for puppet management, since the extracted contents is pushed "as is"
    #creating an isolated location for unzipping the package
    extractDir=extractZone
    
    #always start with a fresh extraction zone
    if [ -d "$extractDir" ]
    then
      rm -rf $extractDir
    fi
    mkdir $extractDir
    
    #copy latest compressed app to extractZone and extract!
    cp $currentApp ./$extractDir
    cd $extractDir
    
    echo should be in extractZone
    pwd

    #extract
    extractWhole=$extractCmd' ./'$currentApp
    $extractWhole 

    #find the depth of packaging
    #move into matching directory there should only be one unziped applicationName at this point
    #wild cards in a command need to be created as a variable
    changeDirIntoZip=cd' '$applicationName'*'
    $changeDirIntoZip
    
    #see if we need to go deeper
      if test -n "$(find $applicationName -maxdepth 1 -print -quit)"
      then
        echo found package one level deep moving to puppet modules
        
      else
        echo package is at zero depth, backing out and moving to puppet
        cd ../
      fi
    
    #BEGIN fix start script ##TEMPORY
      $changeDirIntoZip
      #sed in linux does not work in osx
      #replace java with nohup java
      sed -i 's/exec java/exec nohup java/g' ./start

      #replace and with &, literal & is \& 
      sed -i 's/NettyServer `dirname $0`/NettyServer `dirname $0` \&/g' ./start
      
      #put a port number into the stat script if it exists
      if [ "$applicationPortNumber" != "9000" ]
      then
        sed -i 's/play.core.server.NettyServer/-Dhttp.port='"$applicationPortNumber"' play.core.server.NettyServer/g' ./start  
      fi

      cd ../
    #END fix start
    rm -f *.zip
  ####### End Extraction

  ############# Actual Puppet Module Copying
  #clean up puppet module hive, and set up puppet module locations
  puppetModule=/etc/puppet/modules/"$applicationName"/files/stage
  removeOld=rm' -rf '$puppetModule/"$applicationName"'*'
  $removeOld
  
  #whatever the current naming convention it will be just appName in the end!
  copyAppToPuppetModule=cp' -r '"$applicationName"'* '"$puppetModule";
  #execute copy or move
  echo Copying via command $copyAppToPuppetModule
  $copyAppToPuppetModule
  
  echo 'App should be sent to puppet module @: '$puppetModule
ENDSSH