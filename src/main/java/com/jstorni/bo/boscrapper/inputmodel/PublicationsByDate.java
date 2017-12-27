package com.jstorni.bo.boscrapper.inputmodel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PublicationsByDate {
    @JsonProperty("dataList")
    private PublicationSummaries[] publications;

    public PublicationSummaries[] getPublications() {
        return publications;
    }

    public void setPublications(PublicationSummaries[] publications) {
        this.publications = publications;
    }
}
