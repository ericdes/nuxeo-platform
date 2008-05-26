/*
 * (C) Copyright 2002 - 2006 Nuxeo SARL <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 *
 * $Id$
 */

package org.nuxeo.ecm.core.search.api.querymodel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.SortInfo;
import org.nuxeo.ecm.core.repository.jcr.testing.RepositoryOSGITestCase;
import org.nuxeo.ecm.core.search.api.client.querymodel.QueryModel;
import org.nuxeo.ecm.core.search.api.client.querymodel.QueryModelService;
import org.nuxeo.ecm.core.search.api.client.querymodel.descriptor.QueryModelDescriptor;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="ogrisel@nuxeo.com">Olivier Grisel</a>
 *
 */
public class QueryModelTestCase extends RepositoryOSGITestCase {

    protected QueryModel statelessModel;

    protected QueryModel statelessModelWithSort;

    private QueryModel statelessModelWithListParam;

    private QueryModel statelessModelWithBooleanParam;

    protected QueryModel statefulModel;

    protected DocumentModel documentModel;

    protected QueryModel statefulModel2;

    protected DocumentModel documentModel2;

    private QueryModel statefulModelWithFixedPart;

    private DocumentModel documentModelWithFixedPart;

    private static final String TEST_BUNDLE = "org.nuxeo.ecm.platform.search.api.tests";

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployBundle("org.nuxeo.ecm.platform.search.api"); // ourselves !
        deployContrib(TEST_BUNDLE, "querymodel-components-test-setup.xml");

        openRepository();

        // GR old-style lookup kept to ensure BBB, see NXP-2161
        QueryModelService service = (QueryModelService) Framework.getRuntime().getComponent(
                QueryModelService.NAME);

        statefulModel = initializeStatefulQueryModel(service.getQueryModelDescriptor("statefulModel"));
        documentModel = statefulModel.getDocumentModel();

        statefulModel2 = initializeStatefulQueryModel(service.getQueryModelDescriptor("statefulModel2"));
        documentModel2 = statefulModel2.getDocumentModel();

        statefulModelWithFixedPart = initializeStatefulQueryModel(service.getQueryModelDescriptor("statefulModelWithFixedPart"));
        documentModelWithFixedPart = statefulModelWithFixedPart.getDocumentModel();

        statelessModel = new QueryModel(
                service.getQueryModelDescriptor("statelessModel"), null);

        statelessModelWithSort = new QueryModel(
                service.getQueryModelDescriptor("statelessModelWithSort"), null);

        statelessModelWithListParam = new QueryModel(
                service.getQueryModelDescriptor("statelessModelWithListParam"),
                null);

