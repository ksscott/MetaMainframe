package draft;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static draft.DraftSession.Phase.*;

public class DraftSession {

	private Calculator scorer;
	private ArrayList<Hero> banned;
	private Roster blue;
	private Roster red;
	private int index;
	private Double currentOdds;
	public static final ArrayList<Phase> singleBanPhases = 
			new ArrayList<>(Arrays.asList(
					BLUE_BAN,
					RED_BAN,
					BLUE_PICK,
					RED_PICK,
					RED_PICK,
					BLUE_PICK,
					BLUE_PICK,
					RED_PICK
					));
	
	public DraftSession(HeroMatrix matrix) {
		this.scorer = new Calculator(matrix);
		this.banned = new ArrayList<>();
		int bluePicks = Collections.frequency(singleBanPhases, BLUE_PICK);
		int redPicks = Collections.frequency(singleBanPhases, RED_PICK);
		this.blue = new Roster(bluePicks, new ArrayList<Hero>());
		this.red = new Roster(redPicks, new ArrayList<Hero>());
		this.index = 0;
		this.currentOdds = new Double(.5);
	}
	
	public Phase currentPhase() {
		return singleBanPhases.get(index);
	}
	
	public Double currentOddsForBlue() {
		List<Hero> pool = currentPool();
		if (blue.isEmpty()) {
			// draft hasn't started; assume blue gets best hero
			Hero firstPick = suggestPicks().get(0).getCandidate();
			return scorer.score(new Roster(blue).add(firstPick), red, pool);
		} else {
			return scorer.score(blue, red, pool);
		}
	}

	public List<Pick> suggestions() {
		if (currentPhase().isPick()) {
			return suggestPicks();
		} else {
			// banning time!
			if (remainingPhases().get(1).isPick()) // other team is about to pick
				return suggestDefensiveBans();
			else
				return suggestOffensiveBans();
		}
	}
	
	/**
	 * Pass {@code null} to skip a hero (e.g., skip a ban).
	 * 
	 * @return odds that blue will win
	 */
	public Double pickOrBan(Hero next) {
		Phase phase = singleBanPhases.get(index);
		if (next == null && phase.isPick()) {
			throw new IllegalArgumentException("Cannot skip a pick!");
		}
		if (next != null && index < singleBanPhases.size()) {
			if (phase.isPick()) {
				if (phase.isBlue())
					blue.add(next);
				else
					red.add(next);
			} else {
				banned.add(next);
			}
			currentOdds = currentOddsForBlue();
		}
		index++;
		return currentOdds;
	}

	/**
	 * @return currently available heroes
	 */
	private List<Hero> currentPool() {
		List<Hero> pool = new ArrayList<>(Arrays.asList(Hero.values()));
		pool.removeAll(blue.getPicked());
		pool.removeAll(red.getPicked());
		pool.removeAll(banned);
		return pool;
	}

	private List<Phase> remainingPhases() {
		List<Phase> remainingPhases = singleBanPhases.subList(index, singleBanPhases.size());
		return remainingPhases;
	}
	
	private Roster pickingTeam() {
		return currentPhase().isBlue() ? blue : red;
	}
	
	private Roster enemyTeam() {
		return currentPhase().isBlue() ? red : blue;
	}

	private List<Pick> suggestPicks() {
		return scorer.optimalNextPicks(pickingTeam(), enemyTeam(), currentPool());
	}

	/**
	 * The other team is about to pick. Let's ban away their current best option.
	 */
	private List<Pick> suggestDefensiveBans() {
		// best picks against us => best bans for us
		return scorer.optimalNextPicks(enemyTeam(), pickingTeam(), currentPool());
	}

	private List<Pick> suggestOffensiveBans() {
		return scorer.optimalOffensiveBan(pickingTeam(), enemyTeam(), currentPool());
	}

	enum Phase {
		BLUE_PICK(true,true), RED_PICK(true,false), BLUE_BAN(false,true), RED_BAN(false,false);

		private final boolean isPick;
		private final boolean isBlue;
		
		private Phase(boolean isPick, boolean isBlue) {
			this.isPick = isPick;
			this.isBlue = isBlue;
		}
		public boolean isPick() { return isPick; }
		public boolean isBlue() { return isBlue; }
	}
}
