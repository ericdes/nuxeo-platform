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
 *     george
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.tree;

import java.util.List;

import org.nuxeo.ecm.core.api.Filter;

/**
 * Interface for document tree filter
 *
 * @author Anahide Tchertchian
 */
public interface DocumentTreeFilter extends Filter {

    List<String> getExcludedFacets();

    void setExcludedFacets(List<String> excludedFacets);

    List<String> getIncludedFacets();

    void setIncludedFacets(List<String> includedFacets);

    List<String> getExcludedTypes();

    void setExcludedTypes(List<String> excludedTypes);

}
