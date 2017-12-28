package com.jstorni.bo.boscrapper.inputmodel;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Publication details (official bulletin input model)
 *
 * @author javier
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PublicationDetails {

    @JsonProperty("fechaEfectiva")
    private String effectiveDate;

    @JsonProperty("numeroNorma")
    private String title;

    @JsonProperty("detalleNorma")
    private String content;

    @JsonProperty("idTramite")
    private String identifier;

    @JsonProperty("archivoPDF")
    private String pdfIdentifier;

    @JsonIgnore
    private PublicationSummary summary;

    public String getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(String effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getPdfIdentifier() {
        return pdfIdentifier;
    }

    public void setPdfIdentifier(String pdfIdentifier) {
        this.pdfIdentifier = pdfIdentifier;
    }

    public PublicationSummary getSummary() {
        return summary;
    }

    public void setSummary(PublicationSummary summary) {
        this.summary = summary;
    }
}
