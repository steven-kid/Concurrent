import java.util.concurrent.ThreadLocalRandom;

public class Driver extends Person {
	private Passenger currentPassenger;

	public Driver(String name, int maxDelay) {
		super(name, maxDelay);
	}

	public void pickUpPassenger(Passenger passenger) {
		this.currentPassenger = passenger;
		try {
			int delay = ThreadLocalRandom.current().nextInt(0, maxDelay + 1);
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public void driveToDestination() {
		try {
			if (currentPassenger != null) {
				int travelTime = currentPassenger.getTravelTime();
				Thread.sleep(travelTime);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	// Getters and setters if necessary
}
