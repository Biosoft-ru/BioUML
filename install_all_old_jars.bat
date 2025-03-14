@echo off

:: Define arrays for owl_jars and cdk_jars
setlocal enabledelayedexpansion
set "owl_jars=api apibinding impl util rdfxmlrenderer"
set "cdk_jars=interfaces data core extra io ioformats nonotify isomorphism smiles smarts pdb standard fingerprint"

:: Iterate over owl_jars and execute mvn command for each element
for %%j in (%owl_jars%) do (
    mvn -N install:install-file ^
      -DgroupId=owlapi ^
      -DartifactId=%%j ^
      -Dversion=1.0 ^
      -Dpackaging=jar ^
      -Dfile=plugins/org.semanticweb.owl_2.1.0/owlapi-%%j.jar ^
      -DgeneratePom=true
)

:: Iterate over cdk_jars and execute mvn command for each element
for %%j in (%cdk_jars%) do (
    mvn -N install:install-file ^
      -DgroupId=org.openscience.cdk ^
      -DartifactId=cdk-%%j ^
      -Dversion=1.3.5 ^
      -Dpackaging=jar ^
      -Dfile=plugins/org.openscience.cdk_1.3.5/cdk-%%j.jar ^
      -DgeneratePom=true
)

:: Install individual jars
mvn -N install:install-file ^
  -DgroupId=it.sauronsoftware ^
  -DartifactId=ftp4j ^
  -Dversion=1.6 ^
  -Dpackaging=jar ^
  -Dfile=plugins/it.sauronsoftware.ftp4j_1.6.jar ^
  -DgeneratePom=true

mvn -N install:install-file ^
  -DgroupId=net.sf.samtools ^
  -DartifactId=samtools ^
  -Dversion=1.62 ^
  -Dpackaging=jar ^
  -Dfile=plugins/net.sf.samtools_1.62.jar ^
  -DgeneratePom=true

mvn -N install:install-file ^
  -DgroupId=org.gnu.glpk ^
  -DartifactId=glpk-java ^
  -Dversion=1.12.0 ^
  -Dpackaging=jar ^
  -Dfile=plugins/glpk/glpk-java-1.12.0.jar ^
  -DgeneratePom=true

mvn -N install:install-file ^
  -DgroupId=gurobi ^
  -DartifactId=gurobi ^
  -Dversion=6.0.5 ^
  -Dpackaging=jar ^
  -Dfile=plugins/gurobi/gurobi.jar ^
  -DgeneratePom=true

mvn -N install:install-file ^
  -DgroupId=sabioclient ^
  -DartifactId=sabioclient ^
  -Dversion=1.0 ^
  -Dpackaging=jar ^
  -Dfile=plugconfig/biouml.plugins.sabiork/sabioclient.jar ^
  -DgeneratePom=true

mvn -N install:install-file ^
  -DgroupId=chipmunk ^
  -DartifactId=chipmunk ^
  -Dversion=1.0 ^
  -Dpackaging=jar ^
  -Dfile=plugconfig/biouml.plugins.chipmunk/chipmunk.jar ^
  -DgeneratePom=true

mvn -N install:install-file ^
  -DgroupId=org.ensembl ^
  -DartifactId=ensj ^
  -Dversion=39.2 ^
  -Dpackaging=jar ^
  -Dfile=plugins/org.ensembl.ensj_39.2/ensj-39.2.jar ^
  -DgeneratePom=true

mvn -N install:install-file ^
  -DgroupId=jdbm ^
  -DartifactId=jdbm ^
  -Dversion=2.0 ^
  -Dpackaging=jar ^
  -Dfile=plugins/jdbm_2.0.jar ^
  -DgeneratePom=true

mvn -N install:install-file ^
  -DgroupId=org.openscience.jchempaint ^
  -DartifactId=jchempaint ^
  -Dversion=3.1.2 ^
  -Dpackaging=jar ^
  -Dfile=plugins/org.openscience.jchempaint_3.1.2/jchempaint-3.1.2.jar ^
  -DgeneratePom=true

