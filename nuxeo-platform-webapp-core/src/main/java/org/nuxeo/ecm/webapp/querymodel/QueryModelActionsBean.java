/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.webapp.querymodel;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Events;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.PagedDocumentsProvider;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.search.api.client.query.QueryException;
import org.nuxeo.ecm.core.search.api.client.querymodel.QueryModel;
import org.nuxeo.ecm.core.search.api.client.querymodel.QueryModelService;
import org.nuxeo.ecm.core.search.api.client.querymodel.descriptor.QueryModelDescriptor;
import org.nuxeo.ecm.platform.ui.web.api.SortNotSupportedException;
import org.nuxeo.ecm.platform.ui.web.pagination.ResultsProviderFarmUserException;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.helpers.EventNames;
import org.nuxeo.ecm.webapp.pagination.ResultsProvidersCache;
import org.nuxeo.runtime.api.Framework;

@Scope(CONVERSATION)
@Name("queryModelActions")
public class QueryModelActionsBean extends InputController implements
        QueryModelActions, Serializable {

    private static final long serialVersionUID = 7861380986688336804L;

    private static final Log log = LogFactory.getLog(QueryModelActionsBean.class);

    // need to be required = false since this is accessed also before connecting
    // to a rep
    @In(create = true, required = false)
    transient CoreSession documentManager;

    @In(create = true, required = false)
    transient ResultsProvidersCache resultsProvidersCache;

    protected Map<String, QueryModel> queryModels;

    protected transient QueryModelService queryModelService;

    public boolean isInitialized() {
        return documentManager != null;
    }

    public QueryModel get(String queryModelName) throws ClientException {
        if (queryModels == null) {
            queryModels = new HashMap<String, QueryModel>();
        }
        QueryModel model = queryModels.get(queryModelName);
        if (model == null) {
            // because documentManager is not required for injection we check it
            // before usage to avoid later NPE
            if (documentManager == null) {
                throw new ClientException("Cannot create QueryModel for '"
                        + queryModelName + "'. DocumentManager is null.");
            }
            if (log.isDebugEnabled()) {
                log.debug("(Re)building query model " + queryModelName);
            }
            QueryModelDescriptor descriptor = getQueryModelDescriptor(queryModelName);
            if (descriptor == null) {
                throw new ClientException("No such query model: "
                        + queryModelName);
            }
            if (descriptor.isStateless()) {
                model = new QueryModel(descriptor,
                        (NuxeoPrincipal) documentManager.getPrincipal());
            } else {
                assert descriptor.isStateful() : queryModelName
                        + " is neither stateless nor stateful";

                if (!isInitialized()) {
                    throw new ClientException(
                            "Cannot build a stateful query model without a Core session");
                }
                String docTypeName = descriptor.getDocType();
                model = new QueryModel(descriptor,
                        documentManager.createDocumentModel(docTypeName),
                        (NuxeoPrincipal) documentManager.getPrincipal());
            }
            queryModels.put(queryModelName, model);
        }
        return model;
    }

    public PagedDocumentsProvider getResultsProvider(String queryModelName)
            throws ClientException, ResultsProviderFarmUserException {
        try {
            return getResultsProvider(queryModelName, null);
        } catch (SortNotSupportedException e) {
            throw new ClientException("unexpected exception", e);
        }
    }

    public PagedDocumentsProvider getResultsProvider(String queryModelName,
            SortInfo sortInfo) throws ClientException,
            ResultsProviderFarmUserException {
        QueryModel model = get(queryModelName);
        QueryModelDescriptor descriptor = model.getDescriptor();
        if (descriptor.isStateless()) {
            throw new ClientException("queryModelActions "
                    + "is a ResultsProviderFarm for stateful query models only");
        }

        if (!descriptor.isSortable() && sortInfo != null) {
            throw new SortNotSupportedException();
        }

        try {
            PagedDocumentsProvider provider = model.getResultsProvider(null,
                    sortInfo);
            provider.setName(queryModelName);
            return provider;
        } catch (QueryException e) {
            throw new ResultsProviderFarmUserException(
                    "label.search.service.wrong.query", e);
        }
    }

    @Observer(EventNames.QUERY_MODEL_CHANGED)
    public void queryModelChanged(QueryModel qm) {
        resultsProvidersCache.invalidate(qm.getDescriptor().getName());
    }

    protected QueryModelDescriptor getQueryModelDescriptor(String descriptorName) {
        if (queryModelService == null) {
            queryModelService = (QueryModelService) Framework.getRuntime().getComponent(
                    QueryModelService.NAME);
        }
        return queryModelService.getQueryModelDescriptor(descriptorName);
    }

    public void reset(String queryModelName) throws ClientException {
        if (isPersisted(queryModelName)) {
            queryModels.remove(queryModelName);
        }
        QueryModel qm = get(queryModelName);
        qm.reset();
        Events.instance().raiseEvent(EventNames.QUERY_MODEL_CHANGED, qm);
    }

    public void destroy() {
        log.debug("Removing component");
    }

    public boolean isPersisted(String queryModelName) throws ClientException {
        QueryModelDescriptor descriptor = getQueryModelDescriptor(queryModelName);
        if (!descriptor.isStateful()) {
            return false;
        }
        DocumentModel doc = get(queryModelName).getDocumentModel();
        return doc != null && doc.getRef() != null;
    }

    public QueryModel load(String queryModelName, DocumentRef ref)
            throws ClientException {
        QueryModelDescriptor descriptor = getQueryModelDescriptor(queryModelName);
        if (descriptor == null) {
            throw new ClientException(String.format(
                    "QueryModel '%s' does not exist", queryModelName));
        }
        if (!descriptor.isStateful()) {
            throw new ClientException(String.format(
                    "QueryModel '%s' is not stateful", queryModelName));
        }
        if (!isInitialized()) {
            throw new ClientException("Need a Core Session");
        }
        QueryModel qm = new QueryModel(descriptor, documentManager.getDocument(ref),
                (NuxeoPrincipal) documentManager.getPrincipal());
        queryModels.put(queryModelName, qm);
        Events.instance().raiseEvent(EventNames.QUERY_MODEL_CHANGED, qm);
        return qm;
    }

    public QueryModel persist(String queryModelName, String parentPath,
            String name) throws ClientException {
        return persist(queryModelName, parentPath, name, true);
    }

    public QueryModel persist(String queryModelName, String parentPath,
            String name, boolean saveSession) throws ClientException {
        if (isPersisted(queryModelName)) {
            throw new ClientException(String.format(
                    "QueryModel '%s' has already been persisted",
                    queryModelName));
        }
        QueryModelDescriptor descriptor = getQueryModelDescriptor(queryModelName);
        if (descriptor == null) {
            throw new ClientException(String.format(
                    "QueryModel '%s' does not exist", queryModelName));
        }
        if (!descriptor.isStateful()) {
            throw new ClientException(String.format(
                    "QueryModel '%s' is not stateful", queryModelName));
        }
        if (!isInitialized()) {
            throw new ClientException("Need a Core Session");
        }
        QueryModel qm = get(queryModelName);
        DocumentModel doc = qm.getDocumentModel();
        doc.setPathInfo(parentPath, name);
        doc = documentManager.createDocument(doc);
        if (saveSession) {
            documentManager.save();
        }
        return new QueryModel(descriptor, doc,
                (NuxeoPrincipal) documentManager.getPrincipal());
    }

}
