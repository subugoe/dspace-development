#!/bin/sh

# Shell script to copy message catalogue files of the form messages*.xml
# from DSpace installation directory to DSpace source directory to be committed
# via Git (the Git command not included in this script)

# Specify DSpace installation directory and DSpace source directory here
# Path to DSpace installation directory
DSPACE_DIR="/srv/dspace"
# Path to DSpace source directory
DSPACE_SRC="/srv/dspace-src"

COPY_TO_DIR="$DSPACE_SRC/dspace/modules/xmlui/src/main/webapp/i18n/"
FILES_TO_COPY="$DSPACE_DIR/webapps/xmlui/i18n/messages*.xml"

if [ ! -d $COPY_TO_DIR ]
then
    mkdir $COPY_TO_DIR
fi

cp $FILES_TO_COPY $COPY_TO_DIR
