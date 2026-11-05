@echo off
setlocal enabledelayedexpansion
echo [START] OpenAlex Harvest Script

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
set "HARVEST_NAME=OpenAlex-Harvest"
set "FETCH_CLASS=org.vivoweb.harvester.fetch.JSONFetch"
set "FETCH_CONFIG=fetch.config.xml"
set "USE_JSONFETCH=true"

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
REM Fetch (JSONFetch)
REM =============================================================
echo [STEP 3] Fetching from OpenAlex...

if not exist "%FETCH_CONFIG%" (
    echo [ERROR] Missing configuration file: %FETCH_CONFIG%
    exit /b 1
)

echo Executing JSONFetch...
java %HARVESTER_JAVA_OPTS% -cp "%CLASSPATH%" %FETCH_CLASS% -X "%FETCH_CONFIG%"
if %errorlevel% neq 0 exit /b %errorlevel%

REM =============================================================
REM Translate
REM =============================================================
echo [STEP 4] Translating fetched JSON to RDF...
call "%~dp0..\common\sharedlibraries\harvester-common.bat" execute_translate
if errorlevel 1 exit /b %errorlevel%

REM =============================================================
REM Execute Transfer
REM =============================================================
echo [STEP 5] Transferring translated RDF...
call "%~dp0..\common\sharedlibraries\harvester-common.bat" execute_transfer "%COMMON_CONFIG_DIRECTORY%"
if errorlevel 1 exit /b %errorlevel%

REM =============================================================
REM Perform Update (Diff)
REM =============================================================
echo [STEP 6] Performing diff and applying updates...
call "%~dp0..\common\sharedlibraries\harvester-common.bat" perform_diff "%COMMON_CONFIG_DIRECTORY%"
if errorlevel 1 exit /b %errorlevel%

call "%~dp0..\common\sharedlibraries\harvester-common.bat" apply_changes_to_previous "%COMMON_CONFIG_DIRECTORY%"
if errorlevel 1 exit /b %errorlevel%

call "%~dp0..\common\sharedlibraries\harvester-common.bat" apply_changes_to_vivo "%LOAD_METHOD%" "%COMMON_CONFIG_DIRECTORY%"
if errorlevel 1 exit /b %errorlevel%

REM =============================================================
REM Report - OpenAlex-specific counters
REM =============================================================
echo [STEP 7] Counting OpenAlex imports...

set /a ORGS=0
set /a PEOPLE=0
set /a POSITIONS=0

if exist data\vivo-additions.rdf.xml (

    for /f "delims=" %%i in ('find /c "http://xmlns.com/foaf/0.1/Organization" ^< data\vivo-additions.rdf.xml') do set /a ORGS=%%i
    for /f "delims=" %%i in ('find /c "http://xmlns.com/foaf/0.1/Person" ^< data\vivo-additions.rdf.xml') do set /a PEOPLE=%%i
    for /f "delims=" %%i in ('find /c "positionForPerson" ^< data\vivo-additions.rdf.xml') do set /a POSITIONS=%%i

    echo OpenAlex Import Statistics:
    if !ORGS!     GTR 0 echo   - Organizations: !ORGS!
    if !PEOPLE!   GTR 0 echo   - People: !PEOPLE!
    if !POSITIONS! GTR 0 echo  - Positions: !POSITIONS!

    REM ===== OpenAlex-specific keywords =====
    for /f "delims=" %%i in ('find /c "openalex" ^< data\vivo-additions.rdf.xml') do set /a OPENALEX_RECORDS=%%i
    for /f "delims=" %%i in ('find /c "Concept" ^< data\vivo-additions.rdf.xml') do set /a CONCEPTS=%%i
    for /f "delims=" %%i in ('find /c "Work" ^< data\vivo-additions.rdf.xml') do set /a WORKS=%%i

    if !OPENALEX_RECORDS! GTR 0 echo   - OpenAlex Records: !OPENALEX_RECORDS!
    if !CONCEPTS!         GTR 0 echo   - Concepts: !CONCEPTS!
    if !WORKS!            GTR 0 echo   - Works: !WORKS!

) else (
    echo No additions RDF found — cannot count OpenAlex data.
)

echo.
echo [SUCCESS] OpenAlex harvest completed successfully.
endlocal
exit /b 0
