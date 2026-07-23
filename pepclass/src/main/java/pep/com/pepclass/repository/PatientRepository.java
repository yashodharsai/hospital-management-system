package pep.com.pepclass.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pep.com.pepclass.model.Patient;

@Repository
public interface PatientRepository extends MongoRepository<Patient, String> {
    Optional<Patient> findByPatientUsername(String patientUsername);
}
