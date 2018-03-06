package de.quaddy_services.mule.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.quaddy_services.mule.maven.model.AbstractMuleXmlElement;
import de.quaddy_services.mule.maven.model.Flow;
import de.quaddy_services.mule.maven.model.SetVariable;
import de.quaddy_services.mule.maven.model.SubFlow;

/**
 *
 */
public class FoundElements {

	/**
	 *
	 */
	public FoundElements() {
		super();

	}

	private List<Flow> flows = new ArrayList<>();

	/**
	 *
	 */
	public void add(Flow aFlow) {
		flows.add(aFlow);
	}

	private List<SubFlow> subFlows = new ArrayList<>();

	/**
	 *
	 */
	public void add(SubFlow aFlow) {
		subFlows.add(aFlow);
	}

	private List<SetVariable> setVariables = new ArrayList<>();

	/**
	 *
	 */
	public void add(SetVariable aFlow) {
		setVariables.add(aFlow);
	}

	/**
	 * @see #flows
	 */
	public List<Flow> getFlows() {
		return flows;
	}

	/**
	 * @see #subFlows
	 */
	public List<SubFlow> getSubFlows() {
		return subFlows;
	}

	/**
	 * @see #setVariables
	 */
	public List<SetVariable> getSetVariables() {
		return setVariables;
	}

	/**
	 *
	 */
	public double getAverageFlowsPerFile() {
		List<Flow> tempFlows = getFlows();
		if (tempFlows.size() == 0) {
			return 0;
		}
		Set<File> tempFiles = new HashSet<>();
		for (Flow tempFlow : tempFlows) {
			tempFiles.add(tempFlow.getFile());
		}
		return (1.0 * tempFlows.size()) / (1.0 * tempFiles.size());
	}

	/**
	 *
	 */
	public double getAverageSubFlowsPerFile() {
		List<SubFlow> tempSubFlows = getSubFlows();
		if (tempSubFlows.size() == 0) {
			return 0;
		}
		Set<File> tempFiles = new HashSet<>();
		for (SubFlow tempSubFlow : tempSubFlows) {
			tempFiles.add(tempSubFlow.getFile());
		}
		return (1.0 * tempSubFlows.size()) / (1.0 * tempFiles.size());
	}

	/**
	 *
	 */
	public List<File> getFiles() {
		Set<File> tempFiles = new HashSet<>();
		List<AbstractMuleXmlElement> tempAllElements = getAllElements();
		for (AbstractMuleXmlElement tempElement : tempAllElements) {
			tempFiles.add(tempElement.getFile());
		}
		return new ArrayList<>(tempFiles);
	}

	/**
	 *
	 */
	private List<AbstractMuleXmlElement> getAllElements() {
		List<AbstractMuleXmlElement> tempAll = new ArrayList<>();
		tempAll.addAll(getFlows());
		tempAll.addAll(getSubFlows());
		tempAll.addAll(getSetVariables());
		return tempAll;
	}

}
