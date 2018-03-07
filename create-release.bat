echo on
git pull
call mvn build-helper:parse-version release:prepare release:perform -DuseReleaseProfile=false -DdevelopmentVersion=${parsedVersion.majorVersion}.${parsedVersion.minorVersion}.${parsedVersion.nextIncrementalVersion}-SNAPSHOT -DreleaseVersion=${parsedVersion.majorVersion}.${parsedVersion.minorVersion}.${parsedVersion.incrementalVersion} -Dtag=mule-metrics-report-${parsedVersion.majorVersion}.${parsedVersion.minorVersion}.${parsedVersion.incrementalVersion} -DpreparationGoals=clean -Darguments="-Dmaven.test.skip=true"