        statelessModelWithBooleanParam = new QueryModel(service.getQueryModelDescriptor("statelessModelWithBooleanParam"),
                null);

    }

    protected QueryModel initializeStatefulQueryModel(
            QueryModelDescriptor descriptor) throws ClientException {
        DocumentModel documentModel = coreSession.createDocumentModel(descriptor.getDocType());
        return new QueryModel(descriptor, documentModel, null);
    }

    // NXP-2161
    public void testModernLookup() throws Exception {
        assertNotNull(Framework.getService(QueryModelService.class));
    }

    public void testStatelessQueryModel() throws ClientException {
        QueryModelDescriptor descriptor = statelessModel.getDescriptor();
        assertTrue(descriptor.isStateless());
        assertFalse(descriptor.isStateful());
        assertEquals(
                "SELECT * FROM Document WHERE dc:contributors = 'Administrator' AND ecm:path STARTSWITH 'somelocation'",
                descriptor.getQuery(new Object[] { "Administrator",
                        "somelocation" }));
        try {
            descriptor.getQuery(documentModel);
            fail("Should have raised an exception since stateless models need a parameters array");
        } catch (ClientException e) {
        }

    }

    // NXP-2059
    public void testStatelessQueryModelWithSortAndNullParams()
            throws ClientException {
        QueryModelDescriptor descriptor = statelessModelWithSort.getDescriptor();
        assertTrue(descriptor.isStateless());
        assertFalse(descriptor.isStateful());
        String query = "SELECT * FROM Document WHERE ecm:primaryType in ('File') ORDER BY dc:modified";
        SortInfo sortInfo = descriptor.getDefaultSortInfo(documentModel);
        assertEquals(query, descriptor.getQuery(new Object[0], sortInfo));
        assertEquals(query, descriptor.getQuery((Object[]) null, sortInfo));
    }

    // NXP-2195
    public void testStatelessModelWithListParam() throws ClientException {
        QueryModelDescriptor descriptor = statelessModelWithListParam.getDescriptor();
        assertTrue(descriptor.isStateless());
        assertFalse(descriptor.isStateful());
        String query = "SELECT * FROM Document WHERE ecm:primaryType IN ('File', 'Folder')";

        // test String[] param
        String[] typeArray = { "File", "Folder" };
        assertEquals(query, descriptor.getQuery(new Object[] { typeArray }));

        // test List<String> param
        List<String> typeList = Arrays.asList(typeArray);
        assertEquals(query, descriptor.getQuery(new Object[] { typeList }));

        // test empty param
        query = "SELECT * FROM Document WHERE ecm:primaryType IN ()";
        typeArray = new String[0];
        assertEquals(query, descriptor.getQuery(new Object[] { typeArray }));
        typeList = Arrays.asList(typeArray);
        assertEquals(query, descriptor.getQuery(new Object[] { typeList }));

    }

    // NXP-2418
    public void testStratelessModelWithBooleanParam() throws ClientException {
        QueryModelDescriptor descriptor = statelessModelWithBooleanParam.getDescriptor();
        assertTrue(descriptor.isStateless());
        assertFalse(descriptor.isStateful());

        // test with false boolean
        String query = "SELECT * FROM Document WHERE ecm:booleanParameter = 0";
        assertEquals(query, descriptor.getQuery(new Object[] { false }));

        // test with true boolean
        query = "SELECT * FROM Document WHERE ecm:booleanParameter = 1";
        assertEquals(query, descriptor.getQuery(new Object[] { true }));
    }

    public void testSerialization() throws Exception {
        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        ObjectOutputStream outStream = new ObjectOutputStream(byteOutStream);
        outStream.writeObject(statefulModel);
        ByteArrayInputStream byteInStream = new ByteArrayInputStream(
                byteOutStream.toByteArray());
        ObjectInputStream inStream = new ObjectInputStream(byteInStream);
        QueryModel sm = (QueryModel) inStream.readObject();
        assertEquals(sm.getDescriptor(), statefulModel.getDescriptor());
    }

    public void testStatefulQueryModel() throws ClientException {
        QueryModelDescriptor descriptor = statefulModel.getDescriptor();
        assertFalse(descriptor.isStateless());
        assertTrue(descriptor.isStateful());

        // by default the model is empty, thus the computed query should be very
        // simple:
        assertEquals("SELECT * FROM Document",
                descriptor.getQuery(documentModel));

        // adding a value to the text field
        documentModel.setProperty("querymodel_test", "textfield", "some text");

        assertEquals(
                "SELECT * FROM Document WHERE textparameter = 'some text'",
                descriptor.getQuery(documentModel));

        // adding a value to the int field
        documentModel.setProperty("querymodel_test", "intfield", 3);

        assertEquals(
                "SELECT * FROM Document WHERE textparameter = 'some text' AND intparameter < 3",
                descriptor.getQuery(documentModel));

        // same with a long
        documentModel.setProperty("querymodel_test", "intfield", 123456789123L);

        assertEquals(
                "SELECT * FROM Document WHERE textparameter = 'some text' "
                        + "AND intparameter < 123456789123",
                descriptor.getQuery(documentModel));
        // get back to simpler value
        documentModel.setProperty("querymodel_test", "intfield", 3);

        // setting a null or an empty value removes the corresponding predicate
        // from the where clause:

        documentModel.setProperty("querymodel_test", "textfield", "");

        assertEquals("SELECT * FROM Document WHERE intparameter < 3",
                descriptor.getQuery(documentModel));

        documentModel.setProperty("querymodel_test", "intfield", null);

        assertEquals("SELECT * FROM Document",
                descriptor.getQuery(documentModel));

        // partially filled BETWEEN predicates are transformed into
        // corresponding unary ones

        GregorianCalendar gc = new GregorianCalendar(2006, 9, 12);
        documentModel.setProperty("querymodel_test", "date_min", gc.getTime());

        assertEquals(
                "SELECT * FROM Document WHERE dc:created >= DATE '2006-10-12'",
                descriptor.getQuery(documentModel));

        // adding the second value make the BETWEEN predicate works as expected

        gc = new GregorianCalendar(2006, 11, 15);
        documentModel.setProperty("querymodel_test", "date_max", gc.getTime());

        assertEquals(
                "SELECT * FROM Document WHERE dc:created BETWEEN DATE '2006-10-12' AND DATE '2006-12-15'",
                descriptor.getQuery(documentModel));

        documentModel.setProperty("querymodel_test", "date_min", null);

        assertEquals(
                "SELECT * FROM Document WHERE dc:created <= DATE '2006-12-15'",
                descriptor.getQuery(documentModel));

        // adding ordering info to the model
        // documentModel.setProperty("querymodel_common", "sortColumn",
        // "dc:modified");

        SortInfo sortInfo = new SortInfo("dc:modified", true);
        assertEquals(
                "SELECT * FROM Document WHERE dc:created <= DATE '2006-12-15' ORDER BY dc:modified",
                descriptor.getQuery(documentModel, sortInfo));

        sortInfo = new SortInfo("dc:modified", false);
        // documentModel.setProperty("querymodel_common", "sortAscending",
        // false);

        assertEquals(
                "SELECT * FROM Document WHERE dc:created <= DATE '2006-12-15' ORDER BY dc:modified DESC",
                descriptor.getQuery(documentModel, sortInfo));

        // providing only the direction is not enough to display the ordering
        // clauseOR
        documentModel.setProperty("querymodel_common", "sortColumn", "");

        assertEquals(
                "SELECT * FROM Document WHERE dc:created <= DATE '2006-12-15'",
                descriptor.getQuery(documentModel));

        // testing the use of the wrong method for a stateful qmodel
        try {
            descriptor.getQuery(new Object[] {});
            fail("Should have raised an exception since statelful models do not work with external parameters");
        } catch (ClientException e) {
        }
    }

    public void testStatefulQMBoolean() throws ClientException {
        // In NXQL/SQL there is no true boolean. One uses 0/1 instead
        QueryModelDescriptor descriptor = statefulModel.getDescriptor();
        documentModel.setProperty("querymodel_test", "boolfield", true);
        assertEquals("SELECT * FROM Document WHERE boolparameter = 1",
                descriptor.getQuery(documentModel));

        documentModel.setProperty("querymodel_test", "boolfield", false);
        assertEquals("SELECT * FROM Document WHERE boolparameter = 0",
                descriptor.getQuery(documentModel));
    }

    public void testStatefulQueryModelFullText() throws ClientException {
        QueryModelDescriptor descriptor = statefulModel.getDescriptor();
        assertFalse(descriptor.isStateless());
        assertTrue(descriptor.isStateful());
        // adding a value to the fulltext field

        documentModel.setProperty("querymodel_test", "fulltext_all",
                "some text");
        assertEquals(
                "SELECT * FROM Document WHERE ecm:fulltext LIKE '+some +text'",
                descriptor.getQuery(documentModel));
        documentModel.setProperty("querymodel_test", "fulltext_all", null);

        documentModel.setProperty("querymodel_test", "fulltext_all", "can't");
        assertEquals(
                "SELECT * FROM Document WHERE ecm:fulltext LIKE '+can\\'t'",
                descriptor.getQuery(documentModel));
        documentModel.setProperty("querymodel_test", "fulltext_all", null);

        // Tests the minimal lucene escaper and its registration
        documentModel.setProperty("querymodel_test", "fulltext_all", "can\"t");
        assertEquals(
                "SELECT * FROM Document WHERE ecm:fulltext LIKE '+can\\\"t'",
                descriptor.getQuery(documentModel));
        documentModel.setProperty("querymodel_test", "fulltext_all", null);

        documentModel.setProperty("querymodel_test", "fulltext_all", "NXP-1576");
        assertEquals(
                "SELECT * FROM Document WHERE ecm:fulltext LIKE '+NXP\\-1576'",
                descriptor.getQuery(documentModel));

        documentModel.setProperty("querymodel_test", "fulltext_all", null);

        documentModel.setProperty("querymodel_test", "fulltext_none",
                "some text");
        assertEquals("SELECT * FROM Document WHERE ecm:fulltext "
                + "NOT LIKE 'some text'", descriptor.getQuery(documentModel));
        documentModel.setProperty("querymodel_test", "fulltext_none", null);

        documentModel.setProperty("querymodel_test", "fulltext_one_of",
                "some text");
        assertEquals(
                "SELECT * FROM Document WHERE ecm:fulltext LIKE 'some text'",
                descriptor.getQuery(documentModel));

        // Let's finish with a two statements query
        documentModel.setProperty("querymodel_test", "fulltext_none", "book");
        assertEquals(
                "SELECT * FROM Document WHERE ecm:fulltext NOT LIKE 'book' "
                        + "AND ecm:fulltext LIKE 'some text'",
                descriptor.getQuery(documentModel));
    }

    public void testStatefulQueryModelWithListField() throws ClientException {
        QueryModelDescriptor descriptor = statefulModel2.getDescriptor();
        assertFalse(statefulModel2.getDescriptor().isStateless());
        assertTrue(statefulModel2.getDescriptor().isStateful());

        // by default the model is empty except for default values
        assertEquals(
                "SELECT * FROM Document WHERE (dc:creator = 'default1' OR dc:creator = 'default2')",
                descriptor.getQuery(documentModel2));

        // by adding a single element to the list of options, the predicate is
        // serialized as a '=' predicate:
        documentModel2.setProperty("querymodel_test", "listfield",
                new String[] { "Pedro" });

        assertEquals("SELECT * FROM Document WHERE dc:creator = 'Pedro'",
                descriptor.getQuery(documentModel2));

        // with several options the predicate is serialized as expected
        documentModel2.setProperty("querymodel_test", "listfield",
                new String[] { "Pedro", "Piotr", "Pierre" });

        assertEquals(
                "SELECT * FROM Document WHERE (dc:creator = 'Pedro' OR dc:creator = 'Piotr' OR dc:creator = 'Pierre')",
                descriptor.getQuery(documentModel2));

        // an empty array of options is ignored as if the field was left null
        documentModel2.setProperty("querymodel_test", "listfield",
                new String[] {});

        assertEquals("SELECT * FROM Document",
                descriptor.getQuery(documentModel2));

    }

    public void testStatefulQueryModelWithFixedPart() throws ClientException {
        QueryModelDescriptor descriptor = statefulModelWithFixedPart.getDescriptor();
        assertFalse(statefulModelWithFixedPart.getDescriptor().isStateless());
        assertTrue(statefulModelWithFixedPart.getDescriptor().isStateful());

        // by default the model is empty except for default values
        assertEquals("SELECT * FROM Document WHERE "
                + "sp:specific LIKE 'foo' OR ecm:isProxy = 1",
                descriptor.getQuery(documentModelWithFixedPart));

        documentModelWithFixedPart.setProperty("querymodel_test", "intfield", 1);
        // now with a parameter from the document model
        assertEquals("SELECT * FROM Document WHERE " + "intparameter = 1 AND "
                + "(sp:specific LIKE 'foo' OR ecm:isProxy = 1)",
                descriptor.getQuery(documentModelWithFixedPart));
    }

    // TODO: add tests for the batching clause
}
