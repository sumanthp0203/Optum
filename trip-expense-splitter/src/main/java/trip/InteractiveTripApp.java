package trip;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Console app that builds a trip ledger entirely from user input: the people
 * on the trip, then any number of expense/payment transactions, finishing
 * with the net balances and a minimal settlement plan.
 */
public class InteractiveTripApp {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        List<String> people = readPeople(scanner);
        TripLedger trip = new TripLedger(people);

        System.out.println("\nEnter transactions one at a time. Type 'done' when finished.");
        while (true) {
            System.out.print("\nTransaction type (expense/payment/done): ");
            String type = scanner.nextLine().trim().toLowerCase();

            if (type.equals("done")) {
                break;
            } else if (type.equals("expense")) {
                readExpense(scanner, trip);
            } else if (type.equals("payment")) {
                readPayment(scanner, trip);
            } else {
                System.out.println("Please enter 'expense', 'payment', or 'done'.");
            }
        }

        printSummary(trip);
    }

    private static List<String> readPeople(Scanner scanner) {
        int n = readPositiveInt(scanner, "How many people are on the trip? ");
        List<String> people = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            System.out.print("Name of person " + i + ": ");
            people.add(scanner.nextLine().trim());
        }
        return people;
    }

    private static void readExpense(Scanner scanner, TripLedger trip) {
        try {
            System.out.print("Who paid? ");
            String paidBy = scanner.nextLine().trim();

            double amount = readAmount(scanner, "Amount: ");

            System.out.print("Description: ");
            String description = scanner.nextLine().trim();

            System.out.print("Split among (comma-separated names, blank = everyone): ");
            String participantsLine = scanner.nextLine().trim();
            List<String> participants = participantsLine.isEmpty() ? null : splitNames(participantsLine);

            trip.addExpense(paidBy, amount, participants, description);
            System.out.println("Recorded.");
        } catch (IllegalArgumentException e) {
            System.out.println("Could not record expense: " + e.getMessage());
        }
    }

    private static void readPayment(Scanner scanner, TripLedger trip) {
        try {
            System.out.print("Who is paying (from)? ");
            String from = scanner.nextLine().trim();

            System.out.print("Who receives it (to)? ");
            String to = scanner.nextLine().trim();

            double amount = readAmount(scanner, "Amount: ");

            System.out.print("Description: ");
            String description = scanner.nextLine().trim();

            trip.addPayment(from, to, amount, description);
            System.out.println("Recorded.");
        } catch (IllegalArgumentException e) {
            System.out.println("Could not record payment: " + e.getMessage());
        }
    }

    private static double readAmount(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Double.parseDouble(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private static int readPositiveInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value >= 2) {
                    return value;
                }
                System.out.println("A trip needs at least 2 people.");
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private static List<String> splitNames(String line) {
        List<String> names = new ArrayList<>();
        for (String part : line.split(",")) {
            names.add(part.trim());
        }
        return names;
    }

    private static void printSummary(TripLedger trip) {
        System.out.println("\nTransaction history:");
        for (Transaction t : trip.getHistory()) {
            System.out.println("  " + t.getDescription());
        }

        System.out.println("\nNet balances (positive = should collect, negative = should give):");
        trip.getBalances().forEach((person, balance) -> System.out.println("  " + person + ": $" + balance));

        System.out.println("\nSettlement plan:");
        List<Settlement> settlements = trip.settleUp();
        if (settlements.isEmpty()) {
            System.out.println("  Everyone is already settled up.");
        } else {
            settlements.forEach(s -> System.out.println("  " + s));
        }
    }
}
