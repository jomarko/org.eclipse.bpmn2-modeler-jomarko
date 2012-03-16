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
package org.eclipse.bpmn2.modeler.core.features.activity;

import static org.eclipse.bpmn2.modeler.core.features.activity.UpdateActivityCompensateMarkerFeature.IS_COMPENSATE_PROPERTY;
import static org.eclipse.bpmn2.modeler.core.features.activity.UpdateActivityLoopAndMultiInstanceMarkerFeature.IS_LOOP_OR_MULTI_INSTANCE;

import org.eclipse.bpmn2.Activity;
import org.eclipse.bpmn2.BaseElement;
import org.eclipse.bpmn2.modeler.core.features.AbstractAddBPMNShapeFeature;
import org.eclipse.bpmn2.modeler.core.features.activity.UpdateActivityLoopAndMultiInstanceMarkerFeature.LoopCharacteristicType;
import org.eclipse.bpmn2.modeler.core.features.flow.AbstractCreateFlowFeature;
import org.eclipse.bpmn2.modeler.core.utils.AnchorUtil;
import org.eclipse.bpmn2.modeler.core.utils.BusinessObjectUtil;
import org.eclipse.bpmn2.modeler.core.utils.FeatureSupport;
import org.eclipse.bpmn2.modeler.core.utils.GraphicsUtil;
import org.eclipse.bpmn2.modeler.core.utils.StyleUtil;
import org.eclipse.bpmn2.modeler.core.utils.Tuple;
import org.eclipse.graphiti.datatypes.ILocation;
import org.eclipse.graphiti.features.ICreateConnectionFeature;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.IReconnectionFeature;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.features.context.impl.CreateConnectionContext;
import org.eclipse.graphiti.features.context.impl.ReconnectionContext;
import org.eclipse.graphiti.mm.algorithms.Rectangle;
import org.eclipse.graphiti.mm.algorithms.RoundedRectangle;
import org.eclipse.graphiti.mm.pictograms.AnchorContainer;
import org.eclipse.graphiti.mm.pictograms.Connection;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.FixPointAnchor;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.eclipse.graphiti.mm.pictograms.Shape;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeService;

public abstract class AbstractAddActivityFeature extends AbstractAddBPMNShapeFeature {

	public static final String ACTIVITY_DECORATOR = "activity-decorator";
	public static final String IS_ACTIVITY = "activity";

	public AbstractAddActivityFeature(IFeatureProvider fp) {
		super(fp);
	}

	@Override
	public boolean canAdd(IAddContext context) {
		boolean intoDiagram = context.getTargetContainer().equals(getDiagram());
		boolean intoLane = FeatureSupport.isTargetLane(context) && FeatureSupport.isTargetLaneOnTop(context);
		boolean intoParticipant = FeatureSupport.isTargetParticipant(context);
		boolean intoSubProcess = FeatureSupport.isTargetSubProcess(context);
				
		return intoDiagram || intoLane || intoParticipant || intoSubProcess;
	}

