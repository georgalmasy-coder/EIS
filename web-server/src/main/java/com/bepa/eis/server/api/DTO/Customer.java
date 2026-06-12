package com.bepa.eis.server.api.DTO;

import com.bepa.eis.common.dto.WebSession;
import com.bepa.eis.server.dataprovider.fields.booleans.Active;
import com.bepa.eis.server.dataprovider.fields.integers.ids.CustomerId;
import com.bepa.eis.server.dataprovider.fields.strings.CustomerName;
import com.bepa.eis.server.dataprovider.generic.ListOfElements;

public class Customer {

    private ListOfElements customerElements = null;

    private CustomerId customerId;
    private CustomerName customerName;
    private Active active;

    private final String customerType = "Customer";


    private final WebSession webSession;

    public Customer(WebSession webSession) {
        this.webSession = webSession;
    }

    private WebSession getWebSession() {
        return webSession;
    }

    public void setCustomerId(CustomerId customerId) {
        this.customerId = customerId;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public void setCustomerName(CustomerName customerName) {
        this.customerName = customerName;
    }

    public CustomerName getCustomerName() {
        return customerName;
    }

    public void setActive(Active active) {
        this.active = active;
    }

    public Boolean isActive() {
        return active.getValue();
    }

    public ListOfElements getCustomerElements() {
        if (customerElements == null) {
            customerElements = new ListOfElements(getWebSession(), this.getClass().getSimpleName());
        }
        return customerElements;
    }


}
