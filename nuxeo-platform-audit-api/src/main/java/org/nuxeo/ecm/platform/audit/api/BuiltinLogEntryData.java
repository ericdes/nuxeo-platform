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
 * $Id$
 */

package org.nuxeo.ecm.platform.audit.api;

/**
 * Log entry builtin data
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public final class BuiltinLogEntryData {

    public static final String LOG_ID = "id";

    public static final String LOG_EVENT_ID = "eventId";

    public static final String LOG_EVENT_DATE = "eventDate";

    public static final String LOG_DOC_UUID = "docUUID";

    public static final String LOG_DOC_PATH = "docPath";

    public static final String LOG_DOC_TYPE = "docType";

    public static final String LOG_PRINCIPAL_NAME = "principalName";

    public static final String LOG_COMMENT = "comment";

    public static final String LOG_CATEGORY = "category";

    public static final String LOG_DOC_LIFE_CYCLE = "docLifeCycle";

    private BuiltinLogEntryData() {
    }

}
