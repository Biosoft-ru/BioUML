#!/bin/bash

# Packages missing in Maven Central

owl_jars=("api" "apibinding" "impl" "util" "rdfxmlrenderer")

# Iterate over the list and execute a command for each element
for jar in "${owl_jars[@]}"; do
   mvn -N install:install-file \
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
   mvn -N install:install-file \
     -DgroupId=org.openscience.cdk \
     -DartifactId=cdk-$jar \
     -Dversion=1.3.5 \
     -Dpackaging=jar \
     -Dfile=plugins/org.openscience.cdk_1.3.5/cdk-$jar.jar \
     -DgeneratePom=true
done

## it.sauronsoftware.ftp4j

mvn -N install:install-file \
  -DgroupId=it.sauronsoftware \
  -DartifactId=ftp4j \
  -Dversion=1.6 \
  -Dpackaging=jar \
  -Dfile=plugins/it.sauronsoftware.ftp4j_1.6.jar \
  -DgeneratePom=true 

## org.gnu.glpk

mvn -N install:install-file \
  -DgroupId=org.gnu.glpk \
  -DartifactId=glpk-java \
  -Dversion=1.12.0 \
  -Dpackaging=jar \
  -Dfile=plugins/glpk/glpk-java-1.12.0.jar \
  -DgeneratePom=true 

## gurobi

mvn -N install:install-file \
  -DgroupId=gurobi \
  -DartifactId=gurobi \
  -Dversion=6.0.5 \
  -Dpackaging=jar \
  -Dfile=plugins/gurobi/gurobi.jar  \
  -DgeneratePom=true

## sabioclient

mvn -N install:install-file \
  -DgroupId=sabioclient \
  -DartifactId=sabioclient \
  -Dversion=1.0 \
  -Dpackaging=jar \
  -Dfile=plugconfig/biouml.plugins.sabiork/sabioclient.jar \
  -DgeneratePom=true

## chipmunk

mvn -N install:install-file \
  -DgroupId=chipmunk \
  -DartifactId=chipmunk \
  -Dversion=1.0 \
  -Dpackaging=jar \
  -Dfile=plugconfig/biouml.plugins.chipmunk/chipmunk.jar \
  -DgeneratePom=true

## jdbm

mvn -N install:install-file \
  -DgroupId=jdbm \
  -DartifactId=jdbm \
  -Dversion=2.0 \
  -Dpackaging=jar \
  -Dfile=plugins/jdbm_2.0.jar \
  -DgeneratePom=true

## org.openscience.jchempaint

mvn -N install:install-file \
  -DgroupId=org.openscience.jchempaint \
  -DartifactId=jchempaint \
  -Dversion=3.1.2 \
  -Dpackaging=jar \
  -Dfile=plugins/org.openscience.jchempaint_3.1.2/jchempaint-3.1.2.jar \
  -DgeneratePom=true

mvn -N install:install-file \
  -DgroupId=org.openscience.jchempaint \
  -DartifactId=cdk-jchempaint \
  -Dversion=1.3.5 \
  -Dpackaging=jar \
  -Dfile=plugins/org.openscience.jchempaint_3.1.2/cdk-jchempaint-15.jar \
  -DgeneratePom=true

## smack

mvn -N install:install-file \
  -DgroupId=jivesoftware \
  -DartifactId=smack \
  -Dversion=1.0.0 \
  -Dpackaging=jar \
  -Dfile=plugins/org.jivesoftware.smack_1.0.0/smack.jar \
  -DgeneratePom=true

mvn -N install:install-file \
  -DgroupId=jivesoftware \
  -DartifactId=smackx \
  -Dversion=1.0.0 \
  -Dpackaging=jar \
  -Dfile=plugins/org.jivesoftware.smack_1.0.0/smackx.jar \
  -DgeneratePom=true

## jgraph

mvn -N install:install-file \
  -DgroupId=jgraph \
  -DartifactId=jgraph \
  -Dversion=5.1 \
  -Dpackaging=jar \
  -Dfile=plugins/org.jgraph.jgraph_5.1/jgraph.jar \
  -DgeneratePom=true

## jlibsedml

mvn -N install:install-file \
  -DgroupId=org.jlibsedml \
  -DartifactId=jlibsedml \
  -Dversion=2.2.1 \
  -Dpackaging=jar \
  -Dfile=plugins/org.jlibsedml_2.2.1/jlibsedml.jar \
  -DgeneratePom=true

## htsjdk

