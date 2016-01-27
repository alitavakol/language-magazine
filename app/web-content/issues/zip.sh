#!/bin/bash

pushd $1
zip -r $2.zip $2
popd
