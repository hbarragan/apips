package com.adasoft.pharmasuite.apips.api.common.service.impl;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;

import java.util.Collections;
import java.util.List;

public class CommonEdmProvider extends CsdlAbstractEdmProvider {

    public static final String NAMESPACE = "Apips";

    public static final FullQualifiedName CT_MEASURE_VALUE = new FullQualifiedName(NAMESPACE, "MeasureValue");
    public static final FullQualifiedName CT_MATERIAL = new FullQualifiedName(NAMESPACE, "Material");

    public static CsdlComplexType measureValue() {
        return new CsdlComplexType()
                .setName("MeasureValue")
                .setProperties(List.of(
                        new CsdlProperty().setName("unitOfMeasure").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                        new CsdlProperty().setName("value")
                                .setType(EdmPrimitiveTypeKind.Decimal.getFullQualifiedName())
                                .setPrecision(38)
                                .setScale(18),
                        new CsdlProperty().setName("scale").setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName())
                ));
    }

    public static CsdlComplexType material() {
        return new CsdlComplexType()
                .setName("Material")
                .setProperties(List.of(
                        new CsdlProperty().setName("id").setType(EdmPrimitiveTypeKind.Int64.getFullQualifiedName()),
                        new CsdlProperty().setName("name").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                        new CsdlProperty().setName("category").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                        new CsdlProperty().setName("description").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName()),
                        new CsdlProperty().setName("unitOfMeasure").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName())
                ));
    }

    @Override
    public CsdlComplexType getComplexType(FullQualifiedName complexTypeName) {
        if (CT_MEASURE_VALUE.equals(complexTypeName)) return measureValue();
        if (CT_MATERIAL.equals(complexTypeName)) return material();
        return null;
    }

    @Override
    public List<CsdlSchema> getSchemas() {
        var schema = new CsdlSchema()
                .setNamespace(NAMESPACE)
                .setComplexTypes(List.of(measureValue(), material()));
        return List.of(schema);
    }
}
