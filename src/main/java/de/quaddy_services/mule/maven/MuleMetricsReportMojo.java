package de.quaddy_services.mule.maven;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.quaddy_services.mule.maven.model.Flow;
import de.quaddy_services.mule.maven.model.SetVariable;
import de.quaddy_services.mule.maven.model.SubFlow;

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
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Generate to " + getOutputDirectory());
		getLog().info("Source " + getMuleAppDirectory());
		File tempAppDir = new File(getMuleAppDirectory());
		FoundElements tempFoundElements = new FoundElements();
		collectMuleFiles(tempFoundElements, tempAppDir);
		try {
			printReport(tempFoundElements);
		} catch (IOException e) {
			throw new MojoExecutionException("Error creating report", e);
		}
	}

	/**
	 * @throws IOException
	 *
	 */
	private void printReport(FoundElements aFoundElements) throws IOException {
		File tempDir = new File(getOutputDirectory());
		tempDir.mkdirs();
		File tempIndexFile = new File(tempDir.getAbsolutePath() + "/index.html");
		try (PrintWriter tempWriter = new PrintWriter(new BufferedWriter(new FileWriter(tempIndexFile)))) {
			tempWriter.println("<html>");
			tempWriter.println("<body>");
			tempWriter.println("<h3>Statistics</h3>");
			tempWriter.println("<ul>");
			List<File> tempFiles = aFoundElements.getFiles();
			tempWriter.println("<li>" + tempFiles.size() + " <a href=\"#Files\">files</a>. </li>");
			List<Flow> tempFlows = aFoundElements.getFlows();
			tempWriter.println(
					"<li>" + tempFlows.size() + " <a href=\"#Flows\">flows</a>. Average per file: " + aFoundElements.getAverageFlowsPerFile() + " </li>");
			List<SubFlow> tempSubFlows = aFoundElements.getSubFlows();
			tempWriter.println("<li>" + tempSubFlows.size() + " <a href=\"#SubFlows\">sub-flows</a>. Average per file: "
					+ aFoundElements.getAverageSubFlowsPerFile() + "</li>");
			List<SetVariable> tempSetVariables = aFoundElements.getSetVariables();
			tempWriter.println("<li>" + tempSetVariables.size() + " <a href=\"#SetVariables\">set-variables</a> </li>");
			tempWriter.println("</ul>");
			tempWriter.println("<a name=\"Files\"><h3>Files</h3></a>");
			Collections.sort(tempFiles, new Comparator<File>() {
				@Override
				public int compare(File aO1, File aO2) {
					return aO1.getName().compareTo(aO2.getName());
				}
			});
			for (File tempFile : tempFiles) {
				tempWriter.println("<a href=\"" + createRelativeLink(tempDir, tempFile) + "\">" + tempFile.getName() + "</a><br/>");
			}
			tempWriter.println("<a name=\"Flows\"><h3>Flows</h3></a>");
			Collections.sort(tempFlows, new Comparator<Flow>() {
				@Override
				public int compare(Flow aO1, Flow aO2) {
					return aO1.getName().compareTo(aO2.getName());
				}
			});
			for (Flow tempFlow : tempFlows) {
				tempWriter.println(tempFlow.getName() + " <font size=1>in <a href=\"" + createRelativeLink(tempDir, tempFlow.getFile()) + "\">"
						+ tempFlow.getFile().getName() + "</a></font><br/>");
			}
			tempWriter.println("<a name=\"SubFlows\"><h3>SubFlows</h3></a>");
			Collections.sort(tempSubFlows, new Comparator<SubFlow>() {
				@Override
				public int compare(SubFlow aO1, SubFlow aO2) {
					return aO1.getName().compareTo(aO2.getName());
				}
			});
			for (SubFlow tempSubFlow : tempSubFlows) {
				tempWriter.println(tempSubFlow.getName() + " <font size=1>in " + tempSubFlow.getFile().getName() + "</font><br/>");
			}
			tempWriter.println("<a name=\"SetVariables\"><h3>SetVariables</h3></a>");
			Collections.sort(tempSetVariables, new Comparator<SetVariable>() {
				@Override
				public int compare(SetVariable aO1, SetVariable aO2) {
					return aO1.getName().compareTo(aO2.getName());
				}
			});
			for (SetVariable tempSetVariable : tempSetVariables) {
				tempWriter.println(tempSetVariable.getName() + " <font size=1>in " + tempSetVariable.getFile().getName() + "</font><br/>");
			}
			tempWriter.println("</body>");
			tempWriter.println("</html>");
		}
		getLog().info("Created " + tempIndexFile);
	}

	/**
	 *
	 */
	private String createRelativeLink(File aBaseDir, File aTargetFile) {
		File tempParentFile = new File(aTargetFile.getParentFile().getAbsolutePath().replace('\\', '/'));
		File tempDir = new File(aBaseDir.getAbsolutePath().replace('\\', '/'));
		String tempRelative = "";
		while (!aTargetFile.getAbsolutePath().startsWith(tempDir.getAbsolutePath())) {
			tempRelative += "../";
			tempDir = tempDir.getParentFile();
		}
		String tempRemainingPath = tempParentFile.getAbsolutePath().substring(tempDir.getAbsolutePath().length() + 1);
		String tempRelativeLink = tempRelative + tempRemainingPath + "/" + aTargetFile.getName();
		return tempRelativeLink.replace('\\', '/');
	}

	/**
	 * @param aFoundElements
	 * @throws MojoExecutionException
	 *
	 */
	private void collectMuleFiles(FoundElements aFoundElements, File aAppDir) throws MojoExecutionException {
		getLog().debug("Scan directory " + aAppDir);
		File[] tempFiles = aAppDir.listFiles();
		for (File tempFile : tempFiles) {
			if (tempFile.isDirectory()) {
				collectMuleFiles(aFoundElements, tempFile);
			} else if (tempFile.getName().endsWith(".xml")) {
				getLog().debug("Found xml " + tempFile.getAbsolutePath());
				DocumentBuilderFactory tempDbf = DocumentBuilderFactory.newInstance();
				tempDbf.setNamespaceAware(true);
				try {
					DocumentBuilder tempDb = tempDbf.newDocumentBuilder();
					Document tempDoc = tempDb.parse(tempFile);
					parseFile(aFoundElements, tempFile, null, tempDoc);
				} catch (SAXException | ParserConfigurationException | IOException e) {
					throw new MojoExecutionException("Error parsing " + tempFile.getAbsolutePath(), e);
				}
			}
		}
	}

	/**
	 *
	 */
	private void parseFile(FoundElements aFoundElements, File aFile, Node aParentNode, Node aChildNode) {
		String tempLocalName = aChildNode.getLocalName();
		if (tempLocalName != null) {
			getLog().debug("Found Node " + tempLocalName);
		}

		if ("flow".equals(tempLocalName)) {
			NamedNodeMap tempAttributes = aChildNode.getAttributes();
			Node tempFlowName = tempAttributes.getNamedItem("name");
			if (tempFlowName != null) {
				String tempFlowNameText = tempFlowName.getTextContent();
				aFoundElements.add(new Flow(aParentNode, aFile, tempFlowNameText));
			}
		} else if ("sub-flow".equals(tempLocalName)) {
			NamedNodeMap tempAttributes = aChildNode.getAttributes();
			Node tempFlowName = tempAttributes.getNamedItem("name");
			if (tempFlowName != null) {
				String tempFlowNameText = tempFlowName.getTextContent();
				aFoundElements.add(new SubFlow(aParentNode, aFile, tempFlowNameText));
			}
		} else if ("set-variable".equals(tempLocalName)) {
			NamedNodeMap tempAttributes = aChildNode.getAttributes();
			Node tempVariableName = tempAttributes.getNamedItem("variableName");
			if (tempVariableName != null) {
				String tempFlowNameText = tempVariableName.getTextContent();
				aFoundElements.add(new SetVariable(aParentNode, aFile, tempFlowNameText));
			}
		}

		NodeList tempNextChildNodes = aChildNode.getChildNodes();
		for (int c = 0; c < tempNextChildNodes.getLength(); c++) {
			Node tempChild = tempNextChildNodes.item(c);
			parseFile(aFoundElements, aFile, aParentNode, tempChild);
		}
	}

	/**
	 * @see #muleAppDirectory
	 */
	public String getMuleAppDirectory() {
		return muleAppDirectory;
	}

	/**
	 * @see #muleAppDirectory
	 */
	public void setMuleAppDirectory(String aMuleAppDirectory) {
		muleAppDirectory = aMuleAppDirectory;
	}

	/**
	 * @see #outputDirectory
	 */
	public String getOutputDirectory() {
		return outputDirectory;
	}

	/**
	 * @see #outputDirectory
	 */
	public void setOutputDirectory(String aOutputDirectory) {
		outputDirectory = aOutputDirectory;
	}

}
