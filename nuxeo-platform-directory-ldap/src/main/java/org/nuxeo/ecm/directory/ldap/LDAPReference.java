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

package org.nuxeo.ecm.directory.ldap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SchemaViolationException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.directory.AbstractReference;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.DirectoryFieldMapper;
import org.nuxeo.ecm.directory.Session;

import com.sun.jndi.ldap.LdapURL;

/**
 * Implementation of the directory Reference interface that leverage two common
 * ways of storing relationships in LDAP directories:
 *
 * <ul>
 * <li>the static attribute strategy where a multi-valued attribute store the
 * exhaustive list of distinguished names of the refereed entries (eg. the
 * uniqueMember attribute of the groupOfUniqueNames objectclass)</li>
 * <li>the dynamic attribute strategy where a potentially multi-valued
 * attribute stores a ldap urls intensively describing the refereed LDAP entries
 * (eg. the memberURLs attribute of the groupOfURLs objectclass)</li>
 * </ul>
 *
 * Please note that both static and dynamic references are resolved in read mode
 * whereas only the static attribute strategy is used when creating new
 * references or when deleting existing ones (write / update mode).
 *
 * Some design considerations behind the implementation of such reference can be
 * found at: http://jira.nuxeo.org/browse/NXP-1506
 *
 * @author Olivier Grisel <ogrisel@nuxeo.com>
 */
@XObject(value = "ldapReference")
public class LDAPReference extends AbstractReference {

    private static final Log log = LogFactory.getLog(LDAPReference.class);

    @XNode("@forceDnConsistencyCheck")
    public Boolean forceDnConsistencyCheck = Boolean.FALSE;

    protected LDAPDirectoryDescriptor targetDirectoryDescriptor;

    @XNode("@staticAttributeId")
    protected String staticAttributeId = null;

    @XNode("@dynamicAttributeId")
    protected String dynamicAttributeId = null;

    @XNode("@field")
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public static final List<String> EMPTY_STRING_LIST = Collections.emptyList();

    // private LDAPFilterMatcher filterMatcher;

    private LDAPFilterMatcher getFilterMatcher() {
        // NXP-1886
        /*
         * if (filterMatcher == null) { filterMatcher = new LDAPFilterMatcher(); }
         * return filterMatcher;
         */

        return new LDAPFilterMatcher();
    }

    /**
     * @return true if the reference should resolve statically refereed entries
     *         (identified by dn-valued attribute)
     * @throws DirectoryException
     */
    public boolean isStatic() throws DirectoryException {
        return getStaticAttributeId() != null;
    }

    public String getStaticAttributeId() throws DirectoryException {
        return getStaticAttributeId(null);
    }

    public String getStaticAttributeId(DirectoryFieldMapper sourceFM)
            throws DirectoryException {
        if (staticAttributeId != null) {
            // explicitly provided attributeId
            return staticAttributeId;
        }

        // sourceFM can be passed to avoid infinite loop in LDAPDirectory init
        if (sourceFM == null) {
            sourceFM = ((LDAPDirectory) getSourceDirectory()).getFieldMapper();
        }
        String backendFieldId = sourceFM.getBackendField(fieldName);
        if (fieldName.equals(backendFieldId)) {
            // no specific backendField found and no staticAttributeId found
            // either, this reference should not be statically resolved
            return null;
        } else {
            // BBB: the field mapper has been explicitly used to specify the
            // staticAttributeId value as this was the case before the
            // introduction of the staticAttributeId dynamicAttributeId duality
            log.warn(String.format(
                    "implicit static attribute definition through fieldMapping is deprecated, "
                            + "please update your setup with "
                            + "<ldapReference field=\"%s\" directory=\"%s\" staticAttributeId=\"%s\">",
                    fieldName, sourceDirectoryName, backendFieldId));
            return backendFieldId;
        }
    }

    public String getDynamicAttributeId() {
        return dynamicAttributeId;
    }

    /**
     * @return true if the reference should resolve dynamically refereed entries
     *         (identified by a LDAP url-valued attribute)
     */
    public boolean isDynamic() {
        return dynamicAttributeId != null;
    }

    @Override
    @XNode("@directory")
    public void setTargetDirectoryName(String targetDirectoryName) {
        this.targetDirectoryName = targetDirectoryName;
    }

