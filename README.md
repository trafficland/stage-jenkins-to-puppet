Main ruby scripts in this project are expected to be executed from a Jenkins Job via a Post Build Task. This is posible via the Jenkins Post Build Task plugin.

The script is to take the project name (first argument) and find the latest packaged version of the that application. Once found the package will be copied to the correct staging location. All permissions are to be dealt with prior to running the application by setting public and private ssh keys.
