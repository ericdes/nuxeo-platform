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
 * $Id: CreateTasksActionHandler.java 28924 2008-01-10 14:04:05Z sfermigier $
 */

package org.nuxeo.ecm.platform.publishing.workflow.handler;

import java.util.Map;

import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.graph.node.TaskNode;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.taskmgmt.exe.TaskMgmtInstance;
import org.nuxeo.ecm.platform.publishing.workflow.PublishingConstants;
import org.nuxeo.ecm.platform.workflow.jbpm.handlers.api.client.AbstractWorkflowDocumentActionHandler;

/**
 * Create one task per workflow side declared reviewers.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class CreateTasksActionHandler extends
        AbstractWorkflowDocumentActionHandler {

    private static final long serialVersionUID = 1327637414446001773L;

    public void execute(ExecutionContext executionContext) throws Exception {
        Token token = executionContext.getToken();
        TaskMgmtInstance tmi = executionContext.getTaskMgmtInstance();
        TaskNode taskNode = (TaskNode) executionContext.getNode();

        // Already initialized.
        if (tmi.getTaskInstances() != null) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, ?> tasks = taskNode.getTasksMap();
        String[] reviewers = (String[]) executionContext.getVariable(
                PublishingConstants.WORKFLOW_REVIEWERS);
        for (String k : tasks.keySet()) {
            for (String reviewer : reviewers) {
                log.debug("Create task =" + k + " for user" + reviewer);
                TaskInstance ti = tmi.createTaskInstance(
                        taskNode.getTask(k), token);
                ti.setActorId(reviewer);
                ti.start();
            }
        }
    }

}
