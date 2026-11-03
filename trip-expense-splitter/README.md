# Trip Expense Splitter

A small Splitwise-style ledger for a group trip. Tracks two kinds of
transactions and tells you, at any point, who owes what and the minimal set
of transfers needed to settle everyone up.

## Transaction types

- **Expense** — one person pays upfront for something shared by a group
  (e.g. Person1 pays $100 for 4 train tickets); the cost is split equally
  among the participants.
- **Payment** — a direct transfer of money from one person straight to
  another (settling a debt, or lending cash), with no splitting involved.

## Requirements

- Java 17+
- Maven 3.6+

Check both are installed:

```bash
java -version
mvn -version
```

## Project layout

```
trip-expense-splitter/
├── pom.xml
├── src/main/java/trip/
│   ├── Transaction.java   # abstract base: Expense and Payment extend this
│   ├── Expense.java       # shared cost split equally among participants
│   ├── Payment.java       # direct transfer between two people
│   ├── Settlement.java    # one step of a settle-up plan (payer, receiver, amount)
│   ├── TripLedger.java    # records transactions, computes balances & settle-up
│   ├── Main.java          # runnable demo scenario (hardcoded example data)
│   ├── BusTripDemo.java   # another hardcoded example scenario
│   └── InteractiveTripApp.java # prompts for people/transactions at runtime (no hardcoded values)
└── src/test/java/trip/
    └── TripLedgerTest.java
```

## Running the tests

From the project root:

```bash
cd /Users/KON1362/Documents/trip-expense-splitter
mvn test
```

You should see something like:

```
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

To run a single test class or method:

```bash
mvn -Dtest=TripLedgerTest test
mvn -Dtest=TripLedgerTest#settleUpPlanZeroesOutEveryBalanceWhenReplayed test
```

## Running the demo

```bash
mvn -q compile exec:java
```

This runs `trip.Main`, which simulates 4 friends on a trip:

1. Person1 pays $100 for 4 train tickets (an `Expense`, split 4 ways).
2. Person2 and Person3 each pay their $25 share back to Person1 (`Payment`s).
3. Person4 pays $40 for a group dinner (another `Expense`).
4. Person1 lends Person4 $10 in cash (a `Payment` / loan).

It then prints the transaction history, each person's net balance, and the
minimal settle-up plan (who should pay whom, and how much).

To run a different hardcoded scenario:

```bash
mvn -q compile exec:java -Dexec.mainClass=trip.BusTripDemo
```

## Running it interactively (no hardcoded values)

```bash
mvn -q compile exec:java -Dexec.mainClass=trip.InteractiveTripApp
```

This prompts you for:
1. How many people are on the trip, and each of their names.
2. Any number of transactions, one at a time — each either:
   - `expense`: who paid, the amount, a description, and who it's split
     among (leave blank to split across everyone).
   - `payment`: who paid whom directly, the amount, and a description.
3. Type `done` when you're finished entering transactions.

It then prints the same summary as the other demos: transaction history, net
balances, and the minimal settlement plan — computed entirely from what you
typed in, with no hardcoded names or dollar amounts.

## Using it in your own code

```java
TripLedger trip = new TripLedger(List.of("Person1", "Person2", "Person3", "Person4"));

trip.addExpense("Person1", 100, "4 train tickets");           // split across everyone
trip.addExpense("Person1", 50, List.of("Person1", "Person2"), "Two museum tickets"); // split across a subset
trip.addPayment("Person2", "Person1", 25, "Repaying train ticket share");

Map<String, BigDecimal> balances = trip.getBalances(); // positive = owed money, negative = owes money
List<Settlement> plan = trip.settleUp();               // minimal list of (payer, receiver, amount)
```

## Notes

- All money is `BigDecimal`, rounded to cents (`HALF_UP`), so balances never
  drift due to floating-point rounding.
- `settleUp()` uses a greedy "largest debtor pays largest creditor" strategy,
  which minimizes the number of transactions needed.
