package de.quaddy_services.mule.maven;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * https://maven.apache.org/guides/plugin/guide-java-plugin-development.html
 */
@Mojo(name = "generate")
public class MuleMetricsReportMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project.build.directory}/metrics", readonly = true)
	private String outputDirectory;

	@Parameter(defaultValue = "src/main/app/", readonly = true)
	private String muleAppDirectory;

	/**
	 *
	 */
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Generate to " + outputDirectory);
		getLog().info("Source " + muleAppDirectory);
		File tempAppDir = new File(muleAppDirectory);
	}

}
