package com.jstorni.bo.boscrapper.repositories;


import com.jstorni.bo.boscrapper.model.PublicationEntry;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.repository.CrudRepository;
import reactor.core.publisher.Mono;

public interface PublicationEntryRepository extends ReactiveMongoRepository<PublicationEntry, String> {

    Mono<PublicationEntry> findOneByIdentifierEquals(int identifier);
}
