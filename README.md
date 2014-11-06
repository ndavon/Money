Money
=====

Money is both a language and a compiler.

Money was a project for my formal languages and compiler construction class.
The goal was to design a programming language's grammar and write a compiler for it. 

Money uses [ANTLR](http://www.antlr.org) and [Jasmin](http://jasmin.sourceforge.net) for parsing and translating [JVM instructions](http://jasmin.sourceforge.net/instructions.html) to Java bytecode. 

Installation
=====

Installation is pretty easy and it runs almost out of the box.
Make sure you have antlr 4.2 installed (I haven't checked if more recent versions work without any adjustments) and set the path to its .jar in build.sh and compile.sh. Oh, and btw: You can download antlr 4.2 [here](http://www.antlr.org/download/antlr-4.2-complete.jar). Just move it to /usr/local/lib/.
You'll also need to download [jasmin](http://sourceforge.net/projects/jasmin/files/jasmin/jasmin-2.4/) and adjust the path to jasmin.jar in build.sh and compile.sh

Usage
=====

Execute build.sh to build the required visitors and class files.
Now you can execute compile.sh with two arguments: 
* input file, e.g. sample.money
* true or false, depending on whether you want to have debug symbols etc.
This will execute the generated class file (given that there aren't any errors!)

Other
=====

I really hope somebody can actually learn something from this project, however it's been six months since completion and I don't really intend on maintaining this project furthermore, sorry.

Have fun! 
