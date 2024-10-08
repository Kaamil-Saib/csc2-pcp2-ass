//M. M. Kuttel 2024 mkuttel@gmail.com
//Class to represent a swimmer swimming a race
//Swimmers have one of four possible swim strokes: backstroke, breaststroke, butterfly and freestyle
package medleySimulation;

import java.awt.Color;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class Swimmer extends Thread {

	public static StadiumGrid stadium; // shared
	private FinishCounter finish; // shared

	GridBlock currentBlock;
	private Random rand;
	private int movingSpeed;

	private PeopleLocation myLocation;
	private int ID; // thread ID
	private int team; // team ID
	private GridBlock start;
	private CountDownLatch latch; // latch to synchronise start

	private static AtomicInteger[] swimmerOrderArray = new AtomicInteger[10]; // To track swimmer order
	private static AtomicInteger[] teamArrivalCount = new AtomicInteger[10]; // To track arrivals per team
	private static Object[] teamArrivalLocks = new Object[10]; // Locks for each team

	static {
		for (int i = 0; i < 10; i++) {
			swimmerOrderArray[i] = new AtomicInteger(1); // All teams start with swimmer 1
			teamArrivalCount[i] = new AtomicInteger(0); // Initialize arrival count to 0
			teamArrivalLocks[i] = new Object(); // Initialize locks
		}
	}

	public enum SwimStroke {
		Backstroke(1, 2.5, Color.black),
		Breaststroke(2, 2.1, new Color(255, 102, 0)),
		Butterfly(3, 2.55, Color.magenta),
		Freestyle(4, 2.8, Color.red);

		private final double strokeTime;
		private final int order; // in minutes
		private final Color colour;

		SwimStroke(int order, double sT, Color c) {
			this.strokeTime = sT;
			this.order = order;
			this.colour = c;
		}

		public int getOrder() {
			return order;
		}

		public Color getColour() {
			return colour;
		}
	}

	private final SwimStroke swimStroke;

	// Constructor
	Swimmer(int ID, int t, PeopleLocation loc, FinishCounter f, int speed, SwimStroke s, CountDownLatch latch) {
		this.swimStroke = s;
		this.ID = ID;
		this.movingSpeed = speed; // range of speeds for swimmers
		this.myLocation = loc;
		this.team = t;
		this.start = stadium.returnStartingBlock(team);
		this.finish = f;
		this.latch = latch;
		rand = new Random();
	}

	// getter
	public int getX() {
		return currentBlock.getX();
	}

	// getter
	public int getY() {
		return currentBlock.getY();
	}

	// getter
	public int getSpeed() {
		return movingSpeed;
	}

	public SwimStroke getSwimStroke() {
		return swimStroke;
	}

	// !!!You do not need to change the method below!!!
	// swimmer enters stadium area
	public void enterStadium() throws InterruptedException {
		currentBlock = stadium.enterStadium(myLocation); //
		sleep(200); // wait a bit at door, look around
	}

	// !!!You do not need to change the method below!!!
	// go to the starting blocks
	// printlns are left here for help in debugging
	public void goToStartingBlocks() throws InterruptedException {
		int x_st = start.getX();
		int y_st = start.getY();
		// System.out.println("Thread "+this.ID + " has start position: " + x_st + " "
		// +y_st );
		// System.out.println("Thread "+this.ID + " at " + currentBlock.getX() + " "
		// +currentBlock.getY() );
		while (currentBlock != start) {
			// System.out.println("Thread "+this.ID + " has starting position: " + x_st + "
			// " +y_st );
			// System.out.println("Thread "+this.ID + " at position: " + currentBlock.getX()
			// + " " +currentBlock.getY() );
			sleep(movingSpeed * 3); // not rushing
			currentBlock = stadium.moveTowards(currentBlock, x_st, y_st, myLocation); // head toward starting block
			// System.out.println("Thread "+this.ID + " moved toward start to position: " +
			// currentBlock.getX() + " " +currentBlock.getY() );
		}
		System.out.println(
				"-----------Thread " + this.ID + " at start " + currentBlock.getX() + " " + currentBlock.getY());
	}

	// !!!You do not need to change the method below!!!
	// dive in to the pool
	private void dive() throws InterruptedException {
		int x = currentBlock.getX();
		int y = currentBlock.getY();
		currentBlock = stadium.jumpTo(currentBlock, x, y - 2, myLocation);
	}

	// !!!You do not need to change the method below!!!
	// swim there and back
	private void swimRace() throws InterruptedException {
		int x = currentBlock.getX();
		while ((boolean) ((currentBlock.getY()) != 0)) {
			currentBlock = stadium.moveTowards(currentBlock, x, 0, myLocation);
			// System.out.println("Thread "+this.ID + " swimming " + currentBlock.getX() + "
			// " +currentBlock.getY() );
			sleep((int) (movingSpeed * swimStroke.strokeTime)); // swim
			System.out.println("Thread " + this.ID + " swimming at speed" + movingSpeed);
		}

		while ((boolean) ((currentBlock.getY()) != (StadiumGrid.start_y - 1))) {
			currentBlock = stadium.moveTowards(currentBlock, x, StadiumGrid.start_y, myLocation);
			// System.out.println("Thread "+this.ID + " swimming " + currentBlock.getX() + "
			// " +currentBlock.getY() );
			sleep((int) (movingSpeed * swimStroke.strokeTime)); // swim
		}

	}

	// !!!You do not need to change the method below!!!
	// after finished the race
	public void exitPool() throws InterruptedException {
		int bench = stadium.getMaxY() - swimStroke.getOrder(); // they line up
		int lane = currentBlock.getX() + 1;// slightly offset
		currentBlock = stadium.moveTowards(currentBlock, lane, currentBlock.getY(), myLocation);
		while (currentBlock.getY() != bench) {
			currentBlock = stadium.moveTowards(currentBlock, lane, bench, myLocation);
			sleep(movingSpeed * 3); // not rushing
		}
	}

	private void waitForTurn() throws InterruptedException {
		synchronized (swimmerOrderArray[team]) {
			// Check if its swimmers turn based on the order in the team
			while (swimmerOrderArray[team].get() != swimStroke.getOrder()) {
				swimmerOrderArray[team].wait(); // Wait until the previous swimmer is done }
			}
		}
	}

	public void run() {
		try {

			latch.await();// Wait for the start button to be pressed
			// Swimmer arrives
			sleep(movingSpeed + (rand.nextInt(10))); // arriving takes a while
			myLocation.setArrived();

			enterStadium();

			waitForTurn();// Wait for your turn based on swimmer order

			goToStartingBlocks();

			synchronized (swimmerOrderArray[team]) {
				// Increment the swimmer order for the team, allowing the next swimmer to start
				swimmerOrderArray[team].incrementAndGet();
				swimmerOrderArray[team].notifyAll(); // Notify waiting swimmers that they can check their turn
			}
			dive();
			swimRace();
			if (swimStroke.order == 4) {
				finish.finishRace(ID, team); // fnishline
			} else {
				// System.out.println("Thread "+this.ID + " done " + currentBlock.getX() + " "
				// +currentBlock.getY() );
				exitPool();// if not last swimmer leave pool
			}

		} catch (InterruptedException e1) { // do nothing
		} catch (Exception e) {
			// System.out.println("Exception in Thread " + ID + ": " + e.getMessage());

		}

	}

}
