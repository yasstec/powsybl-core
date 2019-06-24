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
	  	  svname=${sv##*/}
		  ./itools cgmes-loadflow --case-file $file --output-sv-file ${TEST_HOME}/${dirname}_${filename%.*}/${svname%.*}
		  ./itools compare-sv --sv-folder ${TEST_HOME}/${dirname}_${filename%.*} --base-name ${svname%.*} --output-file ${TEST_HOME}/${dirname}_${filename%.*}/compare.csv
	  done
	done
done