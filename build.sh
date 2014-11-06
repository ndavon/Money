#!/bin/sh

#change path if necessary
ANTLR=/usr/local/lib/antlr-4.2-complete.jar
#change path if necessary
JASMIN=./jasmin.jar

#generate visitors
java -jar $ANTLR Money.g4 -visitor 

#compile .java files to .class files
javac -cp $ANTLR:$JASMIN $(ls -a *.java)