package com.adasoft.pharmasuite.apips.api.batch.service.impl;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;

import java.util.List;

public class TransactionHistoryEdmProvider extends CsdlAbstractEdmProvider {

    public static final String NAMESPACE = "Apips";
    public static final String CONTAINER_NAME = "Container";
    public static final FullQualifiedName CONTAINER_FQN = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

    public static final String ES_TRANSACTION_HISTORY = "TransactionHistories";
    public static final String ET_TRANSACTION_HISTORY = "TransactionHistory";
    public static final FullQualifiedName ET_TRANSACTION_HISTORY_FQN = new FullQualifiedName(NAMESPACE, ET_TRANSACTION_HISTORY);

    @Override
    public CsdlEntityType getEntityType(FullQualifiedName fqn) {
        if (!ET_TRANSACTION_HISTORY_FQN.equals(fqn)) return null;
        var props = List.of(
                new CsdlProperty().setName("key").setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName()),
                new CsdlProperty().setName("time").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName()),
                new CsdlProperty().setName("type").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                new CsdlProperty().setName("subtype").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                new CsdlProperty().setName("orderStep").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                new CsdlProperty().setName("batchIdNew").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                new CsdlProperty().setName("batchIdOld").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                new CsdlProperty().setName("sublotIdNew").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                new CsdlProperty().setName("sublotIdOld").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
        );
        return new CsdlEntityType()
                .setName(ET_TRANSACTION_HISTORY)
                .setKey(List.of(new CsdlPropertyRef().setName("key")))
                .setProperties(props);
    }

    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName container, String entitySetName) {
        if (CONTAINER_FQN.equals(container) && ES_TRANSACTION_HISTORY.equals(entitySetName)) {
            return new CsdlEntitySet().setName(ES_TRANSACTION_HISTORY).setType(ET_TRANSACTION_HISTORY_FQN);
        }
        return null;
    }

    @Override
    public List<CsdlSchema> getSchemas() {
        var schema = new CsdlSchema()
                .setNamespace(NAMESPACE)
                .setEntityTypes(List.of(getEntityType(ET_TRANSACTION_HISTORY_FQN)))
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
                .setEntitySets(List.of(new CsdlEntitySet().setName(ES_TRANSACTION_HISTORY).setType(ET_TRANSACTION_HISTORY_FQN)));
    }
}
