package de.quaddy_services.mule.maven.model;

import java.io.File;

import org.w3c.dom.Node;

/**
 *
 */
public class SetVariable extends AbstractMuleXmlElement {
	/**
	 * @param aFile
	 *
	 */
	public SetVariable(Node aParent, File aFile, String aName) {
		super(aFile, aParent, aName);

	}

}
