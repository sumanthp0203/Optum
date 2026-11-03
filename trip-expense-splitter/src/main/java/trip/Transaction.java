package trip;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * A single event on the trip ledger. There are two kinds:
 *  - {@link Expense}: one person pays for something shared by a group; each
 *    participant (including the payer) owes an equal share.
 *  - {@link Payment}: a direct transfer of money from one person to another
 *    (e.g. settling a debt, or lending cash), with no splitting involved.
 */
public abstract class Transaction {
    private final String description;

    protected Transaction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /** Mutates the running balance map to reflect this transaction. */
    abstract void applyTo(Map<String, BigDecimal> balances);

    /** Every person referenced by this transaction, used for validation. */
    abstract List<String> involvedPeople();
}
