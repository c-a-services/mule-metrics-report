package de.quaddy_services.mule.maven.model;

import java.io.File;

import org.w3c.dom.Node;

/**
 *
 */
public class AbstractMuleXmlElement {
	private File file;

	/**
	 * @param aFile
	 *
	 */
	public AbstractMuleXmlElement(File aFile, Node aParent) {
		super();
		parent = aParent;
		file = aFile;
	}

	private Node parent;

	/**
	 * @see #file
	 */
	public File getFile() {
		return file;
	}

	/**
	 * @see #parent
	 */
	public Node getParent() {
		return parent;
	}
}
