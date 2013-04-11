For more latest documentation and updateds please go to the [wikki](https://github.com/trafficland/stage-jenkins-to-puppet/wiki)


Bash scripts in this project are expected to be executed from a Jenkins Job via a Post Build Task. This is possible via the Jenkins Post Build Task plugin.

The script takes three arguments; job-name, hostname-or-ip, and path to save to. Once copied commands to execute puppet routines to install to subscribed puppet clients.

Substantiator is a PlayFramework application that will be used to validate deployed application. State is stored in MongoDB. 

Current approach will have an expected deployment time, and if it is not finished in time it will be rolled back. Actors will handle the delay and execution of rollbacks to Controllers. 

A rollback will execute another bash script to hit puppet master to push an old version via a manifest.




Example to kick off a workflow:

Staging Distributed

"${path}"/stage_distributed jenkins-job-appName-stage somePuppetServer.com validationServiceIP:Port(127.0.0.1:9000) appName commaDelimitedListOfMachinesInstalledTo(ip1,ip2) expectedValue(a version number? 1.0) appNamesPortNUmber delayInSecondsToQueryMachines delayInSecondsToValidateMachines

Real Example:
~/staging/stage_distributed app1-stage puppet.foo.com 127.0.0.1:9000 app1 192.168.0.1,192.168.0.2 1.0.0-20130221-230300 8080 20 40
