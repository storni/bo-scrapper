package com.jstorni.bo.boscrapper.model;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * An instance of the official bulletin, usually one per day
 *
 * @author javier
 */
@Document
public class Publication extends BaseEntity {

    @NotNull
    @Indexed(unique = true)
    private Date appearsOn;

    @NotNull
    private Date importedOn;

    private boolean importCompleted;

    public Publication(@NotNull Date appearsOn) {
        this.appearsOn = appearsOn;
    }

    public Publication() {

    }

    public Date getAppearsOn() {
        return appearsOn;
    }

    public void setAppearsOn(Date appearsOn) {
        this.appearsOn = appearsOn;
    }

    public Date getImportedOn() {
        return importedOn;
    }

    public void setImportedOn(Date importedOn) {
        this.importedOn = importedOn;
    }

    public boolean isImportCompleted() {
        return importCompleted;
    }

    public void setImportCompleted(boolean importCompleted) {
        this.importCompleted = importCompleted;
    }
}