mvn -N install:install-file ^
  -DgroupId=org.openscience.jchempaint ^
  -DartifactId=cdk-jchempaint ^
  -Dversion=1.3.5 ^
  -Dpackaging=jar ^
  -Dfile=plugins/org.openscience.jchempaint_3.1.2/cdk-jchempaint-15.jar ^
  -DgeneratePom=true

mvn -N install:install-file ^
  -DgroupId=jivesoftware ^
  -DartifactId=smack ^
  -Dversion=1.0.0 ^
  -Dpackaging=jar ^
  -Dfile=plugins/org.jivesoftware.smack_1.0.0/smack.jar ^
  -DgeneratePom=true

mvn -N install:install-file ^
  -DgroupId=jivesoftware ^
  -DartifactId=smackx ^
  -Dversion=1.0.0 ^
  -Dpackaging=jar ^
  -Dfile=plugins/org.jivesoftware.smack_1.0.0/smackx.jar ^
  -DgeneratePom=true

mvn -N install:install-file ^
  -DgroupId=jgraph ^
  -DartifactId=jgraph ^
  -Dversion=5.1 ^
  -Dpackaging=jar ^
  -Dfile=plugins/org.jgraph.jgraph_5.1/jgraph.jar ^
  -DgeneratePom=true

mvn -N install:install-file ^
  -DgroupId=org.jlibsedml ^
  -DartifactId=jlibsedml ^
  -Dversion=2.2.1 ^
  -Dpackaging=jar ^
  -Dfile=plugins/org.jlibsedml_2.2.1/jlibsedml.jar ^
  -DgeneratePom=true

mvn -N install:install-file ^
  -DgroupId=org.jetbrains.bio ^
  -DartifactId=big ^
  -Dversion=0.9.1p6-patched ^
  -Dpackaging=jar ^
  -Dfile=plugins/org.jetbrains.bio.big_0.9.1/big-0.9.1p6.jar ^
  -DgeneratePom=true

mvn -N install:install-file ^
  -DgroupId=com.github.samtools ^
  -DartifactId=htsjdk ^
  -Dversion=2.20.3-4-g87ac4d3-SNAPSHOT ^
  -Dpackaging=jar ^
  -Dfile=plugins/htsjdk_2.24.1/htsjdk-2.24.1.jar ^
  -DgeneratePom=true

mvn -N install:install-file ^
  -DgroupId=io.github.spencerpark ^
  -DartifactId=jupyter ^
  -Dversion=2.3.0 ^
  -Dpackaging=jar ^
  -Dfile=plugconfig/biouml.plugins.node/jupyter-jvm-basekernel-2.3.0.jar ^
  -DgeneratePom=true

mvn -N install:install-file ^
  -DgroupId=gnu.trove ^
  -DartifactId=trove ^
  -Dversion=3.0.3p1 ^
  -Dpackaging=jar ^
  -Dfile=plugins/gnu.trove_3.0.3.jar ^
  -DgeneratePom=true

mvn -N install:install-file ^
  -DgroupId=ru.biosoft.physicell ^
  -DartifactId=physicell ^
  -Dversion=1.0-SNAPSHOT ^
  -Dpackaging=jar ^
  -Dfile=plugins/ru.biosoft.physicell_0.9.10.jar ^
  -DgeneratePom=true

mvn -N install:install-file ^
  -DgroupId=com.twosigma ^
  -DartifactId=beakerx ^
  -Dversion=1.0 ^
  -Dpackaging=jar ^
  -Dfile=plugconfig/biouml.plugins.beakerx/beakerx-kernel-base-1.0.jar ^
  -DgeneratePom=true

mvn -N install:install-file ^
  -DgroupId=org.apache.batik ^
  -DartifactId=batik-all ^
  -Dversion=1.7.0 ^
  -Dpackaging=jar ^
  -Dfile=plugins/org.apache.batik_1.7/batik-all.jar ^
  -DgeneratePom=true

mvn -N install:install-file ^
  -DgroupId=org.w3c ^
  -DartifactId=xml-apis-ext ^
  -Dversion=1.0.0 ^
  -Dpackaging=jar ^
  -Dfile=plugins/org.apache.batik_1.7/xml-apis-ext.jar ^
  -DgeneratePom=true

endlocal