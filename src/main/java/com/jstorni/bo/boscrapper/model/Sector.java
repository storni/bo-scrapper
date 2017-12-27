package com.jstorni.bo.boscrapper.model;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Sector (a.k.a.: department, section, organism) referenced in a official bulletin publication entry
 *
 * @author javier
 */
@Document
public class Sector extends BaseEntity{

    @Indexed(unique = true)
    @NotNull
    @Size(max = 128)
    private String name;

    @DBRef
    private Sector parentSector;

    public Sector(@NotNull @Size(max = 128) String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Sector getParentSector() {
        return parentSector;
    }

    public void setParentSector(Sector parentSector) {
        this.parentSector = parentSector;
    }
}
