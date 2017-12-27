package com.jstorni.bo.boscrapper.model;

import org.springframework.data.annotation.Id;

public abstract class BaseEntity {
    @Id
    private long id;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
