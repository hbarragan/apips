package com.adasoft.pharmasuite.apips.api.common.domain.odata;

@FunctionalInterface
public interface TriConsumer<A,B,C> { void accept(A a, B b, C c) throws Exception; }
