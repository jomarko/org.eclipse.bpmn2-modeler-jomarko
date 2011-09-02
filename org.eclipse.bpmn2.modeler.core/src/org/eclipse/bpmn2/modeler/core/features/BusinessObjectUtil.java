/******************************************************************************* 
 * Copyright (c) 2011 Red Hat, Inc. 
 *  All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 *
 * @author Ivar Meikas
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.core.features;

import java.util.Collection;

import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.di.BPMNShape;
import org.eclipse.bpmn2.modeler.core.utils.AnchorUtil;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IPeService;

public class BusinessObjectUtil {

	@SuppressWarnings("rawtypes")
	public static boolean containsElementOfType(PictogramElement elem, Class clazz) {
		if (elem.getLink() == null) {
			return false;
		}
		// if this is a connection point, look at business objects of the connection
		if (AnchorUtil.isConnectionPoint(elem)) {
			elem = AnchorUtil.getConnectionPointOwner((Shape)elem);
		}
		EList<EObject> businessObjs = elem.getLink().getBusinessObjects();
		for (EObject eObject : businessObjs) {
			if (clazz.isInstance(eObject)) {
				return true;
			}
		}
		return false;
	}

	public static <T extends EObject> T getFirstElementOfType(PictogramElement elem, Class<T> clazz) {
		return getFirstElementOfType(elem,clazz,false);
	}

	@SuppressWarnings("unchecked")
	public static <T extends EObject> T getFirstElementOfType(PictogramElement elem, Class<T> clazz, boolean searchParents) {
		if (elem.getLink() == null) {
			if (searchParents) {
				while (elem!=null && elem.getLink()==null && elem.eContainer() instanceof PictogramElement)
					elem = (PictogramElement)elem.eContainer();
			}
			if (elem==null || elem.getLink() == null)
				return null;
		}
		// if this is a connection point, look at business objects of the connection
		if (AnchorUtil.isConnectionPoint(elem)) {
			elem = AnchorUtil.getConnectionPointOwner((Shape)elem);
		}
		EList<EObject> businessObjs = elem.getLink().getBusinessObjects();
		for (EObject eObject : businessObjs) {
			if (clazz.isInstance(eObject)) {
				return (T) eObject;
			}
		}
		return null;
	}

	public static PictogramElement getFirstBaseElementFromDiagram(Diagram diagram, BaseElement e) {
		PictogramElement foundElem = null;

		IPeService peService = Graphiti.getPeService();
		Collection<PictogramElement> elements = peService.getAllContainedPictogramElements(diagram);
		for (PictogramElement pe : elements) {
			BaseElement be = getFirstElementOfType(pe, e.getClass());
			if (be != null && be.equals(e)) {
				foundElem = pe;
				break;
			}
		}

		return foundElem;
	}

	public static PictogramElement getPictogramElementFromDiagram(Diagram diagram, BPMNShape bpmnShape) {
		PictogramElement foundElem = null;

		IPeService peService = Graphiti.getPeService();
		Collection<PictogramElement> elements = peService.getAllContainedPictogramElements(diagram);
		for (PictogramElement pe : elements) {
			BPMNShape s = getFirstElementOfType(pe, BPMNShape.class);
			if (s != null && s.equals(bpmnShape)) {
				foundElem = pe;
				break;
			}
		}

		return foundElem;
	}
}