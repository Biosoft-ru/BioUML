#!/bin/bash

owl_jars=("api" "apibinding" "impl" "util" "rdfxmlrenderer")

# Iterate over the list and execute a command for each element
for jar in "${owl_jars[@]}"; do
   mvn install:install-file \
     -DgroupId=owlapi \
     -DartifactId=$jar \
     -Dversion=1.0 \
     -Dpackaging=jar \
     -Dfile=plugins/org.semanticweb.owl_2.1.0/owlapi-$jar.jar \
     -DgeneratePom=true
done

cdk_jars=("interfaces" "data" "core" "extra" "io" "ioformats" "nonotify" "isomorphism" "smiles" "smarts" "pdb" "standard" "fingerprint")

# Iterate over the list and execute a command for each element
for jar in "${cdk_jars[@]}"; do
   mvn install:install-file \
     -DgroupId=org.openscience.cdk \
     -DartifactId=cdk-$jar \
     -Dversion=1.3.5 \
     -Dpackaging=jar \
     -Dfile=plugins/org.openscience.cdk_1.3.5/cdk-$jar.jar \
     -DgeneratePom=true
done