    @Override
    public Directory getSourceDirectory() throws DirectoryException {

        Directory sourceDir = super.getSourceDirectory();
        if (sourceDir instanceof LDAPDirectoryProxy) {
            return ((LDAPDirectoryProxy) sourceDir).getDirectory();

        } else {
            throw new DirectoryException(
                    sourceDirectoryName
                            + " is not a LDAPDirectory and thus cannot be used in a reference for "
                            + fieldName);
        }
    }

    @Override
    public Directory getTargetDirectory() throws DirectoryException {
        Directory targetDir = super.getTargetDirectory();
        if (targetDir instanceof LDAPDirectoryProxy) {
            return ((LDAPDirectoryProxy) targetDir).getDirectory();

        } else {
            throw new DirectoryException(
                    targetDirectoryName
                            + " is not a LDAPDirectory and thus cannot be referenced as target by "
                            + fieldName);
        }
    }

    protected LDAPDirectory getTargetLDAPDirectory() throws DirectoryException {
        return (LDAPDirectory) getTargetDirectory();
    }

    protected LDAPDirectory getSourceLDAPDirectory() throws DirectoryException {
        return (LDAPDirectory) getSourceDirectory();
    }

    protected LDAPDirectoryDescriptor getTargetDirectoryDescriptor()
            throws DirectoryException {
        if (targetDirectoryDescriptor == null) {
            targetDirectoryDescriptor = getTargetLDAPDirectory().getConfig();
        }
        return targetDirectoryDescriptor;
    }

    /**
     * Store new links using the LDAP staticAttributeId strategy.
     *
     * @see org.nuxeo.ecm.directory.Reference#addLinks(java.lang.String,
     *      java.util.List)
     */
    public void addLinks(String sourceId, List<String> targetIds)
            throws DirectoryException {

        if (targetIds.isEmpty()) {
            // optim: nothing to do, return silently without further creating
            // session instances
            return;
        }

        LDAPDirectory targetDirectory = (LDAPDirectory) getTargetDirectory();
        LDAPDirectory sourceDirectory = (LDAPDirectory) getSourceDirectory();
        String attributeId = getStaticAttributeId();
        if (attributeId == null) {
            log.debug(String.format(
                    "trying to edit a non-static reference from %s in directory %s: ignoring",
                    sourceId, sourceDirectory.getName()));
            return;
        }
        LDAPSession targetSession = (LDAPSession) targetDirectory.getSession();
        LDAPSession sourceSession = (LDAPSession) sourceDirectory.getSession();
        try {
            if (!sourceSession.isReadOnly()) {
                SearchResult ldapEntry = sourceSession.getLdapEntry(sourceId);
                if (ldapEntry == null) {
                    throw new DirectoryException(
                            String.format(
                                    "could not add links from unexisting %s in directory %s",
                                    sourceId, sourceDirectory.getName()));
                }
                String sourceDn = ldapEntry.getNameInNamespace();
                Attribute storedAttr = ldapEntry.getAttributes().get(
                        attributeId);
                String emptyRefMarker = sourceDirectory.getConfig().getEmptyRefMarker();
                Attribute attrToAdd = new BasicAttribute(attributeId);
                for (String targetId : targetIds) {
                    // TODO optim: avoid LDAP search request when targetDn can
                    // be forged client side (rdnAttribute = idAttribute and
                    // scope is onelevel)
                    ldapEntry = targetSession.getLdapEntry(targetId);
                    if (ldapEntry == null) {
                        log.warn(String.format(
                                "entry %s in directory %s not found: could not add link from %s in directory %s",
                                targetId, targetDirectory.getName(), sourceId,
                                sourceDirectory.getName()));
                        continue;
                    }
                    String dn = ldapEntry.getNameInNamespace();
                    if (storedAttr == null || !storedAttr.contains(dn)) {
                        attrToAdd.add(dn);
                    }
                }
                if (attrToAdd.size() > 0) {
                    try {
                        // do the LDAP request to store missing dns
                        Attributes attrsToAdd = new BasicAttributes();
                        attrsToAdd.put(attrToAdd);
                        sourceSession.dirContext.modifyAttributes(sourceDn,
                                DirContext.ADD_ATTRIBUTE, attrsToAdd);

                        // robustly clean any existing empty marker now that we
                        // are sure that the list in not empty
                        if (storedAttr.contains(emptyRefMarker)) {
                            Attributes cleanAttrs = new BasicAttributes(
                                    attributeId, emptyRefMarker);
                            sourceSession.dirContext.modifyAttributes(sourceDn,
                                    DirContext.REMOVE_ATTRIBUTE, cleanAttrs);
                        }
                    } catch (SchemaViolationException e) {
                        if (isDynamic()) {
                            // we are editing an entry that has no static part
                            log.warn(String.format(
                                    "cannot update dynamic reference in field %s for source %s",
                                    getFieldName(), sourceId));
                        } else {
                            // this is a real schema configuration problem,
                            // wrap up the exception
                            throw new DirectoryException(e);
                        }
                    }
                }
            }
        } catch (NamingException e) {
            throw new DirectoryException("addLinks failed: " + e.getMessage(),
                    e);
        } finally {
            sourceSession.close();
            targetSession.close();
        }
    }

