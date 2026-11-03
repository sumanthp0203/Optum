package trip;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A shared cost paid upfront by one person on behalf of a group.
 * Example: Person1 pays $100 for 4 train tickets, shared equally by all 4.
 */
public class Expense extends Transaction {
    private final String paidBy;
    private final BigDecimal amount;
    private final List<String> participants;

    public Expense(String paidBy, BigDecimal amount, List<String> participants, String description) {
        super(description);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        if (participants == null || participants.isEmpty()) {
            throw new IllegalArgumentException("participants cannot be empty");
        }
        Set<String> uniqueParticipants = new HashSet<>(participants);
        if (uniqueParticipants.size() != participants.size()) {
            throw new IllegalArgumentException("participants cannot contain duplicate names");
        }
        if (!participants.contains(paidBy)) {
            throw new IllegalArgumentException("payer must be among participants");
        }
        this.paidBy = paidBy;
        this.amount = round(amount);
        this.participants = List.copyOf(participants);
    }

    public Expense(String paidBy, double amount, List<String> participants, String description) {
        this(paidBy, BigDecimal.valueOf(amount), participants, description);
    }

    public String getPaidBy() {
        return paidBy;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public List<String> getParticipants() {
        return participants;
    }

    /** Split amount equally among participants, fixing rounding drift on the payer's share. */
    public Map<String, BigDecimal> shares() {
        int n = participants.size();
        // Truncate (round DOWN) the per-person share instead of HALF_UP here,
        // because rounding every share up/down independently can make the
        // shares sum to more or less than the original amount (e.g. $100 / 3
        // would become $33.34 x 3 = $100.02 with HALF_UP). Truncating always
        // gives a sum <= amount, and the leftover cents below are added back
        // to the payer's own share so the shares always sum to exactly `amount`.
        BigDecimal base = amount.divide(BigDecimal.valueOf(n), 2, RoundingMode.DOWN);

        Map<String, BigDecimal> shares = new LinkedHashMap<>();
        for (String person : participants) {
            shares.put(person, base);
        }

        // drift = whatever got left over from truncation (0 to a few cents).
        BigDecimal drift = amount.subtract(base.multiply(BigDecimal.valueOf(n)));
        shares.merge(paidBy, drift, BigDecimal::add);
        return shares;
    }

    @Override
    void applyTo(Map<String, BigDecimal> balances) {
        // The payer fronted the full amount, so they're credited the whole
        // thing first, then everyone (including the payer) is debited their
        // equal share - leaving the payer net "amount - their own share".
        balances.merge(paidBy, amount, BigDecimal::add);
        for (Map.Entry<String, BigDecimal> entry : shares().entrySet()) {
            balances.merge(entry.getKey(), entry.getValue(), BigDecimal::subtract);
        }
    }

    @Override
    List<String> involvedPeople() {
        List<String> all = new ArrayList<>(participants);
        if (!all.contains(paidBy)) {
            all.add(paidBy);
        }
        return all;
    }

    static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
