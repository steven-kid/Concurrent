import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;

public class NuberRegion {
	private final String name;
	private final NuberDispatch dispatch;
	private final Semaphore bookingSlots;
	private Queue<Booking> bookingsQueue;

	public NuberRegion(NuberDispatch dispatch, String name, int maxJobs) {
		this.name = name;
		this.dispatch = dispatch;
		this.bookingSlots = new Semaphore(maxJobs);
		this.bookingsQueue = new ConcurrentLinkedQueue<>();
	}

	public void bookPassenger(Passenger passenger) {
		Booking booking = new Booking(dispatch, passenger);
		bookingsQueue.add(booking);
		tryBooking();
	}

	private void tryBooking() {
		if (bookingSlots.tryAcquire()) {
			Booking booking = bookingsQueue.poll();
			if (booking != null) {
				booking.call();
			}
		}
	}

	// Logic for when booking is complete to release a slot
}
