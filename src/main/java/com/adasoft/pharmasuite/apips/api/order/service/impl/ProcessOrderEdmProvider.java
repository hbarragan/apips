package com.adasoft.pharmasuite.apips.api.order.service.impl;

import com.adasoft.pharmasuite.apips.api.common.service.impl.CommonEdmProvider;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;

import java.util.List;

public class ProcessOrderEdmProvider extends CsdlAbstractEdmProvider {

    public static final String NAMESPACE = "Apips";
    public static final String CONTAINER_NAME = "Container";
    public static final FullQualifiedName CONTAINER_FQN = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

    public static final String ES_PROCESS_ORDERS = "ProcessOrders";
    public static final String ET_PROCESS_ORDER = "ProcessOrder";
    public static final FullQualifiedName ET_PROCESS_ORDER_FQN = new FullQualifiedName(NAMESPACE, ET_PROCESS_ORDER);


    @Override
    public CsdlComplexType getComplexType(FullQualifiedName complexTypeName) {
        return null;
    }

    @Override
    public CsdlEntityType getEntityType(FullQualifiedName fqn) {
        if (!ET_PROCESS_ORDER_FQN.equals(fqn)) return null;
        var props = List.of(
                new CsdlProperty().setName("key").setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName()),
                new CsdlProperty().setName("name").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                new CsdlProperty().setName("status").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                new CsdlProperty().setName("material").setType(CommonEdmProvider.CT_MATERIAL),
                new CsdlProperty().setName("quantity").setType(CommonEdmProvider.CT_MEASURE_VALUE),
                new CsdlProperty().setName("consumedQuantity").setType(CommonEdmProvider.CT_MEASURE_VALUE),
                new CsdlProperty().setName("targetBatch").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                new CsdlProperty().setName("creationDate").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName()),
                new CsdlProperty().setName("plannedStart").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName()),
                new CsdlProperty().setName("plannedFinish").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName()),
                new CsdlProperty().setName("actualStart").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName()),
                new CsdlProperty().setName("actualFinish").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName()),
                new CsdlProperty().setName("erpStartDate").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName()),
                new CsdlProperty().setName("erpFinishDate").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName()),
                new CsdlProperty().setName("recipeName").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                new CsdlProperty().setName("recipeDescription").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                new CsdlProperty().setName("batchName").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                new CsdlProperty().setName("workCenter").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                new CsdlProperty().setName("location").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                new CsdlProperty().setName("accessPrivilege").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
        );
        return new CsdlEntityType()
                .setName(ET_PROCESS_ORDER)
                .setKey(List.of(new CsdlPropertyRef().setName("key")))
                .setProperties(props);
    }

    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName container, String entitySetName) {
        if (CONTAINER_FQN.equals(container) && ES_PROCESS_ORDERS.equals(entitySetName)) {
            return new CsdlEntitySet().setName(ES_PROCESS_ORDERS).setType(ET_PROCESS_ORDER_FQN);
        }
        return null;
    }

    @Override
    public List<CsdlSchema> getSchemas() {
        var schema = new CsdlSchema()
                .setNamespace(NAMESPACE)
                .setEntityTypes(List.of(getEntityType(ET_PROCESS_ORDER_FQN)))
                .setEntityContainer(getEntityContainer());
        return List.of(schema);
    }

    @Override
    public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName name) {
        if (name == null || CONTAINER_FQN.equals(name)) {
            return new CsdlEntityContainerInfo().setContainerName(CONTAINER_FQN);
        }
        return null;
    }

    @Override
    public CsdlEntityContainer getEntityContainer() {
        return new CsdlEntityContainer()
                .setName(CONTAINER_NAME)
                .setEntitySets(List.of(new CsdlEntitySet().setName(ES_PROCESS_ORDERS).setType(ET_PROCESS_ORDER_FQN)));
    }
}
