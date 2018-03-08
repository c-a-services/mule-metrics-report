package de.quaddy_services.mule.maven.model;

import java.io.File;

import org.w3c.dom.Node;

/**
 *
 */
public abstract class AbstractMuleXmlElement {
	private File file;
	private String name;

	/**
	 * @param aFile
	 * @param aName
	 *
	 */
	public AbstractMuleXmlElement(File aFile, Node aParent, String aName) {
		super();
		parent = aParent;
		file = aFile;
		name = aName;
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

	/**
	 * @see #name
	 */
	public String getName() {
		return name;
	}

	/**
	 *
	 */
	@Override
	public String toString() {
		StringBuilder tempBuilder = new StringBuilder();
		tempBuilder.append("AbstractMuleXmlElement [");
		if (file != null) {
			tempBuilder.append("file=");
			tempBuilder.append(file);
			tempBuilder.append(", ");
		}
		if (name != null) {
			tempBuilder.append("name=");
			tempBuilder.append(name);
			tempBuilder.append(", ");
		}
		if (parent != null) {
			tempBuilder.append("parent=");
			tempBuilder.append(parent);
		}
		tempBuilder.append("]");
		return tempBuilder.toString();
	}
}
