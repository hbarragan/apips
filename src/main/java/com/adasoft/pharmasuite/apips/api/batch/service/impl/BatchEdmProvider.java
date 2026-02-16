package com.adasoft.pharmasuite.apips.api.batch.service.impl;

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

public class BatchEdmProvider extends CsdlAbstractEdmProvider {

    public static final String NAMESPACE = "Apips";
    public static final String CONTAINER_NAME = "Container";
    public static final FullQualifiedName CONTAINER_FQN = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

    public static final String ES_BATCH = "Batches";
    public static final String ET_BATCH = "Batch";
    public static final FullQualifiedName ET_BATCH_FQN = new FullQualifiedName(NAMESPACE, ET_BATCH);

    // FQN de ComplexTypes
    private static final FullQualifiedName CT_SUBLOT        = new FullQualifiedName(NAMESPACE, "Sublot");
    private static final FullQualifiedName CT_TXHIST        = new FullQualifiedName(NAMESPACE, "TransactionHistoryItem");

    private static CsdlComplexType sublot() {
        return new CsdlComplexType()
                .setName("Sublot")
                .setProperties(List.of(
                        new CsdlProperty().setName("name").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                        new CsdlProperty().setName("key").setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName()),
                        new CsdlProperty().setName("quantity").setType(CommonEdmProvider.CT_MEASURE_VALUE),
                        new CsdlProperty().setName("quantityConsumed").setType(CommonEdmProvider.CT_MEASURE_VALUE),
                        new CsdlProperty().setName("material").setType(CommonEdmProvider.CT_MATERIAL),
                        new CsdlProperty().setName("batchIdentifier").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                        new CsdlProperty().setName("productionOrderStep").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                        new CsdlProperty().setName("storageLocation").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                        new CsdlProperty().setName("storageArea").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                        new CsdlProperty().setName("warehouse").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                        new CsdlProperty().setName("tare").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                        new CsdlProperty().setName("productionDate").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName())
                ));
    }

    private static CsdlComplexType transactionHistory() {
        return new CsdlComplexType()
                .setName("TransactionHistoryItem")
                .setProperties(List.of(
                        new CsdlProperty().setName("orderId").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                        new CsdlProperty().setName("time").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName()),
                        new CsdlProperty().setName("type").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                        new CsdlProperty().setName("subtype").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                        new CsdlProperty().setName("orderStep").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                        new CsdlProperty().setName("batchIdNew").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                        new CsdlProperty().setName("batchIdOld").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                        new CsdlProperty().setName("sublotIdNew").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                        new CsdlProperty().setName("sublotIdOld").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
                ));
    }

    // >>> CLAVE: exponer resolución perezosa de ComplexTypes
    @Override
    public CsdlComplexType getComplexType(FullQualifiedName complexTypeName) {
        if (CT_SUBLOT.equals(complexTypeName))        return sublot();
        if (CT_TXHIST.equals(complexTypeName))        return transactionHistory();
        return null;
    }

    @Override
    public CsdlEntityType getEntityType(FullQualifiedName fqn) {
        if (!ET_BATCH_FQN.equals(fqn)) return null;
        var props = List.of(
                new CsdlProperty().setName("key").setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName()),
                new CsdlProperty().setName("name").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                new CsdlProperty().setName("status").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                new CsdlProperty().setName("material").setType(CommonEdmProvider.CT_MATERIAL),
                new CsdlProperty().setName("quantity").setType(CommonEdmProvider.CT_MEASURE_VALUE),
                new CsdlProperty().setName("potency").setType(CommonEdmProvider.CT_MEASURE_VALUE),
                new CsdlProperty().setName("totalConsumed").setType(CommonEdmProvider.CT_MEASURE_VALUE),
                new CsdlProperty().setName("creationTime").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName()),
                new CsdlProperty().setName("expiryDate").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName()),
                new CsdlProperty().setName("retestDate").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName()),
                new CsdlProperty().setName("productionDate").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName()),
                new CsdlProperty().setName("nextInspectionDate").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName()),
                new CsdlProperty().setName("transactionHistory").setType(CT_TXHIST).setCollection(true)
        );
        return new CsdlEntityType()
                .setName(ET_BATCH)
                .setKey(List.of(new CsdlPropertyRef().setName("key")))
                .setProperties(props);
    }

    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName container, String entitySetName) {
        if (CONTAINER_FQN.equals(container) && ES_BATCH.equals(entitySetName)) {
            return new CsdlEntitySet().setName(ES_BATCH).setType(ET_BATCH_FQN);
        }
        return null;
    }

    @Override
    public List<CsdlSchema> getSchemas() {
        var schema = new CsdlSchema()
                .setNamespace(NAMESPACE)
                .setComplexTypes(List.of(sublot(), transactionHistory()))
                .setEntityTypes(List.of(getEntityType(ET_BATCH_FQN)))
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
                .setEntitySets(List.of(new CsdlEntitySet().setName(ES_BATCH).setType(ET_BATCH_FQN)));
    }
}
