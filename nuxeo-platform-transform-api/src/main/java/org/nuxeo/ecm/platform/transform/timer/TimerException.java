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
 * $Id: TimerException.java 28476 2008-01-04 09:52:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.transform.timer;

import org.nuxeo.ecm.core.api.WrappedException;

/**
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class TimerException extends Exception {

    private static final long serialVersionUID = 1L;

    public TimerException() {
    }

    public TimerException(String message, Throwable cause) {
        super(message, WrappedException.wrap(cause));
    }

    public TimerException(String message) {
        super(message);
    }

    public TimerException(Throwable cause) {
        super(WrappedException.wrap(cause));
    }

}
