// Assuming Person, Passenger, and Driver classes are already defined and properly implemented

import java.util.concurrent.atomic.AtomicInteger;

public class Simulation {
	private NuberDispatch dispatch;
	private int maxDelay;
	private boolean reportEvents;
	private static final AtomicInteger bookingID = new AtomicInteger(1);

	public Simulation(String[] regions, int driverCount, int passengerCount, int maxDelay, boolean reportEvents) {
		this.maxDelay = maxDelay;
		this.reportEvents = reportEvents;
		dispatch = new NuberDispatch(reportEvents);

		System.out.println("Creating Nuber Dispatch");
		System.out.println("Creating " + regions.length + " regions");

		for (String regionName : regions) {
			System.out.println("Creating Nuber region for " + regionName);
			dispatch.addRegion(regionName, new NuberRegion(dispatch, regionName, maxDelay));
		}

		System.out.println("Done creating " + regions.length + " regions");

		// Now create drivers
		for (int i = 0; i < driverCount; i++) {
			Driver driver = new Driver("D-" + (i + 1), maxDelay);
			dispatch.addDriver(driver);
		}

		// Now simulate passenger bookings
		for (int i = 0; i < passengerCount; i++) {
			Passenger passenger = new Passenger("P-" + (i + 1), maxDelay);
			String regionName = regions[i % regions.length]; // Round-robin assignment of passengers to regions
			dispatch.bookPassenger(passenger, regionName);
		}

		// Here you would normally trigger the processing of bookings, for this pseudocode we'll assume that's happening asynchronously
	}

	public static int getNextBookingID() {
		return bookingID.getAndIncrement();
	}

	// More simulation logic as necessary
}
