#!/bin/bash
# Common functions for VIVO Harvester scripts
# Copyright (c) 2010-2025 VIVO Harvester Team

# Exit on first error
set -e

# Load method selection
load_method="${1:-sparql}"
if [[ "$load_method" != "" && "$load_method" != "tdb" && "$load_method" != "sparql" ]]; then
  echo "Invalid argument: $load_method"
  echo "Usage: $0 [tdb|sparql]"
  exit 1
fi

# Global variables
COMMON_CONFIG_DIRECTORY="$(dirname "$(dirname "$0")")/../common"
HARVESTER_INSTALL_DIR="$(pwd)/../../../../../../VIVO-Harvester"

# Function to setup environment
setup_environment() {
    local harvest_name="$1"
    export HARVEST_NAME="$harvest_name"
    export DATE=$(date +%Y-%m-%d'T'%T)

    # Add harvester binaries to path
    export PATH="$PATH:$HARVESTER_INSTALL_DIR/bin"
    export CLASSPATH="$CLASSPATH:$HARVESTER_INSTALL_DIR/bin/harvester.jar:$HARVESTER_INSTALL_DIR/bin/dependency/*"
    export CLASSPATH="$CLASSPATH:$HARVESTER_INSTALL_DIR/build/harvester.jar:$HARVESTER_INSTALL_DIR/build/dependency/*"
}

# Function to setup logging
setup_logging() {
    echo "Full Logging in $HARVEST_NAME.$DATE.log"
    mkdir -p logs
    cd logs
    touch "$HARVEST_NAME.$DATE.log"
    ln -sf "$HARVEST_NAME.$DATE.log" "$HARVEST_NAME.latest.log"
    cd ..
}

# Function to clean old data
clean_data() {
    if [ -d "data" ]; then
        rm -rf data
    fi
}

# Function to execute fetch
execute_fetch() {
    local fetch_tool="$1"
    local config_file="$2"
    local wordiness="${3:-INFO}"

    echo "Executing fetch: $fetch_tool"
    if [ ! -f "$config_file" ]; then
        echo "ERROR: Missing configuration file: $config_file"
        exit 1
    fi

    local cmd=(java $HARVESTER_JAVA_OPTS "$fetch_tool" -w "$wordiness" -X "$config_file")

    if [[ "$fetch_tool" == *JSONFetch ]]; then
        local url="$4"

        if [ -n "$url" ] && [ "$url" != "null" ]; then
            cmd+=(-u "$url")
        fi

        echo "Running: $(printf '%s ' "${cmd[@]}" | sed -E 's/(api_key=)[^&[:space:]]*/\1***/g')"
        "${cmd[@]}"
    else
        local url="$4"
        local start="$5"
        local end="$6"
        local setSpec="$7"

        if [ -n "$url" ] && [ "$url" != "null" ]; then
            cmd+=(-u "$url")
        fi

        if [ -n "$start" ] && [ "$start" != "null" ]; then
            cmd+=(-s "$start")
        fi

        if [ -n "$end" ] && [ "$end" != "null" ]; then
            cmd+=(-e "$end")
        fi

        if [ -n "$setSpec" ] && [ "$setSpec" != "null" ]; then
            cmd+=(-S "$setSpec")
        fi

        echo "Running: $(printf '%s ' "${cmd[@]}" | sed -E 's/(api_key=)[^&[:space:]]*/\1***/g')"
        "${cmd[@]}"
    fi
}

# Function to execute translate
execute_translate() {
    local config_file="${1:-xsltranslator.config.xml}"
    local wordiness="${2:-INFO}"

    echo "Executing translate"
    if [ ! -f "$config_file" ]; then
        echo "ERROR: Missing configuration file: $config_file"
        exit 1
    fi

    harvester-xsltranslator -w "$wordiness" -X "$config_file"
}

