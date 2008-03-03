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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: FakeRepositoryFactory.java 25081 2007-09-18 14:57:22Z atchertchian $
 */

package org.nuxeo.ecm.platform.relations.io.test;

import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.repository.RepositoryDescriptor;
import org.nuxeo.ecm.core.repository.RepositoryFactory;

/**
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class FakeRepositoryFactory implements RepositoryFactory {

    public Repository createRepository(RepositoryDescriptor descriptor)
            throws Exception {
        return new FakeRepository();
    }

}
