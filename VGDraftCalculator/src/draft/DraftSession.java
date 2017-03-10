package draft;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DraftSession {

	private Format phases;
	private Calculator scorer;
	private Set<Hero> banned;
	private Roster blue;
	private Roster red;
	private int phaseNumber;
	
	public DraftSession(Format format, HeroMatrix matrix) {
		this.phases = format;
		this.blue = new Roster(phases.blueRoster());
		this.red = new Roster(phases.redRoster());
		this.banned = new HashSet<>();
		this.phaseNumber = 0;
		this.scorer = new Calculator(matrix);
	}
	
	public Phase currentPhase() {
		return phases.get(phaseNumber);
	}
	
	public Double currentOddsForBlue() {
//		return suggestions().get(0).getScore();
		Set<Hero> pool = currentPool();
		if (blue.isEmpty()) {
			// draft hasn't started; assume blue gets best hero
			Hero firstPick = scorer.optimalNextPicks(pickingTeam(), enemyTeam(), currentPool()).get(0).getCandidate();
			return scorer.score(new Roster(blue).add(firstPick), red, pool);
		} else {
			return scorer.score(blue, red, pool);
		}
	}

	public List<Pick> suggestions() {
		// I'd really like to get this working:
//		return scorer.options(pickingTeam(), enemyTeam(), currentPool(), phases, phaseNumber);
		
		switch (phases.strategy(phaseNumber)) {
		case PICK:
		default:
			return scorer.optimalNextPicks(pickingTeam(), enemyTeam(), currentPool());
		case DEFENSIVE_BAN:
			return scorer.optimalNextPicks(enemyTeam(), pickingTeam(), currentPool());
		case OFFENSIVE_BAN:
			return scorer.optimalOffensiveBan(pickingTeam(), enemyTeam(), currentPool());
		}
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
	private Set<Hero> currentPool() {
		Set<Hero> pool = new HashSet<>(Arrays.asList(Hero.values()));
		pool.removeAll(blue.getPicked());
		pool.removeAll(red.getPicked());
		pool.removeAll(banned);
		return pool;
	}
	
	private Roster pickingTeam() {
		return currentPhase().isBlue() ? blue : red;
	}
	
	private Roster enemyTeam() {
		return currentPhase().isBlue() ? red : blue;
	}

//	private List<Pick> suggestPicks() {
//		return scorer.optimalNextPicks(pickingTeam(), enemyTeam(), currentPool());
//	}
//
//	/**
//	 * The other team is about to pick. Let's ban away their current best option.
//	 */
//	private List<Pick> suggestDefensiveBans() {
//		// best picks against us => best bans for us
//		return scorer.optimalNextPicks(enemyTeam(), pickingTeam(), currentPool());
//	}
//
//	private List<Pick> suggestOffensiveBans() {
//		return scorer.optimalOffensiveBan(pickingTeam(), enemyTeam(), currentPool());
//	}
}
