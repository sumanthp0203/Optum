package trip;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TripLedgerTest {

    private static final List<String> PEOPLE = List.of("Person1", "Person2", "Person3", "Person4");

    private static BigDecimal money(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static void assertMoneyEquals(double expected, BigDecimal actual) {
        assertEquals(0, money(expected).compareTo(actual),
                () -> "expected " + money(expected) + " but was " + actual);
    }

    private static BigDecimal sumOf(Map<String, BigDecimal> balances) {
        return balances.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ---------- Expense transactions ----------

    @Test
    void expenseSplitsEquallyAcrossAllFourPeople() {
        TripLedger trip = new TripLedger(PEOPLE);
        trip.addExpense("Person1", 100, "4 train tickets");

        Map<String, BigDecimal> balances = trip.getBalances();
        assertMoneyEquals(75.0, balances.get("Person1"));
        assertMoneyEquals(-25.0, balances.get("Person2"));
        assertMoneyEquals(-25.0, balances.get("Person3"));
        assertMoneyEquals(-25.0, balances.get("Person4"));
        assertMoneyEquals(0.0, sumOf(balances));
    }

    @Test
    void expenseCanBeScopedToASubsetOfParticipants() {
        TripLedger trip = new TripLedger(PEOPLE);
        trip.addExpense("Person1", 50, List.of("Person1", "Person2"), "Museum tickets for two");

        Map<String, BigDecimal> balances = trip.getBalances();
        assertMoneyEquals(25.0, balances.get("Person1"));
        assertMoneyEquals(-25.0, balances.get("Person2"));
        assertMoneyEquals(0.0, balances.get("Person3"));
        assertMoneyEquals(0.0, balances.get("Person4"));
    }

    @Test
    void expenseRejectsNonPositiveAmount() {
        TripLedger trip = new TripLedger(PEOPLE);
        assertThrows(IllegalArgumentException.class, () -> trip.addExpense("Person1", 0, "bad"));
        assertThrows(IllegalArgumentException.class, () -> trip.addExpense("Person1", -10, "bad"));
    }

    @Test
    void expenseRejectsPayerNotInParticipants() {
        TripLedger trip = new TripLedger(PEOPLE);
        assertThrows(IllegalArgumentException.class,
                () -> trip.addExpense("Person1", 30, List.of("Person2", "Person3"), "bad"));
    }

    @Test
    void expenseRejectsUnknownPerson() {
        TripLedger trip = new TripLedger(PEOPLE);
        assertThrows(IllegalArgumentException.class,
                () -> trip.addExpense("Stranger", 30, "bad"));
    }

    // ---------- Payment (direct transfer / loan) transactions ----------

    @Test
    void paymentMovesBalanceDirectlyBetweenTwoPeople() {
        TripLedger trip = new TripLedger(PEOPLE);
        // With no prior debt, Person2 handing Person1 cash is effectively a loan:
        // Person2 is now owed that money back, Person1 now owes it.
        trip.addPayment("Person2", "Person1", 25, "Lending cash");

        Map<String, BigDecimal> balances = trip.getBalances();
        assertMoneyEquals(-25.0, balances.get("Person1"));
        assertMoneyEquals(25.0, balances.get("Person2"));
        assertMoneyEquals(0.0, balances.get("Person3"));
        assertMoneyEquals(0.0, balances.get("Person4"));
    }

    @Test
    void repayingTrainTicketSharesViaPaymentsBringsThoseTwoPeopleToZero() {
        TripLedger trip = new TripLedger(PEOPLE);
        trip.addExpense("Person1", 100, "4 train tickets");
        trip.addPayment("Person2", "Person1", 25, "Repaying train ticket share");
        trip.addPayment("Person3", "Person1", 25, "Repaying train ticket share");

        Map<String, BigDecimal> balances = trip.getBalances();
        assertMoneyEquals(0.0, balances.get("Person2"));
        assertMoneyEquals(0.0, balances.get("Person3"));
        assertMoneyEquals(25.0, balances.get("Person1")); // Person4 hasn't paid back yet
        assertMoneyEquals(-25.0, balances.get("Person4"));
    }

    @Test
    void paymentRejectsNonPositiveAmount() {
        TripLedger trip = new TripLedger(PEOPLE);
        assertThrows(IllegalArgumentException.class, () -> trip.addPayment("Person1", "Person2", 0, "bad"));
        assertThrows(IllegalArgumentException.class, () -> trip.addPayment("Person1", "Person2", -5, "bad"));
    }

    @Test
    void paymentRejectsSamePersonOnBothSides() {
        TripLedger trip = new TripLedger(PEOPLE);
        assertThrows(IllegalArgumentException.class, () -> trip.addPayment("Person1", "Person1", 10, "bad"));
    }

    @Test
    void paymentRejectsUnknownPerson() {
        TripLedger trip = new TripLedger(PEOPLE);
        assertThrows(IllegalArgumentException.class, () -> trip.addPayment("Person1", "Stranger", 10, "bad"));
    }

    // ---------- Mixed scenarios across many transactions ----------

    @Test
    void mixedExpensesAndPaymentsNetOutCorrectly() {
        TripLedger trip = new TripLedger(PEOPLE);
        trip.addExpense("Person1", 100, "4 train tickets");   // +75 / -25 / -25 / -25
        trip.addExpense("Person4", 40, "Group dinner");       // -10 / -10 / -10 / +30
        trip.addPayment("Person1", "Person4", 10, "Loan for taxi fare"); // Person1 -10, Person4 +10

        Map<String, BigDecimal> balances = trip.getBalances();
        assertMoneyEquals(75.0, balances.get("Person1"));   // +75 (tickets) -10 (dinner share) +10 (loan given)
        assertMoneyEquals(-35.0, balances.get("Person2"));  // -25 (tickets) -10 (dinner share)
        assertMoneyEquals(-35.0, balances.get("Person3"));  // -25 (tickets) -10 (dinner share)
        assertMoneyEquals(-5.0, balances.get("Person4"));   // -25 (tickets) +30 (dinner) -10 (loan received)
        assertMoneyEquals(0.0, sumOf(balances));
    }

    @Test
    void historyRecordsEveryTransactionInOrder() {
        TripLedger trip = new TripLedger(PEOPLE);
        trip.addExpense("Person1", 100, "4 train tickets");
        trip.addPayment("Person2", "Person1", 25, "Repaying train ticket share");

        List<Transaction> history = trip.getHistory();
        assertEquals(2, history.size());
        assertInstanceOf(Expense.class, history.get(0));
        assertInstanceOf(Payment.class, history.get(1));
    }

    // ---------- Settle-up ----------

    @Test
    void settleUpProducesZeroTransactionsWhenAlreadyBalanced() {
        TripLedger trip = new TripLedger(PEOPLE);
        trip.addExpense("Person1", 100, "4 train tickets");
        trip.addPayment("Person2", "Person1", 25, "Repaying train ticket share");
        trip.addPayment("Person3", "Person1", 25, "Repaying train ticket share");
        trip.addPayment("Person4", "Person1", 25, "Repaying train ticket share");

        assertTrue(trip.settleUp().isEmpty());
    }

    @Test
    void settleUpPlanZeroesOutEveryBalanceWhenReplayed() {
        TripLedger trip = new TripLedger(PEOPLE);
        trip.addExpense("Person1", 100, "4 train tickets");
        trip.addExpense("Person4", 40, "Group dinner");
        trip.addPayment("Person1", "Person4", 10, "Loan for taxi fare");

        List<Settlement> plan = trip.settleUp();
        assertFalse(plan.isEmpty());

        for (Settlement s : plan) {
            assertTrue(s.amount().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(PEOPLE.contains(s.payer()));
            assertTrue(PEOPLE.contains(s.receiver()));
            assertNotEquals(s.payer(), s.receiver());
        }

        Map<String, BigDecimal> balances = trip.getBalances();
        for (Settlement s : plan) {
            balances.merge(s.payer(), s.amount(), BigDecimal::add);
            balances.merge(s.receiver(), s.amount(), BigDecimal::subtract);
        }
        for (BigDecimal balance : balances.values()) {
            assertEquals(0, balance.compareTo(BigDecimal.ZERO));
        }
    }

    @Test
    void settleUpUsesAtMostNMinusOneTransactions() {
        TripLedger trip = new TripLedger(PEOPLE);
        trip.addExpense("Person1", 100, "4 train tickets");

        List<Settlement> plan = trip.settleUp();
        assertTrue(plan.size() <= PEOPLE.size() - 1);
        for (Settlement s : plan) {
            assertEquals("Person1", s.receiver());
        }
    }

    @Test
    void settleUpAfterManyRandomPaymentsStillZeroesOut() {
        TripLedger trip = new TripLedger(PEOPLE);
        trip.addExpense("Person1", 100, "4 train tickets");
        trip.addPayment("Person2", "Person1", 10, "Partial repay");
        trip.addPayment("Person3", "Person4", 5, "Side loan");
        trip.addPayment("Person4", "Person1", 2, "Small repay");
        trip.addExpense("Person3", 60, "Group activity");
        trip.addPayment("Person2", "Person3", 15, "Catching up");

        List<Settlement> plan = trip.settleUp();
        Map<String, BigDecimal> balances = trip.getBalances();
        for (Settlement s : plan) {
            balances.merge(s.payer(), s.amount(), BigDecimal::add);
            balances.merge(s.receiver(), s.amount(), BigDecimal::subtract);
        }
        for (BigDecimal balance : balances.values()) {
            assertEquals(0, balance.compareTo(BigDecimal.ZERO));
        }
    }

    @Test
    void noTransactionsMeansNoDebtsAndNoSettlements() {
        TripLedger trip = new TripLedger(PEOPLE);
        assertTrue(trip.settleUp().isEmpty());
        for (BigDecimal balance : trip.getBalances().values()) {
            assertEquals(0, balance.compareTo(BigDecimal.ZERO));
        }
    }

    // ---------- Ledger construction ----------

    @Test
    void ledgerRejectsFewerThanTwoPeople() {
        assertThrows(IllegalArgumentException.class, () -> new TripLedger(List.of("Solo")));
    }

    @Test
    void ledgerRejectsDuplicatePersonNames() {
        assertThrows(IllegalArgumentException.class,
                () -> new TripLedger(List.of("Person1", "Person2", "Person1")));
    }

    @Test
    void expenseRejectsDuplicateParticipantNames() {
        assertThrows(IllegalArgumentException.class,
                () -> new Expense("Person1", 30, List.of("Person1", "Person2", "Person1"), "bad"));
    }
}
