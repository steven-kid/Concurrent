public class BookingResult {
	private boolean success; // Add success field
	public int jobID;
	public Passenger passenger;
	public Driver driver;
	public long tripDuration;
	
	public BookingResult(int jobID, Passenger passenger, Driver driver, long tripDuration)
	{
		this.jobID = jobID;
		this.passenger = passenger;
		this.driver = driver;
		this.tripDuration = tripDuration;
	}
	// Constructor for a failed booking
	public BookingResult(boolean success) {
		this.success = success;
		// Initialize other fields to default values or leave them unset
	}

	// Getter for the success field
	public boolean isSuccess() {
		return success;
	}
}
