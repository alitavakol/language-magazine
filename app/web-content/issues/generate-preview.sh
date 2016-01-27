#!/bin/bash

pushd $1
java -jar cem-zip-issues-folder.jar --min-issue-number $2 --max-issue-number $3 -o issues-preview-$2-$3.zip
popd
