package com.jstorni.bo.boscrapper.model;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * @author javier
 */
@Document
public class PublicationEntry extends BaseEntity {

    @NotNull
    @DBRef
    private Category category;

    @NotNull
    @DBRef
    private Sector sector;

    @Indexed(unique = true)
    @NotNull
    private int identifier;

    private boolean hasAttachment;

    @NotNull
    @Size(max = 128)
    private String title;

    @NotNull
    @Size(max = 256)
    private String summary;

    @Size(max = 16)
    private String pdfIdentifier;

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Sector getSector() {
        return sector;
    }

    public void setSector(Sector sector) {
        this.sector = sector;
    }

    public int getIdentifier() {
        return identifier;
    }

    public void setIdentifier(int identifier) {
        this.identifier = identifier;
    }

    public boolean isHasAttachment() {
        return hasAttachment;
    }

    public void setHasAttachment(boolean hasAttachment) {
        this.hasAttachment = hasAttachment;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getPdfIdentifier() {
        return pdfIdentifier;
    }

    public void setPdfIdentifier(String pdfIdentifier) {
        this.pdfIdentifier = pdfIdentifier;
    }
}
