package org.nuxeo.ecm.platform.transform.plugin.jr;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.extractor.OpenOfficeTextExtractor;
import org.apache.jackrabbit.extractor.TextExtractor;

public class OOoSimpleTextExtractor extends AbstractJRBasedTextExtractorPlugin {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(OOoSimpleTextExtractor.class);

    @Override
    protected TextExtractor getExtractor() {
        return new OpenOfficeTextExtractor();
    }


}
