package pep.com.pepclass;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MedicineRepository extends MongoRepository<Medicine, String> {
    List<Medicine> findByPatientPatientUsername(String patientUsername);
}
