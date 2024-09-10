// Simple class to record when someone has crossed the line first and wins
package medleySimulation;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class FinishCounter {
	private AtomicBoolean firstAcrossLine; // flag - Atomic variable to prevent data races
	private final AtomicInteger winner; // who won - Atomic variable to prevent data races
	private AtomicInteger winningTeam; // counter for patrons who have left the club - Atomic variable to prevent data
										// races

	FinishCounter() {
		firstAcrossLine = new AtomicBoolean(true);// no-one has won at start
		this.winner = new AtomicInteger(-1); // no-one has won at start
		winningTeam = new AtomicInteger(-1); // no-one has won at start
	}

	// This is called by a swimmer when they touch the fnish line
	public synchronized void finishRace(int swimmer, int team) {
		boolean won = false;
		if (firstAcrossLine.get()) {
			firstAcrossLine.set(false);
			won = true;
		}
		if (won) {
			winner.set(swimmer);
			winningTeam.set(team);
		}
	}

	// Has race been won?
	public boolean isRaceWon() {
		return !(firstAcrossLine.get());
	}

	public int getWinner() {
		return winner.get();
	}

	public int getWinningTeam() {
		return winningTeam.get();
	}
}
