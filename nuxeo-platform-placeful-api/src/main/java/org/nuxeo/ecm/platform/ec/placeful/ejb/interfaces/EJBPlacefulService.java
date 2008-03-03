/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: EJBPlacefulService.java 19072 2007-05-21 16:23:42Z sfermigier $
 */
package org.nuxeo.ecm.platform.ec.placeful.ejb.interfaces;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.ec.placeful.Annotation;

/**
 *
 * @author <a href="mailto:rspivak@nuxeo.com">Ruslan Spivak</a>
 *
 */
public interface EJBPlacefulService {

    Annotation getAnnotation(String uuid, String name)
            throws ClassNotFoundException;

    List<Annotation> getAnnotationListByParamMap(Map<String, Object> paramMap,
            String name) throws ClassNotFoundException;

    void setAnnotation(Annotation annotation);

    void removeAnnotation(Annotation annotation);

    void removeAnnotationListByParamMap(Map<String, Object> paramMap,
            String name) throws ClassNotFoundException;

}
