package draft;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DraftSession implements Cloneable {

	private Format phases;
//	private Calculator scorer;
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
//		this.scorer = new Calculator(matrix);
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
	
//	public Double currentOddsForBlue() {
////		return suggestions().get(0).getScore();
//		if (blue.isEmpty()) {
//			// draft hasn't started; assume blue gets best hero
//			Hero firstPick = scorer.optimalNextPicks(pickingTeam(), enemyTeam(), currentPool()).get(0).getCandidate();
//			return scorer.scoreAndFill(blue.whatIf(firstPick), red, currentPool());
//		} else {
////			return scorer.scoreAndFill(blue, red, currentPool());
//			return scorer.scorePlusSynergy(blue, red);
//		}
//	}

//	public List<Pick> suggestions() {
		// I'd really like to get this working:
//		return scorer.options(new Matchup(pickingTeam(), enemyTeam(), scorer), currentPool(), phases, phaseNumber);
//		return scorer.pruningAlgorithm(blue, red, currentPool(), phases, phaseNumber);
		
//		List<Pick> suggestions;
//		switch (phases.strategy(phaseNumber)) {
//		case PICK:
//		default:
//			suggestions = scorer.optimalNextPicks(pickingTeam(), enemyTeam(), currentPool());
//			break;
//		case DEFENSIVE_BAN:
//			suggestions = scorer.optimalNextPicks(enemyTeam(), pickingTeam(), currentPool());
//			break;
//		case OFFENSIVE_BAN:
//			suggestions = scorer.optimalOffensiveBan(pickingTeam(), enemyTeam(), currentPool());
//			break;
//		}
//		if (!phases.get(phaseNumber).isBlue()) {
//			// adjust score to show blue's odds
//			suggestions = suggestions.stream()
//					.map((pick) -> {
//						return new Pick(pick.getCandidate(), 1 - pick.getScore());
//					})
//					.sorted()
//					.collect(Collectors.toList());
//			Collections.reverse(suggestions);
//		}
//		return suggestions;
//	}
	
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
