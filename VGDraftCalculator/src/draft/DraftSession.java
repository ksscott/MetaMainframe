package draft;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DraftSession implements Cloneable {

	private Format phases;
	private Set<Hero> banned;
	private Roster blue;
	private Roster red;
	private int phaseNumber;
	
	public DraftSession(Format format) {
		this.phases = format;
		this.blue = new Roster(phases.blueRoster());
		this.red = new Roster(phases.redRoster());
		this.banned = new HashSet<>();
		this.phaseNumber = 0;
	}
	
	public DraftSession whatIf(Hero hero) {
		DraftSession hypothetical = this.clone();
		hypothetical.pickOrBan(hero);
		return hypothetical;
	}
	
	public Format getFormat() {
		return phases;
	}
	
	public int currentPhaseNo() {
		return phaseNumber;
	}
	
	public Phase currentPhase() {
		return phases.get(phaseNumber);
	}
	
	public boolean isFull() {
		return phaseNumber >= phases.size();
	}
	
	/**
	 * Pass {@code null} to skip a hero (e.g., skip a ban).
	 */
	public void pickOrBan(Hero next) {
		Phase phase = phases.get(phaseNumber);
		
		if (phase.isPick()) {
			if (next == null)
				throw new IllegalArgumentException("Cannot skip a pick!");
			if (phase.isBlue())
				blue.add(next);
			else
				red.add(next);
		} else {
			banned.add(next);
		}
		phaseNumber++;
	}

	/**
	 * @return currently available heroes
	 */
	public Set<Hero> currentPool() {
		Set<Hero> pool = new HashSet<>(Arrays.asList(Hero.values()));
		pool.removeAll(blue.getPicked());
		pool.removeAll(red.getPicked());
		pool.removeAll(banned);
		return pool;
	}
	
	public Strategy getStrategy() {
		return phases.strategy(phaseNumber);
	}
	
	public Roster getBlue() { return blue; }
	public Roster getRed() { return red; }
	
	public Roster pickingTeam() {
		return currentPhase().isBlue() ? blue : red;
	}
	
	public Roster enemyTeam() {
		return currentPhase().isBlue() ? red : blue;
	}

	@Override
	public DraftSession clone() {
		DraftSession clone = new DraftSession(phases);
		clone.banned = new HashSet<>(this.banned);
		clone.blue = this.blue.clone();
		clone.red = this.red.clone();
		clone.phaseNumber = this.phaseNumber;
		return clone;
	}
}
