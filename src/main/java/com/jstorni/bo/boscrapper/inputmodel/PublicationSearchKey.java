package com.jstorni.bo.boscrapper.inputmodel;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PublicationSearchKey {
    @JsonProperty("fecha")
    private String date;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
