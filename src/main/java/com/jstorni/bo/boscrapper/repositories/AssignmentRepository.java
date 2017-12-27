package com.jstorni.bo.boscrapper.repositories;

import com.jstorni.bo.boscrapper.model.Assignment;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface AssignmentRepository extends ReactiveMongoRepository<Assignment, String> {
}
