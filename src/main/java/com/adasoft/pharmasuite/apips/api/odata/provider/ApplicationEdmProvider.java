package com.adasoft.pharmasuite.apips.api.odata.provider;

import com.adasoft.pharmasuite.apips.api.batch.service.impl.BatchEdmProvider;
import com.adasoft.pharmasuite.apips.api.batch.service.impl.TransactionHistoryEdmProvider;
import com.adasoft.pharmasuite.apips.api.common.service.impl.CommonEdmProvider;
import com.adasoft.pharmasuite.apips.api.order.service.impl.ProcessOrderEdmProvider;
import com.adasoft.pharmasuite.apips.api.recipe.service.impl.RecipeEdmProvider;
import com.adasoft.pharmasuite.apips.api.workflow.service.impl.WorkflowEdmProvider;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.apache.olingo.commons.api.edm.provider.CsdlComplexType;
import org.apache.olingo.commons.api.edm.provider.CsdlEdmProvider;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.commons.api.ex.ODataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class ApplicationEdmProvider extends CsdlAbstractEdmProvider {
    private static final Logger log = LoggerFactory.getLogger(ApplicationEdmProvider.class);

    private final List<CsdlEdmProvider> providers;

    public ApplicationEdmProvider() {
        this.providers = List.of(
                new CommonEdmProvider(),
                new RecipeEdmProvider(),
                new BatchEdmProvider(),
                new WorkflowEdmProvider(),
                new ProcessOrderEdmProvider(),
                new TransactionHistoryEdmProvider()
        );
    }

    @Override
    public List<CsdlSchema> getSchemas() {
        // LinkedHashMap para orden determinista (primero Recipe, luego Batch, etc.)
        Map<String, CsdlSchema> schemaMap = new LinkedHashMap<>();

        // owners: key = namespace + "::" + name  / value = provider simple name
        Map<String, String> entityTypeOwner = new HashMap<>();
        Map<String, String> complexTypeOwner = new HashMap<>();
        Map<String, String> entitySetOwner = new HashMap<>();
        Map<String, String> containerOwnerByNs = new HashMap<>();

        for (CsdlEdmProvider p : providers) {
            String provider = p.getClass().getSimpleName();

            List<CsdlSchema> schemas;
            try {
                schemas = p.getSchemas();
            } catch (ODataException e) {
                throw new IllegalStateException("Error reading schemas from provider: " + provider, e);
            }
            if (schemas == null) schemas = Collections.emptyList();

            log.info("OData: loading {} schema(s) from {}", schemas.size(), provider);

            for (CsdlSchema incoming : schemas) {
                if (incoming == null) continue;

                String ns = incoming.getNamespace();
                if (ns == null || ns.isBlank()) {
                    throw new IllegalStateException("Provider " + provider + " returned a schema without namespace");
                }

                CsdlSchema merged = schemaMap.get(ns);
                if (merged == null) {
                    merged = new CsdlSchema();
                    merged.setNamespace(ns);
                    merged.setAlias(incoming.getAlias());
                    schemaMap.put(ns, merged);
                } else {
                    // si alias difiere, es colisión (misma namespace con alias distinto)
                    if (incoming.getAlias() != null && merged.getAlias() != null
                            && !Objects.equals(incoming.getAlias(), merged.getAlias())) {
                        throw new IllegalStateException("OData EDM collision (Schema alias): namespace='" + ns
                                + "' alias='" + merged.getAlias() + "' vs '" + incoming.getAlias()
                                + "' (provider " + provider + ")");
                    }
                    if (merged.getAlias() == null) merged.setAlias(incoming.getAlias());
                }

                merged.setEntityTypes(mergeEntityTypes(
                        merged.getEntityTypes(), incoming.getEntityTypes(), ns, entityTypeOwner, provider));

                merged.setComplexTypes(mergeComplexTypes(
                        merged.getComplexTypes(), incoming.getComplexTypes(), ns, complexTypeOwner, provider));

                if (incoming.getEntityContainer() != null) {
                    merged.setEntityContainer(mergeEntityContainers(
                            merged.getEntityContainer(), incoming.getEntityContainer(),
                            ns, containerOwnerByNs, entitySetOwner, provider));
                }
            }
        }

        // resumen
        for (CsdlSchema s : schemaMap.values()) {
            int ets = s.getEntityTypes() == null ? 0 : s.getEntityTypes().size();
            int cts = s.getComplexTypes() == null ? 0 : s.getComplexTypes().size();
            int ess = (s.getEntityContainer() == null || s.getEntityContainer().getEntitySets() == null)
                    ? 0 : s.getEntityContainer().getEntitySets().size();
            log.info("OData: unified namespace='{}' -> entityTypes={}, complexTypes={}, entitySets={}", s.getNamespace(), ets, cts, ess);
        }

        return new ArrayList<>(schemaMap.values());
    }


    @Override
    public CsdlEntityContainer getEntityContainer() {
        List<CsdlSchema> schemas = getSchemas();
        for (CsdlSchema s : schemas) {
            if (s.getEntityContainer() != null) return s.getEntityContainer();
        }
        return null;
    }

    @Override
    public CsdlEntityType getEntityType(FullQualifiedName fqn) {
        for (CsdlEdmProvider p : providers) {
            try {
                CsdlEntityType et = p.getEntityType(fqn);
                if (et != null) return et;
            } catch (ODataException e) {
                throw new IllegalStateException("Error reading entity type from provider: " + p.getClass().getSimpleName(), e);
            }
        }
        return null;
    }

    @Override
    public CsdlComplexType getComplexType(FullQualifiedName fqn) {
        for (CsdlEdmProvider p : providers) {
            try {
                CsdlComplexType ct = p.getComplexType(fqn);
                if (ct != null) return ct;
            } catch (ODataException e) {
                throw new IllegalStateException("Error reading complex type from provider: " + p.getClass().getSimpleName(), e);
            }
        }
        return null;
    }

    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName container, String entitySetName) {
        for (CsdlEdmProvider p : providers) {
            try {
                CsdlEntitySet es = p.getEntitySet(container, entitySetName);
                if (es != null) return es;
            } catch (ODataException e) {
                throw new IllegalStateException("Error reading entity set from provider: " + p.getClass().getSimpleName(), e);
            }
        }
        return null;
    }

    @Override
    public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName name) {
        for (CsdlEdmProvider p : providers) {
            try {
                CsdlEntityContainerInfo info = p.getEntityContainerInfo(name);
                if (info != null) return info;
            } catch (ODataException e) {
                throw new IllegalStateException("Error reading entity container info from provider: " + p.getClass().getSimpleName(), e);
            }
        }
        return null;
    }

    private CsdlSchema cloneSchema(CsdlSchema s) {
        CsdlSchema copy = new CsdlSchema();
        copy.setNamespace(s.getNamespace());
        copy.setEntityTypes(s.getEntityTypes());
        copy.setComplexTypes(s.getComplexTypes());
        copy.setEntityContainer(s.getEntityContainer());
        return copy;
    }

    private static String k(String ns, String name) {
        return ns + "::" + name;
    }

    private static void failDuplicate(String kind, String ns, String name, String firstProvider, String secondProvider) {
        if (Objects.equals(firstProvider, secondProvider)) {
            throw new IllegalStateException("OData EDM collision (" + kind + "): namespace='" + ns + "', name='"
                    + name + "' duplicated inside provider " + firstProvider);
        }
        throw new IllegalStateException("OData EDM collision (" + kind + "): namespace='" + ns + "', name='"
                + name + "' defined in both " + firstProvider + " and " + secondProvider);
    }

    private List<CsdlEntityType> mergeEntityTypes(
            List<CsdlEntityType> current,
            List<CsdlEntityType> incoming,
            String ns,
            Map<String, String> owner,
            String provider) {

        List<CsdlEntityType> merged = current == null ? new ArrayList<>() : new ArrayList<>(current);
        if (incoming == null) return merged;

        for (CsdlEntityType et : incoming) {
            if (et == null) continue;
            String name = et.getName();
            if (name == null || name.isBlank()) continue;

            String key = k(ns, name);
            String prev = owner.putIfAbsent(key, provider);
            if (prev != null) failDuplicate("EntityType", ns, name, prev, provider);

            merged.add(et);
        }
        return merged;
    }

    private List<CsdlComplexType> mergeComplexTypes(
            List<CsdlComplexType> current,
            List<CsdlComplexType> incoming,
            String ns,
            Map<String, String> owner,
            String provider) {

        List<CsdlComplexType> merged = current == null ? new ArrayList<>() : new ArrayList<>(current);
        if (incoming == null) return merged;

        for (CsdlComplexType ct : incoming) {
            if (ct == null) continue;
            String name = ct.getName();
            if (name == null || name.isBlank()) continue;

            String key = k(ns, name);
            String prev = owner.putIfAbsent(key, provider);
            if (prev != null) failDuplicate("ComplexType", ns, name, prev, provider);

            merged.add(ct);
        }
        return merged;
    }

    private CsdlEntityContainer mergeEntityContainers(
            CsdlEntityContainer current,
            CsdlEntityContainer incoming,
            String ns,
            Map<String, String> containerOwnerByNs,
            Map<String, String> entitySetOwner,
            String provider) {

        if (incoming == null) return current;
        if (current == null) {
            // primera vez que aparece container en este namespace
            containerOwnerByNs.putIfAbsent(ns, provider);

            CsdlEntityContainer copy = new CsdlEntityContainer();
            copy.setName(incoming.getName());

            List<CsdlEntitySet> sets = incoming.getEntitySets() == null ? new ArrayList<>() : new ArrayList<>(incoming.getEntitySets());
            // indexar owners + detectar duplicados internos
            for (CsdlEntitySet es : sets) {
                if (es == null) continue;
                String esName = es.getName();
                if (esName == null || esName.isBlank()) continue;

                String key = k(ns, esName);
                String prev = entitySetOwner.putIfAbsent(key, provider);
                if (prev != null) failDuplicate("EntitySet", ns, esName, prev, provider);
            }

            copy.setEntitySets(sets);
            return copy;
        }

        String ownerProvider = containerOwnerByNs.getOrDefault(ns, "unknown");
        if (!Objects.equals(current.getName(), incoming.getName())) {
            throw new IllegalStateException("OData EDM collision (EntityContainer): namespace='" + ns
                    + "' container name differs: '" + current.getName() + "' from " + ownerProvider
                    + " vs '" + incoming.getName() + "' from " + provider);
        }

        List<CsdlEntitySet> mergedSets = current.getEntitySets() == null ? new ArrayList<>() : new ArrayList<>(current.getEntitySets());
        if (incoming.getEntitySets() != null) {
            for (CsdlEntitySet es : incoming.getEntitySets()) {
                if (es == null) continue;
                String esName = es.getName();
                if (esName == null || esName.isBlank()) continue;

                String key = k(ns, esName);
                String prev = entitySetOwner.putIfAbsent(key, provider);
                if (prev != null) failDuplicate("EntitySet", ns, esName, prev, provider);

                mergedSets.add(es);
            }
        }
        current.setEntitySets(mergedSets);

        return current;
    }
}

