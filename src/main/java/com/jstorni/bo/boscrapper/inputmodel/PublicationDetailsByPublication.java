package com.jstorni.bo.boscrapper.inputmodel;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class PublicationDetailsByPublication {
    @JsonProperty("dataList")
    private List<PublicationDetails> details;

    public List<PublicationDetails> getDetails() {
        return details;
    }

    public void setDetails(List<PublicationDetails> details) {
        this.details = details;
    }
}
