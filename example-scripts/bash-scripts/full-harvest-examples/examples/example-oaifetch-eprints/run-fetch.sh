#!/bin/bash
# EPrints OAI Harvest Script
source "$(dirname "$0")/../common/sharedlibraries/harvester-common.sh"

# Setup
setup_environment "EPrints-OAI-fetch"
setup_logging
clean_data

# Fetch phase
execute_fetch "org.vivoweb.harvester.fetch.OAIFetch" "fetch.conf.xml" "$2" "$4" "$5" "$6" "$7"

# Process phase
execute_translate "xsltranslator.config.xml" "$2"
execute_transfer "$2"

# Update phase
perform_diff "$2"
apply_changes_to_previous "$2"
apply_changes_to_vivo "$2" "$3"

# Report
count_imports
echo "EPrints OAI harvest completed successfully"
