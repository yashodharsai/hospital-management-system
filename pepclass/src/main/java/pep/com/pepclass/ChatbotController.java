package pep.com.pepclass;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ChatbotController {

    private final PrescriptionRepository prescriptionRepository;
    private final MedicalReportRepository medicalReportRepository;
    private final PatientRepository patientRepository;

    public ChatbotController(
            PrescriptionRepository prescriptionRepository,
            MedicalReportRepository medicalReportRepository,
            PatientRepository patientRepository) {
        this.prescriptionRepository = prescriptionRepository;
        this.medicalReportRepository = medicalReportRepository;
        this.patientRepository = patientRepository;
    }

    @GetMapping("/chatbot")
    @PreAuthorize("hasRole('PATIENT')")
    public String chatbotPage(Model model, Authentication authentication) {
        model.addAttribute("patientUsername", authentication.getName());
        List<Prescription> prescriptions = prescriptionRepository.findByPatientPatientUsername(authentication.getName());
        List<MedicalReport> reports = medicalReportRepository.findByPatientPatientUsername(authentication.getName());
        model.addAttribute("prescriptions", prescriptions);
        model.addAttribute("reports", reports);
        model.addAttribute("chatbotResponse", new ChatbotResponse("How can I help you today?", List.of(), ""));
        return "chatbot";
    }

    @PostMapping("/chatbot")
    @PreAuthorize("hasRole('PATIENT')")
    public String handleChatbot(
            @RequestParam String question,
            Model model,
            Authentication authentication) {
        List<Prescription> prescriptions = prescriptionRepository.findByPatientPatientUsername(authentication.getName());
        List<MedicalReport> reports = medicalReportRepository.findByPatientPatientUsername(authentication.getName());

        String response = buildAssistantReply(question, prescriptions, reports);
        List<String> reminders = buildReminderList(prescriptions);
        String summary = buildReportSummary(reports);

        model.addAttribute("patientUsername", authentication.getName());
        model.addAttribute("prescriptions", prescriptions);
        model.addAttribute("reports", reports);
        model.addAttribute("chatbotResponse", new ChatbotResponse(response, reminders, summary));
        return "chatbot";
    }

    private String buildAssistantReply(String question, List<Prescription> prescriptions, List<MedicalReport> reports) {
        String normalized = question.toLowerCase(Locale.ROOT);

        if (normalized.contains("dose") || normalized.contains("dosage") || normalized.contains("medicine") || normalized.contains("prescription")) {
            if (prescriptions.isEmpty()) {
                return "I could not find a prescription for your account. Please contact your doctor or the hospital care team for the latest instructions.";
            }
            return buildPrescriptionExplanation(prescriptions);
        }

        if (normalized.contains("report") || normalized.contains("summary") || normalized.contains("medical")) {
            return buildReportSummary(reports).isBlank()
                    ? "I do not have any medical reports linked to your account yet. Please check with your care team for the latest updates."
                    : "Here is a simple summary of your recent medical reports: " + buildReportSummary(reports)
                        + " If you notice any unusual symptoms or changes in your condition, please consult your doctor promptly.";
        }

        if (normalized.contains("remind") || normalized.contains("schedule") || normalized.contains("when")) {
            return buildReminderSummary(prescriptions);
        }

        if (normalized.contains("miss") || normalized.contains("forgot")) {
            return "Please take the missed dose as soon as you remember, unless it is almost time for your next scheduled dose. If you are unsure, consult your doctor or pharmacist for guidance.";
        }

        return "I can help you review your prescriptions, explain medication timing, summarize medical reports, or remind you about upcoming doses. Please ask about your medicines, schedules, or reports.";
    }

    private String buildPrescriptionExplanation(List<Prescription> prescriptions) {
        StringBuilder builder = new StringBuilder();
        for (Prescription prescription : prescriptions) {
            builder.append("Your prescription for ")
                    .append(prescription.getMedicineName())
                    .append(" is ")
                    .append(prescription.getDosage())
                    .append(". Take it ")
                    .append(formatTiming(prescription))
                    .append(". ");
        }
        builder.append("Please consult your doctor if you notice any side effects or if your condition changes.");
        return builder.toString();
    }

    private String buildReminderSummary(List<Prescription> prescriptions) {
        if (prescriptions.isEmpty()) {
            return "No medication reminders are available yet for your account.";
        }
        StringBuilder builder = new StringBuilder();
        for (Prescription prescription : prescriptions) {
            builder.append("Reminder: ")
                    .append(prescription.getMedicineName())
                    .append(" — ")
                    .append(prescription.getDosage())
                    .append(" — ")
                    .append(formatTiming(prescription))
                    .append(". ");
        }
        return builder.toString();
    }

    private List<String> buildReminderList(List<Prescription> prescriptions) {
        List<String> reminders = new ArrayList<>();
        for (Prescription prescription : prescriptions) {
            reminders.add("Take " + prescription.getMedicineName() + " — " + prescription.getDosage() + " — " + formatTiming(prescription));
        }
        return reminders;
    }

    private String buildReportSummary(List<MedicalReport> reports) {
        if (reports.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (MedicalReport report : reports) {
            builder.append(report.getTitle())
                    .append(": ")
                    .append(report.getNotes() == null || report.getNotes().isBlank() ? "No notes available." : report.getNotes())
                    .append(" ");
        }
        return builder.toString();
    }

    private String formatTiming(Prescription prescription) {
        StringBuilder builder = new StringBuilder();
        boolean includesMorning = prescription.getMorning() != null && !prescription.getMorning().isBlank();
        boolean includesAfternoon = prescription.getAfternoon() != null && !prescription.getAfternoon().isBlank();
        boolean includesEvening = prescription.getEvening() != null && !prescription.getEvening().isBlank();
        boolean includesNight = prescription.getNight() != null && !prescription.getNight().isBlank();

        List<String> times = new ArrayList<>();
        if (includesMorning) times.add("morning");
        if (includesAfternoon) times.add("afternoon");
        if (includesEvening) times.add("evening");
        if (includesNight) times.add("night");

        if (!times.isEmpty()) {
            builder.append("take at ")
                    .append(String.join(", ", times))
                    .append(" and ")
                    .append(prescription.getFoodTiming() == null || prescription.getFoodTiming().isBlank() ? "follow the doctor’s guidance on food timing" : prescription.getFoodTiming());
        } else {
            builder.append("follow the medication schedule provided by your doctor");
        }
        return builder.toString();
    }
}
