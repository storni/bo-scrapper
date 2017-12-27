package com.jstorni.bo.boscrapper.inputmodel;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class PublicationSearch {
    @JsonProperty("dataList")
    private List<PublicationSearchKey> publications;

    public List<PublicationSearchKey> getPublications() {
        return publications;
    }

    public void setPublications(List<PublicationSearchKey> publications) {
        this.publications = publications;
    }
}