    /**
     * Store new links using the LDAP staticAttributeId strategy.
     *
     * @see org.nuxeo.ecm.directory.Reference#addLinks(java.util.List,
     *      java.lang.String)
     */
    public void addLinks(List<String> sourceIds, String targetId)
            throws DirectoryException {
        String attributeId = getStaticAttributeId();
        if (attributeId == null && !sourceIds.isEmpty()) {
            log.warn("trying to edit a non-static reference: ignoring");
            return;
        }
        LDAPDirectory targetDirectory = (LDAPDirectory) getTargetDirectory();
        LDAPSession targetSession = (LDAPSession) targetDirectory.getSession();
        LDAPDirectory sourceDirectory = (LDAPDirectory) getSourceDirectory();
        LDAPSession sourceSession = (LDAPSession) sourceDirectory.getSession();

        String emptyRefMarker = sourceDirectory.getConfig().getEmptyRefMarker();
        try {
            if (!sourceSession.isReadOnly()) {
                // compute the target dn to add to all the matching source
                // entries
                SearchResult ldapEntry = targetSession.getLdapEntry(targetId);
                if (ldapEntry == null) {
                    throw new DirectoryException(
                            String.format(
                                    "could not add links to unexisting %s in directory %s",
                                    targetId, targetDirectory.getName()));
                }
                String targetDn = ldapEntry.getNameInNamespace();
                for (String sourceId : sourceIds) {
                    ldapEntry = sourceSession.getLdapEntry(sourceId);
                    if (ldapEntry == null) {
                        log.warn(String.format(
                                "entry %s in directory %s not found: could not add link to %s in directory %s",
                                sourceId, sourceDirectory.getName(), targetId,
                                targetDirectory.getName()));
                        continue;
                    }
                    String sourceDn = ldapEntry.getNameInNamespace();
                    Attribute storedAttr = ldapEntry.getAttributes().get(
                            attributeId);
                    try {
                        // add the new dn
                        Attributes attrs = new BasicAttributes(attributeId,
                                targetDn);
                        sourceSession.dirContext.modifyAttributes(sourceDn,
                                DirContext.ADD_ATTRIBUTE, attrs);

                        // robustly clean any existing empty marker now that we
                        // are sure that the list in not empty
                        if (storedAttr.contains(emptyRefMarker)) {
                            Attributes cleanAttrs = new BasicAttributes(
                                    attributeId, emptyRefMarker);
                            sourceSession.dirContext.modifyAttributes(sourceDn,
                                    DirContext.REMOVE_ATTRIBUTE, cleanAttrs);
                        }
                    } catch (SchemaViolationException e) {
                        if (isDynamic()) {
                            // we are editing an entry that has no static part
                            log.warn(String.format(
                                    "cannot add dynamic reference in field %s for target %s",
                                    getFieldName(), targetId));
                        } else {
                            // this is a real schema configuration problem,
                            // wrapup the exception
                            throw new DirectoryException(e);
                        }
                    }
                }
            }
        } catch (NamingException e) {
            throw new DirectoryException("addLinks failed: " + e.getMessage(),
                    e);
        } finally {
            sourceSession.close();
            targetSession.close();
        }
    }

