/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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


/**
 * This RESTLET allows to update document properties
 * @author jthimonier
 */
package org.nuxeo.ecm.platform.ui.web.restAPI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.ui.web.tag.fn.LiveEditConstants;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;

public class UpdateDocumentRestlet extends BaseStatelessNuxeoRestlet implements
        LiveEditConstants {

    private static final Log log = LogFactory.getLog(UpdateDocumentRestlet.class);

    @Override
    protected void doHandleStatelessRequest(Request req, Response res) {
        String repoId = (String) req.getAttributes().get("repo");
        String docId = (String) req.getAttributes().get("docid");

        DOMDocumentFactory domfactory = new DOMDocumentFactory();
        DOMDocument result = (DOMDocument) domfactory.createDocument();

        // init repo and document
        Boolean initOk = super.initRepositoryAndTargetDocument(res, repoId,
                docId);
        if (!initOk) {
            return;
        }

        try {
            Form queryParameters = req.getResourceRef().getQueryAsForm();
            for (String paramName : queryParameters.getNames()) {
                if (!DOC_TYPE.equals(paramName)) {
                    // treat all non doctype parameters as string fields
                    targetDocument.setPropertyValue(paramName,
                            getQueryParamValue(req, paramName, null));
                }
            }
            session.saveDocument(targetDocument);
            session.save();

            // build the XML response document holding the ref
            Element docElement = result.addElement(documentTag);
            docElement.addElement(docRefTag).setText(
                    "Document " + docId + " has been updated");
            res.setEntity(result.asXML(), MediaType.TEXT_XML);
        } catch (ClientException e) {
            log.error(e.getMessage(),e);
            handleError(res, e);
        }
    }

}
