package com.bepa.eis.server.entites.configuration;

import com.bepa.eis.common.enums.entity.EntityDataElement;
import com.bepa.eis.common.enums.entity.EntityType;
import com.bepa.eis.server.dataprovider.misc.CustomerProjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.ConfigurationException;
import java.util.concurrent.ConcurrentHashMap;

public class EntityConfiguration {

    private static final Logger log = LoggerFactory.getLogger(EntityConfiguration.class);
    private static EntityConfiguration instance = null;

    private final ConcurrentHashMap<Integer, Integer> mapOfProjects = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, EntityType> mapOfEntities = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, EntityDataElement> mapOfEntityDataElementById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, EntityDataElement> mapOfEntityDataElementByFieldName = new ConcurrentHashMap<>();

    private boolean isInitialized = false;

    public static EntityConfiguration getInstance() {
        if (instance == null) {
            instance = new EntityConfiguration();

            try {
                instance.buildAndValidateConfiguration();
            } catch (ConfigurationException e) {
                throw new RuntimeException(e);
            }
        }

        return instance;
    }

    public void buildAndValidateConfiguration() throws ConfigurationException {

        if ( ! isInitialized ) {

            for (EntityType entityType : EntityType.values()) {

                EntityType type = mapOfEntities.get(entityType.getId());
                if (type != null) {
                    throw new ConfigurationException("Duplicate entity type found: " + entityType);
                }
                mapOfEntities.put(entityType.getId(), entityType);
            }


            for (EntityDataElement entityDataElement : EntityDataElement.values()) {

                EntityDataElement dataElement = mapOfEntityDataElementById.get(entityDataElement.getId());
                if (dataElement != null) {
                    throw new ConfigurationException("Duplicate entity data element type found: " + entityDataElement);
                }
                mapOfEntityDataElementById.put(entityDataElement.getId(), entityDataElement);
            }

            for (EntityDataElement entityDataElement : EntityDataElement.values()) {

                EntityDataElement dataElement = mapOfEntityDataElementByFieldName.get(entityDataElement.getFieldNameUpperCase());
                if (dataElement != null) {
                    throw new ConfigurationException("Duplicate entity data element type found: " + entityDataElement);
                }
                log.debug("Adding EntityDataElement: {} {} ", entityDataElement.getFieldNameUpperCase(), entityDataElement);
                mapOfEntityDataElementByFieldName.put(entityDataElement.getFieldNameUpperCase(), entityDataElement);
            }

            isInitialized = true;
        }
    }

    public EntityDataElement getEntityDataElementByFieldName(String fieldName) {
        return mapOfEntityDataElementByFieldName.get(fieldName.toUpperCase());
    }


    /**
     * Retrieves the customer ID associated with the specified project ID.
     * If the customer ID is not cached, it retrieves the customer ID from
     * the CustomerProjectProvider and updates the local cache.
     *
     * @param projectId The ID of the project for which the customer ID needs to be retrieved.
     * @return The customer ID associated with the specified project ID, or null if no association exists.
     */
    public Integer getCustomerIdByProjectId(Integer projectId) {
        Integer customerId = mapOfProjects.get(projectId);
        if (customerId == null) {
            CustomerProjectProvider customerProjectProvider = new CustomerProjectProvider(null);
            customerId = customerProjectProvider.getCustomerIdByProjectId(projectId);
            mapOfProjects.put(projectId, customerId);
        }
        return customerId;
    }

    public EntityDataElement getEntityDataElement(Integer entityDataElementType) {
         return mapOfEntityDataElementById.get(entityDataElementType);
    }
}
