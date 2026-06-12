package com.bepa.eis.server.api.DTO;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.ArrayList;
import java.util.List;

public class CustomerProject {

    private final List<Customer> customers = new ArrayList<>();

    public void addCustomerAndProject(int customerId,  String customerName, int projectId, String projectName) {

        Customer customer = new Customer(customerId, customerName);
        Project project = new Project(customerId, projectId, projectName);
        boolean customerFound = false;

        for (Customer c : customers) {
            if (c.getCustomerId() == customerId) {
                c.addProject(project);
                customerFound = true;
            }
        }

        if (!customerFound) {
            customers.add(customer);
            customer.addProject(project);
        }
    }

    /**
     * Builds an XML Document representing the content of this CustomerProject instance.
     *
     * XML structure:
     * <CustomerProjects>
     *   <Customer id="..." name="...">
     *     <Project id="..." name="..."/>
     *   </Customer>
     * </CustomerProjects>
     */
    public Document toXmlDocument() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();

        Element root = doc.createElement("customers");
        doc.appendChild(root);

        for (Customer c : customers) {
            Element customerEl = doc.createElement("customer");
            root.appendChild(customerEl);

            Element customerIdEL = doc.createElement("customerId");
            customerIdEL.setTextContent(String.valueOf(c.getCustomerId()));
            customerEl.appendChild(customerIdEL);

            Element customerNameEL = doc.createElement("customerName");
            customerNameEL.setTextContent(String.valueOf(c.getCustomerName()));
            customerEl.appendChild(customerNameEL);

            Element projectsEl = doc.createElement("projects");
            customerEl.appendChild(projectsEl);

            for (Project p : c.getProjects()) {
                Element projectEl = doc.createElement("project");

                Element projectIdEL = doc.createElement("projectId");
                projectIdEL.setTextContent(String.valueOf(p.getProjectId()));
                projectEl.appendChild(projectIdEL);

                Element projectNameEL = doc.createElement("projectName");
                projectNameEL.setTextContent(String.valueOf(p.getProjectName()));
                projectEl.appendChild(projectNameEL);

                projectsEl.appendChild(projectEl);
            }
        }

        return doc;
    }

    private static class Customer {
        private final int customerId;
        private final String customerName;
        private final List<Project> projects = new ArrayList<>();

        public Customer(int customerId, String customerName) {
            this.customerId = customerId;
            this.customerName = customerName;
        }
        public int getCustomerId() {
            return customerId;
        }
        public String getCustomerName() {
            return customerName;
        }
        public List<Project> getProjects() {
            return projects;
        }
        public void addProject(Project project) {
            projects.add(project);
        }

    }

    private static class Project {
        private final int customerId;
        private final int projectId;
        private final String projectName;

        public Project(int customerId, int projectId, String projectName ) {
            this.customerId = customerId;
            this.projectId = projectId;
            this.projectName = projectName;
        }

        public int getCustomerId() {
            return customerId;
        }
        public int getProjectId() {
            return projectId;
        }
        public String getProjectName() {
            return projectName;
        }
    }
}
