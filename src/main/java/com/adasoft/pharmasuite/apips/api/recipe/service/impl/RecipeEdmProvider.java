package com.adasoft.pharmasuite.apips.api.recipe.service.impl;

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

public class RecipeEdmProvider extends CsdlAbstractEdmProvider {

    public static final String NAMESPACE = "Apips";
    public static final String CONTAINER_NAME = "Container";
    public static final FullQualifiedName CONTAINER_FQN = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

    public static final String ES_RECIPES = "Recipes";
    public static final String ET_RECIPE  = "Recipe";
    public static final FullQualifiedName ET_RECIPE_FQN = new FullQualifiedName(NAMESPACE, ET_RECIPE);

    // Referenciados (definidos en CommonEdmProvider)
    public static final String CT_BOM_ITEMS = "BomItems";
    public static final FullQualifiedName CT_BOM_ITEMS_FQN = new FullQualifiedName(NAMESPACE, CT_BOM_ITEMS);

    @Override
    public CsdlEntityType getEntityType(FullQualifiedName fqn) {
        if (!ET_RECIPE_FQN.equals(fqn)) return null;

        var props = List.of(
                new CsdlProperty().setName("key").setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName()),
                new CsdlProperty().setName("name").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                new CsdlProperty().setName("status").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                new CsdlProperty().setName("revision").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),

                new CsdlProperty().setName("creationDate")
                        .setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName())
                        .setNullable(true),
                new CsdlProperty().setName("effectivityStartTime")
                        .setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName())
                        .setNullable(true),
                new CsdlProperty().setName("effectivityEndTime")
                        .setType(EdmPrimitiveTypeKind.DateTimeOffset.getFullQualifiedName())
                        .setNullable(true),

                // Complex types reales del dominio
                new CsdlProperty().setName("material").setType(CommonEdmProvider.CT_MATERIAL).setNullable(true),
                new CsdlProperty().setName("quantity").setType(CommonEdmProvider.CT_MEASURE_VALUE).setNullable(true),
                new CsdlProperty().setName("bomItemsList").setType(CT_BOM_ITEMS_FQN).setCollection(true).setNullable(true),
                new CsdlProperty().setName("procedure").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()).setNullable(true),
                new CsdlProperty().setName("accessPrivilege").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()).setNullable(true)
        );

        return new CsdlEntityType()
                .setName(ET_RECIPE)
                .setKey(List.of(new CsdlPropertyRef().setName("key")))
                .setProperties(props);
    }

    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName container, String entitySetName) {
        if (CONTAINER_FQN.equals(container) && ES_RECIPES.equals(entitySetName)) {
            return new CsdlEntitySet().setName(ES_RECIPES).setType(ET_RECIPE_FQN);
        }
        return null;
    }

    @Override
    public CsdlEntityContainer getEntityContainer() {
        return new CsdlEntityContainer()
                .setName(CONTAINER_NAME)
                .setEntitySets(List.of(new CsdlEntitySet().setName(ES_RECIPES).setType(ET_RECIPE_FQN)));
    }

    @Override
    public List<CsdlSchema> getSchemas() {
        return List.of(new CsdlSchema()
                .setNamespace(NAMESPACE)
                .setEntityTypes(List.of(getEntityType(ET_RECIPE_FQN)))
                .setComplexTypes(List.of(bomItems()))
                .setEntityContainer(getEntityContainer()));
    }

    @Override
    public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName name) {
        if (name == null || CONTAINER_FQN.equals(name)) {
            return new CsdlEntityContainerInfo().setContainerName(CONTAINER_FQN);
        }
        return null;
    }

    @Override
    public CsdlComplexType getComplexType(FullQualifiedName complexTypeName) {
        if (CT_BOM_ITEMS_FQN.equals(complexTypeName)) return bomItems();
        return null;
    }

    private static CsdlComplexType bomItems() {
        return new CsdlComplexType()
                .setName(CT_BOM_ITEMS)
                .setProperties(List.of(
                        new CsdlProperty().setName("materialIdentifier").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                        new CsdlProperty().setName("materialDescription").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                        new CsdlProperty().setName("quantity").setType(CommonEdmProvider.CT_MEASURE_VALUE).setNullable(true),
                        new CsdlProperty().setName("position").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
                ));
    }
}
