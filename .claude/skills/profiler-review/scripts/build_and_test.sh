#!/bin/bash
export JAVA_HOME=/home/zha/.sdkman/candidates/java/21.0.6-tem
export PATH=$JAVA_HOME/bin:$PATH
cd /home/zha/github/BioUML
echo "Java version:"
java -version 2>&1
echo ""
echo "Maven compile:"
mvn package -DskipTests -pl src -q 2>&1 | tail -30
echo ""
echo "Ant compile:"
cd src
ant compile 2>&1 | tail -20
