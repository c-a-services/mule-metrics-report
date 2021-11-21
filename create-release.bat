echo on
git pull
if x%gpg.passphrase%x == xx pause gpg.passphrase not set.
call mvn build-helper:parse-version release:prepare release:perform -DdevelopmentVersion=${parsedVersion.majorVersion}.${parsedVersion.minorVersion}.${parsedVersion.nextIncrementalVersion}-SNAPSHOT -DreleaseVersion=${parsedVersion.majorVersion}.${parsedVersion.minorVersion}.${parsedVersion.incrementalVersion} -Dtag=mule-metrics-report-${parsedVersion.majorVersion}.${parsedVersion.minorVersion}.${parsedVersion.incrementalVersion} -DpreparationGoals=clean -Darguments="-Dmaven.test.skip=true -Dgpg.passphrase=%gpg.passphrase%" -P oss-publish
if errorlevel 1 goto ende

rem call mvn nexus-staging:release

echo.
echo.
echo Next step:
echo.
echo Close and Release Repository at https://oss.sonatype.org/#stagingRepositories
echo.

:ende
