#!/bin/sh

# run with input file and true or false for debug information as arguments

# change path if necessary
ANTLR=/usr/local/lib/antlr-4.2-complete.jar
# change path if necessary
JASMIN=./jasmin.jar

java -cp $ANTLR:$JASMIN:. Main $1 $2