package pep.com.pepclass;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PasswordResetOtpRepository extends MongoRepository<PasswordResetOtp, String> {
    Optional<PasswordResetOtp> findTopByEmailAndUsedFalseOrderByCreatedAtDesc(String email);
    Optional<PasswordResetOtp> findByEmailAndResetTokenAndUsedFalse(String email, String resetToken);
    List<PasswordResetOtp> findByEmail(String email);
}
