package com.jstorni.bo.boscrapper.entityextraction;

import com.jstorni.bo.boscrapper.model.BaseEntity;
import com.jstorni.bo.boscrapper.model.PublicationEntry;
import reactor.core.publisher.Flux;

public interface EntityExtractor {

    Flux<Object> extract(PublicationEntry publicationEntry);

}
