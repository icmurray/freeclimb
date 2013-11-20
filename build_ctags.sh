#!/bin/sh

./extract-dependencies.sh

ctags -h ".scala" -R -f scalatags ./core/src
ctags -h ".scala" -R -f scalatags ./api/src
ctags -h ".scala.java" -a -R -f scalatags ./target/srcs
