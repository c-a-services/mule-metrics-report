package de.quaddy_services.mule.maven;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

import de.quaddy_services.mule.maven.model.AbstractFlow;
import de.quaddy_services.mule.maven.model.AbstractMuleXmlElement;
import de.quaddy_services.mule.maven.model.Flow;
import de.quaddy_services.mule.maven.model.FlowRef;
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
	 * Possibility to make report smaller.
	 */
	@Parameter()
	private List<String> ignoreFiles;

	/**
	 *
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Generate to " + getOutputDirectory());
		getLog().info("Source " + getMuleAppDirectory());
		getLog().info("ignoredFiles=" + (ignoreFiles == null ? "n/a" : Arrays.asList(ignoreFiles).toString()));
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
		if (ignoreFiles != null && !ignoreFiles.isEmpty()) {
			printReport(aFoundElements, "index.html", ignoreFiles);
			printReport(aFoundElements, "index-all-files.html", new ArrayList<String>());
		} else {
			printReport(aFoundElements, "index.html", new ArrayList<String>());
		}
	}

	/**
	 * @param aIgnoreFiles
	 * @param aTargetFileName
	 *
	 */
	public void printReport(FoundElements aFoundElements, String aTargetFileName, List<String> aIgnoreFiles) throws IOException {
		File tempDir = new File(getOutputDirectory());
		tempDir.mkdirs();
		File tempIndexFile = new File(tempDir.getAbsolutePath() + '/' + aTargetFileName);
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

			List<AbstractFlow> tempUnusedFlows = aFoundElements.getUnusedFlows();
			filterByFileName(tempUnusedFlows, aIgnoreFiles);
			tempWriter.println("<li><a href=\"#UnusedFlows\">" + tempUnusedFlows.size() + " unused flows</a> </li>");

			tempWriter.println("<li>" + tempSetVariables.size() + " <a href=\"#SetVariables\">set-variables</a> </li>");

			tempWriter.println("<li><a href=\"call-hierarchy.html\">CallHierarchy</a> </li>");
			printCallHierarchy(tempDir, aFoundElements, aIgnoreFiles);
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
			filterByFileName(tempFlows, aIgnoreFiles);
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
			filterByFileName(tempSubFlows, aIgnoreFiles);
			Collections.sort(tempSubFlows, new Comparator<SubFlow>() {
				@Override
				public int compare(SubFlow aO1, SubFlow aO2) {
					return aO1.getName().compareTo(aO2.getName());
				}
			});
			for (SubFlow tempSubFlow : tempSubFlows) {
				tempWriter.println(tempSubFlow.getName() + " <font size=1>in <a href=\"" + createRelativeLink(tempDir, tempSubFlow.getFile()) + "\">"
						+ tempSubFlow.getFile().getName() + "</a></font><br/>");
			}

			tempWriter.println("<a name=\"UnusedFlows\"><h3>UnusedFlows</h3></a>");
			Collections.sort(tempUnusedFlows, new Comparator<AbstractFlow>() {
				@Override
				public int compare(AbstractFlow aO1, AbstractFlow aO2) {
					return aO1.getName().compareTo(aO2.getName());
				}
			});
			for (AbstractFlow tempUnused : tempUnusedFlows) {
				tempWriter.println(tempUnused.getName() + " <font size=1>in <a href=\"" + createRelativeLink(tempDir, tempUnused.getFile()) + "\">"
						+ tempUnused.getFile().getName() + "</a></font><br/>");
			}

			tempWriter.println("<a name=\"SetVariables\"><h3>SetVariables</h3></a>");
			filterByFileName(tempSetVariables, aIgnoreFiles);
			Collections.sort(tempSetVariables, new Comparator<SetVariable>() {
				@Override
				public int compare(SetVariable aO1, SetVariable aO2) {
					return aO1.getName().compareTo(aO2.getName());
				}
			});
			for (SetVariable tempSetVariable : tempSetVariables) {
				tempWriter.println(tempSetVariable.getName() + " <font size=1>in <a href=\"" + createRelativeLink(tempDir, tempSetVariable.getFile()) + "\">"
						+ tempSetVariable.getFile().getName() + "</a></font><br/>");
			}
			tempWriter.println("</body>");
			tempWriter.println("</html>");
		}
		getLog().info("Created " + tempIndexFile);
	}

	/**
	 * @param aIgnoreFiles
	 * @throws IOException
	 *
	 */
	private void printCallHierarchy(File aDir, FoundElements aFoundElements, List<String> aIgnoreFiles) throws IOException {
		File tempIndexFile = new File(aDir.getAbsolutePath() + "/call-hierarchy.html");
		try (PrintWriter tempWriter = new PrintWriter(new BufferedWriter(new FileWriter(tempIndexFile)))) {
			tempWriter.println("<html>");
			tempWriter.println("<body>");
			tempWriter.println("<a name=\"CallHierarchy\"><h3>CallHierarchy</h3></a>");
			List<AbstractFlow> tempAllFlows = aFoundElements.getAllFlows();
			filterByFileName(tempAllFlows, aIgnoreFiles);
			Collections.sort(tempAllFlows, new Comparator<AbstractFlow>() {
				@Override
				public int compare(AbstractFlow aO1, AbstractFlow aO2) {
					return aO1.getName().compareTo(aO2.getName());
				}
			});
			tempWriter.println("<pre>");
			for (AbstractFlow tempAbstractFlow : tempAllFlows) {
				printCallTree(tempWriter, tempAbstractFlow, aFoundElements, 0, new HashSet<AbstractFlow>());
			}
			tempWriter.println("</pre>");
			tempWriter.println("</body>");
			tempWriter.println("</html>");
			getLog().info("Created " + tempIndexFile);
		}
	}

	/**
	 *
	 */
	private <E extends AbstractMuleXmlElement> void filterByFileName(List<E> anElements, List<String> aIgnoreFiles) {
		if (aIgnoreFiles != null && !aIgnoreFiles.isEmpty()) {
			for (Iterator<E> i = anElements.iterator(); i.hasNext();) {
				E tempElement = i.next();
				for (String tempFileName : aIgnoreFiles) {
					if (tempElement.getFile().getName().equals(tempFileName)) {
						getLog().debug("Remove due to ignored files: " + tempElement);
						i.remove();
					}
				}
			}
		}
	}

	/**
	 * @param aIntend
	 * @param aVisited
	 *
	 */
	private void printCallTree(PrintWriter aWriter, AbstractFlow aAbstractFlow, FoundElements aFoundElements, int aIntend, Set<AbstractFlow> aVisited) {
		List<AbstractFlow> tempCallingFlows = aFoundElements.getCallingFlows(aAbstractFlow);
		if (aIntend == 0) {
			if (tempCallingFlows.isEmpty()) {
				return;
			}
			if (aFoundElements.isReferenced(aAbstractFlow)) {
				return;
			}
		}
		for (int i = 0; i < aIntend; i++) {
			aWriter.print("  ");
		}
		if (aVisited.contains(aAbstractFlow)) {
			aWriter.println("recursion with " + aAbstractFlow.getName());
			return;
		}
		aVisited.add(aAbstractFlow);
		aWriter.println(aAbstractFlow.getName());
		for (AbstractFlow tempCallingFlow : tempCallingFlows) {
			printCallTree(aWriter, tempCallingFlow, aFoundElements, aIntend + 1, new HashSet<>(aVisited));
		}
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
		} else if ("flow-ref".equals(tempLocalName)) {
			NamedNodeMap tempAttributes = aChildNode.getAttributes();
			Node tempFlowName = tempAttributes.getNamedItem("name");
			if (tempFlowName != null) {
				String tempFlowNameText = tempFlowName.getTextContent();
				aFoundElements.add(new FlowRef(aParentNode, aFile, tempFlowNameText, aFoundElements.getLastFoundFlow()));
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
