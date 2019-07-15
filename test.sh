#!/bin/bash

HOME=
CGMES_HOME=
TEST_HOME=

for directory in `find ${CGMES_HOME}/* -type d`
do
	dirname=${directory##*/}
	#for file in `find ${directory}/* -type f`
	for file in `find ${directory}/* -name *1130*`
	do
	  filename=${file##*/}
	  mkdir ${TEST_HOME}/${dirname}_${filename%.*}
	  unzip -d ${TEST_HOME}/${dirname}_${filename%.*} $file
	  for sv in `find ${TEST_HOME}/${dirname}_${filename%.*} -name *SV*`
	  do
	  	  mkdir ${TEST_HOME}/${dirname}_${filename%.*}/1
	  	  cp ${sv} ${TEST_HOME}/${dirname}_${filename%.*}/1/.
	  	  svname=${sv##*/}
		  ./itools cgmes-loadflow --case-file $file --output-sv-folder ${TEST_HOME}/${dirname}_${filename%.*} --base-name ${svname%.*}
		  ./itools compare-sv --case-file $file --sv-folder ${TEST_HOME}/${dirname}_${filename%.*} --base-name ${svname%.*} --output-file ${TEST_HOME}/${dirname}_${filename%.*}/compare.csv
	  done
	done
done