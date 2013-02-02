#!/bin/sh
test -f ~/.sbtconfig && . ~/.sbtconfig
exec java -Xmx512M -XX:MaxPermSize=1G ${SBT_OPTS} -jar ./sbt-launch.jar "$@"
