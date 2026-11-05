@echo off
setlocal enabledelayedexpansion
echo [START] EPrints OAI Harvest Script

REM =============================================================
REM Load method validation
REM =============================================================
call "%~dp0..\common\sharedlibraries\harvester-common.bat" validate_load_method %1
if errorlevel 1 (
    echo [ERROR] Invalid load method specified.
    exit /b 1
)
echo [INFO] Load method detected: %LOAD_METHOD%

REM =============================================================
REM Configuration
REM =============================================================
set "COMMON_CONFIG_DIRECTORY=..\common"
set "HARVEST_NAME=EPrints-OAI-fetch"
set "FETCH_CLASS=org.vivoweb.harvester.fetch.OAIFetch"
set "FETCH_CONFIG=fetch.conf.xml"

REM =============================================================
REM Initialize environment
REM =============================================================
echo [STEP 1] Initializing environment...
call "%~dp0..\common\sharedlibraries\harvester-common.bat" init_environment "%HARVEST_NAME%"
if errorlevel 1 exit /b 1

REM =============================================================
REM Setup
REM =============================================================
echo [STEP 2] Setting up logging and cleaning data...
call "%~dp0..\common\sharedlibraries\harvester-common.bat" setup_logging
call "%~dp0..\common\sharedlibraries\harvester-common.bat" clean_data

REM =============================================================
REM Fetch
REM =============================================================
echo [STEP 3] Fetching from EPrints OAI...
call "%~dp0..\common\sharedlibraries\harvester-common.bat" execute_fetch "%FETCH_CLASS%" "%FETCH_CONFIG%"
if errorlevel 1 exit /b 1

REM =============================================================
REM Translate
REM =============================================================
echo [STEP 4] Translating fetched XML to RDF...
call "%~dp0..\common\sharedlibraries\harvester-common.bat" execute_translate
if errorlevel 1 exit /b 1

REM =============================================================
REM Execute Transfer
REM =============================================================
echo [STEP 5] Transferring translated RDF...
call "%~dp0..\common\sharedlibraries\harvester-common.bat" execute_transfer "%COMMON_CONFIG_DIRECTORY%"
if errorlevel 1 exit /b %errorlevel%

REM =============================================================
REM Perform Update
REM =============================================================
echo [STEP 6] Performing diff and applying updates...
call "%~dp0..\common\sharedlibraries\harvester-common.bat" perform_diff "%COMMON_CONFIG_DIRECTORY%"
if errorlevel 1 exit /b %errorlevel%

call "%~dp0..\common\sharedlibraries\harvester-common.bat" apply_changes_to_previous "%COMMON_CONFIG_DIRECTORY%"
if errorlevel 1 exit /b %errorlevel%

call "%~dp0..\common\sharedlibraries\harvester-common.bat" apply_changes_to_vivo "%LOAD_METHOD%" "%COMMON_CONFIG_DIRECTORY%"
if errorlevel 1 exit /b %errorlevel%

REM =============================================================
REM Report
REM =============================================================
echo [STEP 7] Counting imports...
call "%~dp0..\common\sharedlibraries\harvester-common.bat" count_imports

echo.
echo [SUCCESS] EPrints OAI harvest completed successfully.
endlocal
exit /b 0
