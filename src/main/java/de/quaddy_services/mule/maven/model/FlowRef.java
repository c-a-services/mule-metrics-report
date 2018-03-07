package de.quaddy_services.mule.maven.model;

import java.io.File;

import org.w3c.dom.Node;

/**
 *
 */
public class FlowRef extends AbstractMuleXmlElement {
	private AbstractFlow containedInFlow;

	/**
	 * @param aFile
	 *
	 */
	public FlowRef(Node aParent, File aFile, String aName, AbstractFlow aAbstractFlow) {
		super(aFile, aParent);
		name = aName;
		containedInFlow = aAbstractFlow;
	}

	private String name;

	/**
	 * @see #name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @see #containedInFlow
	 */
	public AbstractFlow getContainedInFlow() {
		return containedInFlow;
	}
}
