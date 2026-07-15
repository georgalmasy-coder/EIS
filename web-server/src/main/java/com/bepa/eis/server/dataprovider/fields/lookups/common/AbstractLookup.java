package com.bepa.eis.server.dataprovider.fields.lookups.common;

import com.bepa.eis.common.GlobalConfiguration;
import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.api.web.application.cache.LookupValue;
import com.bepa.eis.server.api.web.application.enums.FieldControl;
import com.bepa.eis.server.dataprovider.fields.integers.AbstractInteger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

abstract public class AbstractLookup extends AbstractInteger {

    abstract public String getLookupName();
    abstract public String getDropdownSelectText();
    abstract public List<LookupValue> getListOfActiveLookupValues();

    private LookupValue lookupValue;
    private WebSession webSession;

    abstract public void setValue(Integer value);

    public AbstractLookup() {
        super();
        setWebSession(null);
    }

    public AbstractLookup(WebSession webSession) {
        setWebSession(webSession);
    }

    private boolean sortOptions = true;
    private List<SelectOption> options = new ArrayList<>();

    public WebSession getWebSession() {
        return webSession;
    }

    public void setWebSession(WebSession webSession) {
        this.webSession = webSession;
    }

    public void setLookupValue(LookupValue lookupValue) {
        this.lookupValue = lookupValue;
        super.setValue(lookupValue != null ? lookupValue.getLookupId() : null);
    }

    public LookupValue getLookupValue() {
        return lookupValue;
    }

    public Integer getLookupId() {
        if (lookupValue != null) {
            return lookupValue.getLookupId();
        }
        return -1;
//        throw new IllegalStateException("Failed to getLookupId ");
    }

    public String getLookupIdAsString() {
        if (lookupValue != null && lookupValue.getLookupId() != null) {
            return lookupValue.getLookupId().toString();
        }
        return "";
    }

    public String getLookupCode() {
        if (lookupValue != null) {
            return lookupValue.getLookupCode();
        }
        throw new IllegalStateException("Failed to lookupCode ");
    }

    public void setSortOptions(boolean sortOptions) {
        this.sortOptions = sortOptions;
    }

    protected static DataSource getDataSource() {
        try {
            InitialContext ctx = new InitialContext();
            return (DataSource) ctx.lookup(GlobalConfiguration.getJndiName());
        } catch (NamingException e) {
            throw new IllegalStateException("Failed to lookup DataSource via JNDI name: " + GlobalConfiguration.getJndiName(), e);
        }
    }

    public void addAllActiveOptions(WebSession webSession) {
        if (/*isFieldRequired() ||*/ true) {
            Integer lookupId = null;
            String selectText = getDropdownSelectText() != null ? getDropdownSelectText() : "Select ...";
            addOption(lookupId, selectText, false);
        }
        for (LookupValue lookupValue : getListOfActiveLookupValues()) {
            boolean isSelected = getLookupId() != null ? Objects.equals(lookupValue.getLookupId(), getLookupId()) : false;
            addOption(lookupValue.getLookupId(), lookupValue.getLookupCode(), isSelected);
        }
    }

    public void addCurrentOptions(WebSession webSession) {
        LookupValue lookupValue = getLookupValue();
        if (lookupValue != null) {
            addOption(lookupValue.getLookupId(), lookupValue.getLookupCode(), true);
        }
    }

    @Override
    public void setFieldEditable() {
        super.setFieldEditable();
        if (sortOptions) {
            sortOptions();
        }
    }

    public void addOptionNoValue(String label, boolean selected ) {
        addOption((Integer) null, label, selected);
    }

    public void addOption(Integer value, String label, boolean selected ) {
        String valueAsString = value != null ? value.toString() : null;
        addOption(valueAsString, label, selected);
    }

    public void addOption(String value, String label, boolean selected ) {

        SelectOption option = new SelectOption(value, label, selected);
        if ( isFieldEditable() ) {
            options.add(option);
        } else {
            if (selected) {
                options.add(option);
            }
        }
    }

    public void sortOptions() {
        options.sort(Comparator.comparing(SelectOption::getLabel,
                     Comparator.nullsLast(String::compareToIgnoreCase)));
    }


    public boolean hasFieldLookupColor() {
        if (lookupValue != null && lookupValue.getLookupCode() != null && ! lookupValue.getLookupCode().isEmpty()) {
            return true;
        }
        return false;
    }

    public String getFieldLookupColor() {
        if (lookupValue != null && lookupValue.getLookupCode() != null && ! lookupValue.getLookupCode().isEmpty()) {
            return lookupValue.getLookupColor();
        }
        return null;
    }

    public List<SelectOption> getOptions() {
        return options;
    }

    @Override
    public String toString() {
        return getLookupCode()  != null ? getLookupCode() : "";
    }

    @Override
    public FieldControl getFieldControl() {
        return FieldControl.SELECT;
    }

    @Override
    public Integer getFieldMinLength() {
        return null;
    }

    @Override
    public Integer getFieldMaxLength() {
        return null;
    }

    @Override
    public Integer getFieldDisplayLength() {
        return 25;
    }

    @Override
    public Integer getFieldRow() {
        return null;
    }

    @Override
    public Integer getFieldCol() {
        return null;
    }

    public static class SelectOption {
        private String value;
        private String label;
        private boolean selected;

        public SelectOption( String value, String label, boolean selected) {
            this.value = value;
            this.label = label;
            this.selected = selected;
        }

        public String getLabel() {
            return label;
        }
        public String getValue() {
            return value;
        }
        public boolean isSelected() {
            return selected;
        }

    }
}
