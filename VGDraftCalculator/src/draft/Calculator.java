package draft;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

import static draft.Strategy.*;

public class Calculator {

	private final HeroMatrix meta;
	private static final Double fiftyFifty = new Double(.5);
	
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
		List<Pick> suggestions = suggestions(session);
		// old "percentiles" values:
//		List<Pick> picks = suggestions;
//		for (int i = 0; i < picks.size(); i++) {
//			int rating = 0;
//			double topXPercent = (i+1) / (double) picks.size();
//			if (topXPercent < .1)
//				rating = 1;
//			else if (topXPercent < .25)
//				rating = 2;
//			else if (topXPercent > .85)
//				rating = 3;
//			map.put(picks.get(i).getCandidate().getName(), new Integer(rating));
//		}
		// new "percent from worst to best" rank (1-10)
		Double max = suggestions.get(0).getScore();
		Double min = suggestions.get(suggestions.size()-1).getScore();
		Double inc = (max - min) / 10.0;
		for (Pick pick : suggestions) {
			Double score = pick.getScore();
			int rank = (int) (10 - ((max - score) / inc));
			rank = rank == 0 ? 1 : rank;
			map.put(pick.getCandidate().getName(), rank);
		}
		map.put("odds", (int) (suggestions.get(0).getScore()*100) + 1);
		return map;
	}
	
	/**
	 * @return all possible picks, with resulting odds that blue wins
	 */
	public List<Pick> suggestions(DraftSession session) {
//		return pruningAlgorithm(session);
		return priorityAlgorithm(session);
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
		Set<Node> partners = partners(pickingTeam, pool);
		Set<Node> counters = counters(enemyTeam, pickingTeam, pool);
		Set<Node> nodes = blendNodes(partners, counters);
		
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
		
//		double first = state.getFormat().size();
//		double phase = state.currentPhaseNo();
//		int avenuesToExplore = (first - phase) * 2 / 3;
//		int avenuesToExplore = phase/first < 0.5 ? 3 : 2; // 3 branches and reduce to 2 halfway
		int avenuesToExplore = 0;
		
		for (int i = 0; i < optimalAvenues.size(); i++) {
			TreeNode avenue = optimalAvenues.get(i);
			if (i < avenuesToExplore)
				iterate(avenue);
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

	///////////////////////
	//   Scoring Tools   //
	///////////////////////
	
	// NOTE:
	// All scoring tools herein are simple and score AS-IS
	// That means no predictive algorithms or roster-filling are employed
	
	private Double score(Hero hero, Hero adversary) {
		return meta.get(hero, adversary, false);
	}
	
	private Double score(Hero hero, Roster enemyTeam) {
		return geoMean(enemyTeam.getPicked(), enemy -> score(hero, enemy));
	}
	
	private Double score(Roster us, Roster them) {
		return geoMean(us.getPicked(), h -> score(h, them));
	}

	private Double synergy(Hero hero, Hero partner) {
		return meta.get(hero, partner, true);
	}
	
	private Double synergy(Hero hero, Roster team) {
//		if (team.isFull())
//			throw new IllegalArgumentException("Roster is already full");
		
		return geoMean(team.getPicked(), partner -> synergy(hero, partner));
	}
	
	private Double synergy(Roster team) {
		return geoMean(team.getPicked(), hero -> synergy(hero, team));
	}
	
	/**
	 * @return probability that we win assuming either we win or they do
	 */
	private Double synergyWar(Roster us, Roster them) {
		Double usSyn = synergy(us);
		Double themSyn = synergy(them);
		
		// assuming independent probabilities blue and red, this equation gives P(blue | blue xor red)
		return new Double((usSyn * (1 - themSyn)) 
				/ (double) (usSyn * (1 - themSyn) + (themSyn) * (1 - usSyn)));
	}
	
	/**
	 * @return score for blue 
	 * @see #scorePlusSynergy(Roster, Roster)
	 */
	private Double scorePlusSynergy(DraftSession session) {
		return scorePlusSynergy(session.getBlue(), session.getRed());
	}
	
	 // TODO could add special weighting for a fight or a synergy
	private Double scorePlusSynergy(Roster us, Roster them) {
		if (us.isEmpty() || them.isEmpty())
			return fiftyFifty;
		
		Double score = score(us, them);
		double fights = (double) (us.size() + them.size());
		double fightWeight = fights; // can be adjusted
		
		Double usSynergy = synergyWar(us, them);
		double usPairs = (double) (us.size() * (us.size() - 1));
		double themPairs = (double) (them.size() * (them.size() - 1));
		double synergyWeight = usPairs+themPairs; // can be adjusted
		
		return Math.pow(((Math.pow(score, fightWeight)) * (Math.pow(usSynergy, synergyWeight))),
				1 / ((double) (fightWeight + synergyWeight)));
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
	
	/**
	 * uses greedy fill
	 * <p>
	 * does not modify given object instances
	 */
	private Double scoreAndFill(DraftSession session) {
		DraftSession newSession = session.clone();
		newSession = greedyFill(newSession);
		return scorePlusSynergy(newSession);
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
	 * <p>
	 * This looks a bit messy currently. I will consider cleaning it in the future.
	 */
	private Pick greedyPick(Roster us, Roster them, Set<Hero> pool) {
		return greedyPicks(us, them, pool).get(0);
	}
	
	private List<Pick> greedyPicks(Roster us, Roster them, Set<Hero> pool) {
		return pool.stream()
				.map(hero -> new Pick(hero, Math.pow(Math.pow(synergy(hero, us), us.size()) * Math.pow(score(hero, them), them.size()) 
											, 1 / (double) (us.size() + them.size()))))
				.sorted()
				.collect(Collectors.toList());
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
		List<Pick> counters = meta.get(hero, false);
		
		// reverse to prioritize other heroes' strength against the given hero
		Collections.reverse(counters.subList(0, nBest));
		
		// remove heroes not in the current pool
		Set<Pick> toRemove = new HashSet<>();
		for (Pick pick : counters) {
			if (!pool.contains(pick.getCandidate()))
				toRemove.add(pick);
		}
		counters.removeAll(toRemove);
		
		return counters;
	}
	
	/**
	 * @return list of N best counters to a roster, decreasing in strength
	 */
	private List<Pick> bestCounters(Roster countered, Roster countering, int nBest, Set<Hero> pool) {
		List<List<Pick>> lists = new ArrayList<>();
		for (Hero enemy : countered)
			lists.add(bestCounters(enemy, pool.size(), pool));
		return blend(lists);
		
		// TODO use countering roster in calculation
//		return pool.stream()
//				.map(counter -> new Pick(counter, scoreAndFill(counter, countered, subPool(pool, counter))))
//				.sorted()
//				.collect(Collectors.toList());
	}
	
	private Set<Node> counters(Roster countered, Roster countering, Set<Hero> pool) {
		Set<Node> options = new HashSet<>();
		for (Hero enemy : countered) {
			Node to = new Node(enemy);
			for (Hero hero : pool) {
				Node node = new Node(hero);
				Edge edge = new Edge(node, to, score(hero, enemy), false);
				node.addEdge(edge, true);
				options.add(node);
			}
		}
		return options;
	}
	
	private List<Pick> bestPartners(Hero hero, int nBest, Set<Hero> pool) {
		List<Pick> partners = meta.get(hero, true).subList(0, nBest);
		Set<Pick> toRemove = new HashSet<>();
		for (Pick pick : partners) {
			if (!pool.contains(pick.getCandidate()))
				toRemove.add(pick);
		}
		partners.removeAll(toRemove);
		return partners;
	}
	
	private List<Pick> bestPartners(Roster allies, int nBest, Set<Hero> pool) {
		List<List<Pick>> lists = new ArrayList<>();
		for (Hero ally : allies) {
			lists.add(bestPartners(ally, pool.size(), pool));
		}
		return blend(lists);
		
//		return pool.stream()
//				.map(partner -> new Pick(partner, synergy(allies.whatIf(partner))))
//				.sorted()
//				.collect(Collectors.toList())
//				.subList(0, nBest);
	}
	
	private Set<Node> partners(Roster allies, Set<Hero> pool) {
		Set<Node> options = new HashSet<>();
		for (Hero ally : allies) {
			Node to = new Node(ally);
			for (Hero hero : pool) {
				Node node = new Node(hero);
				Edge edge = new Edge(node, to, synergy(hero, ally), true);
				node.addEdge(edge, true);
				options.add(node);
			}
		}
		return options;
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
	 * @return all lists merged into the first, with each pick of the same hero blended with {@link Pick#blend(Pick)}
	 */
	private List<Pick> blend(List<List<Pick>> options) {
		List<Pick> result = new ArrayList<>();
		if (options == null || options.isEmpty())
			return result;
		if (options.size() == 1) {
			result.addAll(options.get(0));
			return result;
		}
		
		// ok, let's try this a dumb way, and maybe a better solution will occur to me later
		for (Pick pick : options.get(0)) {
			// for each pick in the first list,
			for (int i=1; i<options.size(); i++) {
				// iterate through the other lists,
				for (Pick other : options.get(i)) {
					// and check if they contain a pick matching ours
					if (pick.getCandidate() == other.getCandidate()) {
						result.add(pick.blend(other));
						break;
					}
				}
			}
		}
		Collections.sort(result);
		return result;
	}
	
	/**
	 * @return all lists merged into the first, with each node of the same hero absorbed with {@link Node#absorb(Node)}
	 */
	private Set<Node> blendNodes(Set<Node>... options) {
		Set<Node> result = new HashSet<>();
		if (options == null || options.length == 0)
			return result;
		result.addAll(options[0]);
		if (options.length == 1) {
			return result;
		}
		
		// again, the dumb way. I'll fix it later
		for (Node node : result) {
			// for each node in the first list,
			for (int i=1; i<options.length; i++) {
				// iterate through the other lists,
				for (Node other : options[i]) {
					// and check if they contain a node matching ours
					if (node.hero() == other.hero()) {
						node.absorb(other);
						break;
					}
				}
			}
		}
		return result;
	}
	
	/**
	 * measured by the given hero's strength against their <b>single</b> best counter
	 */
	private List<Pick> leastCounterable(Set<Hero> pool) {
		List<Pick> picks = new ArrayList<>();
		
		for (Hero hero : pool) {
			List<Pick> counters = bestCounters(hero, pool.size() - 1, subPool(pool, hero));
			picks.add(new Pick(hero, 1 - counters.get(0).getScore()));
			// TODO pick some dynamic number of counters to average together
		}
		Collections.sort(picks);
		
		return picks;
	}
	
	/**
	 * measured by the given hero's strength with their <b>single</b> best partner
	 */
	private List<Pick> mostSynergizable(Set<Hero> pool) {
		List<Pick> picks = new ArrayList<>();
		
		for (Hero hero : pool) {
			List<Pick> counters = bestPartners(hero, pool.size() - 1, subPool(pool, hero));
			picks.add(new Pick(hero, 1 - counters.get(0).getScore()));
			// TODO pick some dynamic number of partners to average together
		}
		Collections.sort(picks);
		
		return picks;
	}

	/**
	 * Arithmetic Mean
	 * 
	 * @param heroes to be measured
	 * @param scorer a means to measure each hero
	 * @return average score, or 0.5 if given an empty list
	 */
	@SuppressWarnings("unused")
	private Double arithMean(Collection<Hero> heroes, ToDoubleFunction<Hero> scorer) {
		if (heroes.size() < 1)
			return fiftyFifty;
		
		double score = 0.0;
		for (Hero hero : heroes)
			score += scorer.applyAsDouble(hero);
		return score / (double) heroes.size();
	}
	
	/**
	 * Geometric Mean
	 * <p>
	 * This averaging method should be chosen for averaging probabilities
	 * 
	 * @param heroes to be measured
	 * @param scorer a means to measure each hero
	 * @return geometric average of the scores, or 0.5 if given an empty list
	 */
	private Double geoMean(Collection<Hero> heroes, ToDoubleFunction<Hero> scorer) {
		if (heroes.size() < 1)
			return fiftyFifty;
		
		double score = 1.0;
		for (Hero hero : heroes)
			score *= scorer.applyAsDouble(hero);
		return Math.pow(score, (1 / (double) heroes.size()));
	}
	
	/**
	 * I plan to use this later for optimization purposes (passing around data points)
	 */
	@SuppressWarnings("unused")
	private Double geoMean(Collection<Double> scores) {
		if (scores.size() < 1)
			return fiftyFifty;
		
		double result = 1.0;
		for (Double score : scores)
			result *= score;
		return Math.pow(result, (1 / (double) scores.size()));
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
				this.currentOdds = scoreAndFill(state);
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
