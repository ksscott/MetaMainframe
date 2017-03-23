package draft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static draft.Strategy.*;

public class Calculator {

	private final HeroMatrix meta;
	
	public Calculator(HeroMatrix meta) {
		this.meta = meta;
	}

	///////////////////
	//   API Layer   //
	///////////////////
	
	/**
	 * stateless
	 */
	public Map<String, Integer> coachMeSenpai (String draftFormat, final List<String> selected) {
		Format format = Format.valueOf(draftFormat);
		DraftSession session = new DraftSession(format);
		for (String hero : selected) {
			session.pickOrBan(Hero.fromName(hero));
		}
		Map<String, Integer> map = new HashMap<>();
		List<Pick> picks = suggestions(session);
		for (int i = 0; i < picks.size(); i++) {
			int rating = 0;
			double topXPercent = (i+1) / (double) picks.size();
			if (topXPercent < .1)
				rating = 1;
			else if (topXPercent < .25)
				rating = 2;
			else if (topXPercent > .85)
				rating = 3;
			map.put(picks.get(i).getCandidate().getName(), new Integer(rating));
		}
		return map;
	}
	
	/**
	 * @return all possible picks, with resulting odds that blue wins
	 */
	public List<Pick> suggestions(DraftSession session) {
		return pruningAlgorithm(session);
	}

	/**
	 * @return a simplistic, non-predictive scoring
	 */
	public Double currentOddsForBlue(DraftSession session) {
		return scorePlusSynergy(session);
	}

	////////////////////////////
	//   Picking Algorithms   //
	////////////////////////////
	
	/**
	 * A tree-search algorithm. Attempts to coarsely evaluate all options 
	 * and then dive deeply into promising ones.
	 */
	private List<Pick> pruningAlgorithm(DraftSession session) {
		TreeNode current = new TreeNode(null, session);
		
		// fill out tree:
		iterate(current);
		
		List<Pick> picks = current.bestPicks().stream()
				.map(node -> new Pick(node.getLastPick(), node.odds()))
				.sorted()
				.collect(Collectors.toList());
		if (!session.currentPhase().isBlue())
			Collections.reverse(picks);
		return picks;
	}

	private void iterate(TreeNode current) {
		if (current.isFull())
			return; // all done!
		
		ArrayList<TreeNode> optimalAvenues = new ArrayList<>();
		
		DraftSession state = current.getState();
		for (Hero hero : state.currentPool()) {
			TreeNode node = new TreeNode(hero, state.whatIf(hero));
			current.addChild(node);
			optimalAvenues.add(node);
		}
		
		Collections.sort(optimalAvenues);
		optimalAvenues = current.bestPicks();
		
		double first = state.getFormat().size();
		double phase = state.currentPhaseNo();
//		int avenuesToExplore = (first - phase) * 2 / 3;
		int avenuesToExplore = phase/first < 0.5 ? 3 : 2; // 3 branches and reduce to 2 halfway
		
		for (int i = 0; i < optimalAvenues.size(); i++) {
			TreeNode avenue = optimalAvenues.get(i);
			if (i < avenuesToExplore)
				iterate(avenue);
			else // greedy fill
				avenue = new TreeNode(avenue.getLastPick(), greedyFill(avenue.getState()));
		}
	}

	/**
	 * Greedy algorithm. Assuming each possible pick, fill out the rest of the draft 
	 * with the greediest picks ({@link #greedyPick(us, them, pool)}) and score the result. 
	 */
	@SuppressWarnings("unused")
	private List<Pick> greedyAlgorithm(DraftSession session) {
		Set<Hero> pool = session.currentPool();
		List<Pick> picks = pool.stream().map(hero -> {
					DraftSession possible = greedyFill(session.whatIf(hero));
					return new Pick(hero, scorePlusSynergy(possible));
				})
				.sorted()
				.collect(Collectors.toList());
		if (!session.currentPhase().isBlue())
			Collections.reverse(picks);
		return picks;
	}

