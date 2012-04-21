package org.graylog2.log4j;

import java.util.Map;

public interface GelfMessageProvider {
    boolean isExtractStacktrace();
    String getOriginHost();
    String getFacility();
    Map<String, String> getFields();
    boolean isAddExtendedInformation();
}
