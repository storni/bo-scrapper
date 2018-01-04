package com.jstorni.bo.boscrapper.repositories;

import com.jstorni.bo.boscrapper.model.Person;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.repository.CrudRepository;

public interface PersonRepository extends ReactiveMongoRepository<Person, String> {
}
