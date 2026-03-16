package rw.ticketing.util;

import org.springframework.stereotype.Component;
import java.security.SecureRandom;

@Component
public class TicketCodeUtil {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public String generate() {
        // Format: RWT-XXXX-XXXX-XXXX  (Rwanda Ticket)
        return "RWT-" + segment() + "-" + segment() + "-" + segment();
    }

    private String segment() {
        StringBuilder sb = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