    /**
     * Fetch both statically and dynamically defined references and merge the
     * results.
     *
     * @see org.nuxeo.ecm.directory.Reference#getSourceIdsForTarget(java.lang.String)
     */
    public List<String> getSourceIdsForTarget(String targetId)
            throws DirectoryException {

        // container to hold merged references
        Set<String> sourceIds = new TreeSet<String>();
        SearchResult targetLdapEntry = null;
        String targetDn = null;

        // step #1: resolve static references
        String staticAttributeId = getStaticAttributeId();
        if (staticAttributeId != null) {
            // step #1.1: fetch the dn of the targetId entry in the target
            // directory by the static dn valued strategy
            LDAPDirectory targetDir = getTargetLDAPDirectory();
            LDAPSession targetSession = (LDAPSession) targetDir.getSession();
            try {
                targetLdapEntry = targetSession.getLdapEntry(targetId, true);
                if (targetLdapEntry == null) {
                    throw new DirectoryException(targetId
                            + " does not exist in " + targetDirectoryName);
                }
                targetDn = pseudoNormalizeDn(targetLdapEntry.getNameInNamespace());
            } catch (NamingException e) {
                throw new DirectoryException("error fetching " + targetId, e);
            } finally {
                targetSession.close();
            }

            // step #1.2: search for entries that reference that dn in the
            // source directory and collect their ids
            LDAPDirectory sourceDirectory = getSourceLDAPDirectory();

            String filterExpr = String.format("(&(%s={0})%s)",
                    staticAttributeId, sourceDirectory.getBaseFilter());
            String[] filterArgs = new String[] { targetDn };

            String searchBaseDn = sourceDirectory.getConfig().getSearchBaseDn();
            LDAPSession sourceSession = (LDAPSession) sourceDirectory.getSession();
            SearchControls sctls = sourceDirectory.getSearchControls();
            try {
                NamingEnumeration<SearchResult> results = sourceSession.dirContext.search(
                        searchBaseDn, filterExpr, filterArgs, sctls);

                Attributes attributes;
                Object value;
                while (results.hasMore()) {
                    attributes = results.next().getAttributes();
                    // NXP-2461: check that id field is filled
                    Attribute attr = attributes.get(sourceSession.idAttribute);
                    if (attr != null) {
                        value = attr.get();
                        if (value != null) {
                            sourceIds.add(value.toString());
                        }
                    }
                }
            } catch (NamingException e) {
                throw new DirectoryException(
                        "error during reference search for " + targetDn, e);
            } finally {
                sourceSession.close();
            }
        }
        // step #2: resolve dynamic references
        String dynamicAttributeId = this.dynamicAttributeId;
        if (dynamicAttributeId != null) {

            LDAPDirectory sourceDirectory = getSourceLDAPDirectory();
            LDAPDirectory targetDirectory = getTargetLDAPDirectory();
            String searchBaseDn = sourceDirectory.getConfig().getSearchBaseDn();
            LDAPSession sourceSession = (LDAPSession) sourceDirectory.getSession();
            LDAPSession targetSession = (LDAPSession) targetDirectory.getSession();

            try {
                // step #2.1: fetch the target entry to apply the ldap url
                // filters of the candidate sources on it
                if (targetLdapEntry == null) {
                    // only fetch the entry if not already fetched by the static
                    // attributes references resolution
                    targetLdapEntry = targetSession.getLdapEntry(targetId, true);
                }
                if (targetLdapEntry == null) {
                    if (targetLdapEntry == null) {
                        throw new DirectoryException(targetId
                                + " does not exist in " + targetDirectoryName);
                    }
                }
                targetDn = pseudoNormalizeDn(targetLdapEntry.getNameInNamespace());
                Attributes targetAttributes = targetLdapEntry.getAttributes();

                // step #2.2: find the list of entries that hold candidate
                // dynamic links in the source directory
                SearchControls sctls = sourceDirectory.getSearchControls();
                String filterExpr = String.format("%s=*", dynamicAttributeId);
                NamingEnumeration<SearchResult> results = sourceSession.dirContext.search(
                        searchBaseDn, filterExpr, sctls);
                while (results.hasMore()) {
                    // step #2.3: for each sourceId and each ldapUrl test
                    // whether the current target entry matches the collected
                    // URL
                    Attributes sourceAttributes = results.next().getAttributes();

                    NamingEnumeration<?> ldapUrls = sourceAttributes.get(
                            dynamicAttributeId).getAll();
                    while (ldapUrls.hasMore()) {
                        LdapURL ldapUrl = new LdapURL(
                                ldapUrls.next().toString());

                        // check base URL
                        if (!targetDn.endsWith(ldapUrl.getDN())) {
                            continue;
                        }

                        // check onelevel scope constraints
                        if ("onelevel".equals(ldapUrl.getScope())) {
                            int targetDnSize = targetDn.split(",").length;
                            int urlDnSize = ldapUrl.getDN().split(",").length;
                            if (targetDnSize - urlDnSize > 1) {
                                // target is not a direct child of the DN of the
                                // LDAP URL
                                continue;
                            }
                        }

                        // check that the target entry matches the filter
                        if (getFilterMatcher().match(targetAttributes,
                                ldapUrl.getFilter())) {
                            // the target match the source url, add it to the
                            // collected ids
                            sourceIds.add(sourceAttributes.get(
                                    sourceSession.idAttribute).get().toString());
                        }
                    }

                }
            } catch (Exception e) {
                throw new DirectoryException(
                        "error during reference search for " + targetId, e);
            } finally {
                sourceSession.close();
                targetSession.close();
            }
        }
        return new ArrayList<String>(sourceIds);
    }