mvn -N install:install-file \
  -DgroupId=com.github.samtools \
  -DartifactId=htsjdk \
  -Dversion=4.3.0 \
  -Dpackaging=jar \
  -Dfile=plugins/htsjdk_4.3.0/htsjdk-4.3.0.jar \
  -DgeneratePom=true

## io.github.spencerpark

mvn -N install:install-file \
  -DgroupId=io.github.spencerpark \
  -DartifactId=jupyter \
  -Dversion=2.3.0 \
  -Dpackaging=jar \
  -Dfile=plugconfig/biouml.plugins.node/jupyter-jvm-basekernel-2.3.0.jar \
  -DgeneratePom=true

## GNU trove

mvn -N install:install-file \
  -DgroupId=gnu.trove \
  -DartifactId=trove \
  -Dversion=3.0.3p1 \
  -Dpackaging=jar \
  -Dfile=plugins/gnu.trove_3.0.3.jar \
  -DgeneratePom=true

## JPhysiCell 

mvn -N install:install-file \
  -DgroupId=ru.biosoft.physicell \
  -DartifactId=physicell \
  -Dversion=1.0-SNAPSHOT \
  -Dpackaging=jar \
  -Dfile=plugins/ru.biosoft.physicell_2025.2.jar \
  -DgeneratePom=true

## BeakerX

mvn -N install:install-file \
  -DgroupId=com.twosigma \
  -DartifactId=beakerx \
  -Dversion=1.0 \
  -Dpackaging=jar \
  -Dfile=plugconfig/biouml.plugins.beakerx/beakerx-kernel-base-1.0.jar \
  -DgeneratePom=true

## SBOL 

#mvn -N install:install-file \
#  -DgroupId=org.sbolstandard \
#  -DartifactId=libSBOLj \
#  -Dversion=2.4.0 \
#  -Dpackaging=jar \
#  -Dfile=plugins/org.sbolstandard.core2_4.0/libSBOLj-2.4.0-withDependencies.jar \
#  -DgeneratePom=true

mvn -N install:install-file \
  -DgroupId=org.sbolstandard \
  -DartifactId=sbol-data-core \
  -Dversion=0.2.0-SNAPSHOT \
  -Dpackaging=jar \
  -Dfile=plugins/org.sbolstandard.core2_4.0/sbol-data-core-0.2.0-SNAPSHOT.jar \
  -DgeneratePom=true
  
mvn -N install:install-file \
  -DgroupId=org.sbolstandard \
  -DartifactId=sbol-data-examples \
  -Dversion=0.2.0-SNAPSHOT \
  -Dpackaging=jar \
  -Dfile=plugins/org.sbolstandard.core2_4.0/sbol-data-examples-0.2.0-SNAPSHOT.jar \
  -DgeneratePom=true
  
mvn -N install:install-file \
  -DgroupId=org.sbolstandard \
  -DartifactId=sbol-data-io-RDF \
  -Dversion=0.2.0-SNAPSHOT \
  -Dpackaging=jar \
  -Dfile=plugins/org.sbolstandard.core2_4.0/sbol-data-io-RDF-0.2.0-SNAPSHOT.jar \
  -DgeneratePom=true
  
mvn -N install:install-file \
  -DgroupId=org.sbolstandard \
  -DartifactId=sbol-data-nativeSbol \
  -Dversion=0.2.0-SNAPSHOT \
  -Dpackaging=jar \
  -Dfile=plugins/org.sbolstandard.core2_4.0/sbol-data-nativeSbol-0.2.0-SNAPSHOT.jar \
  -DgeneratePom=true

mvn -N install:install-file \
  -DgroupId=org.apache.batik \
  -DartifactId=batik-all \
  -Dversion=1.7.0 \
  -Dpackaging=jar \
  -Dfile=plugins/org.apache.batik_1.7/batik-all.jar \
  -DgeneratePom=true

mvn -N install:install-file \
  -DgroupId=org.w3c \
  -DartifactId=xml-apis-ext \
  -Dversion=1.0.0 \
  -Dpackaging=jar \
  -Dfile=plugins/org.apache.batik_1.7/xml-apis-ext.jar \
  -DgeneratePom=true


mvn -N install:install-file \
  -DgroupId=javastraw \
  -DartifactId=javastraw \
  -Dversion=1.0.0 \
  -Dpackaging=jar \
  -Dfile=plugins/javastraw_1/javastraw.jar \
  -DgeneratePom=true
