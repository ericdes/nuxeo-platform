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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.cache;

import java.io.InputStream;

/**
 * This interface defines cache configuration to be used at for an instance
 * of cache service. It provides methods to get the jboss cache configuration
 * file content.
 *
 * @author DM
 *
 */
public interface CacheConfiguration {

    /**
     * Different types of cache configuration that can be used.
     *
     */
    enum Config {
        CFG_REPL_ASYNC, CFG_REPL_SYNC, CFG_INVALIDATION_ASYNC, CFG_INVALIDATION_SYNC,
    }

    String getConfigFile(Config conf) throws CacheServiceException;

    /**
     *
     * @param conf
     * @return the content of jboss cache configuration file
     * @throws CacheServiceException
     */
    InputStream getConfigAsStream(Config conf) throws CacheServiceException;

}
