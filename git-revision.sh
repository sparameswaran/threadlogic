#!/bin/bash
#set -xv
revisioncount=`git log --oneline | wc -l  | tr -d ' '`
projectversion=`git describe --tags --long`
#cleanversion=${projectversion%*-*}
cleanversion1=${projectversion#*-*}
cleanversion=$(echo $cleanversion1 | sed -e 's/-.*//g' ) #projectversion#*-*}


#echo "$projectversion-$revisioncount"
echo "$cleanversion.$revisioncount"