	/**
	 * Recursive algorithm. Explores every possible draft (heroes^phases in number) 
	 * and returns the absolute ordering of picks.
	 * <p>
	 * NOTE: assumes all parameters are properly in sync
	 */
	@SuppressWarnings("unused")
	private List<Pick> bruteForce(Roster blue, Roster red, Set<Hero> pool, Format format, int phase) {
		return pool.stream().map((hero) -> {
			if (blue.isFull() && red.isFull())
				return new Pick(hero, scorePlusSynergy(blue, red));
			
			Roster newBlue = blue;
			Roster newRed = red;
			if (format.get(phase).isPick()) {
				if (format.get(phase).isBlue())
					newBlue = blue.whatIf(hero);
				else
					newRed = red.whatIf(hero);
			}
			return bruteForce(newBlue, newRed, subPool(pool, hero), format, phase+1).get(0);
		})
		.sorted()
		.collect(Collectors.toList());
	}

	/**
	 * NOTE: intended to be identical to {@link #bruteForce(Roster, Roster, Set, Format, int)}, but more efficient
	 */
	@SuppressWarnings("unused")
	private List<Pick> bruteForce(MatchupSpecial matchup, Set<Hero> pool, Format format, int phase) {
		//		System.out.println("Listing options for Phase " + phase + ": " + format.get(phase == format.size() ? format.size() - 1 : phase));
		return pool.stream().map((hero) -> {
			if (matchup.isFull())
				return new Pick(hero, matchup.oddsForBlue());

			MatchupSpecial newMatchup = matchup;
			if (format.get(phase).isPick())
				newMatchup = newMatchup.whatIf(hero, format.get(phase).isBlue());
			return bruteForce(newMatchup, subPool(pool, hero), format, phase+1).get(0);
		})
		.sorted()
		.collect(Collectors.toList());
	}

	///////////////////////
	//   Scoring Tools   //
	///////////////////////
	
	Double score(Hero hero, Hero adversary) {
		return meta.get(hero, adversary, false);
	}
	
	private Double score(Hero hero, Roster enemyTeam) {
		if (enemyTeam.size() < 1)
			return new Double(.5);
		
		double score = 0.0;
		for (Hero enemy : enemyTeam)
			score += score(hero, enemy);
		return score / (double) enemyTeam.size();
		
		// equivalent:
//		return enemyTeam.getPicked().stream()
//				.mapToDouble((enemy) -> score(hero, enemy))
//				.average()
//				.getAsDouble();
	}
	
	/**
	 * intended for full rosters only // TODO
	 * does not fill rosters
	 */
	private Double score(Roster us, Roster them) {
		double score = 0.0;
		for (Hero hero : us.getPicked()) {
			for (Hero enemy : them.getPicked()) {
				score += score(hero, enemy);
			}
		}
		score /= (double) (us.size() * them.size());
		return score;
	}

	private Double synergy(Hero hero, Hero partner) {
		return meta.get(hero, partner, true);
	}
	
	private Double synergy(Hero hero, Roster team) {
		if (team.size() < 1)
			return new Double(.5);
		if (team.isFull())
			throw new IllegalArgumentException("Roster is already full");
		
		// logic effectively identical to synergy(Roster)
		double synergy = 0.0;
		for (Hero partner : team.getPicked())
			synergy += synergy(hero, partner);
		return synergy / (double) team.size();
	}
	
	private Double synergy(Roster team) {
		if (team.size() <= 1)
			new Double(.5);
		
		double synergy = 0.0;
		for (Hero hero : team.getPicked()) {
			for (Hero partner : team.getPicked()) {
				if (hero != partner)
					synergy += synergy(hero, partner); // TODO optimize by removing double access
			}
		}
		synergy /= (double) (team.size() * (team.size() - 1)); // nP2 should be the number of reads we did
		return synergy;
	}
	
	/**
	 * @return probability that we win assuming either we win or they do
	 */
	private Double synergyWar(Roster us, Roster them) {
		Double usSyn = synergy(us);
		Double themSyn = synergy(them);
		
		return new Double((usSyn * (1 - themSyn)) 
				/ (usSyn * (1 - themSyn) + (themSyn) * (1 - usSyn)));
	}
	
