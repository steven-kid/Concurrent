import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

public class Booking implements Callable<BookingResult> {
	private static final AtomicInteger nextId = new AtomicInteger(1);

	public int getBookingId() {
		return bookingId;
	}

	private final int bookingId;
	private final NuberDispatch dispatch;
	private final Passenger passenger;
	private final Date startTime;
	private Driver driver;

	public Booking(NuberDispatch dispatch, Passenger passenger)
	{
		this.dispatch = dispatch;
		this.passenger = passenger;
		this.startTime = new Date();  // record start time
		this.bookingId = nextId.getAndIncrement();  // ensure unique, sequential ID
		dispatch.logEvent(this + "Create booking");
	}

	@Override
	public BookingResult call() {
		try {
			driver = dispatch.getDriver(); // request a driver

			while (driver == null) {  // wait for a driver to become available
				Thread.sleep(100);  // check for driver availability at intervals
				driver = dispatch.getDriver();
			}

			driver.pickUpPassenger(passenger);
			driver.driveToDestination();

			// record end time and calculate duration
			long tripDuration = new Date().getTime() - startTime.getTime();

			// add driver back to dispatch's available drivers
			dispatch.addDriver(driver);

			// create and return booking result
			return new BookingResult(bookingId, passenger, driver, tripDuration);
		} catch (InterruptedException e) {
			// handle interruption (e.g., cancellation)
			Thread.currentThread().interrupt();
			return null;  // or handle accordingly
		}
	}

	@Override
	public String toString()
	{
		String driverName = (driver != null) ? driver.getName() : "null";
		String passengerName = (passenger != null) ? passenger.getName() : "null";
		return bookingId + ":" + driverName + ":" + passengerName;
	}
}
