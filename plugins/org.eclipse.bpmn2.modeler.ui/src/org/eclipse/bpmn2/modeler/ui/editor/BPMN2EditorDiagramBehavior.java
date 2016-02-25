/*******************************************************************************
 * Copyright (c) 2011, 2012, 2013 Red Hat, Inc.
 * All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 * 	Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.bpmn2.modeler.ui.editor;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.transaction.NotificationFilter;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.graphiti.mm.pictograms.ChopboxAnchor;
import org.eclipse.graphiti.ui.editor.DefaultRefreshBehavior;
import org.eclipse.graphiti.ui.editor.DiagramEditorContextMenuProvider;
import org.eclipse.graphiti.ui.internal.editor.DiagramChangeListener;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;

public class BPMN2EditorDiagramBehavior extends DefaultBPMN2EditorDiagramBehavior {

	BPMN2Editor bpmn2Editor;
	private DefaultRefreshBehavior refreshBehavior;
	
	public BPMN2EditorDiagramBehavior(BPMN2Editor bpmn2Editor) {
		super(bpmn2Editor);
		this.bpmn2Editor = bpmn2Editor;
	}

	@Override
	protected ContextMenuProvider createContextMenuProvider() {
		return new DiagramEditorContextMenuProvider(getDiagramContainer().getGraphicalViewer(),
				getDiagramContainer().getActionRegistry(),
				getConfigurationProvider()) {
			@Override
			public void buildContextMenu(IMenuManager manager) {
				super.buildContextMenu(manager);
				IAction action = getDiagramContainer().getActionRegistry().getAction("show.or.hide.source.view"); //$NON-NLS-1$
				action.setText(action.getText());
				manager.add(action);

				int pageIndex = bpmn2Editor.getMultipageEditor().getActivePage();
				int lastPage = bpmn2Editor.getMultipageEditor().getDesignPageCount();
				if (pageIndex > 0 && pageIndex < lastPage) {
					action = getDiagramContainer().getActionRegistry().getAction("delete.page"); //$NON-NLS-1$
					action.setText(action.getText());
					action.setEnabled(action.isEnabled());
					manager.add(action);
				}

				action = getDiagramContainer().getActionRegistry().getAction("show.property.view"); //$NON-NLS-1$
				action.setText(action.getText());
				manager.add(action);

//				action = getDiagramContainer().getActionRegistry().getAction("show.hide.elements"); //$NON-NLS-1$
//				action.setText(action.getText());
//				manager.add(action);
			}
		};
	}
	
	DiagramChangeListener diagramChangeListener;

	@Override
	protected void registerDiagramResourceSetListener() {
		diagramChangeListener = new DiagramChangeListener(this) {

			public NotificationFilter getFilter() {
				return new NotificationFilter.Custom() {

					@Override
					public boolean matches(Notification notification) {
						Object notifier = notification.getNotifier();
						if (notifier instanceof ChopboxAnchor) {
							ChopboxAnchor anchor = (ChopboxAnchor) notifier;
							if (anchor.getParent()==null)
								return false;
						}
						return !notification.isTouch();
					}
					
				};
			}

		};
		TransactionalEditingDomain eDomain = getEditingDomain();
		eDomain.addResourceSetListener(diagramChangeListener);
	}
	
	protected void unregisterDiagramResourceSetListener() {
		if (diagramChangeListener != null) {
			diagramChangeListener.stopListening();
			TransactionalEditingDomain editingDomain = getEditingDomain();
			if (editingDomain != null) {
				editingDomain.removeResourceSetListener(diagramChangeListener);
			}
		}
	}

}
