package draft;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Calculator {

	private HeroMatrix meta;
	
	public Calculator(HeroMatrix meta) {
		this.meta = meta;
	}
	
	public Double score(Hero hero, Hero adversary) {
		return meta.get(hero, adversary, false);
	}
	
	public Double synergy(Hero hero, Hero partner) {
		return meta.get(hero, partner, true);
	}
	
	public Double score(Hero hero, Roster enemyTeam, Collection<Hero> pool) {
		Roster futureEnemyRoster = enemyTeam;
		if (!futureEnemyRoster.isFull())
			futureEnemyRoster = enemyTeam.clone().fill(bestCounters(hero, pool.size(), pool));
		
		return futureEnemyRoster.getPicked().stream()
				.mapToDouble((enemy) -> score(hero, enemy))
				.average()
				.getAsDouble();
	}
	
	/**
	 * assumes rosters and format are properly in sync
	 * 
	 * NOTE: intended to be identical to other "options" method, but more efficient
	 */
	public List<Pick> options(Matchup matchup, Collection<Hero> pool, Format format, int phase) {
//		System.out.println("Listing options for Phase " + phase + ": " + format.get(phase == format.size() ? format.size() - 1 : phase));
		return pool.stream().map((hero) -> {
			if (matchup.isFull())
				return new Pick(hero, matchup.oddsForBlue());
			
			Matchup newMatchup = matchup;
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
	public List<Pick> options(Roster blue, Roster red, Collection<Hero> pool, Format format, int phase) {
		return pool.stream().map((hero) -> {
			if (blue.isFull() && red.isFull())
				return new Pick(hero, score(blue, red));
			
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
	public Double score(Roster us, Roster them) {
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
		double fights = (double) (us.size() + them.size());
		Double score = score(us, them);
		
		double usPairs = (double) (us.size() * (us.size() - 1));
		Double usSynergy = synergy(us);
		
		double themPairs = (double) (them.size() * (them.size() - 1));
		Double themSynergy = synergy(them);
		
		return ((fights*score) + (usPairs*usSynergy) + (themPairs*themSynergy)) 
				/ ((double) (fights + usPairs + themPairs));
	}
	
	public Double synergy(Roster team) {
		if (team.size() <= 1)
			new Double(.5);
		
		double synergy = 0.0;
		for (Hero hero : team.getPicked()) {
			for (Hero partner : team.getPicked()) {
				if (hero != partner)
					synergy += synergy(hero, partner); // TODO optimize by removing double access
			}
		}
		synergy /= (team.size() * (team.size() - 1)); // nP2 should be the number of reads we did
		return synergy;
	}
	
	public Double score(Roster us, Roster them, Set<Hero> pool) {
		List<Pick> bestCounters = bestCounters(us, them, them.fullSize() - them.size(), pool);
		Roster futureEnemyRoster = them.clone().fill(bestCounters);
		
		// looks dumb, but can't write to pool and newNewPool must be final
		Set<Hero> newPool = pool;
		for (Pick pick : bestCounters)
			newPool = subPool(newPool, pick.getCandidate());
		Set<Hero> newNewPool = new HashSet<Hero>(newPool);
		
		return us.getPicked()
				.stream()
				.mapToDouble((hero) -> score(hero, futureEnemyRoster, newNewPool))
				.average()
				.getAsDouble();
		
//		double score = 0.0;
//		for (Hero hero : us.getPicked()) {
//			score += score(hero, them, pool);
//		}
//		score /= (double) us.size();
//		return score;
	}
	
	/**
	 * @return list of all possible hero picks, decreasing in strength
	 */
	public List<Pick> optimalNextPicks(Roster pickingTeam, Roster enemyRoster, Collection<Hero> pool) {
//		System.out.println("Calculating optimal picks for situation: " + pickingTeam + " vs " + enemyRoster);
		return pool.stream()
				// score our roster's strength assuming we pick the hero:
				.map((hero) -> new Pick(hero, 
						score(pickingTeam.whatIf(hero), enemyRoster, subPool(pool, hero))))
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

	private Set<Hero> subPool(Collection<Hero> pool, Hero toRemove) {
		Set<Hero> subPool = new HashSet<Hero>(pool);
		subPool.remove(toRemove);
		return subPool;
	}
	
	/**
	 * @return list of N best counters to a hero, decreasing in strength
	 */
	private List<Pick> bestCounters(Hero hero, int nBest, Collection<Hero> pool) {
		return pool.stream()
				.map((counter) -> new Pick(counter, score(counter, hero)))
				.sorted()
				.collect(Collectors.toList()).subList(0, nBest);
	}
	
	/**
	 * @return list of N best counters to a roster, decreasing in strength
	 */
	private List<Pick> bestCounters(Roster countered, Roster countering, int nBest, Collection<Hero> pool) {
		// TODO use countering roster in calculation
		return pool.stream()
				.map((counter) -> new Pick(counter, score(counter, countered, subPool(pool, counter))))
				.sorted()
				.collect(Collectors.toList());
	}
}
