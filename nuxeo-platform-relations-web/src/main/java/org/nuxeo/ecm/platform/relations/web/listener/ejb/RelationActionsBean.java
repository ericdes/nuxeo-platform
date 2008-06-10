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
 * $Id: RelationActionsBean.java 28951 2008-01-11 13:35:15Z tdelprat $
 */

package org.nuxeo.ecm.platform.relations.web.listener.ejb;

import static org.jboss.seam.ScopeType.CONVERSATION;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ejb.PostActivate;
import javax.ejb.PrePassivate;
import javax.ejb.Remove;
import javax.faces.application.FacesMessage;
import javax.faces.event.ActionEvent;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.CoreEvent;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.impl.CoreEventImpl;
import org.nuxeo.ecm.core.query.QueryParseException;
import org.nuxeo.ecm.core.query.sql.SQLQueryParser;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.search.api.client.SearchException;
import org.nuxeo.ecm.core.search.api.client.SearchService;
import org.nuxeo.ecm.core.search.api.client.common.SearchServiceDelegate;
import org.nuxeo.ecm.core.search.api.client.query.ComposedNXQuery;
import org.nuxeo.ecm.core.search.api.client.query.QueryException;
import org.nuxeo.ecm.core.search.api.client.query.impl.ComposedNXQueryImpl;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultSet;
import org.nuxeo.ecm.core.search.api.client.search.results.document.SearchPageProvider;
import org.nuxeo.ecm.platform.ejb.EJBExceptionHandler;
import org.nuxeo.ecm.platform.events.api.DocumentMessage;
import org.nuxeo.ecm.platform.events.api.DocumentMessageProducer;
import org.nuxeo.ecm.platform.events.api.DocumentMessageProducerException;
import org.nuxeo.ecm.platform.events.api.delegate.DocumentMessageProducerBusinessDelegate;
import org.nuxeo.ecm.platform.events.api.impl.DocumentMessageImpl;
import org.nuxeo.ecm.platform.relations.api.Literal;
import org.nuxeo.ecm.platform.relations.api.Node;
import org.nuxeo.ecm.platform.relations.api.QNameResource;
import org.nuxeo.ecm.platform.relations.api.RelationManager;
import org.nuxeo.ecm.platform.relations.api.Resource;
import org.nuxeo.ecm.platform.relations.api.ResourceAdapter;
import org.nuxeo.ecm.platform.relations.api.Statement;
import org.nuxeo.ecm.platform.relations.api.Subject;
import org.nuxeo.ecm.platform.relations.api.event.RelationEvents;
import org.nuxeo.ecm.platform.relations.api.impl.LiteralImpl;
import org.nuxeo.ecm.platform.relations.api.impl.QNameResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.RelationDate;
import org.nuxeo.ecm.platform.relations.api.impl.ResourceImpl;
import org.nuxeo.ecm.platform.relations.api.impl.StatementImpl;
import org.nuxeo.ecm.platform.relations.web.NodeInfo;
import org.nuxeo.ecm.platform.relations.web.NodeInfoImpl;
import org.nuxeo.ecm.platform.relations.web.RelationConstants;
import org.nuxeo.ecm.platform.relations.web.StatementInfo;
import org.nuxeo.ecm.platform.relations.web.StatementInfoComparator;
import org.nuxeo.ecm.platform.relations.web.StatementInfoImpl;
import org.nuxeo.ecm.platform.relations.web.listener.RelationActions;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.base.InputController;
import org.nuxeo.ecm.webapp.helpers.EventNames;

