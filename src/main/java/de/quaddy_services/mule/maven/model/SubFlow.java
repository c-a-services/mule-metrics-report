package de.quaddy_services.mule.maven.model;

import java.io.File;

import org.w3c.dom.Node;

/**
 *
 */
public class SubFlow extends AbstractMuleXmlElement {
	/**
	 * @param aFile
	 *
	 */
	public SubFlow(Node aParent, File aFile, String aName) {
		super(aFile, aParent);
		name = aName;

	}

	private String name;

	/**
	 * @see #name
	 */
	public String getName() {
		return name;
	}
}
