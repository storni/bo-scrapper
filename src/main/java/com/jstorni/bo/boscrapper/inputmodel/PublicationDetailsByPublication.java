package com.jstorni.bo.boscrapper.inputmodel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PublicationDetailsByPublication {
    @JsonProperty("dataList")
    private PublicationDetails details;

    public PublicationDetails getDetails() {
        return details;
    }

    public void setDetails(PublicationDetails details) {
        this.details = details;
    }
}
