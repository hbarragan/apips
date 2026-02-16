package com.adasoft.pharmasuite.apips.api.common.util;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.Edm;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEdmProvider;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.core.edm.EdmProviderImpl;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.uri.queryoption.CountOption;

public class ODataResponseWriter {
    private final OData odata;
    private final ServiceMetadata md;
    private final Edm edm;

    public ODataResponseWriter(CsdlEdmProvider edmProvider) {
        this.odata = OData.newInstance();
        this.md    = odata.createServiceMetadata(edmProvider, java.util.List.of());
        this.edm   = new EdmProviderImpl(edmProvider);
    }

    /** Serializa EntityCollection a JSON (minimal metadata). */
    public byte[] writeEntityCollection(FullQualifiedName entityTypeFqn,
                                        String entitySetName,
                                        EntityCollection collection,
                                        CountOption countOpt) // <-- añade este parámetro
            throws SerializerException, java.io.IOException {

        var serializer = odata.createSerializer(ContentType.JSON);
        var entityType = edm.getEntityType(entityTypeFqn);

        var contextUrl = ContextURL.with()
                .entitySet(edm.getEntityContainer().getEntitySet(entitySetName))
                .build();

        var opts = EntityCollectionSerializerOptions.with()
                .contextURL(contextUrl)
                .count(countOpt) // <-- aquí va el CountOption (no boolean)
                .build();

        var result = serializer.entityCollection(md, entityType, collection, opts);
        return result.getContent().readAllBytes();
    }
}
