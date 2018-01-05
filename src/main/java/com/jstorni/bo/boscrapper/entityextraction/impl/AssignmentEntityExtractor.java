package com.jstorni.bo.boscrapper.entityextraction.impl;

import com.jstorni.bo.boscrapper.entityextraction.EntityExtractor;
import com.jstorni.bo.boscrapper.filestorage.FileStorageService;
import com.jstorni.bo.boscrapper.model.Assignment;
import com.jstorni.bo.boscrapper.model.PublicationEntry;
import com.jstorni.bo.boscrapper.repositories.AssignmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class AssignmentEntityExtractor implements EntityExtractor {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AssignmentRepository assignmentRepository;
    private final FileStorageService fileStorageService;

    public AssignmentEntityExtractor(AssignmentRepository assignmentRepository, FileStorageService fileStorageService) {
        this.assignmentRepository = assignmentRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public Flux<Object> extract(PublicationEntry publicationEntry) {
        return null;
    }
}
