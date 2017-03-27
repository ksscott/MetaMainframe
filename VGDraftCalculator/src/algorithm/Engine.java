package algorithm;

import static draft.Strategy.PICK;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import model.Node;
import data.Calculator;
import data.Hero;
import data.Pick;
import draft.DraftSession;
import draft.Format;
import draft.Roster;
import draft.Strategy;

public class Engine {
	
	private Calculator scorer;
	
	public Engine(Calculator calculator) {
		this.scorer = calculator;
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
		double currentOdds;
		if (!session.isFull()) {
			List<Pick> suggestions = suggestions(session);

			// new "percent from worst to best" rank (1-10)
			Double max = suggestions.get(0).getScore();
			Double min = suggestions.get(suggestions.size()-1).getScore();
			Double inc = (max - min) / 10.0;
			for (Pick pick : suggestions) {
				Double score = pick.getScore();
				int rank = (int) (10 - ((max - score) / inc));
				rank = rank <= 0 ? 1 : rank;
				map.put(pick.getCandidate().getName(), rank);
			}
			currentOdds = suggestions.get(0).getScore();
		} else {
			currentOdds = scorer.scorePlusSynergy(session);
		}
		map.put("odds", (int) (currentOdds * 100));
		return map;
	}
	
	/**
	* @return all possible picks, with resulting odds that blue wins
	*/
	public List<Pick> suggestions(DraftSession session) {
		return pruningAlgorithm(session);
//		return greedyAlgorithm(session);
		//return priorityAlgorithm(session);
	}
	
	/**
	* @return a simplistic, non-predictive scoring
	*/
	public Double currentOddsForBlue(DraftSession session) {
		return scorer.scorePlusSynergy(session);
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
		iterate(current, 0);
		
		List<Pick> picks = current.bestPicks().stream()
				.map(node -> new Pick(node.getLastPick(), node.odds()))
				.sorted()
				.collect(Collectors.toList());
		if (!session.currentPhase().isBlue())
			Collections.reverse(picks);
		return picks;
	}

	private void iterate(TreeNode current, int iterations) {
		if (current.isFull())
			return; // all done!
		
		
		DraftSession state = current.getState();
		for (Hero hero : state.currentPool()) {
			TreeNode node = new TreeNode(hero, state.whatIf(hero));
			current.addChild(node);
		}
		
		List<TreeNode> optimalAvenues = current.bestPicks();
		if (!state.currentPhase().isBlue())
			Collections.reverse(optimalAvenues);
		
		// some magic to approximate a feasible runtime
		int heroes = Hero.values().length;
		int phases = state.getFormat().size();
		int phase = state.currentPhaseNo();
//		int remaining = phases - phase - 1;
		int avenuesToExplore = (heroes*1/2 - phases*1 + phase*4) / (1 + iterations*4);
		
		for (int i = 0; i < optimalAvenues.size(); i++) {
			TreeNode avenue = optimalAvenues.get(i);
			if (i < avenuesToExplore)
				iterate(avenue, iterations+1);
//			else // greedy fill
//				avenue = new TreeNode(avenue.getLastPick(), greedyFill(avenue.getState()));
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
					return new Pick(hero, fillAndScore(session.whatIf(hero)));
				})
				.sorted()
				.collect(Collectors.toList());
		if (!session.currentPhase().isBlue())
			Collections.reverse(picks);
		return picks;
	}

	@SuppressWarnings("unused")
	private List<Pick> priorityAlgorithm(DraftSession sesh) {
		// picks or defensive bans:
		Roster teamWithNextPickPhase = sesh.getStrategy() == PICK ? sesh.pickingTeam() : sesh.enemyTeam();
		Roster otherTeam = sesh.getStrategy() == PICK ? sesh.enemyTeam() : sesh.pickingTeam();
		Set<Hero> pool = sesh.currentPool();
		
		Set<Node> pickingPriorities = priorities(teamWithNextPickPhase, otherTeam, pool);
		Set<Node> enemyPriorities = priorities(otherTeam, teamWithNextPickPhase, pool);
		
		// FIXME this algorithm is half baked, and I'm not sure if it's going anywhere in its current form
		
		List<Pick> list = new ArrayList<Pick>();
		
		for (Node node : pickingPriorities)
			list.add(new Pick(node.hero(), node.score()));
		
		return list;
	}
	
	private Set<Node> priorities(Roster pickingTeam, Roster enemyTeam, Set<Hero> pool) {
		Set<Node> partners = scorer.partners(pickingTeam, pool);
		Set<Node> counters = scorer.counters(enemyTeam, pickingTeam, pool);
		Set<Node> nodes = Node.blendNodes(partners, counters);
		
		return nodes;
		
		// XXX I don't really like this algorithm right now; it doesn't pay attention to draft format at all
//		List<List<Pick>> lists = new ArrayList<>();
		
//		// rank each potential partner for synergy
//		lists.add(bestPartners(pickingTeam, pool.size() / 2, pool)); // XXX what if ban?
//		// rank each potential partner for versus
//		lists.add(bestCounters(enemyTeam, pickingTeam, pool.size() / 2, pool)); // XXX what if ban?
//		// rank each potential partner for future strength
//		lists.add(leastCounterable(pool));
//		lists.add(mostSynergizable(pool));
//		
//		return blend(lists);
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
				return new Pick(hero, scorer.scorePlusSynergy(blue, red));
			
			Roster newBlue = blue;
			Roster newRed = red;
			if (format.get(phase).isPick()) {
				if (format.get(phase).isBlue())
					newBlue = blue.whatIf(hero);
				else
					newRed = red.whatIf(hero);
			}
			return bruteForce(newBlue, newRed, Calculator.subPool(pool, hero), format, phase+1).get(0);
		})
		.sorted()
		.collect(Collectors.toList());
	}

	/**
	 * NOTE: intended to be identical to {@link #bruteForce(Roster, Roster, Set, Format, int)}, but more efficient
	 */
