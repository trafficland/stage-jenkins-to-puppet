#!/bin/bash
###variables
basedir="/opt"
appdir="REPLACE_MODULE_NAME"
stagedir="stage"
initdir="/etc/init.d/"
ziptime=`stat -c %Y /opt/stage/REPLACE_MODULE_NAME-* | sort -rn | head -n1`
apptime=`stat -c %Y /opt/REPLACE_MODULE_NAME`
newapp=`stat -c %n /opt/stage/REPLACE_MODULE_NAME-* | sort -rn`

if [ -d "$basedir/$appdir" ] && [ "$ziptime" -gt "$apptime" ] ; then
#        service REPLACE_MODULE_NAME stop
        cd $basedir
        rm -rf $appdir.last
        mv $appdir $appdir.last
        mkdir $appdir
        cp -r "$newapp/REPLACE_MODULE_NAME" "$basedir"
#       service vqmdv start &&
exit 0
elif [ -d $basedir/$appdir ] && [ $ziptime -lt $apptime ] ; then
echo "nothing to do"
exit 0
fi


