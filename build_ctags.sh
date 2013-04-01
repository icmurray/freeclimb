#!/bin/sh

./extract-dependencies.sh

ctags -h ".scala" -R -f scalatags ./src
ctags -h ".scala.java" -a -R -f scalatags ./target/srcs
