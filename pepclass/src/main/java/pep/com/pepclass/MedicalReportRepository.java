package pep.com.pepclass;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MedicalReportRepository extends MongoRepository<MedicalReport, String> {
    List<MedicalReport> findByPatientPatientUsername(String patientUsername);
}
