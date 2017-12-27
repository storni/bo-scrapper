package com.jstorni.bo.boscrapper.inputmodel;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.List;

public class PublicationSummaries {
    private PublicationSummary[] publications;

    @JsonCreator
    public PublicationSummaries(PublicationSummary[] publications) {
        this.publications = publications;
    }

    public PublicationSummary[] getPublications() {
        return publications;
    }

    public void setPublications(PublicationSummary[] publications) {
        this.publications = publications;
    }

}
