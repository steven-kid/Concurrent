public class Main {
    public static void main(String[] args) {
        String[] regions = new String[]{"South", "North"};
        new Simulation(regions, 5, 10, 1000, true); // With debug output
        // new Simulation(regions, 5, 10, 1000, false); // Without debug output
    }
}

