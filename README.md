# mule-metrics-report
maven plugin which creates a metrics report for [mule applications](https://www.mulesoft.com/platform/mule)

Usage:

```
mvn de.quaddy-services.mule.maven:mule-metrics-report:generate
```

and then a file `target/matrics/index.html` will be created.

Commandline attribtes needs to be prefixed to the configuration ones with "mulemetrics.":

```
-Dmulemetrics.muleAppDirectory=
-Dmulemetrics.mule4AppDirectory=
-Dmulemetrics.outputDirectory=
-Dmulemetrics.ignoreFiles=
```

For automatic file creation when using mvn package you can add

```
			<plugin>
				<groupId>de.quaddy-services.mule.maven</groupId>
				<artifactId>mule-metrics-report</artifactId>
				<version>2018.3.4</version>
				<executions>
					<execution>
						<id>generate</id>
						<phase>process-resources</phase>
						<goals>
							<goal>generate</goal>
						</goals>
					</execution>
				</executions>
				<configuration>
					<!-- optional ignore some generated files -->
					<ignoreFiles>
						<ignoreFile>api.xml</ignoreFile>
					</ignoreFiles>
				</configuration>
			</plugin>

```

to your <plugins> section in the pom.xml.

Example report: (https://rawgit.com/c-a-services/mule-metrics-report/master/src/main/site/resources/metrics/index.html)
