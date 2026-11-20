#!/bin/bash
# DSpace OAI Harvest Script
source "$(dirname "$0")/../common/sharedlibraries/harvester-common.sh"

# Setup
setup_environment "DSpace-OAI-fetch"
setup_logging
clean_data

# Fetch phase
execute_fetch "org.vivoweb.harvester.fetch.OAIFetch" "fetch.conf.xml" "$2" "$4" "$5" "$6" "$7"

# Process phase
execute_translate "xsltranslator.config.xml" "$2"
execute_transfer "$2"

# Optional: Scoring and matching (commented out by default)
# echo "Scoring publications..."
#harvester-score -X score-author.conf.xml
#harvester-score -X score-publisher.conf.xml

#harvester-score -X score-interval.conf.xml
#harvester-score -X score-datetime.conf.xml

#Match
# Rename matches scored above.
#harvester-match -X match-main.conf.xml

#Score on authorships
# Same as above, but for authorships.
#harvester-score -X score-authorship.conf.xml

#Match
# Rename matching authorships.
#harvester-match -X match-authorship.conf.xml

#Truncate Score Data model
#harvester-jenaconnect -X truncate.config.xml

# Execute ChangeNamespace to get unmatched publications into current namespace
# This is where the new people from the harvest are given uris within the namespace of Vivo
# 	If there is an issue with uris being in another namespace after import, make sure this step
#   was completed for those uris.
#echo "changenamespace"
#harvester-changenamespace -X changenamespace-all.config.xml

# harvester-jenaconnect -X harvested-dump.config.xml

# Update phase
perform_diff "$2"
apply_changes_to_previous "$2"
apply_changes_to_vivo "$2" "$3"

# Report
count_imports
echo "DSpace OAI harvest completed successfully"