	/**
	 * @return score for blue 
	 * @see #scorePlusSynergy(Roster, Roster)
	 */
	public Double scorePlusSynergy(DraftSession session) {
		return scorePlusSynergy(session.getBlue(), session.getRed());
	}
	
	/**
	 * Does not fill rosters.
	 * <p>
	 * // TODO could add special weighting for a fight or a synergy
	 * 
	 * @return a simplistic, non-predictive scoring
	 */
	private Double scorePlusSynergy(Roster us, Roster them) {
		if (us.isEmpty() || them.isEmpty())
			return new Double(.5);
		
		Double score = score(us, them);
		double fights = (double) (us.size() + them.size());
		double fightWeight = fights; // can be adjusted
		
		Double usSynergy = synergyWar(us, them);
		double usPairs = (double) (us.size() * (us.size() - 1));
		double themPairs = (double) (them.size() * (them.size() - 1));
		double synergyWeight = usPairs+themPairs; // can be adjusted
		
		return ((fightWeight*score) + (synergyWeight*usSynergy)) 
				/ ((double) (fightWeight + synergyWeight));
	}
	
	/////////////////////////
	//   Filling Rosters   //
	/////////////////////////
	
	/**
	 * @return the given roster instance, modified to counter the given hero (but not for synergy)
	 */
	private Roster fillForCountering(Hero hero, Roster enemyTeam, Set<Hero> pool) {
		if (!enemyTeam.isFull())
			enemyTeam.fill(bestCounters(hero, enemyTeam.room(), pool));
		return enemyTeam;
	}
	
	/**
	 * does not modify given object instances
	 */
	private Double scoreAndFill(Hero hero, Roster enemyTeam, Set<Hero> pool) {
		return score(hero, fillForCountering(hero, enemyTeam.clone(), pool));
	}
	
	/**
	 * currently sub-optimal. first fill our roster for synergy, then fill theirs for countering
	 * <p>
	 * does not modify given object instances
	 */
	private Double scoreAndFill(Roster us, Roster them, Set<Hero> pool) {
		// fill our roster with best synergies
		Roster futureAllies = us;
		if (!us.isFull()) {
			List<Pick> bestPartners = bestPartners(us, us.fullSize() - us.size(), pool);
			futureAllies = us.clone().fill(bestPartners);
		}
		
		// TODO subPool-out allied picks
		
		// fill enemy roster with best counters to our roster
		Roster futureEnemyRoster = them;
		if (!them.isFull()) {
			List<Pick> bestCounters = bestCounters(futureAllies, them, them.fullSize() - them.size(), pool);
			futureEnemyRoster = them.clone().fill(bestCounters);
		}
		
		return scorePlusSynergy(futureAllies, futureEnemyRoster);
	}
	
	private DraftSession greedyFill(DraftSession sesh) {
		while (!sesh.isFull()) {
			// pick or defensive ban:
			Roster teamWithNextPickPhase = sesh.getStrategy() == PICK ? sesh.pickingTeam() : sesh.enemyTeam();
			Roster otherTeam = sesh.getStrategy() == PICK ? sesh.enemyTeam() : sesh.pickingTeam();
			Pick pick = greedyPick(teamWithNextPickPhase, otherTeam, sesh.currentPool());
			sesh.pickOrBan(pick.getCandidate());
		}
		return sesh;
	}
	
	///////////////////////
	//   Picking Tools   //
	///////////////////////
	
	/**
	 * Pick the {@code Hero} with the greatest combination of score against the enemy team
	 * and synergy with the ally team. No other calculation or prediction is involved.
	 */
	private Pick greedyPick(Roster us, Roster them, Set<Hero> pool) {
		return pool.stream()
				.map(hero -> new Pick(hero, (synergy(hero, us)*us.size() + score(hero, them)*them.size()) 
											/ (double) (us.size() + them.size())))
				.sorted()
				.collect(Collectors.toList())
				.get(0);
	}
	
