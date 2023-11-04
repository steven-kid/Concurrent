import java.sql.Driver;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

public class NuberRegion {

	private NuberDispatch dispatch;
	private String regionName;
	private int maxSimultaneousJobs;
	private ExecutorService jobExecutor;
	private LinkedBlockingQueue<Passenger> waitingPassengers;
	private boolean isShutdownInitiated;

	public NuberRegion(NuberDispatch dispatch, String regionName, int maxSimultaneousJobs) {
		this.dispatch = dispatch;
		this.regionName = regionName;
		this.maxSimultaneousJobs = maxSimultaneousJobs;
		this.jobExecutor = Executors.newFixedThreadPool(maxSimultaneousJobs);
		this.waitingPassengers = new LinkedBlockingQueue<>();
		this.isShutdownInitiated = false;
	}

	public Future<BookingResult> bookPassenger(Passenger waitingPassenger) {
		if (isShutdownInitiated) {
			System.out.println("Booking was rejected as the region is shutting down.");
			return null;
		}

		Future<BookingResult> futureBookingResult = null;
		try {
			waitingPassengers.put(waitingPassenger);
			futureBookingResult = jobExecutor.submit(() -> {
				try {
					// Simulate checking for an available driver
					Driver driver = dispatch.getAvailableDriver();
					if (driver != null) {
						// Simulate booking process
						return new BookingResult(waitingPassenger, driver, true);
					} else {
						return new BookingResult(waitingPassenger, null, false);
					}
				} catch (InterruptedException e) {
					// Handle the exception if the thread was interrupted
					Thread.currentThread().interrupt();
					return new BookingResult(waitingPassenger, null, false);
				}
			});
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		return futureBookingResult;
	}

	public void shutdown() {
		isShutdownInitiated = true;
		jobExecutor.shutdown();
		// You may want to wait for all jobs to finish before fully shutting down
		// Consider using jobExecutor.awaitTermination with a timeout
	}
}
