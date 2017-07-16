#!/bin/bash
set -ev
echo -e "Starting downloading dependencies\n"
cd $TRAVIS_BUILD_DIR
mkdir lib
curl -o lib/GeneralLib.jar https://api.spiget.org/v2/resources/25348/download
curl -o lib/Kingdoms.jar https://api.spiget.org/v2/resources/6392/download
curl -o lib/Residence.jar http://ltcraft.lt/Residence/files/Residence4.7.0.5.jar
curl -o lib/RedProtect.jar https://api.spiget.org/v2/resources/15841/download
echo -e "Downloading of dependencies finished!\n"