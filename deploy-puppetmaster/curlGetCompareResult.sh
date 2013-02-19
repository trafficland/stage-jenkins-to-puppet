#!/bin/sh
#curl get result and compare it to expected
# return 0 true/Success (BASHWORLD) or 1 false (FAIL BASH)
resourceUrl=${1?need a url to get from}
expectedResult=${2?missing expected result} #if parameter contains spaces they should be enclosed in "" or ''

curl_command(){
  url=$1
  curl "$url"
}

res=$(curl_command "$resourceUrl")

[ "$expectedResult" == "$res" ]
returnVal=$?
echo $returnVal
exit $returnVal