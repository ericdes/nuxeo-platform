/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 *     Florent Guillaume
 *
 * $Id: ExportRestlet.java 30251 2008-02-18 19:17:33Z fguillaume $
 */

package org.nuxeo.ecm.platform.ui.web.restAPI;

import static org.jboss.seam.ScopeType.STATELESS;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Map;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentTreeReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveWriter;
import org.nuxeo.ecm.core.io.impl.plugins.SingleDocumentReader;
import org.nuxeo.ecm.core.io.impl.plugins.XMLDocumentTreeWriter;
import org.nuxeo.ecm.core.io.impl.plugins.XMLDocumentWriter;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Representation;

import com.noelios.restlet.http.HttpConstants;

@Name("exportRestlet")
@Scope(STATELESS)
public class ExportRestlet extends BaseNuxeoRestlet implements Serializable {

    private static final long serialVersionUID = 7831287875548588711L;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @Override
    public void handle(Request req, Response res) {
        boolean exportAsTree;
        boolean exportAsZip;
        String action = req.getResourceRef().getSegments().get(4);
        if (action.equals("exportTree")) {
            exportAsTree = true;
            exportAsZip = true;
        } else { // "export", "exportSingle"
            exportAsTree = false;
            String format = req.getResourceRef().getQueryAsForm().getFirstValue(
                    "format").toLowerCase();
            exportAsZip = "zip".equals(format);
        }

        String repo = (String) req.getAttributes().get("repo");
        if (repo == null || repo.equals("*")) {
            handleError(res, "you must specify a repository");
            return;
        }

        DocumentModel root;
        String docid = (String) req.getAttributes().get("docid");
        try {
            navigationContext.setCurrentServerLocation(new RepositoryLocation(
                    repo));
            CoreSession documentManager = navigationContext.getOrCreateDocumentManager();
            if (docid == null || docid.equals("*")) {
                root = documentManager.getRootDocument();
            } else {
                root = documentManager.getDocument(new IdRef(docid));
            }
        } catch (ClientException e) {
            handleError(res, e);
            return;
        }

        if (exportAsZip) {
            // set the content disposition and file name
            String FILENAME = "export.zip";

            // use the Facelets APIs to set a new header
            Map<String, Object> attributes = res.getAttributes();
            Form headers = (Form) attributes.get(HttpConstants.ATTRIBUTE_HEADERS);
            if (headers == null) {
                headers = new Form();
            }
            headers.add("Content-Disposition", String.format(
                    "attachment; filename=\"%s\";", FILENAME));
            attributes.put(HttpConstants.ATTRIBUTE_HEADERS, headers);
        }

        MediaType mediaType = exportAsZip ? MediaType.APPLICATION_ZIP
                : MediaType.TEXT_XML;
        Representation entity = makeRepresentation(mediaType, root,
                exportAsTree, exportAsZip);

        res.setEntity(entity);
    }

    protected Representation makeRepresentation(MediaType mediaType,
            DocumentModel root, final boolean exportAsTree,
            final boolean exportAsZip) {

        return new ExportRepresentation(mediaType, root) {

            @Override
            protected DocumentPipe makePipe() {
                if (exportAsTree) {
                    return new DocumentPipeImpl(10);
                } else {
                    return new DocumentPipeImpl();
                }
            }

            @Override
            protected DocumentReader makeDocumentReader(
                    CoreSession documentManager, DocumentModel root)
                    throws ClientException {
                DocumentReader documentReader;
                if (exportAsTree) {
                    documentReader = new DocumentTreeReader(documentManager,
                            root, false);
                    if (!exportAsZip) {
                        ((DocumentTreeReader) documentReader).setInlineBlobs(true);
                    }
                } else {
                    documentReader = new SingleDocumentReader(documentManager,
                            root);
                }
                return documentReader;
            }

            @Override
            protected DocumentWriter makeDocumentWriter(
                    OutputStream outputStream) throws IOException {
                DocumentWriter documentWriter;
                if (exportAsZip) {
                    documentWriter = new NuxeoArchiveWriter(outputStream);
                } else {
                    if (exportAsTree) {
                        documentWriter = new XMLDocumentTreeWriter(outputStream);
                    } else {
                        documentWriter = new XMLDocumentWriter(outputStream);
                    }
                }
                return documentWriter;
            }
        };
    }
}
