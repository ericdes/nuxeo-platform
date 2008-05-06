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

package org.nuxeo.ecm.webapp.base;

import java.util.ArrayList;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.core.FacesMessages;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.platform.types.TypeManager;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.action.TypesTool;
import org.nuxeo.ecm.webapp.helpers.EventManager;
import org.nuxeo.ecm.webapp.helpers.ResourcesAccessor;
import org.nuxeo.platform.cache.web.CacheUpdateNotifier;

/**
 * Contains generic functionality usable by all action listeners.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
public abstract class InputController {

    private static final Log log = LogFactory.getLog(InputController.class);

    @In(value = CacheUpdateNotifier.SEAM_NAME_CACHE_NOTIFIER, create = true)
    protected CacheUpdateNotifier cacheUpdateNotifier;

    @In(create = true)
    protected ActionManager actionManager;

    @In(create = true)
    protected TypeManager typeManager;

    @In(create = true)
    protected NavigationContext navigationContext;

    @In(create = true)
    protected EventManager eventManager;

    @In(required = false, create = true)

    /**
     * @deprecated injecting current document is not a good idea, should be
     *             fetched from navigationContext directly.
     */
    @Deprecated
    protected DocumentModel currentDocument;

    @In(create = true, required = false)
    protected FacesMessages facesMessages;

    @In(create = true)
    // won't inject this because of seam problem after activation
    // ::protected Map<String, String> messages;
    protected ResourcesAccessor resourcesAccessor;

    @In(create = true)
    protected TypesTool typesTool;

    /**
     * Utility method that helps remove a {@link DocumentModel} from a list. The
     * document models are compared on {@link DocumentRef}s.
     *
     * @param documentList
     * @param document
     */
    public void removeDocumentFromList(List<DocumentModel> documentList,
            DocumentModel document) {
        if (null == document) {
            log.error("Received nul doc, not removing anything...");
            return;
        }

        log.debug("Removing document "
                + document.getProperty("dublincore", "title") + " from list...");

        for (int i = 0; i < documentList.size(); i++) {
            if (documentList.get(i).getRef().equals(document.getRef())) {
                documentList.remove(i);
            }
        }
    }

    /**
     * Logs a {@link DocumentModel} title and the passed string (info).
     *
     * @param someLogString
     * @param document
     */
    public void logDocumentWithTitle(String someLogString,
            DocumentModel document) {
        if (null != document) {
            log.trace('[' + getClass().getSimpleName() + "] "
                    + someLogString + ' '
                    + document.getProperty("dublincore", "title"));
            log.debug("CURRENT DOC PATH: " + document.getPathAsString());
        } else {
            log.trace('[' + getClass().getSimpleName() + "] "
                    + someLogString + " NULL DOC");
        }
    }

    /**
     * Logs a {@link DocumentModel} name and the passed string (info).
     *
     * @param someLogString
     * @param document
     */
    public void logDocumentWithName(String someLogString, DocumentModel document) {
        if (null != document) {
            log.debug('[' + getClass().getSimpleName() + "] "
                    + someLogString + ' ' + document.getName());
        } else {
            log.debug('[' + getClass().getSimpleName() + "] "
                    + someLogString + " NULL DOC");
        }
    }

    /**
     * Extracts references from a list of document models.
     *
     * @param documents
     * @return
     */
    protected List<DocumentRef> extractReferences(List<DocumentModel> documents) {
        List<DocumentRef> references = new ArrayList<DocumentRef>();

        for (DocumentModel docModel : documents) {
            references.add(docModel.getRef());
        }

        return references;
    }

    protected void setFacesMessage(String msg) {
        facesMessages.add(FacesMessage.SEVERITY_INFO,
                resourcesAccessor.getMessages().get(msg));
    }

    /**
     * Is the current logged user an administrator?
     *
     * @return
     */
    public boolean getAdministrator() {
        boolean administrator = false;

        NuxeoPrincipal principal = (NuxeoPrincipal) FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal();

        List<String> groups = principal.getGroups();

        if (groups.contains("administrators")) {
            administrator = true;
        }

        return administrator;
    }

    /**
     * Utility method to return non 'null' JSF outcome that do not change the
     * current view. The problem with null outcome is that some seam components
     * are not refetched and thus the JSF tree might hold references that are no
     * longer up-to-date, esp. in search results views whose documents lists are
     * computed by an EVENT scoped seam factory.
     *
     * @param actionOutcome a string that might be used in the future to compute
     *            the JSF outcome in a cleaner way
     *
     * @return the same view as previously based on the expectation that the
     *         'outcome_name' match the view id '/outcome_name.xhtml'
     *         faces-config.xml
     */
    public String computeOutcome(String actionOutcome) {
        // actionOutcome is currently ignored on purpose but might be useful in
        // the future
        String viewId = FacesContext.getCurrentInstance().getViewRoot().getViewId();
        return viewId.substring(1, viewId.indexOf(".xhtml"));
    }

}
