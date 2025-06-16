package com.devsuperior.dscatalog.projections;

public interface ProductProjection extends IdProjection<Long> {

    Long getId();
    String getName();
}