    /**
     * Fetch both statically and dynamically defined references and merge the
     * results.
     *
     * @see org.nuxeo.ecm.directory.Reference#getSourceIdsForTarget(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public List<String> getTargetIdsForSource(String sourceId)
            throws DirectoryException {
        // YAGNI?
        // XXX: use getLdapTargetIds() for a proper implementation instead?
        Session session = getSourceDirectory().getSession();
        String schemaName = getSourceDirectory().getSchema();
        try {
            // XXX: looks broken
            return (List<String>) session.getEntry(sourceId).getProperty(
                    schemaName, fieldName);
        } finally {
            session.close();
        }
    }

    /**
     * Simple helper that replace ", " by "," in the provided dn and return the
     * lower case version of the result for comparison purpose.
     *
     * @param dn the raw unnormalized dn
     * @return lowercase version without whitespace after commas
     */
    protected static String pseudoNormalizeDn(String dn) {
        // this method does not respect the LDAP DN RFCs
        // but this is enough to compare our base dns in getLdapTargetIds
        dn = dn.replaceAll(", ", ",");
        return dn.toLowerCase();
    }

    /**
     * Optim method to spare a LDAP request when the caller is a LDAPSession
     * object that has already fetched the LDAP Attribute instances.
     * <p>
     * This method should return the same results as the sister method:
     * org.nuxeo.ecm.directory.Reference#getTargetIdsForSource(java.lang.String)
     *
     * @param staticAttribute the LDAP attribute with explicit dn references
     * @param dynamicAttribute the LDAP attribute with implicit ldap url
     *            references
     * @return target reference ids
     * @throws DirectoryException
     */
    public List<String> getLdapTargetIds(Attributes attributes)
            throws DirectoryException {

        Set<String> targetIds = new TreeSet<String>();

        LDAPDirectory targetDirectory = (LDAPDirectory) getTargetDirectory();
        LDAPDirectoryDescriptor targetDirconfig = getTargetDirectoryDescriptor();
        LDAPSession targetSession = (LDAPSession) targetDirectory.getSession();

        try {
            String baseDn = pseudoNormalizeDn(targetDirconfig.getSearchBaseDn());

            // step #1: fetch ids referenced by static attributes
            String staticAttributeId = getStaticAttributeId();
            Attribute staticAttribute = null;
            if (staticAttributeId != null) {
                staticAttribute = attributes.get(staticAttributeId);
            }
            if (staticAttribute != null) {
                NamingEnumeration<?> targetDns = staticAttribute.getAll();
                String[] attributeIdsToCollect = new String[] { targetSession.idAttribute };

                while (targetDns.hasMore()) {
                    String targetDn = targetDns.next().toString();

                    if (!pseudoNormalizeDn(targetDn).endsWith(baseDn)) {
                        // optim: avoid network connections when obvious
                        log.debug(String.format(
                                "ignoring: dn=%s (does not match %s)",
                                targetDn, baseDn));
                        continue;
                    }
                    // find the id of the referenced entry
                    String id = null;

                    if (targetSession.rdnMatchesIdField()) {
                        // optim: do not fetch the entry to get its true id but
                        // guess it by reading the targetDn
                        final int beginIndex = targetDn.indexOf("=") + 1;
                        final int endIndex = targetDn.indexOf(",");
                        id = targetDn.substring(beginIndex, endIndex).trim();
                    } else {
                        // the entry id is not based on the rdn, we thus need to
                        // fetch the LDAP entry to grab it
                        Attributes entry;
                        try {
                            entry = targetSession.dirContext.getAttributes(
                                    targetDn, attributeIdsToCollect);
                        } catch (NamingException e) {
                            log.error("could not find " + targetDn);
                            continue;
                        }
                        // NXP-2461: check that id field is filled
                        Attribute attr = entry.get(targetSession.idAttribute);
                        if (attr != null) {
                            id = attr.get().toString();
                        }
                    }
                    if (forceDnConsistencyCheck.booleanValue()) {
                        // check that the referenced entry is actually part of
                        // the target directory (takes care of the filters and
                        // the scope)
                        // this check can be very expensive on large groups
                        // and thus not enabled by default
                        if (!targetSession.hasEntry(id)) {
                            log.debug("ignoring: " + targetDn
                                    + " (not part of target directory)");
                            continue;
                        }
                    }
                    // NXP-2461: check that id field is filled
                    if (id != null) {
                        targetIds.add(id);
                    }
                }
            }
            // step #2: fetched dynamically referenced ids
            String dynamicAttributeId = this.dynamicAttributeId;
            Attribute dynamicAttribute = null;
            if (dynamicAttributeId != null) {
                dynamicAttribute = attributes.get(dynamicAttributeId);
            }
            if (dynamicAttribute != null) {
                NamingEnumeration<?> rawldapUrls = dynamicAttribute.getAll();
                while (rawldapUrls.hasMore()) {
                    LdapURL ldapUrl = new LdapURL(rawldapUrls.next().toString());
                    String linkDn = pseudoNormalizeDn(ldapUrl.getDN());
                    String directoryDn = pseudoNormalizeDn(targetDirconfig.getSearchBaseDn());
                    int scope = "subtree".equalsIgnoreCase(ldapUrl.getScope()) ? SearchControls.SUBTREE_SCOPE
                            : SearchControls.ONELEVEL_SCOPE;

                    if (!linkDn.endsWith(directoryDn)
                            && !directoryDn.endsWith(linkDn)) {
                        // optim #1: if the dns do not match, abort
                        continue;
                    } else if (directoryDn.endsWith(linkDn)
                            && linkDn.length() < directoryDn.length()
                            && scope == SearchControls.ONELEVEL_SCOPE) {
                        // optim #2: the link dn is pointing to elements that at
                        // upperlevel than directory elements
                        continue;
                    } else {
                        // combine the ldapUrl search query with target
                        // directory own constraints
                        SearchControls scts = new SearchControls();

                        // use the most specific scope
                        scts.setSearchScope(Math.min(scope,
                                targetDirconfig.getSearchScope()));

                        // only fetch the ids of the targets
                        scts.setReturningAttributes(new String[] { targetSession.idAttribute });

                        // combine the filter of the target directory with the
                        // provided filter if any
                        String filter = ldapUrl.getFilter();
                        String targetFilter = targetDirconfig.getSearchFilter();
                        if (filter == null || filter.length() == 0) {
                            filter = targetFilter;
                        } else if (targetFilter != null
                                && targetFilter.length() > 0) {
                            filter = String.format("(&(%s)(%s))", targetFilter,
                                    filter);
                        }

                        // perform the request and collect the ids
                        NamingEnumeration<SearchResult> results = targetSession.dirContext.search(
                                linkDn, filter, scts);
                        while (results.hasMore()) {
                            // NXP-2461: check that id field is filled
                            Attribute attr = results.next().getAttributes().get(
                                    targetSession.idAttribute);
                            if (attr != null) {
                                String collectedId = attr.get().toString();
                                if (collectedId != null) {
                                    targetIds.add(collectedId);
                                }
                            }

                        }
                    }
                }
            }
            // return merged attributes
            return new ArrayList<String>(targetIds);
        } catch (NamingException e) {
            throw new DirectoryException("error computing LDAP references", e);
        } finally {
            targetSession.close();
        }
    }

