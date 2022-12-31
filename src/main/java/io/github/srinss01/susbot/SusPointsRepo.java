package io.github.srinss01.susbot;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SusPointsRepo extends MongoRepository<SusPoints, SusPoints.ID> {
}
