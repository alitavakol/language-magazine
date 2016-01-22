#!/bin/bash

pushd "/home/hamed/Desktop/cem-data/issues"
java -jar cem-zip-issues-folder.jar --min-issue-number $1 --max-issue-number $2 -o issues-preview-$1-$2.zip
popd
