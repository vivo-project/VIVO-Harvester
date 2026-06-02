#!/bin/bash
mvn clean dependency:copy-dependencies package -DskipTests=true
#check for failure
sudo dpkg -i build/harvester*.deb
