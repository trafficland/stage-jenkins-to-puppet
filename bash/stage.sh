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
cmdSSH=ssh' '$destinationAddress
sshCmdMoveIntoStagePath=$cmdSSH' "cd $stagePath; exit bash"'
cmdCopyLastToLastBak=mv' '$applicationName'.last '$applicationName'.last.bak'
sshCmdCopyLastToLastBak=$cmdSSH' "$cmdCopyLastToLastBak; exit bash"'
cmdCopyOldLatestToLast=mv' '$applicationName' '$applicationName'.last'
sshCmdCopyOldLatestToLast=$cmdSSH' "$cmdCopyOldLatestToLast; exit bash"'
cmdCopyNewToDefault=mv' '$applicationNameNew' '$applicationName
sshCmdCopyNewToDefault=$cmdSSH' "$cmdCopyNewToDefault; exit bash"'
#cmdExit=exit

$sshCmdMoveIntoStagePath
$sshCmdCopyLastToLastBak
$sshCmdCopyOldLatestToLast
$sshCmdCopyNewToDefault
#$cmdExit