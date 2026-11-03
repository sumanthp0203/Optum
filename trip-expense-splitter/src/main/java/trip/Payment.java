package trip;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * A direct transfer of money from one person straight to another — settling a
 * debt, or lending cash — with no splitting involved.
 */
public class Payment extends Transaction {
    private final String from;
    private final String to;
    private final BigDecimal amount;

    public Payment(String from, String to, BigDecimal amount, String description) {
        super(description);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (from.equals(to)) {
            throw new IllegalArgumentException("from and to must be different people");
        }
        this.from = from;
        this.to = to;
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public Payment(String from, String to, double amount, String description) {
        this(from, to, BigDecimal.valueOf(amount), description);
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    @Override
    void applyTo(Map<String, BigDecimal> balances) {
        // `from` handed over cash, improving their net position by `amount`;
        // `to` received it, so their net position drops by the same amount.
        balances.merge(from, amount, BigDecimal::add);
        balances.merge(to, amount, BigDecimal::subtract);
    }

    @Override
    List<String> involvedPeople() {
        return List.of(from, to);
    }
}