	/**
	 * @return list of all possible hero picks, decreasing in strength
	 */
	private List<Pick> optimalNextPicks(Roster pickingTeam, Roster enemyRoster, Set<Hero> pool) {
//		System.out.println("Calculating optimal picks for situation: " + pickingTeam + " vs " + enemyRoster);
		return pool.stream()
				// score our roster's strength assuming we pick the hero:
				.map(hero -> new Pick(hero, 
						scoreAndFill(pickingTeam.whatIf(hero), enemyRoster, subPool(pool, hero))))
				.sorted()
				.collect(Collectors.toList());
	}
	
	@SuppressWarnings("unused")
	private List<Pick> optimalOffensiveBan(Roster banningTeam, Roster enemyTeam, Set<Hero> pool) {
		List<Pick> bestBans = new ArrayList<>();
		
		for (Hero ban : pool) {
			// "if we ban this..."
			List<Pick> picks = optimalNextPicks(banningTeam, enemyTeam, subPool(pool, ban));
			Double futureHeroScore = picks.get(1).getScore(); // "...then our next hero is this good"
			bestBans.add(new Pick(ban, futureHeroScore));
		}
		Collections.sort(bestBans);
		return bestBans;
	}

	/**
	 * @return list of N best counters to a hero, decreasing in strength
	 */
	private List<Pick> bestCounters(Hero hero, int nBest, Set<Hero> pool) {
		// list ordered decreasing by hero's strength against other heroes
		List<Pick> counters = meta.get(hero, false).subList(0, nBest);
		
		// reverse to prioritize other heroes' strength against the given hero
		Collections.reverse(counters);
		
		// remove heroes not in the current pool
		counters.retainAll(pool);
		
		return counters;
	}
	
	/**
	 * @return list of N best counters to a roster, decreasing in strength
	 */
	private List<Pick> bestCounters(Roster countered, Roster countering, int nBest, Set<Hero> pool) {
		// TODO use countering roster in calculation
		return pool.stream()
				.map(counter -> new Pick(counter, scoreAndFill(counter, countered, subPool(pool, counter))))
				.sorted()
				.collect(Collectors.toList());
	}
	
	@SuppressWarnings("unused")
	private List<Pick> bestPartners(Hero hero, int nBest, Set<Hero> pool) {
		List<Pick> partners = meta.get(hero, true).subList(0, nBest);
		partners.retainAll(pool);
		return partners;
	}
	
	private List<Pick> bestPartners(Roster allies, int nBest, Set<Hero> pool) {
		return pool.stream()
				.map(partner -> new Pick(partner, synergy(allies.whatIf(partner))))
				.sorted()
				.collect(Collectors.toList())
				.subList(0, nBest);
	}
	
	////////////////
	//   Utility  //
	////////////////
	
	private Set<Hero> subPool(Set<Hero> pool, Hero toRemove) {
		Set<Hero> subPool = new HashSet<Hero>(pool);
		subPool.remove(toRemove);
		return subPool;
	}

	/**
	 * Essentially a container for a particular {@link DraftSession} state. 
	 * Used to build a tree for the pruning algorithm: {@link Calculator#pruningAlgorithm(DraftSession)}.
	 */
	private class TreeNode implements Comparable<TreeNode> {
			private Hero lastPick;
			private DraftSession state;
			private Double currentOdds;
			private ArrayList<TreeNode> children;
			
			public TreeNode(Hero lastPick, DraftSession state) {
				this.lastPick = lastPick;
				this.state = state;
				this.currentOdds = scoreAndFill(state.getBlue(), state.getRed(), state.currentPool());
				this.children = new ArrayList<>();
			}
			
			public Double odds() {
				return children.isEmpty() ? currentOdds 
						: Math.max(currentOdds, bestPicks().get(0).odds());
			}
			
			public Hero getLastPick() { return lastPick; }
			public DraftSession getState() { return state; }
			public ArrayList<TreeNode> bestPicks() {
				Collections.sort(children);
				return children;
			}
			
			public void addChild(TreeNode child) {
				children.add(child);
			}
			
			public boolean isFull() {
				return state.isFull();
			}
	
			@Override
			public int compareTo(TreeNode o) {
				// "higher" scores come "first" -> (.70, .65, .55, ...)
				return o.odds().compareTo(odds());
			}
		}
	
	///////////////////////////
	//   WORK IN PROGRESS   ///
	///////////////////////////
	
	
}
