package com.jstorni.bo.boscrapper.repositories;

import com.jstorni.bo.boscrapper.model.Publication;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.repository.CrudRepository;
import reactor.core.publisher.Mono;

import java.util.Date;

public interface PublicationRepository extends ReactiveMongoRepository<Publication, String> {

    Mono<Publication> findOneByAppearsOnEquals(Date appearsOn);
}
