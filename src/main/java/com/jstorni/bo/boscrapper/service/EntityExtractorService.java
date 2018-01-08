package com.jstorni.bo.boscrapper.service;

import com.jstorni.bo.boscrapper.entityextraction.EntityExtractor;
import com.jstorni.bo.boscrapper.model.BaseEntity;
import com.jstorni.bo.boscrapper.model.Category;
import com.jstorni.bo.boscrapper.repositories.PublicationEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.UnicastProcessor;

import java.util.List;
import java.util.Objects;

/**
 * Coordinates information extraction from <code>PublicationEntry</code> instances with <code>entitiesExtracted</code>
 * equals to <code>false</code>
 *
 * @author javier
 */
@Service
public class EntityExtractorService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final PublicationEntryRepository entryRepository;
    private final List<EntityExtractor> extractors;

    public EntityExtractorService(PublicationEntryRepository entryRepository, List<EntityExtractor> extractors) {
        this.entryRepository = entryRepository;
        this.extractors = extractors;
    }

    public Flux<Object> extract() {
        UnicastProcessor<Object> hotSource = UnicastProcessor.create();
        Flux<Object> extractedEntitiesFlux = hotSource.publish().autoConnect();

        entryRepository.findByEntitiesExtractedFalse()
            .subscribe(publicationEntry -> {
                publicationEntry.setEntitiesExtracted(true);
                entryRepository.save(publicationEntry);
                try {
                    extractors.stream()
                            .parallel()
                            .map(entityExtractor -> entityExtractor.extract(publicationEntry))
                            .filter(Objects::nonNull)
                            .forEach(flux -> flux.subscribe(hotSource::onNext));
                } catch (Exception ex) {
                    logger.error("Error while extracting entities from publication id {}", publicationEntry.getIdentifier(), ex);
                    publicationEntry.setEntitiesExtracted(false);
                    entryRepository.save(publicationEntry);
                }
            });

        hotSource.onComplete();

        return extractedEntitiesFlux;
    }
}
