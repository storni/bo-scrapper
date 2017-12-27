package com.jstorni.bo.boscrapper.repositories;

import com.jstorni.bo.boscrapper.model.Category;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface CategoryRepository extends ReactiveMongoRepository<Category, String> {

    Mono<Category> findOneByNameEquals(String categoryName);

}
