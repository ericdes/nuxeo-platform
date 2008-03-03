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

package org.nuxeo.ecm.core.search.backend.compass;

import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableFieldDescriptor;

@SuppressWarnings("serial")
public class FakeIndexableResourceDataDescriptor extends
        IndexableFieldDescriptor {

   public FakeIndexableResourceDataDescriptor(String name,
           String analyzer, String type, Boolean multiple, boolean sortable) {
       this.name = name;
       this.indexableAnalyzerName = analyzer;
       this.indexableFieldTypeName = type;
       this.multiple = multiple;
       this.sortable = sortable;
   }

}
