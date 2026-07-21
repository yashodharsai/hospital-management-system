package pep.com.pepclass;

import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ReportController {

    private final MedicalReportRepository medicalReportRepository;
    private final PatientRepository patientRepository;

    public ReportController(MedicalReportRepository medicalReportRepository, PatientRepository patientRepository) {
        this.medicalReportRepository = medicalReportRepository;
        this.patientRepository = patientRepository;
    }

    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN')")
    public String reports(Model model) {
        model.addAttribute("reports", medicalReportRepository.findAll());
        return "reports";
    }

    @GetMapping("/reports/new")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN')")
    public String newReport(Model model) {
        model.addAttribute("report", new MedicalReport());
        model.addAttribute("patients", patientRepository.findAll());
        return "report-form";
    }

    @GetMapping("/reports/{id}/edit")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN')")
    public String editReport(@PathVariable String id, Model model) {
        model.addAttribute("report", medicalReportRepository.findById(id).orElseThrow());
        model.addAttribute("patients", patientRepository.findAll());
        return "report-form";
    }

    @PostMapping("/reports")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN')")
    public String saveReport(
            @Valid @ModelAttribute("report") MedicalReport report,
            BindingResult result,
            Model model,
            Authentication authentication) {
        if (result.hasErrors()) {
            model.addAttribute("patients", patientRepository.findAll());
            return "report-form";
        }
        if (report.getUploadedBy() == null || report.getUploadedBy().isBlank()) {
            report.setUploadedBy(authentication.getName());
        }
        if (report.getFileName() == null || report.getFileName().isBlank()) {
            report.setFileName(report.getTitle().replaceAll("[^A-Za-z0-9_-]+", "_") + ".txt");
        }
        medicalReportRepository.save(report);
        return "redirect:/reports";
    }

    @GetMapping("/reports/{id}/delete")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN')")
    public String deleteReport(@PathVariable String id) {
        medicalReportRepository.deleteById(id);
        return "redirect:/reports";
    }

    @GetMapping("/reports/{id}/download")
    @PreAuthorize("hasAnyRole('DOCTOR', 'CHAIRMAN') or hasRole('PATIENT')")
    public ResponseEntity<byte[]> downloadReport(@PathVariable String id, Authentication authentication) {
        MedicalReport report = medicalReportRepository.findById(id).orElseThrow();
        boolean patientOwnsReport = report.getPatient().getPatientUsername() != null
                && report.getPatient().getPatientUsername().equals(authentication.getName());
        boolean staff = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_DOCTOR")
                        || authority.getAuthority().equals("ROLE_CHAIRMAN"));
        if (!staff && !patientOwnsReport) {
            return ResponseEntity.status(403).build();
        }
        String content = report.getReportContent() == null ? "" : report.getReportContent();
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        String fileName = report.getFileName() == null ? "report.txt" : report.getFileName();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .contentType(MediaType.TEXT_PLAIN)
                .body(bytes);
    }

    @GetMapping("/patient-reports")
    @PreAuthorize("hasRole('PATIENT')")
    public String patientReports(Model model, Authentication authentication) {
        model.addAttribute("reports", medicalReportRepository.findByPatientPatientUsername(authentication.getName()));
        return "patient-reports";
    }
}
