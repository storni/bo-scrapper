package com.jstorni.bo.boscrapper.model;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Category (a.k.a: section) of a official bulletin publication
 *
 * @author javier
 */
@Document
public class Category extends BaseEntity {

    @Indexed(unique = true)
    @NotNull
    @Size(max = 64)
    private String name;

    public Category() {

    }

    public Category(@NotNull @Size(max = 64) String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
