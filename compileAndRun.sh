#!/bin/bash
find src -name '*.java' > sources.list
mkdir -p out
javac @sources.list -d out && java -cp out:resources com.codecool.klondike.Klondike
