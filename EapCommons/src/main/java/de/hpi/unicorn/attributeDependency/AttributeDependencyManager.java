/*******************************************************************************
 *
 * Copyright (c) 2012-2015, Business Process Technology (BPT),
 * http://bpt.hpi.uni-potsdam.de.
 * All Rights Reserved.
 *
 *******************************************************************************/
package de.hpi.unicorn.attributeDependency;

import de.hpi.unicorn.event.EapEventType;
import de.hpi.unicorn.event.attribute.TypeTreeNode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * The AttributeDependencyManager allows to request dependencies and their values for a given event type.
 * It is used to reduce the database-access for basic requests.
 *
 */
public class AttributeDependencyManager {
	private EapEventType eventType;
	private List<AttributeDependency> attributeDependencies;
	private Map<AttributeDependency, List<AttributeValueDependency>> attributeValueDependencies = new HashMap<>();

	public AttributeDependencyManager(EapEventType eventType) {
		this.eventType = eventType;
		this.attributeDependencies = AttributeDependency.getAttributeDependenciesWithEventType(eventType);
		for(AttributeDependency attributeDependency : attributeDependencies) {
			attributeValueDependencies.put(attributeDependency, AttributeValueDependency.getAttributeValueDependenciesForAttributeDependency
					(attributeDependency));
		}
	}

	/**
	 * Returns a List with all dependencies where the given attribute is set as base attribute.
	 *
	 * @param baseAttribute the attribute all dependencies should be returned for
	 * @return a list with attribute dependencies
	 */
	public List<AttributeDependency> getAttributeDependenciesForAttribute(final TypeTreeNode baseAttribute) {
		ArrayList<AttributeDependency> filteredDependenciesList = new ArrayList<>(attributeDependencies);
		CollectionUtils.filter(filteredDependenciesList, new Predicate() {
			@Override
			public boolean evaluate(Object attributeDependency) {
				return baseAttribute.equals(((AttributeDependency) attributeDependency).getBaseAttribute());
			}
		});
		return filteredDependenciesList;
	}

	/**
	 * Returns a list with all value dependencies for a given attribute dependency.
	 *
	 * @param attributeDependency the dependency all value dependencies should be returned for
	 * @return a list with the attribute value dependencies
	 */
	public List<AttributeValueDependency> getAttributeValueDependenciesForAttributeDependency(final AttributeDependency attributeDependency) {
		return attributeValueDependencies.get(attributeDependency);
	}

	/**
	 * Checks whether a dependency was configured so that this attribute is dependent of another.
	 *
	 * @param attribute Attribute to be checked to be a dependent attribute
	 */
	public boolean isDependentAttributeInDependency(TypeTreeNode attribute) {
		for(AttributeDependency attributeDependency : attributeDependencies) {
			if(attributeDependency.getDependentAttribute().equals(attribute)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether a dependency was configured so that this attribute defines the value of another attribute.
	 *
	 * @param attribute Attribute to be checked to be a base attribute
	 */
	public boolean isBaseAttributeInDependency(TypeTreeNode attribute) {
		for(AttributeDependency attributeDependency : attributeDependencies) {
			if(attributeDependency.getBaseAttribute().equals(attribute)) {
				return true;
			}
		}
		return false;
	}
}
