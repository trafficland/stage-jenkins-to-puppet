Bash scripts (may add ruby) in this project are expected to be executed from a Jenkins Job via a Post Build Task. This is possible via the Jenkins Post Build Task plugin.

The script takes three arguments; job-name, hostname-or-ip, and path to save to. Once copied commands to execute puppet routines to install to subscribed puppet clients.