//	@SuppressWarnings("unused")
//	private List<Pick> bruteForce(MatchupSpecial matchup, Set<Hero> pool, Format format, int phase) {
//		//		System.out.println("Listing options for Phase " + phase + ": " + format.get(phase == format.size() ? format.size() - 1 : phase));
//		return pool.stream().map((hero) -> {
//			if (matchup.isFull())
//				return new Pick(hero, matchup.oddsForBlue());
//
//			MatchupSpecial newMatchup = matchup;
//			if (format.get(phase).isPick())
//				newMatchup = newMatchup.whatIf(hero, format.get(phase).isBlue());
//			return bruteForce(newMatchup, subPool(pool, hero), format, phase+1).get(0);
//		})
//		.sorted()
//		.collect(Collectors.toList());
//	}
	
	////////////////////////////
	//   Picking Algorithms   //
	////////////////////////////
	
	private List<Pick> optimalNextSelections(DraftSession session) {
		Strategy strategy = session.getStrategy();
		switch (strategy) {
			default:
			case PICK:
				return optimalNextPicks(session.pickingTeam(), session.enemyTeam(), session.currentPool());
			case DEFENSIVE_BAN:
				return optimalNextPicks(session.enemyTeam(), session.pickingTeam(), session.currentPool());
			case OFFENSIVE_BAN:
				return optimalOffensiveBan(session.pickingTeam(), session.enemyTeam(), session.currentPool());
		}
	}
	
	/**
	 * Currently uses a greedy algorithm to determine optimal next picks
	 * 
	 * @return list of all possible hero picks, decreasing in strength
	 */
	private List<Pick> optimalNextPicks(Roster pickingTeam, Roster enemyTeam, Set<Hero> pool) {
//		return greedyPick(pickingTeam, enemyTeam, pool);
		
		// for each possibility, evaluate the marginal value added to our team
		return pool.stream()
				.map(hero -> new Pick(hero, scorer.marginalScore(hero, pickingTeam, enemyTeam, pool)))
				.sorted()
				.collect(Collectors.toList());
		
		// the previously less efficient way of evaluating the next state in full (rather than marginally)
//				.map(hero -> new Pick(hero, scoreAndFill(pickingTeam.whatIf(hero), enemyRoster, subPool(pool, hero))))
	}
	
//	private List<Pick> greedyPick(Roster us, Roster them, Set<Hero> pool) {
//		return pool.stream()
//				.map(hero -> new Pick(hero, Math.pow(Math.pow(calculator.synergy(hero, us), us.size()) * Math.pow(calculator.score(hero, them), them.size()) 
//											, 1 / (double) (us.size() + them.size()))))
//				.sorted()
//				.collect(Collectors.toList());
//	}
	
	private List<Pick> optimalOffensiveBan(Roster banningTeam, Roster enemyTeam, Set<Hero> pool) {
		List<Pick> bestBans = new ArrayList<>();
		
		for (Hero ban : pool) {
			// "if we ban this..."
			List<Pick> picks = optimalNextPicks(banningTeam, enemyTeam, Calculator.subPool(pool, ban));
			Double futureHeroScore = picks.get(1).getScore(); // "...then our next hero is the second best remaining hero"
			bestBans.add(new Pick(ban, futureHeroScore));
		}
		Collections.sort(bestBans);
		return bestBans;
	}
	
	/////////////////
	//   Utility   //
	/////////////////
	
	/**
	 * uses greedy fill
	 * <p>
	 * does not modify given object instances
	 */
	public Double fillAndScore(DraftSession session) {
		DraftSession newSession = session.clone();
		newSession = greedyFill(newSession);
		return scorer.scorePlusSynergy(newSession);
	}
	
	private DraftSession greedyFill(DraftSession sesh) {
		while (!sesh.isFull()) {
			sesh.pickOrBan(optimalNextSelections(sesh).get(0).getCandidate());
		}
		return sesh;
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
//				this.currentOdds = scoreAndFill(state.getBlue(), state.getRed(), state.currentPool());
				this.currentOdds = fillAndScore(state);
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
	
}
