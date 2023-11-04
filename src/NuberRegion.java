import java.awt.print.Book;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class NuberRegion {
	public static AtomicInteger nextId = new AtomicInteger(1);
	private final NuberDispatch dispatch;
	private final String regionName;
	private final int maxSimultaneousJobs;
	private final ExecutorService executorService;
	private final Semaphore availableJobs;
	private volatile boolean isShutdown = false;

	public int getNextId() {
		return nextId.getAndIncrement();
	}

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
			return CompletableFuture.completedFuture(null);
		}
		dispatch.incrementBookingsAwaitingDrivers(); // Increment counter for awaiting drivers

		try {
			availableJobs.acquire();  // Ensure we have an available job slot
			Booking booking = new Booking(dispatch, waitingPassenger);

			Future<BookingResult> result = executorService.submit(booking);
			dispatch.decrementBookingsAwaitingDrivers(); // Decrement counter as we did get a driver
			return result;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
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
