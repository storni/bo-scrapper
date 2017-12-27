package com.jstorni.bo.boscrapper.model;

import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Date;

/**
 * Assignment (a.k.a.: position) of a person in a sector defined in a publication entry
 *
 * @author javier
 */
@Document
public class Assignment extends BaseEntity {
    @NotNull
    @Size(max = 64)
    private String name;

    @DBRef
    private Person person;

    @DBRef
    private Sector sector;

    @DBRef
    private PublicationEntry publicationEntry;

    @NotNull
    private Date dtFrom;

    private Date dtTo;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public Sector getSector() {
        return sector;
    }

    public void setSector(Sector sector) {
        this.sector = sector;
    }

    public Date getDtFrom() {
        return dtFrom;
    }

    public void setDtFrom(Date dtFrom) {
        this.dtFrom = dtFrom;
    }

    public Date getDtTo() {
        return dtTo;
    }

    public void setDtTo(Date dtTo) {
        this.dtTo = dtTo;
    }

    public PublicationEntry getPublicationEntry() {
        return publicationEntry;
    }

    public void setPublicationEntry(PublicationEntry publicationEntry) {
        this.publicationEntry = publicationEntry;
    }
}