    /**
     * Remove existing statically defined links for the given source id (dynamic
     * references remain unaltered)
     *
     * @see org.nuxeo.ecm.directory.Reference#removeLinksForSource(java.lang.String)
     */
    public void removeLinksForSource(String sourceId) throws DirectoryException {
        LDAPDirectory targetDirectory = (LDAPDirectory) getTargetDirectory();
        LDAPDirectory sourceDirectory = (LDAPDirectory) getSourceDirectory();
        LDAPSession sourceSession = (LDAPSession) sourceDirectory.getSession();
        String attributeId = getStaticAttributeId();
        try {
            if (sourceSession.isReadOnly() || attributeId == null) {
                // do not try to do anything on a read only server or to a
                // purely dynamic reference
                return;
            }
            // get the dn of the entry that matches sourceId
            SearchResult sourceLdapEntry = sourceSession.getLdapEntry(sourceId);
            if (sourceLdapEntry == null) {
                throw new DirectoryException(
                        String.format(
                                "cannot edit the links hold by missing entry '%s' in directory '%s'",
                                sourceId, sourceDirectory.getName()));
            }
            String sourceDn = pseudoNormalizeDn(sourceLdapEntry.getNameInNamespace());

            Attribute oldAttr = sourceLdapEntry.getAttributes().get(attributeId);
            if (oldAttr == null) {
                // consider it as an empty attribute to simplify the following
                // code
                oldAttr = new BasicAttribute(attributeId);
            }
            Attribute attrToRemove = new BasicAttribute(attributeId);

            NamingEnumeration<? extends Object> oldAttrs = oldAttr.getAll();
            String targetBaseDn = pseudoNormalizeDn(targetDirectory.getConfig().getSearchBaseDn());
            while (oldAttrs.hasMore()) {
                String dn = pseudoNormalizeDn(oldAttrs.next().toString());
                if (dn.endsWith(targetBaseDn)) {
                    // this is an entry managed by the current reference
                    attrToRemove.add(dn);
                }
            }
            try {
                if (attrToRemove.size() == oldAttr.size()) {
                    // use the empty ref marker to avoid empty attr
                    String emptyRefMarker = sourceDirectory.getConfig().getEmptyRefMarker();
                    Attributes emptyAttribute = new BasicAttributes(
                            attributeId, emptyRefMarker);
                    sourceSession.dirContext.modifyAttributes(sourceDn,
                            DirContext.REPLACE_ATTRIBUTE, emptyAttribute);
                } else if (attrToRemove.size() > 0) {
                    // remove the attribute managed by the current reference
                    Attributes attrsToRemove = new BasicAttributes();
                    attrsToRemove.put(attrToRemove);
                    sourceSession.dirContext.modifyAttributes(sourceDn,
                            DirContext.REMOVE_ATTRIBUTE, attrsToRemove);
                }
            } catch (SchemaViolationException e) {
                if (isDynamic()) {
                    // we are editing an entry that has no static part
                    log.warn(String.format(
                            "cannot remove dynamic reference in field %s for source %s",
                            getFieldName(), sourceId));
                } else {
                    // this is a real schma configuration problem, wrapup the
                    // exception
                    throw new DirectoryException(e);
                }
            }
        } catch (NamingException e) {
            throw new DirectoryException("removeLinksForSource failed: "
                    + e.getMessage(), e);
        } finally {
            sourceSession.close();
        }
    }

