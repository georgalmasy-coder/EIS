package com.bepa.eis.server.dataprovider.fields.lookups.common;

import com.bepa.eis.server.api.DTO.User;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.CustomerLookupCache;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class AbstractUserLookup extends AbstractLookup {

    private static final Logger log = LoggerFactory.getLogger(AbstractUserLookup.class);

    public AbstractUserLookup() { }

    public AbstractUserLookup(WebSession webSession) {
        setWebSession(webSession);
    }

    public AbstractUserLookup(Integer userId) {
        setValue(userId);
    }

    public void setValue(Integer userId) {
        LookupValue lookupValue = CustomerLookupCache.getUserLookupValue(getWebSession(), userId);
        setLookupValue(lookupValue);
    }

    public Integer getValue() {
        return getLookupId();
    }

    public User getUser() {
        if (getValue() == null) {
            return new User();
        }
        return CustomerLookupCache.getUser(getWebSession(), getValue());
    }

    @Override
    public String toString() {
        return getLookupCode();
    }
}
