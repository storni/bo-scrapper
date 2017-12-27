package com.jstorni.bo.boscrapper.model;

import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * A person identified in an official bulletin publication entry
 *
 * @author javier
 */
@Document
public class Person extends BaseEntity {

    @NotNull
    @Size(max = 128)
    private String fullName;

    @Size(max = 16)
    private String identifier;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}
