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
 *     George Lefter
 *
 * $Id$
 */

package org.nuxeo.ecm.directory;

/**
 * @author <a href='mailto:glefter@nuxeo.com'>George Lefter</a>
 *
 */
public class SizeLimitExceededException extends DirectoryException {

    private static final long serialVersionUID = 1L;

    public SizeLimitExceededException() {
    }

    /**
     * @param message
     * @param th
     */
    public SizeLimitExceededException(String message, Throwable th) {
        super(message, th);
    }

    /**
     * @param message
     */
    public SizeLimitExceededException(String message) {
        super(message);
    }

    /**
     * @param th
     */
    public SizeLimitExceededException(Throwable th) {
        super(th);
    }

}
