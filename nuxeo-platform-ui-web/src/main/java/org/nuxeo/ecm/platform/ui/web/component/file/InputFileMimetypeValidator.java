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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: InputFileMimetypeValidator.java 28610 2008-01-09 17:13:52Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.component.file;

import javax.faces.component.StateHolder;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.faces.util.MessageFactory;

/**
 * Input file mimetype validator.
 * <p>
 * Validates an {@link InputFileInfo} blob value in case it's been uploaded.
 * Accepted mimetypes are set using the "extensions" attribute, representing the
 * list of accepted extension suffixes separated by commas (for instance:
 * ".jpeg, .png").
 * <p>
 * Validation is done on the filename, no actual mimetype check is done for now.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class InputFileMimetypeValidator implements Validator, StateHolder {

    public static final String VALIDATOR_ID = "InputFileMimetypeValidator";

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(InputFileSizeValidator.class);

    private String[] extensions;

    private boolean authorized = true;

    private boolean transientValue = false;

    /**
     * The message identifier of the
     * {@link javax.faces.application.FacesMessage} to be created if the
     * authorized extensions check fails. The message format string for this
     * message may optionally include the following placeholders:
     * <ul>
     * <li><code>{0}</code> replaced by the configured auhtorized extensions.</li>
     * </ul>
     * </p>
     */
    public static final String MIMETYPE_AUTHORIZED_EXTENSIONS_MESSAGE_ID = "error.inputFile.authorizedExtensions";

    /**
     * The message identifier of the
     * {@link javax.faces.application.FacesMessage} to be created if the
     * unauthorized extensions check fails. The message format string for this
     * message may optionally include the following placeholders:
     * <ul>
     * <li><code>{0}</code> replaced by the configured unauthorized
     * extensions.</li>
     * </ul>
     * </p>
     */
    public static final String MIMETYPE_UNAUTHORIZED_EXTENSIONS_MESSAGE_ID = "error.inputFile.unauthorizedExtensions";

    public void validate(FacesContext context, UIComponent component,
            Object value) throws ValidatorException {
        if (value != null && extensions != null && extensions.length > 0) {
            if (value instanceof InputFileInfo) {
                InputFileInfo info = (InputFileInfo) value;
                InputFileChoice choice = info.getConvertedChoice();
                if (!InputFileChoice.tempKeep.equals(choice)
                        && !InputFileChoice.upload.equals(choice)) {
                    return;
                }
                String filename = info.getConvertedFilename();
                if (filename != null) {
                    boolean error = authorized;
                    for (String extension : extensions) {
                        if (filename.endsWith(extension.trim())) {
                            error = !authorized;
                            break;
                        }
                    }
                    // TODO: handle content types
                    if (error) {
                        String messageId = authorized ? MIMETYPE_AUTHORIZED_EXTENSIONS_MESSAGE_ID
                                : MIMETYPE_UNAUTHORIZED_EXTENSIONS_MESSAGE_ID;
                        throw new ValidatorException(MessageFactory.getMessage(
                                context, messageId, StringUtils.join(
                                        extensions, ", ")));
                    }
                }
            }
        }
    }

    public String[] getExtensions() {
        return extensions;
    }

    public void setExtensions(String[] extensions) {
        this.extensions = extensions;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public boolean isTransient() {
        return transientValue;
    }

    public void setTransient(boolean newTransientValue) {
        transientValue = newTransientValue;
    }

    public Object saveState(FacesContext context) {
        Object[] values = new Object[2];
        values[0] = extensions;
        values[1] = authorized;
        return values;
    }

    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        extensions = (String[]) values[0];
        authorized = ((Boolean) values[1]).booleanValue();
    }

}