# Function to execute transfer
execute_transfer() {
    local wordiness="${1:-INFO}"

    local source_config="$COMMON_CONFIG_DIRECTORY/translation/translated-records.config.xml"
    local dest_model="$COMMON_CONFIG_DIRECTORY/harvestertransfer/harvested-data.model.xml"
    local dump_file="data/harvested-data/imported-records.rdf.xml"

    echo "Executing initial transfer to triple store"
    harvester-transfer -w "$wordiness" -s "$source_config" -o "$dest_model" -d "$dump_file"
}

# Function to perform diff operations
perform_diff() {
    local wordiness="${1:-INFO}"

    echo "Finding Subtractions"
    harvester-diff -w "$wordiness" -X "$COMMON_CONFIG_DIRECTORY/diff/diff-subtractions.config.xml"

    echo "Finding Additions"
    harvester-diff -w "$wordiness" -X "$COMMON_CONFIG_DIRECTORY/diff/diff-additions.config.xml"
}

# Function to apply changes to previous model
apply_changes_to_previous() {
    local previous_model="$COMMON_CONFIG_DIRECTORY/harvestertransfer/previous-harvest.model.xml"
    local wordiness="${1:-INFO}"

    echo "Applying Subtractions to Previous model"
    harvester-transfer -w "$wordiness" -o "$previous_model" -r "data/vivo-subtractions.rdf.xml" -m

    echo "Applying Additions to Previous model"
    harvester-transfer -w "$wordiness" -o "$previous_model" -r "data/vivo-additions.rdf.xml"
}

# Function to apply changes to VIVO
apply_changes_to_vivo() {
    local vivo_model="$COMMON_CONFIG_DIRECTORY/modelupdate/vivo.model.xml"
    local wordiness="${1:-INFO}"

    if [[ "$load_method" == "tdb" ]]; then
        echo "Applying Subtractions to VIVO model"
        harvester-transfer -w "$wordiness" -o "$vivo_model" -r "data/vivo-subtractions.rdf.xml" -m

        echo "Applying Additions to VIVO model"
        harvester-transfer -w "$wordiness" -o "$vivo_model" -r "data/vivo-additions.rdf.xml"
    else
        echo "Applying changes using SPARQL update"
        local model="$2"

        if [ -n "$model" ] && [ "$model" != "null" ]; then
            java $HARVESTER_JAVA_OPTS org.vivoweb.harvester.services.SparqlUpdate \
                         -X "$COMMON_CONFIG_DIRECTORY/modelupdate/sparqlupdate.conf.xml" -m "$model"
        else
            java $HARVESTER_JAVA_OPTS org.vivoweb.harvester.services.SparqlUpdate \
                         -X "$COMMON_CONFIG_DIRECTORY/modelupdate/sparqlupdate.conf.xml"
        fi
    fi
}

# Function to print some import statistics
count_imports() {
    local additions_file="data/vivo-additions.rdf.xml"

    if [ -f "$additions_file" ]; then
        local pubs=$(grep -c "oai" "$additions_file" 2>/dev/null || echo 0)
        pubs=${pubs//[^0-9]/}

        local authors=$(grep -c 'http://xmlns.com/foaf/0.1/Person' "$additions_file" 2>/dev/null || echo 0)
        authors=${authors//[^0-9]/}

        local authorships=$(grep -c "Authorship" "$additions_file" 2>/dev/null || echo 0)
        authorships=${authorships//[^0-9]/}

        local orgs=$(grep -c 'http://xmlns.com/foaf/0.1/Organization' "$additions_file" 2>/dev/null || echo 0)
        orgs=${orgs//[^0-9]/}

        local positions=$(grep -c "positionForPerson" "$additions_file" 2>/dev/null || echo 0)
        positions=${positions//[^0-9]/}

        echo "Import Statistics:"
        [ "$pubs" -gt 0 ] && echo "  - Publications: $pubs"
        [ "$authors" -gt 0 ] && echo "  - Authors: $authors"
        [ "$authorships" -gt 0 ] && echo "  - Authorships: $authorships"
        [ "$orgs" -gt 0 ] && echo "  - Organizations: $orgs"
        [ "$positions" -gt 0 ] && echo "  - Positions: $positions"
    else
        echo "No import data found"
    fi
}
