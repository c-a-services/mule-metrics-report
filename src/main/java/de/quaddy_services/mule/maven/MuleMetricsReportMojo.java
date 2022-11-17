package de.quaddy_services.mule.maven;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
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

	/**
	 *
	 */
	private static final String CALL_HIERARCHY_HTML = "call-hierarchy.html";

	@Parameter(defaultValue = "${project.build.directory}/site/metrics", property = "mulemetrics.outputDirectory")
	private String outputDirectory;

	/**
	 * This is mule3 default.
	 */
	@Parameter(defaultValue = "${project.basedir}/src/main/app/", property = "mulemetrics.muleAppDirectory")
	private String muleAppDirectory;

	/**
	 * Default mule4 *.xml folder is src/main/mule/
	 */
	@Parameter(defaultValue = "${project.basedir}/src/main/mule/", property = "mulemetrics.mule4AppDirectory")
	private String mule4AppDirectory;

	/**
	 * Possibility to make report smaller.
	 */
	@Parameter(property = "mulemetrics.ignoreFiles")
	private List<String> ignoreFiles;

	@Parameter(property = "mulemetrics.skip")
	private boolean skip;

	/**
	 *
	 */
	@Override
	public void execute() throws MojoExecutionException {
		getLog().info("Generate to " + getOutputDirectory());
		getLog().info("Source: Mule3: " + getMuleAppDirectory());
		getLog().info("Source: Mule4: " + getMule4AppDirectory());
		getLog().info("ignoredFiles=" + (getIgnoreFiles() == null ? "n/a" : getIgnoreFiles().toString()));
		if (skip) {
			getLog().info("Skip is set. Do nothing.");
			return;
		}
		FoundElements tempFoundElements = new FoundElements();
		File tempAppDir = new File(getMuleAppDirectory());
		int tempFoundMule3Files = collectMuleFiles(tempFoundElements, tempAppDir);
		getLog().info("Found " + tempFoundMule3Files + " files in mule3: " + tempAppDir);
		File tempMule4AppDir = new File(getMule4AppDirectory());
		int tempFoundMule4Files = collectMuleFiles(tempFoundElements, tempMule4AppDir);
		getLog().debug("Found " + tempFoundMule4Files + " files in mule4: " + tempAppDir);

		if (tempFoundMule3Files > 0 || tempFoundMule4Files > 0) {
			try {
				printReport(tempFoundElements);
			} catch (IOException e) {
				throw new MojoExecutionException("Error creating report", e);
			}
		} else {
			getLog().info("Skipping No mule files found.");
		}
	}

	/**
	 * @throws IOException
	 *
	 */
	private void printReport(FoundElements aFoundElements) throws IOException {
		List<String> tempIgnoreFiles = getIgnoreFiles();
		if (tempIgnoreFiles != null && !tempIgnoreFiles.isEmpty()) {
			printReport(aFoundElements, "index.html", tempIgnoreFiles);
			printReport(aFoundElements, "index-all-files.html", new ArrayList<String>());
		} else {
			printReport(aFoundElements, "index.html", new ArrayList<String>());
		}
		File tempDir = new File(getOutputDirectory());
		printCallHierarchy(tempDir, aFoundElements);
	}

	/**
	 * @param aIgnoreFiles
	 * @param aTargetFileName
	 *
	 */
	private void printReport(FoundElements aFoundElements, String aTargetFileName, List<String> aIgnoreFiles) throws IOException {
		File tempDir = new File(getOutputDirectory());
		tempDir.mkdirs();
		File tempIndexFile = new File(tempDir.getAbsolutePath() + '/' + aTargetFileName);
		try (PrintWriter tempWriter = new PrintWriter(new BufferedWriter(new FileWriter(tempIndexFile)))) {
			tempWriter.println("<html>");
			tempWriter.println("<body>");
			tempWriter.println("<h3>Statistics</h3>");
			tempWriter.println("<ul>");
			// take tempFiles with all elements (to refer to ...-all-files.html)
			List<File> tempFiles = aFoundElements.getFiles();
			FoundElements tempFoundElements = aFoundElements.removeIgnoredFiles(aIgnoreFiles);
			tempWriter.println("<li>" + tempFoundElements.getFiles().size() + " <a href=\"#Files\">files</a>. </li>");
			List<Flow> tempFlows = tempFoundElements.getFlows();
			tempWriter.println(
					"<li>" + tempFlows.size() + " <a href=\"#Flows\">flows</a>. Average per file: " + tempFoundElements.getAverageFlowsPerFile() + " </li>");
			List<SubFlow> tempSubFlows = tempFoundElements.getSubFlows();
			tempWriter.println("<li>" + tempSubFlows.size() + " <a href=\"#SubFlows\">sub-flows</a>. Average per file: "
					+ tempFoundElements.getAverageSubFlowsPerFile() + "</li>");
			List<SetVariable> tempSetVariables = tempFoundElements.getSetVariables();

			List<AbstractFlow> tempUnusedFlows = tempFoundElements.getUnusedFlows();
			tempWriter.println("<li><a href=\"#UnusedFlows\">" + tempUnusedFlows.size() + " unused flows</a> </li>");

			tempWriter.println("<li>" + tempSetVariables.size() + " <a href=\"#SetVariables\">set-variables</a> </li>");

			tempWriter.println("<li><a href=\"" + CALL_HIERARCHY_HTML + "\">CallHierarchy</a> </li>");
			tempWriter.println("</ul>");

			tempWriter.println("<a name=\"Files\"><h3>Files</h3></a>");
			Collections.sort(tempFiles, new Comparator<File>() {
				@Override
				public int compare(File aO1, File aO2) {
					return aO1.getName().compareTo(aO2.getName());
				}
			});
			for (File tempFile : tempFiles) {
				String tempOtherLink;
				if (isIgnoredFile(tempFile, aIgnoreFiles)) {
					tempOtherLink = " (ignored here, see <a href=\"index-all-files.html\">index-all-files.html</a>)";
				} else {
					tempOtherLink = "";
				}
				tempWriter.println("<a href=\"" + createRelativeLink(tempDir, tempFile) + "\">" + tempFile.getName() + "</a> " + tempOtherLink + "<br/>");
			}
			tempWriter.println("<a name=\"Flows\"><h3>Flows</h3></a>");
			sortByName(tempFlows);
			for (Flow tempFlow : tempFlows) {
				tempWriter.println(tempFlow.getName() + " <font size=1>in <a href=\"" + createRelativeLink(tempDir, tempFlow.getFile()) + "\">"
						+ tempFlow.getFile().getName() + "</a></font><br/>");
			}
			tempWriter.println("<a name=\"SubFlows\"><h3>SubFlows</h3></a>");
			sortByName(tempSubFlows);
			for (SubFlow tempSubFlow : tempSubFlows) {
				tempWriter.println(tempSubFlow.getName() + " <font size=1>in <a href=\"" + createRelativeLink(tempDir, tempSubFlow.getFile()) + "\">"
						+ tempSubFlow.getFile().getName() + "</a></font><br/>");
			}

			tempWriter.println("<a name=\"UnusedFlows\"><h3>UnusedFlows</h3></a>");
			sortByName(tempUnusedFlows);
			for (AbstractFlow tempUnused : tempUnusedFlows) {
				tempWriter.println(tempUnused.getName() + " <font size=1>in <a href=\"" + createRelativeLink(tempDir, tempUnused.getFile()) + "\">"
						+ tempUnused.getFile().getName() + "</a></font><br/>");
			}

			tempWriter.println("<a name=\"SetVariables\"><h3>SetVariables</h3></a>");
			sortByName(tempSetVariables);
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
	 *
	 */
	private <E extends AbstractMuleXmlElement> void sortByName(List<E> aFiles) {
		Collections.sort(aFiles, new Comparator<E>() {
			@Override
			public int compare(E aO1, E aO2) {
				int c = aO1.getName().compareTo(aO2.getName());
				if (c != 0) {
					return c;
				}
				return aO1.getFile().getName().compareTo(aO2.getFile().getName());
			}
		});
	}

	/**
	 * @param aIgnoreFiles
	 * @param aCallHierarchyFileName
	 * @throws IOException
	 *
	 */
	private void printCallHierarchy(File aDir, FoundElements aFoundElements) throws IOException {
		File tempIndexFile = new File(aDir.getAbsolutePath() + '/' + CALL_HIERARCHY_HTML);
		try (PrintWriter tempWriter = new PrintWriter(new BufferedWriter(new FileWriter(tempIndexFile)))) {
			tempWriter.println("<html>");
			tempWriter.println("<body>");
			tempWriter.println("<a name=\"CallHierarchy\"><h3>CallHierarchy</h3></a>");
			List<AbstractFlow> tempAllFlows = aFoundElements.getAllFlows();
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

	private boolean isIgnoredFile(File aFile, List<String> aIgnoreFiles) {
		if (aIgnoreFiles != null && !aIgnoreFiles.isEmpty()) {
			for (String tempFileName : aIgnoreFiles) {
				if (aFile.getName().equals(tempFileName)) {
					return true;
				}
			}
		}
		return false;
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
	private int collectMuleFiles(FoundElements aFoundElements, File aAppDir) throws MojoExecutionException {
		int tempFilesFound = 0;
		getLog().debug("Scan directory " + aAppDir);
		File[] tempFiles = aAppDir.listFiles();
		if (tempFiles != null) {
			for (File tempFile : tempFiles) {
				if (tempFile.isDirectory()) {
					int tempFilesFoundInSubDir = collectMuleFiles(aFoundElements, tempFile);
					tempFilesFound += tempFilesFoundInSubDir;
				} else if (tempFile.getName().endsWith(".xml")) {
					tempFilesFound += 1;
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
		return tempFilesFound;
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
			} else {
				getLog().warn("Did not find attribute name in " + aChildNode.getNamespaceURI() + " " + tempLocalName + " in " + aFile);
			}
		} else if ("sub-flow".equals(tempLocalName)) {
			NamedNodeMap tempAttributes = aChildNode.getAttributes();
			Node tempFlowName = tempAttributes.getNamedItem("name");
			if (tempFlowName != null) {
				String tempFlowNameText = tempFlowName.getTextContent();
				aFoundElements.add(new SubFlow(aParentNode, aFile, tempFlowNameText));
			} else {
				getLog().warn("Did not find attribute name in " + aChildNode.getNamespaceURI() + " " + tempLocalName + " in " + aFile);
			}
		} else if ("flow-ref".equals(tempLocalName)) {
			NamedNodeMap tempAttributes = aChildNode.getAttributes();
			Node tempFlowName = tempAttributes.getNamedItem("name");
			if (tempFlowName != null) {
				String tempFlowNameText = tempFlowName.getTextContent();
				AbstractFlow tempLastFoundFlow = aFoundElements.getLastFoundFlow();
				if (tempLastFoundFlow == null) {
					getLog().info("found flow-ref " + tempFlowNameText + " outside of flow in file " + aFile);
				} else {
					aFoundElements.add(new FlowRef(aParentNode, aFile, tempFlowNameText, tempLastFoundFlow));
				}
			} else {
				getLog().warn("Did not find attribute name in " + aChildNode.getNamespaceURI() + " " + tempLocalName + " in " + aFile);
			}
		} else if ("set-variable".equals(tempLocalName)) {
			// covers
			// <set-variable variableName="
			// and
			// <dw:set-variable variableName="
			NamedNodeMap tempAttributes = aChildNode.getAttributes();
			Node tempVariableName = tempAttributes.getNamedItem("variableName");
			if (tempVariableName != null) {
				String tempFlowNameText = tempVariableName.getTextContent();
				aFoundElements.add(new SetVariable(aParentNode, aFile, tempFlowNameText));
			} else {
				getLog().warn("Did not find attribute variableName in " + aChildNode.getNamespaceURI() + " " + tempLocalName + " in " + aFile);
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

	/**
	 * @see #ignoreFiles
	 */
	public List<String> getIgnoreFiles() {
		return ignoreFiles;
	}

	/**
	 * @see #ignoreFiles
	 */
	public void setIgnoreFiles(List<String> aIgnoreFiles) {
		ignoreFiles = aIgnoreFiles;
	}

	/**
	 * @see #mule4AppDirectory
	 */
	public String getMule4AppDirectory() {
		return mule4AppDirectory;
	}

}
