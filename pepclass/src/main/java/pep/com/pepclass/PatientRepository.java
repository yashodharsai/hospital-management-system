package pep.com.pepclass;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PatientRepository extends MongoRepository<Patient, String> {
    Optional<Patient> findByPatientUsername(String patientUsername);
}
