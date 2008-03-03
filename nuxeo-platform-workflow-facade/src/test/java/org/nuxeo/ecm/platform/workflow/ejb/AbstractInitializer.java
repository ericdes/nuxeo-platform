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
 * $Id: AbstractInitializer.java 19070 2007-05-21 16:05:43Z sfermigier $
 */

package org.nuxeo.ecm.platform.workflow.ejb;

import junit.framework.TestCase;

import org.nuxeo.ecm.platform.workflow.api.client.wfmc.WAPI;

/**
 * Abstract test initializer.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
public abstract class AbstractInitializer extends TestCase {

    protected WAPI wapi;

    protected void setUp() throws Exception {
        super.setUp();
        _initializeTest();
    }

    protected void tearDown() throws Exception {
        _unInitializeTest();
        super.tearDown();
    }

    protected abstract void _initializeTest() throws Exception;

    protected abstract void _unInitializeTest() throws Exception;

}
