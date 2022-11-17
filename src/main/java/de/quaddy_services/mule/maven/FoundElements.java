package de.quaddy_services.mule.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.quaddy_services.mule.maven.model.AbstractFlow;
import de.quaddy_services.mule.maven.model.AbstractMuleXmlElement;
import de.quaddy_services.mule.maven.model.Flow;
import de.quaddy_services.mule.maven.model.FlowRef;
import de.quaddy_services.mule.maven.model.SetVariable;
import de.quaddy_services.mule.maven.model.SubFlow;

/**
 *
 */
public class FoundElements {

	private static final Logger LOGGER = LoggerFactory.getLogger(FoundElements.class);

	/**
	 *
	 */
	public FoundElements() {
		super();

	}

	private List<Flow> flows = new ArrayList<>();
	private List<FlowRef> flowRefs = new ArrayList<>();
	private AbstractFlow lastFoundFlow;

	/**
	 *
	 */
	public void add(Flow aFlow) {
		LOGGER.debug("Add Flow={}", aFlow);
		flows.add(aFlow);
		lastFoundFlow = aFlow;
	}

	private List<SubFlow> subFlows = new ArrayList<>();

	/**
	 *
	 */
	public void add(SubFlow aFlow) {
		LOGGER.debug("Add SubFlow={}", aFlow);
		subFlows.add(aFlow);
		lastFoundFlow = aFlow;
	}

	private List<SetVariable> setVariables = new ArrayList<>();

	/**
	 *
	 */
	public void add(SetVariable aFlow) {
		LOGGER.debug("Add SetVariable={}", aFlow);
		setVariables.add(aFlow);
	}

	/**
	 * @see #flows
	 */
	public List<Flow> getFlows() {
		return new ArrayList<>(flows);
	}

	/**
	 * @see #subFlows
	 */
	public List<SubFlow> getSubFlows() {
		return new ArrayList<>(subFlows);
	}

	/**
	 * @see #setVariables
	 */
	public List<SetVariable> getSetVariables() {
		return new ArrayList<>(setVariables);
	}

	/**
	 *
	 */
	public double getAverageFlowsPerFile() {
		List<Flow> tempFlows = getFlows();
		if (tempFlows.isEmpty()) {
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
		if (tempSubFlows.isEmpty()) {
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

	/**
	 *
	 */
	public void add(FlowRef aFlowRef) {
		LOGGER.debug("Add FlowRef={}", aFlowRef);
		if (aFlowRef.getName() == null) {
			throw new IllegalArgumentException("FlowRef " + aFlowRef + " must have a name.");
		}
		if (aFlowRef.getContainedInFlow() == null) {
			throw new IllegalArgumentException("FlowRef " + aFlowRef + " must have be in a surrounding flow.");
		}
		flowRefs.add(aFlowRef);
	}

	/**
	 * @see #flowsRefs
	 */
	public List<FlowRef> getFlowRefs() {
		return new ArrayList<>(flowRefs);
	}

	/**
	 *
	 */
	public AbstractFlow getLastFoundFlow() {
		return lastFoundFlow;
	}

	/**
	 *
	 */
	public List<AbstractFlow> getAllFlows() {
		List<AbstractFlow> tempAll = new ArrayList<>();
		tempAll.addAll(getFlows());
		tempAll.addAll(getSubFlows());
		return tempAll;
	}

	/**
	 *
	 */
	public List<AbstractFlow> getCallingFlows(AbstractFlow aAbstractFlow) {
		List<AbstractFlow> tempCallingFlows = new ArrayList<>();
		List<FlowRef> tempFlowRefs = getFlowRefs();
		for (FlowRef tempFlowRef : tempFlowRefs) {
			if (tempFlowRef.getName().equals(aAbstractFlow.getName())) {
				tempCallingFlows.add(tempFlowRef.getContainedInFlow());
			}
		}
		return tempCallingFlows;
	}

	/**
	 *
	 */
	public boolean isReferenced(AbstractFlow aAbstractFlow) {
		List<FlowRef> tempFlowRefs = getFlowRefs();
		for (FlowRef tempFlowRef : tempFlowRefs) {
			if (aAbstractFlow.equals(tempFlowRef.getContainedInFlow())) {
				return true;
			}
		}
		return false;
	}

	/**
	 *
	 */
	public List<AbstractFlow> getUnusedFlows() {
		List<AbstractFlow> tempUnusedFlows = new ArrayList<>();
		for (AbstractFlow tempAbstractFlow : getAllFlows()) {
			if (getCallingFlows(tempAbstractFlow).isEmpty()) {
				tempUnusedFlows.add(tempAbstractFlow);
			}
		}
		return tempUnusedFlows;
	}

	/**
	 *
	 */
	public FoundElements removeIgnoredFiles(List<String> aIgnoreFiles) {
		FoundElements tempClone = new FoundElements();
		// do not filter flowRefs to detect unused flows correctly.
		tempClone.flowRefs = getFlowRefs();
		tempClone.flows = filterByFileName(getFlows(), aIgnoreFiles);
		tempClone.subFlows = filterByFileName(getSubFlows(), aIgnoreFiles);
		tempClone.setVariables = filterByFileName(getSetVariables(), aIgnoreFiles);

		return tempClone;
	}

	/**
	 *
	 */
	private <E extends AbstractMuleXmlElement> List<E> filterByFileName(List<E> anElements, List<String> aIgnoreFiles) {
		if (aIgnoreFiles != null && !aIgnoreFiles.isEmpty()) {
			for (Iterator<E> i = anElements.iterator(); i.hasNext();) {
				E tempElement = i.next();
				if (isIgnoredFile(tempElement.getFile(), aIgnoreFiles)) {
					i.remove();
				}
			}
		}
		return anElements;
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

}