/**
 * Seam component that manages statements involving current document as well as
 * creation, edition and deletion of statements involving current document.
 * <p>
 * Current document is the subject of the relation. The predicate is resolved
 * thanks to a list of predicates URIs. The object is resolved using a type
 * (literal, resource, qname resource), an optional namespace (for qname
 * resources) and a value.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
@Name("relationActions")
@Scope(CONVERSATION)
public class RelationActionsBean extends InputController implements
        RelationActions, Serializable {

    private static final long serialVersionUID = 2336539966097558178L;

    private static final Log log = LogFactory.getLog(RelationActionsBean.class);

    @In(create = true, required = false)
    protected transient CoreSession documentManager;

    @In(create = true)
    protected RelationManager relationManager;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    @In(required = false)
    protected transient Principal currentUser;

    // statements lists

    protected List<Statement> incomingStatements;

    protected List<StatementInfo> incomingStatementsInfo;

    protected List<Statement> outgoingStatements;

    protected List<StatementInfo> outgoingStatementsInfo;

    // fields for relation creation

    protected String predicateUri;

    protected String objectType;

    protected String objectLiteralValue;

    protected String objectUri;

    protected String objectDocumentUid;

    protected String objectDocumentTitle;

    protected String comment;

    protected Boolean showCreateForm = false;

    protected List<DocumentModel> resultDocuments;

    protected boolean hasSearchResults = false;

    protected String searchKeywords;

    public DocumentModel getDocumentModel(Node node) throws ClientException {
        if (node.isQNameResource()) {
            QNameResource resource = (QNameResource) node;
            Map<String, Serializable> context = new HashMap<String, Serializable>();
            context.put(ResourceAdapter.CORE_SESSION_ID_CONTEXT_KEY,
                    documentManager.getSessionId());
            Object o = relationManager.getResourceRepresentation(
                    resource.getNamespace(), resource, context);
            if (o instanceof DocumentModel) {
                return (DocumentModel) o;
            }
        }
        return null;
    }

    // XXX AT: for BBB when repo name was not included in the resource uri
    @Deprecated
    private QNameResource getOldDocumentResource(DocumentModel document) {
        QNameResource documentResource = null;
        if (document != null) {
            documentResource = new QNameResourceImpl(
                    RelationConstants.DOCUMENT_NAMESPACE, document.getId());
        }
        return documentResource;
    }

    public QNameResource getDocumentResource(DocumentModel document)
            throws ClientException {
        QNameResource documentResource = null;
        if (document != null) {
            documentResource = (QNameResource) relationManager.getResource(
                    RelationConstants.DOCUMENT_NAMESPACE, document, null);
        }
        return documentResource;
    }

    protected List<StatementInfo> getStatementsInfo(List<Statement> statements)
            throws ClientException {
        if (statements == null) {
            return null;
        }
        List<StatementInfo> infoList = new ArrayList<StatementInfo>();
        for (Statement statement : statements) {
            Subject subject = statement.getSubject();
            // TODO: filter on doc visibility (?)
            NodeInfo subjectInfo = new NodeInfoImpl(subject,
                    getDocumentModel(subject), true);
            Resource predicate = statement.getPredicate();
            Node object = statement.getObject();
            NodeInfo objectInfo = new NodeInfoImpl(object,
                    getDocumentModel(object), true);
            StatementInfo info = new StatementInfoImpl(statement, subjectInfo,
                    new NodeInfoImpl(predicate), objectInfo);
            infoList.add(info);
        }
        return infoList;
    }

    public List<StatementInfo> getIncomingStatementsInfo()
            throws ClientException {
        if (incomingStatementsInfo != null) {
            return incomingStatementsInfo;
        }
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        Resource docResource = getDocumentResource(currentDoc);
        if (docResource == null) {
            incomingStatements = Collections.emptyList();
            incomingStatementsInfo = Collections.emptyList();
        } else {
            Statement pattern = new StatementImpl(null, null, docResource);
            incomingStatements = relationManager.getStatements(
                    RelationConstants.GRAPH_NAME, pattern);
            // add old statements, BBB
            Resource oldDocResource = getOldDocumentResource(currentDoc);
            Statement oldPattern = new StatementImpl(null, null, oldDocResource);
            incomingStatements.addAll(relationManager.getStatements(
                    RelationConstants.GRAPH_NAME, oldPattern));
            incomingStatementsInfo = getStatementsInfo(incomingStatements);
            // sort by modification date, reverse
            Comparator<StatementInfo> comp = Collections.reverseOrder(new StatementInfoComparator());
            Collections.sort(incomingStatementsInfo, comp);
        }
        return incomingStatementsInfo;
    }

    public List<StatementInfo> getOutgoingStatementsInfo()
            throws ClientException {
        if (outgoingStatementsInfo != null) {
            return outgoingStatementsInfo;
        }
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        Resource docResource = getDocumentResource(currentDoc);
        if (docResource == null) {
            outgoingStatements = Collections.emptyList();
            outgoingStatementsInfo = Collections.emptyList();
        } else {
            Statement pattern = new StatementImpl(docResource, null, null);
            outgoingStatements = relationManager.getStatements(
                    RelationConstants.GRAPH_NAME, pattern);
            // add old statements, BBB
            Resource oldDocResource = getOldDocumentResource(currentDoc);
            Statement oldPattern = new StatementImpl(oldDocResource, null, null);
            outgoingStatements.addAll(relationManager.getStatements(
                    RelationConstants.GRAPH_NAME, oldPattern));
            outgoingStatementsInfo = getStatementsInfo(outgoingStatements);
            // sort by modification date, reverse
            Comparator<StatementInfo> comp = Collections.reverseOrder(new StatementInfoComparator());
            Collections.sort(outgoingStatementsInfo, comp);
        }
        return outgoingStatementsInfo;
    }

    @Observer(value = { EventNames.DOCUMENT_SELECTION_CHANGED,
            EventNames.CONTENT_ROOT_SELECTION_CHANGED,
            EventNames.DOCUMENT_CHANGED }, create = false, inject = false)
    public void resetStatements() {
        incomingStatements = null;
        incomingStatementsInfo = null;
        outgoingStatements = null;
        outgoingStatementsInfo = null;
    }

    // getters & setters for creation items

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getObjectDocumentTitle() {
        return objectDocumentTitle;
    }

    public void setObjectDocumentTitle(String objectDocumentTitle) {
        this.objectDocumentTitle = objectDocumentTitle;
    }

    public String getObjectDocumentUid() {
        return objectDocumentUid;
    }

    public void setObjectDocumentUid(String objectDocumentUid) {
        this.objectDocumentUid = objectDocumentUid;
    }

    public String getObjectLiteralValue() {
        return objectLiteralValue;
    }

    public void setObjectLiteralValue(String objectLiteralValue) {
        this.objectLiteralValue = objectLiteralValue;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getObjectUri() {
        return objectUri;
    }

    public void setObjectUri(String objectUri) {
        this.objectUri = objectUri;
    }

    public String getPredicateUri() {
        return predicateUri;
    }

    public void setPredicateUri(String predicateUri) {
        this.predicateUri = predicateUri;
    }

    protected void notifyEvent(String eventId, DocumentModel source,
            Map<String, Object> options, String comment) {
        CoreEvent coreEvent = new CoreEventImpl(eventId, source, options,
                documentManager.getPrincipal(), RelationEvents.CATEGORY,
                comment);
        DocumentMessage msg = new DocumentMessageImpl(source, coreEvent);
        try {
            DocumentMessageProducer producer = DocumentMessageProducerBusinessDelegate.getRemoteDocumentMessageProducer();
            producer.produce(msg);
        } catch (DocumentMessageProducerException e) {
            log.error("Error when trying to notify");
        }
    }

    public String addStatement() throws ClientException {
        DocumentModel currentDoc = navigationContext.getCurrentDocument();
        Resource documentResource = getDocumentResource(currentDoc);
        if (documentResource == null) {
            throw new ClientException(
                    "Document resource could not be retrieved");
        }

        Resource predicate = new ResourceImpl(predicateUri);
        Node object = null;
        if (objectType.equals("literal")) {
            objectLiteralValue = objectLiteralValue.trim();
            object = new LiteralImpl(objectLiteralValue);
        } else if (objectType.equals("uri")) {
            objectUri = objectUri.trim();
            object = new ResourceImpl(objectUri);
        } else if (objectType.equals("document")) {
            objectDocumentUid = objectDocumentUid.trim();
            String repositoryName = navigationContext.getCurrentServerLocation().getName();
            String localName = repositoryName + "/" + objectDocumentUid;
            object = new QNameResourceImpl(
                    RelationConstants.DOCUMENT_NAMESPACE, localName);
        }

        Statement stmt = new StatementImpl(documentResource, predicate, object);
        if (!outgoingStatements.contains(stmt)) {
            // add statement to the graph
            List<Statement> stmts = new ArrayList<Statement>();
            String eventComment = null;
            if (comment != null) {
                comment = comment.trim();
                if (comment.length() > 0) {
                    stmt.addProperty(RelationConstants.COMMENT,
                            new LiteralImpl(comment));
                    eventComment = comment;
                }
            }
            Literal now = RelationDate.getLiteralDate(new Date());
            stmt.addProperty(RelationConstants.CREATION_DATE, now);
            stmt.addProperty(RelationConstants.MODIFICATION_DATE, now);
            if (currentUser != null) {
                stmt.addProperty(RelationConstants.AUTHOR, new LiteralImpl(
                        currentUser.getName()));
            }

            stmts.add(stmt);

            // notifications

            Map<String, Object> options = new HashMap<String, Object>();
            String currentLifeCycleState = currentDoc.getCurrentLifeCycleState();
            options.put(CoreEventConstants.DOC_LIFE_CYCLE,
                    currentLifeCycleState);
            putStatements(options, stmt);
            options.put(RelationEvents.GRAPH_NAME_EVENT_KEY,
                    RelationConstants.GRAPH_NAME);

            // before notification
            notifyEvent(RelationEvents.BEFORE_RELATION_CREATION, currentDoc,
                    options, eventComment);

            // add statement
            relationManager.add(RelationConstants.GRAPH_NAME, stmts);

            // XXX AT: try to refetch it from the graph so that resources are
            // transformed into qname resources: useful for indexing
            putStatements(options, relationManager.getStatements(
                    RelationConstants.GRAPH_NAME, stmt));

            // after notification
            notifyEvent(RelationEvents.AFTER_RELATION_CREATION, currentDoc,
                    options, eventComment);

            // make sure statements will be recomputed
            resetStatements();

            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get(
                            "label.relation.created"));
            resetCreateFormValues();
        } else {
            facesMessages.add(FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get(
                            "label.relation.already.exists"));
        }
        return "document_relations";
    }

    // for consistency for callers only
    private static void putStatements(Map<String, Object> options,
            List<Statement> statements) {
        options.put(RelationEvents.STATEMENTS_EVENT_KEY, statements);
    }

    private static void putStatements(Map<String, Object> options,
            Statement statement) {
        List<Statement> statements = new LinkedList<Statement>();
        statements.add(statement);
        options.put(RelationEvents.STATEMENTS_EVENT_KEY, statements);
    }

    public void toggleCreateForm(ActionEvent event) {
        showCreateForm = !showCreateForm;
    }

    private void resetCreateFormValues() {
        predicateUri = "";
        objectType = "";
        objectLiteralValue = "";
        objectUri = "";
        objectDocumentUid = "";
        objectDocumentTitle = "";
        comment = "";
        showCreateForm = false;
    }

    public String deleteStatement(StatementInfo stmtInfo)
            throws ClientException {
        if (stmtInfo != null && outgoingStatementsInfo != null
                && outgoingStatementsInfo.contains(stmtInfo)) {
            Statement stmt = stmtInfo.getStatement();
            // notifications
            Map<String, Object> options = new HashMap<String, Object>();
            DocumentModel source = navigationContext.getCurrentDocument();
            String currentLifeCycleState = source.getCurrentLifeCycleState();
            options.put(CoreEventConstants.DOC_LIFE_CYCLE,
                    currentLifeCycleState);
            options.put(RelationEvents.GRAPH_NAME_EVENT_KEY,
                    RelationConstants.GRAPH_NAME);
            putStatements(options, stmt);

            // before notification
            notifyEvent(RelationEvents.BEFORE_RELATION_REMOVAL, source,
                    options, null);

            // remove statement
            List<Statement> stmts = new ArrayList<Statement>();
            stmts.add(stmt);
            relationManager.remove(RelationConstants.GRAPH_NAME, stmts);

            // after notification
            notifyEvent(RelationEvents.AFTER_RELATION_REMOVAL, source, options,
                    null);

            // make sure statements will be recomputed
            resetStatements();

            facesMessages.add(FacesMessage.SEVERITY_INFO,
                    resourcesAccessor.getMessages().get(
                            "label.relation.deleted"));
        }
        return "document_relations";
    }

    public String getSearchKeywords() {
        return searchKeywords;
    }

    public void setSearchKeywords(String searchKeywords) {
        this.searchKeywords = searchKeywords;
    }

    public String searchDocuments() throws ClientException {
        try {
            log.debug("Making call to get documents list for keywords: "
                    + searchKeywords);
            // reset existing search results
            resultDocuments = null;
            List<String> constraints = new ArrayList<String>();
            if (searchKeywords != null) {
                searchKeywords = searchKeywords.trim();
                if (searchKeywords.length() > 0) {
                    if (!searchKeywords.equals("*")) {
                        // full text search
                        constraints.add(String.format("ecm:fulltext LIKE '%s'",
                                searchKeywords));
                    }
                }
            }
            // no folderish doc nor hidden doc
            constraints.add("ecm:mixinType != 'Folderish'");
            constraints.add("ecm:mixinType != 'HiddenInNavigation'");
            // no archived revisions
            constraints.add("ecm:isCheckedInVersion = 0");
            // filter current document
            DocumentModel currentDocument = navigationContext.getCurrentDocument();
            if (currentDocument != null) {
                constraints.add(String.format("ecm:id != '%s'",
                        currentDocument.getId()));
            }
            // search keywords
            String query = String.format("SELECT * FROM Document WHERE %s",
                    StringUtils.join(constraints.toArray(), " AND "));
            log.info("query: " + query);
            SQLQuery nxqlQuery = SQLQueryParser.parse(query);
            ComposedNXQuery cQuery = new ComposedNXQueryImpl(nxqlQuery);
            SearchService searchService = SearchServiceDelegate.getRemoteSearchService();
            ResultSet queryResults = searchService.searchQuery(cQuery, 0, 100);
            if (queryResults != null) {
                SearchPageProvider provider = new SearchPageProvider(
                        queryResults);
                resultDocuments = provider.getCurrentPage();
            }
            log.debug("FTQ query result contains: " + resultDocuments.size()
                    + " docs.");
            hasSearchResults = !resultDocuments.isEmpty();
        } catch (QueryException e) {
            facesMessages.add(FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get(
                            "label.search.service.wrong.query"));
            // throw EJBExceptionHandler.wrapException(e);
            log.error("QueryException in search popup : " + e.getMessage());
        } catch (QueryParseException e) {
            facesMessages.add(FacesMessage.SEVERITY_WARN,
                    resourcesAccessor.getMessages().get(
                            "label.search.service.wrong.query"));
            log.error("QueryParseException in search popup : " + e.getMessage());
        } catch (SearchException e) {
            throw EJBExceptionHandler.wrapException(e);
        }
        return "create_relation_search_document";
    }

    public List<DocumentModel> getSearchDocumentResults() {
        return resultDocuments;
    }

    public boolean getHasSearchResults() {
        return hasSearchResults;
    }

    public Boolean getShowCreateForm() {
        return showCreateForm;
    }

    public void initialize() {
        log.debug("Initializing...");
    }

    @Destroy
    @Remove
    public void destroy() {
        log.debug("Removing Seam action listener...");
    }

    @PrePassivate
    public void saveState() {
        log.debug("PrePassivate");
    }

    @PostActivate
    public void readState() {
        log.debug("PostActivate");
    }

}