	@Override
	public PictogramElement add(IAddContext context) {
		Activity activity = (Activity) context.getNewObject();

		IGaService gaService = Graphiti.getGaService();
		IPeService peService = Graphiti.getPeService();

		int width = context.getWidth() > 0 ? context.getWidth() : this.getWidth();
		int height = context.getHeight() > 0 ? context.getHeight() : this.getHeight();

		ContainerShape containerShape = peService.createContainerShape(context.getTargetContainer(), true);
		Rectangle invisibleRect = gaService.createInvisibleRectangle(containerShape);
		gaService.setLocationAndSize(invisibleRect, context.getX(), context.getY(), width, height);

		Shape rectShape = peService.createShape(containerShape, false);
		RoundedRectangle rect = gaService.createRoundedRectangle(rectShape, 5, 5);
		StyleUtil.applyBGStyle(rect, this);
		gaService.setLocationAndSize(rect, 0, 0, width, height);
		link(rectShape, activity);
		decorateActivityRectangle(rect);
		peService.setPropertyValue(rectShape, IS_ACTIVITY, Boolean.toString(true));

		ContainerShape markerContainer = peService.createContainerShape(containerShape, false);
		Rectangle markerInvisibleRect = gaService.createInvisibleRectangle(markerContainer);
		int h = 10;
		int y = height - h - 3 - getMarkerContainerOffset();
		gaService.setLocationAndSize(markerInvisibleRect, 0, y, invisibleRect.getWidth(), h);
		peService.setPropertyValue(markerContainer, GraphicsUtil.ACTIVITY_MARKER_CONTAINER, Boolean.toString(true));

		hook(activity, containerShape, context, width, height); // hook for subclasses to inject extra code

		peService.createChopboxAnchor(containerShape);
		AnchorUtil.addFixedPointAnchors(containerShape, rect);

		createDIShape(containerShape, activity);

		Graphiti.getPeService().setPropertyValue(containerShape, IS_COMPENSATE_PROPERTY, Boolean.toString(false));
		Graphiti.getPeService().setPropertyValue(containerShape, IS_LOOP_OR_MULTI_INSTANCE,
		        LoopCharacteristicType.NULL.getName());
		// set a property on the decorators so we can distinguish them from the real children (i.e. tasks, etc.)
		for (PictogramElement pe : containerShape.getChildren()) {
			Graphiti.getPeService().setPropertyValue(pe, ACTIVITY_DECORATOR, "true");
		}
		
		if (context.getTargetConnection()!=null) {
			Connection connection = context.getTargetConnection();
			AnchorContainer oldSourceContainer = connection.getStart().getParent();
			AnchorContainer oldTargetContainer = connection.getEnd().getParent();
			BaseElement baseElement = BusinessObjectUtil.getFirstElementOfType(connection, BaseElement.class);
			ILocation targetLocation = Graphiti.getLayoutService().getLocationRelativeToDiagram(containerShape);
			
			Tuple<FixPointAnchor, FixPointAnchor> anchors = AnchorUtil.getSourceAndTargetBoundaryAnchors(oldSourceContainer, containerShape, connection);

			ReconnectionContext rc = new ReconnectionContext(connection, connection.getEnd(), anchors.getSecond(), targetLocation);
			rc.setTargetPictogramElement(containerShape);
			rc.setReconnectType(ReconnectionContext.RECONNECT_TARGET);
			IReconnectionFeature rf = getFeatureProvider().getReconnectionFeature(rc);
			rf.reconnect(rc);
			
			// connection = get create feature, create connection
			CreateConnectionContext ccc = new CreateConnectionContext();
			ccc.setSourcePictogramElement(containerShape);
			ccc.setTargetPictogramElement(oldTargetContainer);
			anchors = AnchorUtil.getSourceAndTargetBoundaryAnchors(containerShape, oldTargetContainer, connection);
			ccc.setSourceAnchor(anchors.getFirst());
			ccc.setTargetAnchor(anchors.getSecond());
			
			Connection newConnection = null;
			for (ICreateConnectionFeature cf : getFeatureProvider().getCreateConnectionFeatures()) {
				if (cf instanceof AbstractCreateFlowFeature) {
					AbstractCreateFlowFeature acf = (AbstractCreateFlowFeature) cf;
					if (acf.getBusinessObjectClass().isAssignableFrom(baseElement.getClass())) {
						newConnection = acf.create(ccc);
						break;
					}
				}
			}
		}
		
		updatePictogramElement(containerShape);
		layoutPictogramElement(containerShape);
		
		return containerShape;
	}

	protected void decorateActivityRectangle(RoundedRectangle rect) {
	}

	protected void hook(Activity activity, ContainerShape container, IAddContext context, int width, int height) {
	}

	protected int getMarkerContainerOffset() {
		return 0;
	}

	protected abstract int getWidth();

	protected abstract int getHeight();
}