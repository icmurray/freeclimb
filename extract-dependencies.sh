#!/bin/bash

mkdir -p target/srcs
rm -rf target/srcs/*
find ~/.ivy2/cache -name '*-sources.jar' -print0 | xargs -0 cp -t ./target/srcs/
cd target/srcs
for f in *.jar
do
	mkdir "$f.dir"
	cd "$f.dir"
	jar -xf "../$f"
	cd ..
done
cd ../..

