#!/bin/sh
buildDir="tmpBuild"
classPath="import/gson-2.10.1.jar:import/commons-lang3-3.14.0.jar:import/commons-text-1.11.0.jar"

mkdir $buildDir

# Compile java files
javac -d "$buildDir" -sourcepath . -cp "$classPath" *.java

# Copy resources
mkdir $buildDir/resources
mkdir $buildDir/import
cp -r resources/* $buildDir/resources
cp -r import/* $buildDir/import

# Extract dependencies
cd $buildDir
for f in import/*.jar; do
  unzip -o $f
done
# Delete useless directories
rm -rf META-INF import

# Copy manifests
mkdir manifests
cp -r ../manifests/* manifests/

# Create JAR files
mkdir ../build
for d in manifests/*; do
  jar cmf "$d/META-INF/MANIFEST.MF" "../build/$(basename $d).jar" .
done

# Clean build directory
cd ..
rm -rf $buildDir
