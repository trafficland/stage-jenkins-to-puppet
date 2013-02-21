#!/bin/sh
REPLACE_WITH_TOP_ARGS


#do something in ssh land
SSHCMD REPLACE_WITH_ARGS
 'bash -s' <<'ENDSSH'

# commands to run on remote host

REPLACE_WITH_EXEC_CONTENT

ENDSSH