import java.io.Serializable;

public class Trip implements Serializable {
    private String destination;
    private double cost;
    private int seats;
    private String deadline;
    private String status;

    public Trip(String destination, double cost, int seats, String deadline, String status) {
        this.destination = destination;
        this.cost = cost;
        this.seats = seats;
        this.deadline = deadline;
        this.status = status;
    }

    public String getDestination() { return destination; }
    public double getCost() { return cost; }
    public int getSeats() { return seats; }
    public String getDeadline() { return deadline; }
    public String getStatus() { return status; }

    // Required for the functional status feature
    public void setStatus(String status) { this.status = status; }

    public boolean bookSeat() {
        if (seats > 0 && status.equalsIgnoreCase("Available")) {
            seats--;
            return true;
        }
        return false;
    }

    public void incrementSeats() { seats++; }
}