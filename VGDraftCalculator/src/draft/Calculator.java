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

	private HeroMatrix meta;
	
	public Calculator(HeroMatrix meta) {
		this.meta = meta;
	}
	
	public Double score(Hero hero, Hero adversary) {
		return meta.get(hero, adversary, false);
	}
	
	private Double score(Hero hero, Roster enemyTeam) {
		if (enemyTeam.size() < 1)
			return new Double(.5);
		
		double score = 0.0;
		for (Hero enemy : enemyTeam)
			score += score(hero, enemy);
		return score / (double) enemyTeam.size();
	}
	
	private Double synergy(Hero hero, Hero partner) {
		return meta.get(hero, partner, true);
	}
	
	private Double synergy(Hero hero, Roster team) {
		if (team.size() < 1)
			return new Double(.5);
		
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
	
	public List<Pick> suggestions(DraftSession session) {
		return pruningAlgorithm(session);
	}
	
	/**
	 * A tree-search algorithm. Attempts to coarsely evaluate all options 
	 * and then dive deeply into promising ones.
	 */
	public List<Pick> pruningAlgorithm(DraftSession session) {
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
	
	public void iterate(TreeNode current) {
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
		
		int first = state.getFormat().size();
		int phase = state.currentPhaseNo();
		int avenuesToExplore = (first - phase) * 2 / 3;
		
		for (int i = 0; i < optimalAvenues.size(); i++) {
			TreeNode avenue = optimalAvenues.get(i);
			if (i < avenuesToExplore)
				iterate(avenue);
			else // greedy fill
				avenue = new TreeNode(avenue.getLastPick(), greedyFill(avenue.getState()));
		}
	}
	
	/**
	 * assumes rosters and format are properly in sync
	 * 
	 * NOTE: intended to be identical to other "options" method, but more efficient
	 */
	public List<Pick> options(MatchupSpecial matchup, Set<Hero> pool, Format format, int phase) {
//		System.out.println("Listing options for Phase " + phase + ": " + format.get(phase == format.size() ? format.size() - 1 : phase));
		return pool.stream().map((hero) -> {
			if (matchup.isFull())
				return new Pick(hero, matchup.oddsForBlue());
			
			MatchupSpecial newMatchup = matchup;
			if (format.get(phase).isPick())
				newMatchup = newMatchup.whatIf(hero, format.get(phase).isBlue());
			return options(newMatchup, subPool(pool, hero), format, phase+1).get(0);
		})
		.sorted()
		.collect(Collectors.toList());
	}
	
	/**
	 * assumes rosters and format are properly in sync
	 */
	public List<Pick> options(Roster blue, Roster red, Set<Hero> pool, Format format, int phase) {
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
			return options(newBlue, newRed, subPool(pool, hero), format, phase+1).get(0);
		})
		.sorted()
		.collect(Collectors.toList());
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
	
	/**
	 * does not fill rosters
	 * // TODO could add special weighting for a fight or a synergy
	 */
	public Double scorePlusSynergy(Roster us, Roster them) {
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
	
	/**
	 * @return probability that we win assuming either we win or they do
	 */
	private Double synergyWar(Roster us, Roster them) {
		Double usSyn = synergy(us);
		Double themSyn = synergy(them);
		
		return new Double((usSyn * (1 - themSyn)) 
				/ (usSyn * (1 - themSyn) + (themSyn) * (1 - usSyn)));
	}
	
	private Double scoreAndFill(Hero hero, Roster enemyTeam, Set<Hero> pool) {
		Roster futureEnemyRoster = enemyTeam;
		if (!futureEnemyRoster.isFull())
			futureEnemyRoster = enemyTeam.clone().fill(bestCounters(hero, enemyTeam.fullSize() - enemyTeam.size(), pool));
		
		return futureEnemyRoster.getPicked().stream()
				.mapToDouble((enemy) -> score(hero, enemy))
				.average()
				.getAsDouble();
	}

	/**
	 * currently sub-optimal. first fill our roster for synergy, then fill theirs for countering
	 */
	public Double scoreAndFill(Roster us, Roster them, Set<Hero> pool) {
		// fill our roster with best synergies
		Roster futureAllies = us;
		if (!us.isFull()) {
			List<Pick> bestPartners = bestPartners(us, us.fullSize() - us.size(), pool);
			futureAllies = us.clone().fill(bestPartners);
		}
		
		// fill enemy roster with best counters to our roster
		Roster futureEnemyRoster = them;
		if (!them.isFull()) {
			List<Pick> bestCounters = bestCounters(futureAllies, them, them.fullSize() - them.size(), pool);
			futureEnemyRoster = them.clone().fill(bestCounters);
		}
		
		return scorePlusSynergy(futureAllies, futureEnemyRoster);
	}
	
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
	 * Greedy algorithm. Assuming each possible pick, fill out the rest of the draft 
	 * with the greediest picks ({@link #greedyPick(us, them, pool)}) and score the result. 
	 */
	public List<Pick> greedySuggest(DraftSession session) {
		Set<Hero> pool = session.currentPool();
		List<Pick> picks = pool.stream().map(hero -> {
					DraftSession possible = greedyFill(session.whatIf(hero));
					return new Pick(hero, scorePlusSynergy(possible.getBlue(), possible.getRed()));
				})
				.sorted()
				.collect(Collectors.toList());
		if (!session.currentPhase().isBlue())
			Collections.reverse(picks);
		return picks;
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
	public List<Pick> optimalNextPicks(Roster pickingTeam, Roster enemyRoster, Set<Hero> pool) {
//		System.out.println("Calculating optimal picks for situation: " + pickingTeam + " vs " + enemyRoster);
		return pool.stream()
				// score our roster's strength assuming we pick the hero:
				.map(hero -> new Pick(hero, 
						scoreAndFill(pickingTeam.whatIf(hero), enemyRoster, subPool(pool, hero))))
				.sorted()
				.collect(Collectors.toList());
	}
	
	/**
	 * @param enemyTeam
	 * @return
	 */
	public List<Pick> optimalOffensiveBan(Roster banningTeam, Roster enemyTeam, Set<Hero> pool) {
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

	private Set<Hero> subPool(Set<Hero> pool, Hero toRemove) {
		Set<Hero> subPool = new HashSet<Hero>(pool);
		subPool.remove(toRemove);
		return subPool;
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
}
