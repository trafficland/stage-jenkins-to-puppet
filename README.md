Bash scripts in this project are expected to be executed from a Jenkins Job via a Post Build Task. This is possible via the Jenkins Post Build Task plugin.

The script takes three arguments; job-name, hostname-or-ip, and path to save to. Once copied commands to execute puppet routines to install to subscribed puppet clients.

Substantiator is a PlayFramework application that will be used to validate deployed application. State is stored in MongoDB. 

Current approach will have an expected deployment time, and if it is not finished in time it will be rolled back. Actors will handle the delay and execution of rollbacks to Controllers. 

A rollback will execute another bash script to hit puppet master to push an old version via a manifest.
