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