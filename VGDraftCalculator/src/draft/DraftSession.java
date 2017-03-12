package draft;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
		if (blue.isEmpty()) {
			// draft hasn't started; assume blue gets best hero
			Hero firstPick = scorer.optimalNextPicks(pickingTeam(), enemyTeam(), currentPool()).get(0).getCandidate();
			return scorer.score(blue.whatIf(firstPick), red, currentPool());
		} else {
			return scorer.score(blue, red, currentPool());
		}
	}

	public List<Pick> suggestions() {
		// I'd really like to get this working:
//		return scorer.options(new Matchup(pickingTeam(), enemyTeam(), scorer), currentPool(), phases, phaseNumber);
		
		List<Pick> suggestions;
		switch (phases.strategy(phaseNumber)) {
		case PICK:
		default:
			suggestions = scorer.optimalNextPicks(pickingTeam(), enemyTeam(), currentPool());
			break;
		case DEFENSIVE_BAN:
			suggestions = scorer.optimalNextPicks(enemyTeam(), pickingTeam(), currentPool());
			break;
		case OFFENSIVE_BAN:
			suggestions = scorer.optimalOffensiveBan(pickingTeam(), enemyTeam(), currentPool());
			break;
		}
		if (!phases.get(phaseNumber).isBlue()) {
			// adjust score to show blue's odds
			suggestions = suggestions.stream()
					.map((pick) -> {
						return new Pick(pick.getCandidate(), 1 - pick.getScore());
					})
					.sorted()
					.collect(Collectors.toList());
			Collections.reverse(suggestions);
		}
		return suggestions;
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
