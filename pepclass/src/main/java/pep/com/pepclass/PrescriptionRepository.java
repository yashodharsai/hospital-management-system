package pep.com.pepclass;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PrescriptionRepository extends MongoRepository<Prescription, String> {
    List<Prescription> findByPatientPatientUsername(String patientUsername);
}
