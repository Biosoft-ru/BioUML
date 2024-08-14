#!/bin/bash

export JAVA_HOME=/var/lib/hudson/openlogic-openjdk-17.0.12+7-linux-x64

export ANT_ARGS="-quiet -emacs"

if [ "$MYSQL5" = "true" ]
then
  cd $WORKSPACE

  grep --include=MANIFEST.MF -lrw plugconfig -e 'com\.mysql' | xargs -d '\n' sed -i 's/com\.mysql\.cj/com.mysql.jdbc/g' 
  grep --include=MANIFEST.MF -lrw plugins -e 'com\.mysql' | xargs -d '\n' sed -i 's/com\.mysql\.cj/com.mysql.jdbc/g' 

  cd $WORKSPACE/plugins

  rm -f com.mysql.jdbc_8.0.25.jar
  cp ../server_config/com.mysql.jdbc_5.1.18.jar .
else
  cd $WORKSPACE

  grep --include=MANIFEST.MF -lrw plugconfig -e 'com\.mysql' | xargs -d '\n' sed -i 's/com\.mysql\.jdbc/com.mysql.cj/g' 
  grep --include=MANIFEST.MF -lrw plugins -e 'com\.mysql' | xargs -d '\n' sed -i 's/com\.mysql\.jdbc/com.mysql.cj/g' 

  cd $WORKSPACE/plugins

  rm -f com.mysql.jdbc_5.1.18.jar
  cp ../server_config/com.mysql.jdbc_8.0.25.jar .
fi

cd $WORKSPACE/src

mkdir -p $WORKSPACE/BioUML_Server/build

git checkout -- ru/biosoft/server/servlets/webservices/webfiles/defines.js
git checkout -- ru/biosoft/server/servlets/webservices/webfiles/lib/messageBundle.js

echo ""
echo "Files modification for biouml2test"
echo ""
sed -i -e '/serverAddress: /s/localhost/biouml2 Test/g' ./ru/biosoft/server/servlets/webservices/webfiles/defines.js
#sed -i -e "/version: /s/[0-9][0-9][0-9][0-9]\.[0-9]\"\,/\(`date +'%Y-%m-%d'`\)\"\,/g" ./ru/biosoft/server/servlets/webservices/webfiles/defines.js
sed -i -e "/version: /s/\"\,/ \(`date +'%Y-%m-%d'`\)\"\,/g" ./ru/biosoft/server/servlets/webservices/webfiles/defines.js

ant -DDEPLOY_DIR=$WORKSPACE/BioUML_Server/build -DSERVER_PATH=$WORKSPACE/BioUML_Server -DWEB_XML_SERVER_PATH=/BioUML_Server \
   clean biouml.war 
ant -DDEPLOY_DIR=$WORKSPACE/BioUML_Server/build -DSERVER_PATH=$WORKSPACE/BioUML_Server -DWEB_XML_SERVER_PATH=/BioUML_Server \
   biouml.webserver


echo "Removing unnecessary plugins..."
cd $WORKSPACE/plugins
rm -r biouml.plugins.cellml_*
rm -r biouml.plugins.jupyter_*
