import java.util.concurrent.*;

/**
 * A single Nuber region that operates independently of other regions, other than getting 
 * drivers from bookings from the central dispatch.
 * 
 * A region has a maxSimultaneousJobs setting that defines the maximum number of bookings 
 * that can be active with a driver at any time. For passengers booked that exceed that 
 * active count, the booking is accepted, but must wait until a position is available, and 
 * a driver is available.
 * 
 * Bookings do NOT have to be completed in FIFO order.
 * 
 * @author james
 *
 */
public class NuberRegion {

	private final NuberDispatch dispatch;
	private final String regionName;
	private final int maxSimultaneousJobs;
	private final ExecutorService executorService;
	private final Semaphore availableJobs;
	private volatile boolean isShutdown = false;

	/**
	 * Creates a new Nuber region
	 * 
	 * @param dispatch The central dispatch to use for obtaining drivers, and logging events
	 * @param regionName The regions name, unique for the dispatch instance
	 * @param maxSimultaneousJobs The maximum number of simultaneous bookings the region is allowed to process
	 */
	public NuberRegion(NuberDispatch dispatch, String regionName, int maxSimultaneousJobs)
	{
		this.dispatch = dispatch;
		this.regionName = regionName;
		this.maxSimultaneousJobs = maxSimultaneousJobs;
		this.executorService = Executors.newFixedThreadPool(maxSimultaneousJobs);
		this.availableJobs = new Semaphore(maxSimultaneousJobs);
	}
	
	/**
	 * Creates a booking for given passenger, and adds the booking to the 
	 * collection of jobs to process. Once the region has a position available, and a driver is available, 
	 * the booking should commence automatically. 
	 * 
	 * If the region has been told to shutdown, this function should return null, and log a message to the 
	 * console that the booking was rejected.
	 * 
	 * @param waitingPassenger
	 * @return a Future that will provide the final BookingResult object from the completed booking
	 */
	public Future<BookingResult> bookPassenger(Passenger waitingPassenger) {
		if (isShutdown) {
			dispatch.logEvent(new Booking(dispatch, waitingPassenger), "Booking rejected - region is shutdown");
			return CompletableFuture.completedFuture(null);
		}
		System.out.println("Starting booking, getting driver");
		dispatch.incrementBookingsAwaitingDrivers(); // Increment counter for awaiting drivers

		try {
			availableJobs.acquire();  // Ensure we have an available job slot
			Booking booking = new Booking(dispatch, waitingPassenger);
			Future<BookingResult> result = executorService.submit(booking);
			return result;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			dispatch.decrementBookingsAwaitingDrivers(); // Decrement counter as we did not get a driver
			dispatch.logEvent(new Booking(dispatch, waitingPassenger), "Booking interrupted - region is shutdown or thread interrupted");
			return CompletableFuture.completedFuture(null);
		}
	}
	
	/**
	 * Called by dispatch to tell the region to complete its existing bookings and stop accepting any new bookings
	 */
	public void shutdown()
	{
		isShutdown = true;
		executorService.shutdown();  // Reject new tasks
		try {
			if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
				executorService.shutdownNow();  // Cancel currently executing tasks
			}
		} catch (InterruptedException e) {
			executorService.shutdownNow();
		}
	}

}
