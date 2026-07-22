package pep.com.pepclass;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface BmiRecordRepository extends MongoRepository<BmiRecord, String> {
    List<BmiRecord> findByUsernameOrderByCreatedAtDesc(String username);
}
