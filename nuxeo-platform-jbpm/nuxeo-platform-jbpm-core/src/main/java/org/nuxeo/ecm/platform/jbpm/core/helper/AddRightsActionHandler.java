/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.jbpm.core.helper;

import java.util.List;

import org.jbpm.graph.exe.ExecutionContext;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.platform.jbpm.AbstractJbpmHandlerHelper;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Action handler that add READ rights to given participants.
 *
 * @author Anahide Tchertchian
 *
 */
public class AddRightsActionHandler extends AbstractJbpmHandlerHelper {

    private static final long serialVersionUID = 1L;

    private String item;
    private String list;

    protected NuxeoPrincipal getNuxeoPrincipal(String user) throws Exception {
        UserManager userManager = Framework.getService(UserManager.class);
        return userManager.getPrincipal(user);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute(ExecutionContext executionContext) throws Exception {
        this.executionContext = executionContext;
        if (nuxeoHasStarted() && list != null) {
            List<String> participants = (List<String>) executionContext.getContextInstance().getTransientVariable(
                    list);
            if (participants == null) {
                participants = (List<String>) executionContext.getVariable(list);
            }
            CoreSession session = null;
            try {
                String user = getSwimlaneUser(getInitiator());
                session = getCoreSession(getNuxeoPrincipal(user));
                DocumentRef docRef = getDocumentRef();
                ACP acp = session.getACP(docRef);
                String aclName = getACLName();
                ACL acl = acp.getOrCreateACL(aclName);
                for (String participant : participants) {
                    // get rid of user/group prefix
                    String pname = participant;
                    if (pname.startsWith(NuxeoPrincipal.PREFIX)) {
                        pname = pname.substring(NuxeoPrincipal.PREFIX.length());
                    } else if (pname.startsWith(NuxeoGroup.PREFIX)) {
                        pname = pname.substring(NuxeoGroup.PREFIX.length());
                    }
                    acl.add(new ACE(participant, SecurityConstants.READ, true));
                }
                acp.addACL(acl);
                session.setACP(docRef, acp, true);
                session.save();
            } finally {
                closeCoreSession(session);
            }
        }
        executionContext.getToken().signal();
    }
}
