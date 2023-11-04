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
		logEvent("Creating Nuber Dispatch");
		logEvent("Creating " +  regionInfo.size() + " regions");
		// Initialize regions based on the provided info
		regionInfo.forEach((regionName, capacity) -> {
			regions.put(regionName, new NuberRegion(this, regionName, capacity));
			logEvent("Creating Nuber region for " + regionName);
		});
		logEvent("Done creating " +  regionInfo.size() + " regions");
	}

	public boolean addDriver(Driver newDriver)
	{
		if (drivers.size() < MAX_DRIVERS) {
			drivers.add(newDriver);
			return true;
		}
		return false;
	}

	public Driver getDriver()
	{
		return drivers.poll();
	}

	public Future<BookingResult> bookPassenger(Passenger passenger, String region) {
		if (executorService.isShutdown()) {
			return null;
		}

		NuberRegion nuberRegion = regions.get(region);
		if (nuberRegion == null) {
			// Handle case where region is not found
			logEvent("Booking rejected: Region not found");
			return CompletableFuture.completedFuture(null); // return null or a completed future with a specific exception
		}

		return nuberRegion.bookPassenger(passenger);
	}

	void logEvent(String message) {
//		if(logEvents){
			System.out.println(message);
//		}
	}

	public void logEvent(Booking booking, String message) {
		if(logEvents){
			System.out.println(booking + ": " + message);
		}
	}

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
