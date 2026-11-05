@echo off

REM ============================================================
REM FUNCTION DISPATCHER
REM ============================================================
set "FUNC=%~1"
shift

if "%FUNC%"=="validate_load_method"          goto :validate_load_method
if "%FUNC%"=="init_environment"              goto :init_environment
if "%FUNC%"=="setup_logging"                 goto :setup_logging
if "%FUNC%"=="clean_data"                    goto :clean_data
if "%FUNC%"=="execute_fetch"                 goto :execute_fetch
if "%FUNC%"=="execute_translate"             goto :execute_translate
if "%FUNC%"=="execute_transfer"              goto :execute_transfer
if "%FUNC%"=="perform_diff"                  goto :perform_diff
if "%FUNC%"=="apply_changes_to_previous"     goto :apply_changes_to_previous
if "%FUNC%"=="apply_changes_to_vivo"         goto :apply_changes_to_vivo

echo ERROR: Unknown function "%FUNC%"
exit /b 1


REM ============================================================
:validate_load_method
REM %1 = load method argument
set "INPUT=%~1"

if "%INPUT%"=="" (
    set "LOAD_METHOD=sparql"
    exit /b 0
)

if /I "%INPUT%"=="tdb" (
    set "LOAD_METHOD=tdb"
    exit /b 0
)

if /I "%INPUT%"=="sparql" (
    set "LOAD_METHOD=sparql"
    exit /b 0
)

echo Invalid argument: %INPUT%
exit /b 1


REM ============================================================
:init_environment
REM %1 = harvest name
set "HARVEST_NAME=%~1"

for /f "tokens=2 delims==" %%i in ('wmic os get localdatetime /value') do set dt=%%i

set YEAR=%dt:~0,4%
set MONTH=%dt:~4,2%
set DAY=%dt:~6,2%
set HOUR=%dt:~8,2%
set MINUTE=%dt:~10,2%
set SECOND=%dt:~12,2%

set "DATE=%YEAR%-%MONTH%-%DAY%T%HOUR%:%MINUTE%:%SECOND%"

set "HARVESTER_INSTALL_DIR=%cd%\..\..\..\..\..\..\VIVO-Harvester"
set "PATH=%PATH%;%HARVESTER_INSTALL_DIR%\bin"
set "CLASSPATH=%HARVESTER_INSTALL_DIR%\bin\harvester.jar;%HARVESTER_INSTALL_DIR%\bin\dependency\*;%HARVESTER_INSTALL_DIR%\build\harvester.jar;%HARVESTER_INSTALL_DIR%\build\dependency\*"
set "HARVESTER_JAVA_OPTS=-Xms1024M -Xmx2048M"

exit /b 0


REM ============================================================
:setup_logging
if not exist logs mkdir logs
pushd logs
echo. > "%HARVEST_NAME%.%DATE%.log"
del "%HARVEST_NAME%.latest.log" 2>nul
copy "%HARVEST_NAME%.%DATE%.log" "%HARVEST_NAME%.latest.log" >nul
popd
exit /b 0


REM ============================================================
:clean_data
if exist data rmdir /s /q data
exit /b 0


REM ============================================================
:execute_fetch
set "FETCH_CLASS=%~1"
set "CONFIG_FILE=%~2"

if not exist "%CONFIG_FILE%" (
    echo Missing config file: %CONFIG_FILE%
    exit /b 1
)

java %HARVESTER_JAVA_OPTS% -cp "%CLASSPATH%" %FETCH_CLASS% -X "%CONFIG_FILE%"
exit /b %errorlevel%


REM ============================================================
:execute_translate
set "CONFIG_FILE=%~1"
if "%CONFIG_FILE%"=="" set "CONFIG_FILE=xsltranslator.config.xml"

if not exist "%CONFIG_FILE%" (
    echo Missing config file: %CONFIG_FILE%
    exit /b 1
)

java %HARVESTER_JAVA_OPTS% -cp "%CLASSPATH%" org.vivoweb.harvester.translate.XSLTranslator -X "%CONFIG_FILE%"
exit /b %errorlevel%


REM ============================================================
:execute_transfer
set "COMMON_DIR=%~1"

echo Transferring translated RDF...

