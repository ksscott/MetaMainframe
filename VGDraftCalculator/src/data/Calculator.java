package data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToDoubleFunction;

import model.Edge;
import model.Node;
import draft.DraftSession;
import draft.Roster;

public class Calculator {

	private final HeroMatrix meta;
	private static final Double fiftyFifty = new Double(.5);
	
	public Calculator(HeroMatrix meta) {
		this.meta = meta;
	}

	///////////////////////
	//   Scoring Tools   //
	///////////////////////
	
	// NOTE:
	// All scoring tools herein are simple and score AS-IS
	// That means no predictive algorithms or roster-filling are employed
	
	private Double score(Hero hero, Hero adversary) {
		return meta.get(hero, adversary, false);
	}
	
	public Double score(Hero hero, Roster enemyTeam) {
		return geoMean(enemyTeam.getPicked(), enemy -> score(hero, enemy));
	}
	
	private Double score(Roster us, Roster them) {
		return geoMean(us.getPicked(), h -> score(h, them));
	}

	private Double synergy(Hero hero, Hero partner) {
		return meta.get(hero, partner, true);
	}
	
	public Double synergy(Hero hero, Roster team) {
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
	public Double scorePlusSynergy(DraftSession session) {
		return scorePlusSynergy(session.getBlue(), session.getRed());
	}
	
	 // TODO could add special weighting for a fight or a synergy
	public Double scorePlusSynergy(Roster us, Roster them) {
		if (us.isEmpty() || them.isEmpty())
			return fiftyFifty;
		
		Double score = score(us, them);
		double fights = (double) (us.size() + them.size());
		double fightWeight = fights; // can be adjusted
		
		Double usSynergy = synergyWar(us, them);
		double usPairs = (double) (us.size() * (us.size() - 1));
		double themPairs = (double) (them.size() * (them.size() - 1));
		double synergyWeight = 2*(usPairs+themPairs); // can be adjusted
		
		return Math.pow(((Math.pow(score, fightWeight)) * (Math.pow(usSynergy, synergyWeight))),
				1 / ((double) (fightWeight + synergyWeight)));
	}
	
	/**
	 * @return the marginal score of adding a hero to our roster
	 */
	public Double marginalScore(Hero heroForUs, Roster us, Roster them, Set<Hero> pool) {
		Double synergy = synergy(heroForUs, us);
		Double score = score(heroForUs, them);
		return Math.pow(Math.pow(synergy, us.size()) * Math.pow(score, 2*them.size()) 
				, 1 / (double) (us.size() + them.size()));
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
	
	@SuppressWarnings("unused")
	private Double scoreAndFill(Hero hero, Roster enemyTeam, Set<Hero> pool) {
		return score(hero, fillForCountering(hero, enemyTeam.clone(), pool));
	}
	
	/**
	 * currently sub-optimal. first fill our roster, then fill theirs
	 * <p>
	 * does not modify given object instances
	 */
	@SuppressWarnings("unused")
	private Double scoreAndFill(Roster us, Roster them, Set<Hero> pool) {
		// fill our roster first
		Roster futureAllies = us;
		if (!us.isFull()) {
			List<List<Pick>> lists = new ArrayList<>();
			lists.add(bestPartners(us, us.room(), pool));
			lists.add(bestCounters(them, us, us.room(), pool));
			futureAllies = us.clone().fill(blend(lists));
		}
		
		Set<Hero> subPool = new HashSet<>(pool);
		for (Hero hero : us)
			subPool = subPool(subPool, hero);
		
		// fill enemy roster after
		Roster futureEnemyRoster = them;
		if (!them.isFull()) {
			List<List<Pick>> lists = new ArrayList<>();
			lists.add(bestPartners(them, them.room(), subPool));
			lists.add(bestCounters(futureAllies, them, them.room(), subPool));
			futureEnemyRoster = them.clone().fill(blend(lists));
		}
		
		return scorePlusSynergy(futureAllies, futureEnemyRoster);
	}
	
	///////////////////////
	//   Picking Tools   //
	///////////////////////
	
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
	
	public Set<Node> counters(Roster countered, Roster countering, Set<Hero> pool) {
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
	
	public Set<Node> partners(Roster allies, Set<Hero> pool) {
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
	
	public static Set<Hero> subPool(Set<Hero> pool, Hero toRemove) {
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
	 * measured by the given hero's strength against their <b>single</b> best counter
	 */
	@SuppressWarnings("unused")
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
	@SuppressWarnings("unused")
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

}
