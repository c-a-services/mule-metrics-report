package de.quaddy_services.mule.maven.model;

import java.io.File;

import org.w3c.dom.Node;

/**
 *
 */
public abstract class AbstractFlow extends AbstractMuleXmlElement {
	/**
	 * @param aFile
	 *
	 */
	public AbstractFlow(Node aParent, File aFile, String aName) {
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
