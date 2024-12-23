import java.sql.*;
import java.util.*;

// Superclass
class Flight {
    protected String flightNumber;
    protected String airline;
    protected String departureTime;
    protected String arrivalTime;
    protected String terminal;
    protected String gate;

    public Flight(String flightNumber, String airline, String departureTime, String arrivalTime, String terminal, String gate) {
        this.flightNumber = flightNumber;
        this.airline = airline;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.terminal = terminal;
        this.gate = gate;
    }

    @Override
    public String toString() {
        return "Flight{" +
                "flightNumber='" + flightNumber + '\'' +
                ", airline='" + airline + '\'' +
                ", departureTime='" + departureTime + '\'' +
                ", arrivalTime='" + arrivalTime + '\'' +
                ", terminal='" + terminal + '\'' +
                ", gate='" + gate + '\'' +
                '}';
    }
}

// Subclass
class DomesticFlight extends Flight {
    public DomesticFlight(String flightNumber, String airline, String departureTime, String arrivalTime, String terminal, String gate) {
        super(flightNumber, airline, departureTime, arrivalTime, terminal, gate);
    }
}

class InternationalFlight extends Flight {
    public InternationalFlight(String flightNumber, String airline, String departureTime, String arrivalTime, String terminal, String gate) {
        super(flightNumber, airline, departureTime, arrivalTime, terminal, gate);
    }
}

// Interface
interface FlightManager {
    void addFlight(Flight flight) throws SQLException;
    void viewFlights() throws SQLException;
    void updateFlight(String flightNumber, String newGate) throws SQLException;
    void deleteFlight(String flightNumber) throws SQLException;
    void syncWithDatabase() throws SQLException; // New method for syncing data
}

// Implementation with Temporary Storage and Database
class FlightManagerHybrid implements FlightManager {
    private final Connection connection;
    private final List<Flight> flightCache = new ArrayList<>();

    public FlightManagerHybrid(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void addFlight(Flight flight) throws SQLException {
        flightCache.add(flight); // Add to cache
        String query = "INSERT INTO flights (flight_number, airline, departure_time, arrival_time, terminal, gate) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, flight.flightNumber);
            stmt.setString(2, flight.airline);
            stmt.setString(3, flight.departureTime);
            stmt.setString(4, flight.arrivalTime);
            stmt.setString(5, flight.terminal);
            stmt.setString(6, flight.gate);
            stmt.executeUpdate();
            System.out.println("Flight added to database: " + flight);
        }
    }

    @Override
    public void viewFlights() throws SQLException {
        // Display cached flights
        System.out.println("Cached Flights:");
        if (flightCache.isEmpty()) {
            System.out.println("No cached flights.");
        } else {
            for (Flight flight : flightCache) {
                System.out.println(flight);
            }
        }

        // Display flights from database
        System.out.println("\nDatabase Flights:");
        String query = "SELECT * FROM flights";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                String flightNumber = rs.getString("flight_number");
                String airline = rs.getString("airline");
                String departureTime = rs.getString("departure_time");
                String arrivalTime = rs.getString("arrival_time");
                String terminal = rs.getString("terminal");
                String gate = rs.getString("gate");
                System.out.printf("Flight: %s | Airline: %s | Departure: %s | Arrival: %s | Terminal: %s | Gate: %s\n",
                        flightNumber, airline, departureTime, arrivalTime, terminal, gate);
            }
        }
    }

    @Override
    public void updateFlight(String flightNumber, String newGate) throws SQLException {
        // Update cache
        boolean updated = false;
        for (Flight flight : flightCache) {
            if (flight.flightNumber.equals(flightNumber)) {
                flight.gate = newGate;
                updated = true;
                System.out.println("Flight updated in cache: " + flight);
                break;
            }
        }

        // Update database
        String query = "UPDATE flights SET gate = ? WHERE flight_number = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, newGate);
            stmt.setString(2, flightNumber);
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("Flight updated in database.");
            } else {
                System.out.println("Flight not found in database.");
            }
        }

        if (!updated) {
            System.out.println("Flight not found in cache.");
        }
    }

    @Override
    public void deleteFlight(String flightNumber) throws SQLException {
        // Remove from cache
        flightCache.removeIf(flight -> flight.flightNumber.equals(flightNumber));
        System.out.println("Flight removed from cache.");

        // Remove from database
        String query = "DELETE FROM flights WHERE flight_number = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, flightNumber);
            int rowsDeleted = stmt.executeUpdate();
            if (rowsDeleted > 0) {
                System.out.println("Flight deleted from database.");
            } else {
                System.out.println("Flight not found in database.");
            }
        }
    }

    @Override
    public void syncWithDatabase() throws SQLException {
        for (Flight flight : flightCache) {
            addFlight(flight);
        }
        flightCache.clear();
        System.out.println("Cache synchronized with database.");
    }
}

// Main Application
public class FlightManagementApp {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try (Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3308/flight_db", "root", "")) {
            FlightManager manager = new FlightManagerHybrid(connection);
            int choice;

            do {
                System.out.println("\n=== Flight Management System ===");
                System.out.println("1. Add Flight");
                System.out.println("2. View Flights");
                System.out.println("3. Update Flight");
                System.out.println("4. Delete Flight");
                System.out.println("5. Sync Cache with Database");
                System.out.println("6. Exit");
                System.out.print("Enter your choice: ");
                choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline

                switch (choice) {
                    case 1 -> {
                        System.out.print("Enter flight number: ");
                        String flightNumber = scanner.nextLine();
                        System.out.print("Enter airline: ");
                        String airline = scanner.nextLine();
                        System.out.print("Enter departure time (HH:mm): ");
                        String departureTime = scanner.nextLine();
                        System.out.print("Enter arrival time (HH:mm): ");
                        String arrivalTime = scanner.nextLine();
                        System.out.print("Enter terminal: ");
                        String terminal = scanner.nextLine();
                        System.out.print("Enter gate: ");
                        String gate = scanner.nextLine();

                        Flight flight = new DomesticFlight(flightNumber, airline, departureTime, arrivalTime, terminal, gate);
                        manager.addFlight(flight);
                    }
                    case 2 -> manager.viewFlights();
                    case 3 -> {
                        System.out.print("Enter flight number to update: ");
                        String flightNumber = scanner.nextLine();
                        System.out.print("Enter new gate: ");
                        String newGate = scanner.nextLine();
                        manager.updateFlight(flightNumber, newGate);
                    }
                    case 4 -> {
                        System.out.print("Enter flight number to delete: ");
                        String flightNumber = scanner.nextLine();
                        manager.deleteFlight(flightNumber);
                    }
                    case 5 -> manager.syncWithDatabase();
                    case 6 -> System.out.println("Exiting system...");
                    default -> System.out.println("Invalid choice. Please try again.");
                }
            } while (choice != 6);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
