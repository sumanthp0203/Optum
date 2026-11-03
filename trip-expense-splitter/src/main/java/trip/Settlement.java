package trip;

import java.math.BigDecimal;

/** One step of a minimal settle-up plan: {@code payer} should give {@code amount} to {@code receiver}. */
public record Settlement(String payer, String receiver, BigDecimal amount) {
    @Override
    public String toString() {
        return payer + " pays " + receiver + " $" + amount;
    }
}
