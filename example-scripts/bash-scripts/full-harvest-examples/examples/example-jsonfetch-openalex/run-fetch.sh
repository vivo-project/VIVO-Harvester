#!/bin/bash

source "$(dirname "$0")/../common/sharedlibraries/harvester-common.sh"

# Setup
setup_environment "OpenAlex-Harvest"
setup_logging
clean_data

# Fetch phase
execute_fetch "org.vivoweb.harvester.fetch.JSONFetch" "fetch.config.xml" "$2" "$4"

# Process phase
execute_translate "xsltranslator.config.xml" "$2"
execute_transfer "$2"

# Update phase
perform_diff "$2"
apply_changes_to_previous "$2"
apply_changes_to_vivo "$2" "$3"

# Report
count_imports
echo "OpenAlex harvest completed successfully"
