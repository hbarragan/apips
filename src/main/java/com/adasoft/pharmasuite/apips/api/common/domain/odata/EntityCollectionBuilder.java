package com.adasoft.pharmasuite.apips.api.common.domain.odata;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;

import java.net.URI;
import java.util.List;
import java.util.function.Function;

public final class EntityCollectionBuilder<T> {
    private final Function<T, Entity> mapper;

    public EntityCollectionBuilder(Function<T, Entity> mapper) {
        this.mapper = mapper;
    }

    /** Construye la colección, setea @odata.count y @odata.nextLink si procede. */
    public EntityCollection build(List<T> slice, Integer totalOrNull, URI nextLinkOrNull) {
        EntityCollection col = new EntityCollection();
        slice.forEach(t -> col.getEntities().add(mapper.apply(t)));
        if (totalOrNull != null) col.setCount(totalOrNull);   // -> @odata.count
        if (nextLinkOrNull != null) col.setNext(nextLinkOrNull); // -> @odata.nextLink
        return col;
    }
}
