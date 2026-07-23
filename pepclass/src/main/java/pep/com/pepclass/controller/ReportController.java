package pep.com.pepclass.controller;

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

import pep.com.pepclass.model.MedicalReport;
import pep.com.pepclass.repository.MedicalReportRepository;
import pep.com.pepclass.repository.PatientRepository;

@Controller
public class ReportController {

    private final MedicalReportRepository medicalReportRepository;
    private final PatientRepository patientRepository;

    public ReportController(MedicalReportRepository medicalReportRepository, PatientRepository patientRepository) {
        this.medicalReportRepository = medicalReportRepository;
        this.patientRepository = patientRepository;
    }

    @GetMapping("/reports")
    @PreAuthorize("hasAnyRole('CHAIRMAN', 'DOCTOR')")
    public String reports(Model model) {
        model.addAttribute("reports", medicalReportRepository.findAll());
        return "reports";
    }

    @GetMapping("/reports/new")
    @PreAuthorize("hasAnyRole('CHAIRMAN', 'DOCTOR')")
    public String newReport(Model model) {
        model.addAttribute("report", new MedicalReport());
        model.addAttribute("patients", patientRepository.findAll());
        return "report-form";
    }

    @GetMapping("/reports/{id}/edit")
    @PreAuthorize("hasAnyRole('CHAIRMAN', 'DOCTOR')")
    public String editReport(@PathVariable String id, Model model) {
        model.addAttribute("report", medicalReportRepository.findById(id).orElseThrow());
        model.addAttribute("patients", patientRepository.findAll());
        return "report-form";
    }

    @PostMapping("/reports")
    @PreAuthorize("hasAnyRole('CHAIRMAN', 'DOCTOR')")
    public String saveReport(
            @Valid @ModelAttribute("report") MedicalReport report,
            BindingResult result,
            Model model,
            Authentication authentication) {
        if (result.hasErrors()) {
            model.addAttribute("patients", patientRepository.findAll());
            return "report-form";
        }
        if (report.getPatient() != null && report.getPatient().getId() != null) {
            patientRepository.findById(report.getPatient().getId()).ifPresent(report::setPatient);
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
    @PreAuthorize("hasAnyRole('CHAIRMAN', 'DOCTOR')")
    public String deleteReport(@PathVariable String id) {
        medicalReportRepository.deleteById(id);
        return "redirect:/reports";
    }

    @GetMapping("/reports/{id}/download")
    @PreAuthorize("hasAnyRole('CHAIRMAN', 'DOCTOR', 'PATIENT')")
    public ResponseEntity<byte[]> downloadReport(@PathVariable String id, Authentication authentication) {
        MedicalReport report = medicalReportRepository.findById(id).orElseThrow();
        boolean patientOwnsReport = report.getPatient() != null && report.getPatient().getPatientUsername() != null
                && report.getPatient().getPatientUsername().equals(authentication.getName());
        boolean staff = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_CHAIRMAN") || authority.getAuthority().equals("ROLE_DOCTOR"));
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
        var userReports = medicalReportRepository.findByPatientPatientUsername(authentication.getName());
        model.addAttribute("reports", userReports);

        MedicalReport dischargeReport = userReports.stream()
                .filter(r -> r.getDischargeDate() != null && !r.getDischargeDate().isBlank())
                .findFirst()
                .orElse(null);
        model.addAttribute("latestDischargeReport", dischargeReport);

        return "patient-reports";
    }
}
