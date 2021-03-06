/*******************************************************************************
 *
 * Copyright (c) 2012-2015, Business Process Technology (BPT),
 * http://bpt.hpi.uni-potsdam.de. 
 * All Rights Reserved.
 *
 *******************************************************************************/
package de.hpi.unicorn.bpmn.decomposition;

import de.hpi.unicorn.bpmn.element.AbstractBPMNElement;

public class BondComponent extends Component {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new component as a container.
	 *
	 * @param entryPoint    - not included
	 * @param sourceElement - included
	 * @param exitPoint     - not included
	 * @param sinkElement   - included
	 */
	public BondComponent(final AbstractBPMNElement entryPoint, final AbstractBPMNElement sourceElement, final AbstractBPMNElement exitPoint, final AbstractBPMNElement sinkElement) {
		super(entryPoint, sourceElement, exitPoint, sinkElement);
	}

}
