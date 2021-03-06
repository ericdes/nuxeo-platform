/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.ecm.webapp.tree;

import static org.jboss.seam.ScopeType.CONVERSATION;
import static org.jboss.seam.annotations.Install.FRAMEWORK;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.security.PermitAll;
import javax.ejb.Remove;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Filter;
import org.nuxeo.ecm.core.api.Sorter;
import org.nuxeo.ecm.core.search.api.client.querymodel.QueryModel;
import org.nuxeo.ecm.core.search.api.client.querymodel.descriptor.QueryModelDescriptor;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.runtime.api.Framework;
import org.richfaces.component.UITree;
import org.richfaces.event.NodeExpandedEvent;

/**
 * Manages the navigation tree.
 *
 * @author Razvan Caraghin
 * @author Anahide Tchertchian
 */
@Scope(CONVERSATION)
@Name("treeActions")
@Install(precedence = FRAMEWORK)
public class TreeActionsBean implements TreeActions, Serializable {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(TreeActionsBean.class);

    public static final String NODE_SELECTED_MARKER = TreeActionsBean.class.getName() +
            "_NODE_SELECTED_MARKER";

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    protected List<DocumentTreeNode> tree;

    protected String currentDocumentPath;

    public List<DocumentTreeNode> getTreeRoots() throws ClientException {
        if (tree == null) {
            tree = new ArrayList<DocumentTreeNode>();
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            DocumentModel firstAccessibleParent = null;
            if (currentDocument != null) {
                List<DocumentModel> parents = documentManager.getParentDocuments(currentDocument.getRef());
                if (!parents.isEmpty()) {
                    firstAccessibleParent = parents.get(0);
                } else if (!"Root".equals(currentDocument.getType()) &&
                        currentDocument.isFolder()) {
                    // default on current doc
                    firstAccessibleParent = currentDocument;
                }
            }
            if (firstAccessibleParent != null) {
                Filter filter = null;
                Filter leafFilter = null;
                Sorter sorter = null;
                QueryModel queryModel = null;
                try {
                    TreeManager treeManager = Framework.getService(TreeManager.class);
                    filter = treeManager.getFilter(DEFAULT_TREE_PLUGIN_NAME);
                    leafFilter = treeManager.getLeafFilter(DEFAULT_TREE_PLUGIN_NAME);
                    sorter = treeManager.getSorter(DEFAULT_TREE_PLUGIN_NAME);
                    QueryModelDescriptor queryModelDescriptor = treeManager.getQueryModelDescriptor(DEFAULT_TREE_PLUGIN_NAME);
                    queryModel = queryModelDescriptor == null ? null
                            : new QueryModel(queryModelDescriptor);
                } catch (Exception e) {
                    log.error(
                            "Could not fetch filter, sorter or node type for tree ",
                            e);
                }
                DocumentTreeNode treeRoot = new DocumentTreeNodeImpl(
                        firstAccessibleParent, filter, leafFilter, sorter,
                        queryModel);
                tree.add(treeRoot);
                log.debug("Tree initialized with document: " +
                        firstAccessibleParent.getId());
            } else {
                log.warn("Could not initialize the navigation tree: no parent"
                        + " found for current document");
            }
        }
        return tree;
    }

    public void changeExpandListener(NodeExpandedEvent event) {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        Map<String, Object> requestMap = facesContext.getExternalContext().getRequestMap();
        requestMap.put(NODE_SELECTED_MARKER, Boolean.TRUE);
    }

    protected Boolean isNodeExpandEvent() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            ExternalContext externalContext = facesContext.getExternalContext();
            if (externalContext != null) {
                return Boolean.TRUE.equals(externalContext.getRequestMap().get(
                        NODE_SELECTED_MARKER));
            }
        }
        return false;
    }

    protected String getCurrentDocumentPath() {
        if (currentDocumentPath == null) {
            DocumentModel currentDoc = navigationContext.getCurrentDocument();
            if (currentDoc != null) {
                currentDocumentPath = currentDoc.getPathAsString();
            }
        }
        return currentDocumentPath;
    }

    public Boolean adviseNodeOpened(UITree treeComponent) {
        if (!isNodeExpandEvent()) {
            Object value = treeComponent.getRowData();
            if (value instanceof DocumentTreeNode) {
                DocumentTreeNode treeNode = (DocumentTreeNode) value;
                String nodePath = treeNode.getPath();
                String currentDocPath = getCurrentDocumentPath();
                if (currentDocPath != null && nodePath != null &&
                        currentDocPath.startsWith(nodePath)) {
                    // additional slower check for strict path prefix
                    if ((currentDocPath + '/').startsWith(nodePath + '/')) {
                        return true;
                    }
                }
            }
        }
        return null;
    }

    @Observer(value = { EventNames.USER_ALL_DOCUMENT_TYPES_SELECTION_CHANGED }, create = false)
    @BypassInterceptors
    public void resetCurrentDocumentData() {
        currentDocumentPath = null;
    }

    @Observer(value = { EventNames.GO_HOME,
            EventNames.DOMAIN_SELECTION_CHANGED, EventNames.DOCUMENT_CHANGED,
            EventNames.DOCUMENT_SECURITY_CHANGED,
            EventNames.DOCUMENT_CHILDREN_CHANGED }, create = false)
    @BypassInterceptors
    public void reset() {
        tree = null;
        resetCurrentDocumentData();
    }

    @Destroy
    @Remove
    @PermitAll
    public void destroy() {
        log.debug("Removing SEAM component...");
    }

}
