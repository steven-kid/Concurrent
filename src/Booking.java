import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

public class Booking implements Callable<BookingResult> {
	private static final AtomicInteger idGenerator = new AtomicInteger(0);
	private final int jobId;
	private final Passenger passenger;
	private final region;
	private final NuberDispatch dispatch;
	private final boolean debug;

	public Booking(NuberDispatch dispatch, nuber.students.NuberRegion region, Passenger passenger, boolean debug) {
		this.jobId = idGenerator.incrementAndGet();
		this.passenger = passenger;
		this.dispatch = dispatch;
		this.debug = debug;
	}

	public int getJobId() {
		return jobId;
	}


	@Override
	public BookingResult call() throws Exception {
		// Additional logic for when no drivers are available
		Driver driver = dispatch.provideDriver();
		if (driver == null) {
			// Output rejected booking if no driver available
			if (debug) {
				System.out.println(jobId + ":null:" + passenger.getName() + ": Rejected booking");
			}
			return new BookingResult(jobId, passenger, null, 0); // Assuming a constructor that takes these params
		}

		if (debug) {
			System.out.println(jobId + ":null:" + passenger.getName() + ": Starting booking, getting driver");
		}
		driver.pickUpPassenger(passenger);
		if (debug) {
			System.out.println(jobId + ":" + driver.getName() + ":" + passenger.getName() + ": Starting, on way to passenger");
		}
		driver.driveToDestination();
		if (debug) {
			System.out.println(jobId + ":" + driver.getName() + ":" + passenger.getName() + ": Collected passenger, on way to destination");
		}
		// When the drive is complete
		if (debug) {
			System.out.println(jobId + ":" + driver.getName() + ":" + passenger.getName() + ": At destination, driver is now free");
		}

		// Now mark the driver as available again and process the next booking if any
		dispatch.addDriver(driver);
//		region.tryBooking();

		// Assume travel time is stored somewhere, and we fetch it here
		int travelTime = passenger.getTravelTime();
		return new BookingResult(jobId, passenger, driver, travelTime);
	}

	private void debugMessage(String message) {
		if (debug) {
			System.out.println(message);
		}
	}
}
