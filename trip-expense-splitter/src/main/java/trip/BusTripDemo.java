package trip;

import java.util.List;

/**
 * Demo scenario: 4 friends each cover one category of cost, split equally
 * across everyone, plus a separate cash loan between two of them.
 *  - Person1 pays $40 for the bus ticket.
 *  - Person2 pays $200 for the hotel.
 *  - Person3 pays $120 for the full (train/flight) ticket.
 *  - Person4 pays $60 for drinks.
 *  - Person1 separately lends Person2 $50 in cash (a Payment, not split).
 */
public class BusTripDemo {
    public static void main(String[] args) {
        TripLedger trip = new TripLedger(List.of("Person1", "Person2", "Person3", "Person4"));

        trip.addExpense("Person1", 40, "Bus ticket");
        trip.addExpense("Person2", 200, "Hotel");
        trip.addExpense("Person3", 120, "Full ticket");
        trip.addExpense("Person4", 60, "Drinks");
        trip.addPayment("Person1", "Person2", 50, "Person1 lends Person2 cash");

        System.out.println("Transaction history:");
        for (Transaction t : trip.getHistory()) {
            System.out.println("  " + t.getDescription());
        }

        System.out.println();
        System.out.println("Net balances (positive = should collect, negative = should give):");
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
