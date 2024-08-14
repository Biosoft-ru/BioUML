#!/bin/bash

FOLDER=/tmp/$2
rm -rf $FOLDER
deluser galaxy_$1
groupdel galaxy_$1