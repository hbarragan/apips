package com.adasoft.pharmasuite.apips.api.workflow.service.impl;

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


public class WorkflowEdmProvider extends CsdlAbstractEdmProvider {


    public static final String NAMESPACE = "Apips";
    public static final String CONTAINER_NAME = "Container";
    public static final FullQualifiedName CONTAINER_FQN = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);


    public static final String ES_WORKFLOWS = "Workflows";
    public static final String ET_WORKFLOW = "Workflow";
    public static final FullQualifiedName ET_WORKFLOW_FQN = new FullQualifiedName(NAMESPACE, ET_WORKFLOW);


    @Override
    public CsdlEntityType getEntityType(FullQualifiedName fqn) {
        if (!ET_WORKFLOW_FQN.equals(fqn)) return null;


        var props = List.of(
                new CsdlProperty().setName("key").setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName()),
                new CsdlProperty().setName("name").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                new CsdlProperty().setName("description").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                new CsdlProperty().setName("status").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                new CsdlProperty().setName("productionRelevant").setType(EdmPrimitiveTypeKind.Boolean.getFullQualifiedName()),
                new CsdlProperty().setName("masterWorkflowName").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                new CsdlProperty().setName("masterWorkflowDescription").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                new CsdlProperty().setName("creationDate").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName()),
                new CsdlProperty().setName("actualStart").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName()),
                new CsdlProperty().setName("actualFinish").setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName()),
                new CsdlProperty().setName("appendable").setType(EdmPrimitiveTypeKind.Boolean.getFullQualifiedName()),
                new CsdlProperty().setName("ordersAssociated").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                new CsdlProperty().setName("upAssociated").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()).setCollection(true),
                new CsdlProperty().setName("accessPrivilege").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
        );


        return new CsdlEntityType()
                .setName(ET_WORKFLOW)
                .setKey(List.of(new CsdlPropertyRef().setName("key")))
                .setProperties(props);
    }


    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName container, String entitySetName) {
        if (CONTAINER_FQN.equals(container) && ES_WORKFLOWS.equals(entitySetName)) {
            return new CsdlEntitySet().setName(ES_WORKFLOWS).setType(ET_WORKFLOW_FQN);
        }
        return null;
    }

    @Override
    public List<CsdlSchema> getSchemas() {
        var schema = new CsdlSchema()
                .setNamespace(NAMESPACE)
                .setEntityTypes(List.of(getEntityType(ET_WORKFLOW_FQN)))
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
                .setEntitySets(List.of(new CsdlEntitySet().setName(ES_WORKFLOWS).setType(ET_WORKFLOW_FQN)));
    }
}