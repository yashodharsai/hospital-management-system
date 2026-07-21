package pep.com.pepclass;

import java.util.List;

public record ChatbotResponse(String response, List<String> reminders, String summary) {
}
