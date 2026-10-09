package br.com.cnab.hub.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OutboxEventRepository extends MongoRepository<OutboxEventDocument, String> {

    List<OutboxEventDocument> findTop50ByPublishedAtIsNullOrderByCreatedAtAsc();
}
