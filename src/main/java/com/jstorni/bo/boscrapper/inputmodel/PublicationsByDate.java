package com.jstorni.bo.boscrapper.inputmodel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PublicationsByDate {
    @JsonProperty("dataList")
    private List<Object> publications;

    public List<Object> getPublications() {
        return publications;
    }

    public void setPublications(List<Object> publications) {
        this.publications = publications;
    }
}
