import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

public class Booking implements Callable<BookingResult> {
	public static  AtomicInteger nextId = new AtomicInteger(1);

	private final int bookingId;
	private final NuberDispatch dispatch;
	private final Passenger passenger;
	private final Date startTime;
	private Driver driver;

	public Booking(NuberDispatch dispatch, Passenger passenger)
	{
		this.bookingId = nextId.getAndIncrement();  // ensure unique, sequential ID
		dispatch.logEvent(this, "Create booking");
		this.dispatch = dispatch;
		this.passenger = passenger;
		this.startTime = new Date();  // record start time
		dispatch.logEvent(this, "Start booking, getting for driver");
		dispatch.incrementBookingsAwaitingDrivers(); // Decrement counter once driver is assigned or booking failed
	}

	@Override
	public BookingResult call() {
		try {
			driver = dispatch.getDriver(); // request a driver

			while (driver == null) {  // wait for a driver to become available
				Thread.sleep(100);  // check for driver availability at intervals
				driver = dispatch.getDriver();
			}
			dispatch.logEvent(this, "Starting, on way to passenger");
			dispatch.decrementBookingsAwaitingDrivers(); // Decrement counter once driver is assigned or booking failed
			driver.pickUpPassenger(passenger);
			dispatch.logEvent(this, "Collected passenger, on way to destination");
			driver.driveToDestination();
			dispatch.logEvent(this, "At destination, driver is now free");
			// record end time and calculate duration
			long tripDuration = new Date().getTime() - startTime.getTime();

			// add driver back to dispatch's available drivers
			dispatch.addDriver(driver);

			// create and return booking result
			return new BookingResult(bookingId, passenger, driver, tripDuration);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	public static void reSetNextId() {
		Booking.nextId.set(1);
	}

	@Override
	public String toString()
	{
		String driverName = (driver != null) ? driver.getName() : "null";
		String passengerName = (passenger != null) ? passenger.getName() : "null";
		return bookingId + ":" + driverName + ":" + passengerName + ":";
	}
}
