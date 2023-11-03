import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NuberDispatch {
	private boolean debug;
	private Map<String, NuberRegion> regions;
	private Queue<Driver> availableDrivers;

	public NuberDispatch(HashMap<String, Integer> regionInfo, boolean debug)

		this.debug = debug;
		this.regions = regionInfo;
		this.availableDrivers = new ConcurrentLinkedQueue<>();
	}

	public void addRegion(String name, NuberRegion region) {
		regions.put(name, region);
	}

	public synchronized void bookPassenger(Passenger passenger, String regionName) {
		NuberRegion region = regions.get(regionName);
		if (region != null) {
			region.bookPassenger(passenger);
		}
	}

	public synchronized Driver provideDriver() {
		return availableDrivers.poll();
	}

	public synchronized void addDriver(Driver driver) {
		availableDrivers.offer(driver);
	}

	// Shutting down logic and other necessary methods

	// Reporting method
	public void reportEvent(String event) {
		if (debug) {
			System.out.println(event);
		}
	}
}
