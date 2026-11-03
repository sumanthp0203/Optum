package trip;

import java.util.List;

/**
 * Demo: 4 friends on a trip, using both kinds of transaction.
 *  - Person1 fronts $100 for 4 train tickets (an Expense, split 4 ways).
 *  - Person2 and Person3 pay their $25 share straight back to Person1 (Payments).
 *  - Person4 pays for a $40 group dinner (another Expense).
 *  - Person1 lends Person4 $10 in cash (a Payment / loan).
 */
public class Main {
    public static void main(String[] args) {
        TripLedger trip = new TripLedger(List.of("Person1", "Person2", "Person3", "Person4"));

        trip.addExpense("Person1", 100, "4 train tickets");
        trip.addPayment("Person2", "Person1", 25, "Repaying train ticket share");
        trip.addPayment("Person3", "Person1", 25, "Repaying train ticket share");
        trip.addExpense("Person4", 40, "Group dinner");
        trip.addPayment("Person1", "Person4", 10, "Loan for taxi fare");

        System.out.println("Transaction history:");
        for (Transaction t : trip.getHistory()) {
            System.out.println("  " + t.getDescription());
        }

        System.out.println();
        System.out.println("Net balances (positive = should receive, negative = owes):");
        trip.getBalances().forEach((person, balance) -> System.out.println("  " + person + ": $" + balance));

        System.out.println();
        System.out.println("Settlement plan:");
        List<Settlement> settlements = trip.settleUp();
        if (settlements.isEmpty()) {
            System.out.println("  Everyone is already settled up.");
        } else {
            settlements.forEach(s -> System.out.println("  " + s));
        }
    }
}
