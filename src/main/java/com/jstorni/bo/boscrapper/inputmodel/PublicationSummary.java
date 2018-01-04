package com.jstorni.bo.boscrapper.inputmodel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PublicationSummary {

    @JsonProperty("sintesis")
    private String publicationSummary;

    @JsonProperty("numeroNorma")
    private String identifier;

    @JsonProperty("fechaPublicacion")
    private String publicationDate;

    @JsonProperty("organismo")
    private String sectorName;

    @JsonProperty("rubro")
    private String categoryName;

    public String getPublicationSummary() {
        return publicationSummary;
    }

    public void setPublicationSummary(String publicationSummary) {
        this.publicationSummary = publicationSummary;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(String publicationDate) {
        this.publicationDate = publicationDate;
    }

    public String getSectorName() {
        return sectorName;
    }

    public void setSectorName(String sectorName) {
        this.sectorName = sectorName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }
}
