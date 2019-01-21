#!/bin/bash

for dir in target/dot/*
do
    for file in $dir/*.dot
    do
        echo $file
        filename="${file%.*}"
        dot -Tpng $file > $filename.png
    done
done