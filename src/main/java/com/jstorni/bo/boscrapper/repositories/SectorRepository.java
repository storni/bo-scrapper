package com.jstorni.bo.boscrapper.repositories;

import com.jstorni.bo.boscrapper.model.Sector;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface SectorRepository extends ReactiveMongoRepository<Sector, String> {

    Mono<Sector> findOneByNameEquals(String sectorName);
}
