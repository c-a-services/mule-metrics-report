package de.quaddy_services.mule.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 *
 */
public class MuleMetricsReportMojoTestApp {

	public static void main(String[] args) throws MojoExecutionException, MojoFailureException {
		MuleMetricsReportMojo tempMuleMetricsReportMojo = new MuleMetricsReportMojo();
		tempMuleMetricsReportMojo.setMuleAppDirectory(args[0] + "\\src\\main\\app");
		tempMuleMetricsReportMojo.setOutputDirectory(args[0] + "\\target\\metrics");
		tempMuleMetricsReportMojo.execute();
	}

}
