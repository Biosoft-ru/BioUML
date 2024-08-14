#!/bin/bash

ssh biouml2test "cd /servers/biouml2; docker-compose -f /servers/biouml2/docker-compose.yaml stop bioumlweb"

if [ "$RM_CONTAINERS" = "true" ]
then
    ssh biouml2test "cd /servers/biouml2; docker-compose -f /servers/biouml2/docker-compose.yaml rm -f bioumlweb"
fi

ssh biouml2test 'rm -rf /servers/biouml2/plugins/*'

ssh biouml2test 'sudo rm -rfv /servers/biouml2/configuration/org.eclipse.core.runtime'
ssh biouml2test 'sudo rm -rfv /servers/biouml2/configuration/org.eclipse.equinox.app'
ssh biouml2test 'sudo rm -rfv /servers/biouml2/configuration/org.eclipse.osgi'
ssh biouml2test 'sudo rm -rfv /servers/biouml2/configuration/org.eclipse.update'

ssh biouml2test 'sudo rm -rf /servers/biouml2/docker.out/logs/*'

echo "Copying WARs to the production server..."
scp $WORKSPACE/BioUML_Server/biouml.war $WORKSPACE/BioUML_Server/bioumlweb.war biouml2test:/servers/biouml2/
echo "Copying plugins to the production server..."
scp -r $WORKSPACE/plugins biouml2test:/servers/biouml2/

if [ "$RM_CONTAINERS" = "true" ]
then
    ssh biouml2test "cd /servers/biouml2; docker-compose -f /servers/biouml2/docker-compose.yaml up -d"
else
    ssh biouml2test "cd /servers/biouml2; docker-compose -f /servers/biouml2/docker-compose.yaml start bioumlweb"
fi

sleep 10

echo Updating permissions of log files...
ssh biouml2test 'sudo chmod 777 /servers/biouml2/docker.out/logs/*'