    /**
     * Remove existing statically defined links for the given target id (dynamic
     * references remain unaltered)
     *
     * @see org.nuxeo.ecm.directory.Reference#removeLinksForTarget(java.lang.String)
     */
    public void removeLinksForTarget(String targetId) throws DirectoryException {
        LDAPDirectory targetDirectory = (LDAPDirectory) getTargetDirectory();
        LDAPSession targetSession = (LDAPSession) targetDirectory.getSession();
        LDAPDirectory sourceDirectory = (LDAPDirectory) getSourceDirectory();
        LDAPSession sourceSession = (LDAPSession) sourceDirectory.getSession();
        String attributeId = getStaticAttributeId();
        try {
            if (!sourceSession.isReadOnly()) {
                // get the dn of the target that matches targetId
                String targetDn;
                SearchResult targetLdapEntry = targetSession.getLdapEntry(targetId);
                if (targetLdapEntry == null) {
                    String rdnAttribute = targetDirectory.getConfig().getRdnAttribute();
                    if (!rdnAttribute.equals(targetSession.idAttribute)) {
                        log.warn(String.format(
                                "cannot remove links to missing entry %s in directory %s",
                                targetId, targetDirectory.getName()));
                        return;
                    }
                    // the entry might have already been deleted, try to
                    // re-forge it if possible (might not work if scope is
                    // subtree)
                    targetDn = String.format("%s=%s,%s", rdnAttribute,
                            targetId,
                            targetDirectory.getConfig().getSearchBaseDn());
                } else {
                    targetDn = pseudoNormalizeDn(targetLdapEntry.getNameInNamespace());
                }

                // build a LDAP query to find entries that point to targetDn
                String searchFilter = String.format("(%s=%s)", attributeId,
                        targetDn);
                String sourceFilter = sourceDirectory.getBaseFilter();

                if (sourceFilter != null && !"".equals(sourceFilter)) {
                    searchFilter = String.format("(&(%s)(%s))", searchFilter,
                            sourceFilter);
                }

                SearchControls scts = new SearchControls();
                scts.setSearchScope(sourceDirectory.getConfig().getSearchScope());
                scts.setReturningAttributes(new String[] { attributeId });

                // find all source entries that point to targetDn and clean
                // those references
                NamingEnumeration<SearchResult> results = sourceSession.dirContext.search(
                        sourceSession.searchBaseDn, searchFilter, scts);

                String emptyRefMarker = sourceDirectory.getConfig().getEmptyRefMarker();
                Attributes emptyAttribute = new BasicAttributes(attributeId,
                        emptyRefMarker);

                while (results.hasMore()) {
                    SearchResult result = results.next();
                    Attributes attrs = result.getAttributes();
                    Attribute attr = attrs.get(attributeId);
                    try {
                        if (attr.size() == 1) {
                            // the attribute holds the last reference, put the
                            // empty ref. marker before removing the attribute
                            // since empty attribute are often not allowed by
                            // the server schema
                            sourceSession.dirContext.modifyAttributes(
                                    result.getNameInNamespace(),
                                    DirContext.ADD_ATTRIBUTE, emptyAttribute);
                        }
                        // remove the reference to targetDn
                        attrs = new BasicAttributes();
                        attr = new BasicAttribute(attributeId);
                        attr.add(targetDn);
                        attrs.put(attr);
                        sourceSession.dirContext.modifyAttributes(
                                result.getNameInNamespace(),
                                DirContext.REMOVE_ATTRIBUTE, attrs);
                    } catch (SchemaViolationException e) {
                        if (isDynamic()) {
                            // we are editing an entry that has no static part
                            log.warn(String.format(
                                    "cannot remove dynamic reference in field %s for target %s",
                                    getFieldName(), targetId));
                        } else {
                            // this is a real schema configuration problem,
                            // wrapup the exception
                            throw new DirectoryException(e);
                        }
                    }
                }
            }
        } catch (NamingException e) {
            throw new DirectoryException("removeLinksForTarget failed: "
                    + e.getMessage(), e);
        } finally {
            sourceSession.close();
            targetSession.close();
        }
    }

    /**
     * Edit the list of statically defined references for a given target
     * (dynamic references remain unaltered)
     *
     * @see org.nuxeo.ecm.directory.Reference#setSourceIdsForTarget(java.lang.String,
     *      java.util.List)
     */
    public void setSourceIdsForTarget(String targetId, List<String> sourceIds)
            throws DirectoryException {
        removeLinksForTarget(targetId);
        addLinks(sourceIds, targetId);
    }

    /**
     * Set the list of statically defined references for a given source (dynamic
     * references remain unaltered)
     *
     * @see org.nuxeo.ecm.directory.Reference#setTargetIdsForSource(java.lang.String,
     *      java.util.List)
     */
    public void setTargetIdsForSource(String sourceId, List<String> targetIds)
            throws DirectoryException {
        removeLinksForSource(sourceId);
        addLinks(sourceId, targetIds);
    }
}
