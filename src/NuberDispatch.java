import java.util.HashMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The core Dispatch class that instantiates and manages everything for Nuber
 * 
 * @author james
 *
 */
public class NuberDispatch {

	/**
	 * The maximum number of idle drivers that can be awaiting a booking 
	 */
	private final int MAX_DRIVERS = 999;
	
	private boolean logEvents = false;

	private ExecutorService executorService;
	private ConcurrentLinkedQueue<Driver> drivers;
	private ConcurrentHashMap<String, NuberRegion> regions;

	private final AtomicInteger bookingsAwaitingDrivers;

	/**
	 * Creates a new dispatch objects and instantiates the required regions and any other objects required.
	 * It should be able to handle a variable number of regions based on the HashMap provided.
	 * 
	 * @param regionInfo Map of region names and the max simultaneous bookings they can handle
	 * @param logEvents Whether logEvent should print out events passed to it
	 */
	public NuberDispatch(HashMap<String, Integer> regionInfo, boolean logEvents) {
		this.logEvents = logEvents;
		drivers = new ConcurrentLinkedQueue<>();
		regions = new ConcurrentHashMap<>();
		executorService = Executors.newCachedThreadPool(); // Handle bookings asynchronously
		bookingsAwaitingDrivers = new AtomicInteger(0);
		// Initialize regions based on the provided info
		regionInfo.forEach((regionName, capacity) -> {
			regions.put(regionName, new NuberRegion(this, regionName, capacity));
		});
	}
	
	/**
	 * Adds drivers to a queue of idle driver.
	 *  
	 * Must be able to have drivers added from multiple threads.
	 * 
	 * @param The driver to add to the queue.
	 * @return Returns true if driver was added to the queue
	 */
	public boolean addDriver(Driver newDriver)
	{
		if (drivers.size() < MAX_DRIVERS) {
			drivers.add(newDriver);
			return true;
		}
		return false;
	}
	
	/**
	 * Gets a driver from the front of the queue
	 *  
	 * Must be able to have drivers added from multiple threads.
	 * 
	 * @return A driver that has been removed from the queue
	 */
	public Driver getDriver()
	{
		return drivers.poll();
	}

	/**
	 * Prints out the string
	 * 	    booking + ": " + message
	 * to the standard output only if the logEvents variable passed into the constructor was true
	 * 
	 * @param booking The booking that's responsible for the event occurring
	 * @param message The message to show
	 */
	public void logEvent(Booking booking, String message) {
		
		if (!logEvents) return;
		
		System.out.println(booking + ": " + message);
		
	}

	/**
	 * Books a given passenger into a given Nuber region.
	 * 
	 * Once a passenger is booked, the getBookingsAwaitingDriver() should be returning one higher.
	 * 
	 * If the region has been asked to shutdown, the booking should be rejected, and null returned.
	 * 
	 * @param passenger The passenger to book
	 * @param region The region to book them into
	 * @return returns a Future<BookingResult> object
	 */
	public Future<BookingResult> bookPassenger(Passenger passenger, String region) {
		if (executorService.isShutdown()) {
			throw new IllegalStateException("Cannot book passengers after shutdown");
		}

		return executorService.submit(() -> {
			NuberRegion nuberRegion = regions.get(region);
			if (nuberRegion == null || !nuberRegion.canBook()) {
				// Rejection handling logic goes here, for example:
				logEvent(new Booking(-1, passenger, null, 0), "Booking rejected: Region not found or cannot accept new bookings");
				return null; // return null or consider throwing a specific exception
			}

			Driver driver = getDriver(); // Attempt to get a driver for the booking
			if (driver == null) {
				// Handle case where no driver is available
				logEvent(new Booking(-1, passenger, null, 0), "Booking rejected: No available driver");
				return null; // return null or consider throwing a specific exception
			}

			// Assuming there is some logic to create a jobID and calculate trip duration
			int jobID = generateJobId(); // This is a hypothetical method to generate a job ID
			long tripDuration = calculateTripDuration(passenger, driver, nuberRegion); // Also hypothetical

			// Create the booking result
			BookingResult bookingResult = new BookingResult(jobID, passenger, driver, tripDuration);

			// Log the event
			logEvent(new Booking(jobID, passenger, driver, tripDuration), "Booking successful");

			// Here you would likely have additional logic to actually process the booking

			return bookingResult;
		});
	}

	private long calculateTripDuration(Passenger passenger, Driver driver, NuberRegion nuberRegion) {
		// Hypothetical calculation for trip duration.
		// It would typically involve distance and possibly traffic considerations.
		// For this placeholder, let's just use a mock value.
		return 30 * 60 * 1000; // Let's say every trip takes 30 minutes by default.
	}

	private void logEvent(String message) {
		// Assuming there's a logger that takes a string message.
		// Implement your logging mechanism here.
	}


	/**
	 * Gets the number of non-completed bookings that are awaiting a driver from dispatch
	 * 
	 * Once a driver is given to a booking, the value in this counter should be reduced by one
	 * 
	 * @return Number of bookings awaiting driver, across ALL regions
	 */
	public int getBookingsAwaitingDriver()
	{
		return bookingsAwaitingDrivers.get();
	}

	// Method to increment the counter of bookings awaiting drivers
	public void incrementBookingsAwaitingDrivers() {
		bookingsAwaitingDrivers.incrementAndGet();
	}

	// Method to decrement the counter of bookings awaiting drivers
	public void decrementBookingsAwaitingDrivers() {
		bookingsAwaitingDrivers.decrementAndGet();
	}
	
	/**
	 * Tells all regions to finish existing bookings already allocated, and stop accepting new bookings
	 */
	public void shutdown() {
		regions.values().forEach(NuberRegion::shutdown);
		executorService.shutdown();
	}
}
