#!/bin/bash
rm -Rf build/
mkdir build
cp -R ../../* build

docker build -t os2services:auladagtilbud .

rm -Rf build/
