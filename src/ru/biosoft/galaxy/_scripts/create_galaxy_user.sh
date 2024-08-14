#!/bin/bash

USER=galaxy_$1
useradd $USER

FOLDER=/tmp/$2
mkdir $FOLDER
chown $USER:$USER $FOLDER
chmod 777 $FOLDER
