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
 * $Id: MethodValueExpression.java 28491 2008-01-04 19:04:30Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.binding;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.el.ELContext;
import javax.el.MethodExpression;
import javax.el.ValueExpression;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Method value expression encapsulates a method expression so that it invokes
 * it when evaluated as a standard value expression.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class MethodValueExpression extends ValueExpression implements
        Externalizable {

    private static final Log log = LogFactory.getLog(MethodValueExpression.class);

    private static final long serialVersionUID = 1228707110702282837L;

    private MethodExpression methodExpression;

    private Class[] paramTypesClasses;

    public MethodValueExpression() {
    }

    public MethodValueExpression(MethodExpression methodExpression,
            Class[] paramTypesClasses) {
        this.methodExpression = methodExpression;
        this.paramTypesClasses = paramTypesClasses;
    }

    // Expression interface

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MethodValueExpression)) {
            return false;
        }
        MethodValueExpression other = (MethodValueExpression) obj;
        return methodExpression.equals(other.methodExpression);
    }

    @Override
    public int hashCode() {
        return methodExpression.hashCode();
    }

    @Override
    public String getExpressionString() {
        return methodExpression.getExpressionString();
    }

    @Override
    public boolean isLiteralText() {
        return methodExpression.isLiteralText();
    }

    // ValueExpression interface

    @Override
    public Class<?> getExpectedType() {
        return Object.class;
    }

    @Override
    public Class<?> getType(ELContext arg0) {
        return MethodExpression.class;
    }

    @Override
    public Object getValue(ELContext arg0) {
        // invoke method instead of resolving value
        Object res;
        try {
            return methodExpression.invoke(arg0, paramTypesClasses);
        }
        catch (Throwable t) {
            log.error(
                    "Error while evaluation MethodValueExpression " + methodExpression.getExpressionString());
            log.error("parameters are : " + paramTypesClasses.toString());
            return null;
        }
    }

    @Override
    public boolean isReadOnly(ELContext arg0) {
        return true;
    }

    @Override
    public void setValue(ELContext arg0, Object arg1) {
        // do nothing
    }

    // Externalizable interface

    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {
        methodExpression = (MethodExpression) in.readObject();
        paramTypesClasses = (Class[]) in.readObject();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(methodExpression);
        out.writeObject(paramTypesClasses);
    }

}
