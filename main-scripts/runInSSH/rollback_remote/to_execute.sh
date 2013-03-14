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
    
      echo 'prior to cd "$applicationName" pwd'
      pwd
      #BEGIN fix start script ##TEMPORY
      if [ "$startName" ]; then
        $changeDirIntoZip
        #sed in OSX is BSD and does not work the same as linux sed,
        #you can install gnu-sed with brew to override BSD sed, you will need /usr/bin/local added to your path
        echo 'after to cd "$applicationName", pwd'
        pwd
        startNameAndPath='./'"$startName"
        echo 'Original startName and path'"$startNameAndPath"
        #remove stage.conf from basic start
        sed -i 's/-Dconfig.file=`dirname $0`/conf/stage.conf//g' "$startNameAndPath"

        startBark="$startNameAndPath"'BarkJavaArgs'
        cp "$startNameAndPath" "$startBark";
        echo 'Removing from copyStart stage.conf so that it is called from init.d instead!'
        #insert argument at the top to force a conf to be called into a startCopy
        sed -i '2s/^/${1?need java args like -Dconfig.file=./conf/stage.conf -Dhttp.port=9000}\n/' "$$startBark"
        cd ../
      fi
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