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