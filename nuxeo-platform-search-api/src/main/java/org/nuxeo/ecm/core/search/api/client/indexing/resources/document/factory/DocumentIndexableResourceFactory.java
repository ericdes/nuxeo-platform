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
 *     anguenot
 *
 * $Id: DocumentIndexableResourceFactory.java 28476 2008-01-04 09:52:52Z sfermigier $
 */

package org.nuxeo.ecm.core.search.api.client.indexing.resources.document.factory;

import java.io.Serializable;

import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResource;
import org.nuxeo.ecm.core.search.api.backend.indexing.resources.ResolvedResources;
import org.nuxeo.ecm.core.search.api.client.IndexingException;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.AbstractIndexableResourceFactory;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.IndexableResource;
import org.nuxeo.ecm.core.search.api.client.indexing.resources.document.impl.DocumentIndexableResourceImpl;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableResourceConf;

/**
 * Document indexable resource factory.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class DocumentIndexableResourceFactory extends
        AbstractIndexableResourceFactory {

    private static final long serialVersionUID = 1L;

    public IndexableResource createEmptyIndexableResource() {
        return new DocumentIndexableResourceImpl();
    }

    public IndexableResource createIndexableResourceFrom(
            Serializable targetResource, IndexableResourceConf conf,
            String sid) {
        // TODO refactor the IndexableResourcesFactory.
        return null;
    }

    public ResolvedResource resolveResourceFor(IndexableResource resource)
            throws IndexingException {
        // TODO Auto-generated method stub
        return null;
    }

    public ResolvedResources resolveResourcesFor(IndexableResource resource) {
        // TODO Auto-generated method stub
        return null;
    }

}