java %HARVESTER_JAVA_OPTS% -cp "%CLASSPATH%" org.vivoweb.harvester.transfer.Transfer -w INFO ^
    -s "%COMMON_DIR%\translation\translated-records.config.xml" ^
    -o "%COMMON_DIR%\harvestertransfer\harvested-data.model.xml" ^
    -d "data\harvested-data\imported-records.rdf.xml"

exit /b %errorlevel%


REM ============================================================
:perform_diff
set "COMMON_DIR=%~1"

echo Performing diff (subtractions)...
java %HARVESTER_JAVA_OPTS% -cp "%CLASSPATH%" org.vivoweb.harvester.diff.Diff -X "%COMMON_DIR%\diff\diff-subtractions.config.xml"
if errorlevel 1 exit /b %errorlevel%

echo Performing diff (additions)...
java %HARVESTER_JAVA_OPTS% -cp "%CLASSPATH%" org.vivoweb.harvester.diff.Diff -X "%COMMON_DIR%\diff\diff-additions.config.xml"

exit /b %errorlevel%


REM ============================================================
:apply_changes_to_previous
set "COMMON_DIR=%~1"

echo Applying subtractions to previous model...
java %HARVESTER_JAVA_OPTS% -cp "%CLASSPATH%" org.vivoweb.harvester.transfer.Transfer -w INFO ^
    -o "%COMMON_DIR%\harvestertransfer\previous-harvest.model.xml" ^
    -r "data\vivo-subtractions.rdf.xml" -m
if errorlevel 1 exit /b %errorlevel%

echo Applying additions to previous model...
java %HARVESTER_JAVA_OPTS% -cp "%CLASSPATH%" org.vivoweb.harvester.transfer.Transfer -w INFO ^
    -o "%COMMON_DIR%\harvestertransfer\previous-harvest.model.xml" ^
    -r "data\vivo-additions.rdf.xml"

exit /b %errorlevel%


REM ============================================================
:apply_changes_to_vivo
set "LOAD_METHOD=%~1"
set "COMMON_DIR=%~2"

echo Applying updates to VIVO instance using (%LOAD_METHOD%)...
if /I "%LOAD_METHOD%"=="tdb" (
    echo Applying subtractions (TDB mode)...
    java %HARVESTER_JAVA_OPTS% -cp "%CLASSPATH%" org.vivoweb.harvester.transfer.Transfer -w INFO ^
        -o "%COMMON_DIR%\modelupdate\vivo.model.xml" ^
        -r "data\vivo-subtractions.rdf.xml" -m
    if errorlevel 1 exit /b %errorlevel%

    echo Applying additions (TDB mode)...
    java %HARVESTER_JAVA_OPTS% -cp "%CLASSPATH%" org.vivoweb.harvester.transfer.Transfer -w INFO ^
        -o "%COMMON_DIR%\modelupdate\vivo.model.xml" ^
        -r "data\vivo-additions.rdf.xml"
) else (
    echo Applying updates via SPARQL endpoint...
    java %HARVESTER_JAVA_OPTS% -cp "%CLASSPATH%" org.vivoweb.harvester.services.SparqlUpdate ^
        -X "%COMMON_DIR%\modelupdate\sparqlupdate.conf.xml"
)

exit /b %errorlevel%


REM ============================================================
:count_imports
set /a ORGS=0
set /a PEOPLE=0
set /a POSITIONS=0

if exist data\vivo-additions.rdf.xml (

    for /f "delims=" %%i in ('find /c "http://xmlns.com/foaf/0.1/Organization" ^< data\vivo-additions.rdf.xml') do set /a ORGS=%%i
    for /f "delims=" %%i in ('find /c "http://xmlns.com/foaf/0.1/Person" ^< data\vivo-additions.rdf.xml') do set /a PEOPLE=%%i
    for /f "delims=" %%i in ('find /c "positionForPerson" ^< data\vivo-additions.rdf.xml') do set /a POSITIONS=%%i

    echo Import Statistics:
    if !ORGS! GTR 0 echo   - Organizations: !ORGS!
    if !PEOPLE! GTR 0 echo   - People: !PEOPLE!
    if !POSITIONS! GTR 0 echo   - Positions: !POSITIONS!

) else (
    echo No additions RDF found to count.
)
exit /b %errorlevel%

