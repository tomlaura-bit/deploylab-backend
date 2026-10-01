package edu.deploylab;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

@Service
public class EmailTemplateService {
    private final String template;

    public EmailTemplateService() {
        try {
            template = new ClassPathResource("templates/notification-email.html")
                .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo cargar la plantilla de correo", exception);
        }
    }

    public String notification(String title, String message) {
        return template
            .replace("{{title}}", HtmlUtils.htmlEscape(title))
            .replace("{{message}}", HtmlUtils.htmlEscape(message));
    }
}
