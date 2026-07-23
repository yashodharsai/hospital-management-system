package pep.com.pepclass.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pep.com.pepclass.model.BmiRecord;

@Repository
public interface BmiRecordRepository extends MongoRepository<BmiRecord, String> {
    List<BmiRecord> findByUsernameOrderByCreatedAtDesc(String username);
}
