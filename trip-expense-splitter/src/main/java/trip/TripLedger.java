package trip;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tracks every expense and payment made during a trip and reports, at any
 * point, who owes money, who is owed money, and the minimal set of transfers
 * needed to settle everyone up.
 *
 * Balance sign convention: positive = this person is owed money (should
 * receive), negative = this person owes money (should pay).
 */
public class TripLedger {
    private final List<String> people;
    private final Map<String, BigDecimal> balances = new LinkedHashMap<>();
    private final List<Transaction> history = new ArrayList<>();

    public TripLedger(List<String> people) {
        if (people == null || people.size() < 2) {
            throw new IllegalArgumentException("a trip needs at least 2 people");
        }
        Set<String> uniqueNames = new HashSet<>(people);
        if (uniqueNames.size() != people.size()) {
            throw new IllegalArgumentException("people list cannot contain duplicate names");
        }
        this.people = new ArrayList<>(people);
        for (String person : people) {
            balances.put(person, BigDecimal.ZERO.setScale(2));
        }
    }

    /** Record a shared cost paid upfront by {@code paidBy}, split equally among {@code participants}. */
    public Expense addExpense(String paidBy, double amount, List<String> participants, String description) {
        List<String> who = (participants == null || participants.isEmpty()) ? people : participants;
        Expense expense = new Expense(paidBy, amount, who, description);
        apply(expense);
        return expense;
    }

    /** Shortcut for an expense shared equally by everyone on the trip. */
    public Expense addExpense(String paidBy, double amount, String description) {
        return addExpense(paidBy, amount, null, description);
    }

    /** Record a direct transfer of money from one person to another (e.g. settling a debt). */
    public Payment addPayment(String from, String to, double amount, String description) {
        Payment payment = new Payment(from, to, amount, description);
        apply(payment);
        return payment;
    }

    private void apply(Transaction transaction) {
        // Validate before mutating anything, so a bad transaction never partially
        // updates balances and leaves the ledger in an inconsistent state.
        for (String person : transaction.involvedPeople()) {
            if (!people.contains(person)) {
                throw new IllegalArgumentException("Unknown person on this trip: " + person);
            }
        }
        transaction.applyTo(balances);
        history.add(transaction);
    }

    /** Current net balance per person. */
    public Map<String, BigDecimal> getBalances() {
        return new LinkedHashMap<>(balances);
    }

    /** Every expense and payment recorded so far, in order. */
    public List<Transaction> getHistory() {
        return List.copyOf(history);
    }

    /**
     * Computes the minimum number of transfers needed to zero out every
     * balance, using a greedy largest-debtor-pays-largest-creditor approach.
     */
    public List<Settlement> settleUp() {
        // Split people into two buckets: debtors (owe money, balance < 0) and
        // creditors (are owed money, balance > 0). Anyone already at exactly
        // zero needs no transaction and is left out of both lists.
        // NOTE: BigDecimal.equals() treats "0.0" and "0.00" as different values
        // (different scale), so we always compare with compareTo() instead.
        List<String> debtorNames = new ArrayList<>();
        List<BigDecimal> debtorAmounts = new ArrayList<>();
        List<String> creditorNames = new ArrayList<>();
        List<BigDecimal> creditorAmounts = new ArrayList<>();

        for (Map.Entry<String, BigDecimal> entry : balances.entrySet()) {
            int cmp = entry.getValue().compareTo(BigDecimal.ZERO);
            if (cmp < 0) {
                debtorNames.add(entry.getKey());
                debtorAmounts.add(entry.getValue());
            } else if (cmp > 0) {
                creditorNames.add(entry.getKey());
                creditorAmounts.add(entry.getValue());
            }
        }

        List<Settlement> settlements = new ArrayList<>();

        // Greedy strategy: each round, match the person who owes the most
        // against the person who is owed the most, and transfer the smaller
        // of the two amounts between them. This is what keeps the number of
        // transactions minimal (at most people.size() - 1), instead of every
        // debtor paying every creditor they individually owe something to.
        while (!debtorNames.isEmpty() && !creditorNames.isEmpty()) {
            int debtorIdx = indexOfMostNegative(debtorAmounts);
            int creditorIdx = indexOfMostPositive(creditorAmounts);

            BigDecimal debtBalance = debtorAmounts.get(debtorIdx);
            BigDecimal creditBalance = creditorAmounts.get(creditorIdx);

            // Only transfer as much as fully satisfies the smaller side;
            // the larger side carries its remainder into the next round.
            BigDecimal amount = debtBalance.negate().min(creditBalance);
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                settlements.add(new Settlement(debtorNames.get(debtorIdx), creditorNames.get(creditorIdx), amount));
            }

            BigDecimal remainingDebt = debtBalance.add(amount);
            BigDecimal remainingCredit = creditBalance.subtract(amount);

            // Whichever side hit exactly zero drops out of the round; the
            // other side stays in with its updated (smaller) balance.
            if (remainingDebt.compareTo(BigDecimal.ZERO) == 0) {
                debtorNames.remove(debtorIdx);
                debtorAmounts.remove(debtorIdx);
            } else {
                debtorAmounts.set(debtorIdx, remainingDebt);
            }

            if (remainingCredit.compareTo(BigDecimal.ZERO) == 0) {
                creditorNames.remove(creditorIdx);
                creditorAmounts.remove(creditorIdx);
            } else {
                creditorAmounts.set(creditorIdx, remainingCredit);
            }
        }

        return settlements;
    }

    private static int indexOfMostNegative(List<BigDecimal> values) {
        int idx = 0;
        for (int i = 1; i < values.size(); i++) {
            if (values.get(i).compareTo(values.get(idx)) < 0) {
                idx = i;
            }
        }
        return idx;
    }

    private static int indexOfMostPositive(List<BigDecimal> values) {
        int idx = 0;
        for (int i = 1; i < values.size(); i++) {
            if (values.get(i).compareTo(values.get(idx)) > 0) {
                idx = i;
            }
        }
        return idx;
    }
}
