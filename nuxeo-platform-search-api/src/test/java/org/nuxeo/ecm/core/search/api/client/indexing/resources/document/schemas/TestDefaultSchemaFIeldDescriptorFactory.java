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
 *     gracinet
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.api.client.indexing.resources.document.schemas;

import java.util.HashMap;

import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.search.api.client.common.TypeManagerServiceDelegate;
import org.nuxeo.ecm.core.search.api.indexing.resources.configuration.IndexableFieldDescriptor;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author gracinet
 *
 */
public class TestDefaultSchemaFIeldDescriptorFactory extends NXRuntimeTestCase {

    final DefaultSchemaFieldDescriptorsFactory factory = new DefaultSchemaFieldDescriptorsFactory();
    private HashMap<String, IndexableFieldDescriptor> fields;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle("nuxeo-core-schema");
        deployContrib("default-fieldfactory-components-test-setup.xml");
    }

    public void testConfiguration() throws Exception {
        SchemaManager typeManager = TypeManagerServiceDelegate.getRemoteTypeManagerService();
        assertNotNull(typeManager);
        assertNotNull(factory.getSchemaByPrefix("v"));
        assertNotNull(factory.getSchemaByPrefix("cpl"));
    }

    public void loadFieldsFromSchema(String name) {
        fields = new HashMap<String, IndexableFieldDescriptor>();
        for (IndexableFieldDescriptor field: factory.getFieldDescriptorsBySchemaName(name, null)) {
            fields.put(field.getIndexingName(), field);
        }
    }

    public void testSimpleProperties() throws Exception {
        loadFieldsFromSchema("various");
        IndexableFieldDescriptor field;

        field = fields.get("title");
        assertNotNull(field);
        assertTrue(field.isIndexed());
        assertEquals("keyword", field.getIndexingType());

        field = fields.get("created");
        assertNotNull(field);
        assertTrue(field.isIndexed());
        assertEquals("date", field.getIndexingType());
        assertFalse(field.isMultiple());

        field = fields.get("available");
        assertNotNull(field);
        assertTrue(field.isIndexed());
        assertEquals("boolean", field.getIndexingType());
        assertFalse(field.isMultiple());
    }

    public void testListProperties() throws Exception {
        loadFieldsFromSchema("lists");
        IndexableFieldDescriptor field;

        field = fields.get("contributors");
        assertNotNull(field);
        assertTrue(field.isIndexed());
        assertEquals("keyword", field.getIndexingType());
        assertTrue(field.isMultiple());

        field = fields.get("dates");
        assertNotNull(field);
        assertTrue(field.isIndexed());
        assertEquals("date", field.getIndexingType());
    }

    public void testComplexProperty() throws Exception {
        loadFieldsFromSchema("complex");

        IndexableFieldDescriptor field;

        field = fields.get("tasks:what");
        assertNotNull(field);
        assertEquals("keyword", field.getIndexingType());
        assertTrue(field.isIndexed());
        assertTrue(field.isStored());
        assertTrue(field.isMultiple());

        field = fields.get("tasks:when");
        assertNotNull(field);
        assertEquals("date", field.getIndexingType());
        assertTrue(field.isIndexed());
        assertTrue(field.isStored());
        assertTrue(field.isMultiple());
    }

}
