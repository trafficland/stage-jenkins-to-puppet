#!/bin/sh
#works with gnu sed
moduleName=${1?need module name}
pathToModuleName=${2?need module name path}
OSX=${3:-}
pathToRunInSSH_SH=${4:-'./'}
pathAndmoduleName="$pathToModuleName""$moduleName"


newSshModuleName=$moduleName'.sh'
cp "$pathToRunInSSH_SH"runInSSH.sh  "$newSshModuleName"


sshArgs="$pathAndmoduleName"'/sshargs.sh'
sshExecutionContent="$pathAndmoduleName"'/toexecute.sh'
generatedScriptArgs="$pathAndmoduleName"'/topargs.sh'

sed -i '/REPLACE_WITH_TOP_ARGS/{
	s/REPLACE_WITH_TOP_ARGS//g
	r '"$generatedScriptArgs"'
}' "$newSshModuleName"

sed -i '/REPLACE_WITH_ARGS/{
	s/REPLACE_WITH_ARGS//g
	r '"$sshArgs"'
}' "$newSshModuleName"

sed -i 's/SSHCMD /ssh /;N;G;s/\n//g' "$newSshModuleName"

sed -i '/REPLACE_WITH_EXEC_CONTENT/{
	s/REPLACE_WITH_EXEC_CONTENT//
	r '"$sshExecutionContent"'
	N
}' "$newSshModuleName"