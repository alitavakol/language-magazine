#!/bin/bash

# usage:
# generate-preview.sh <issues root path> <first issue number to include in zip> <last issue number to include in zip>

pushd $1
java -jar generate-issues-preview.jar --min-issue-number $2 --max-issue-number $3 -o issues-preview-$2-$3.zip
popd
