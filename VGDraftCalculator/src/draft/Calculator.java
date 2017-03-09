package draft;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Calculator {

	private HeroMatrix meta;
	
	public Calculator(HeroMatrix meta) {
		this.meta = meta;
	}
	
	public Double score(Hero hero, Hero adversary) {
		return meta.get(hero, adversary);
	}
	
	public Double score(Hero hero, Roster enemyTeam, Collection<Hero> pool) {
		Roster futureEnemyRoster = 
				new Roster(enemyTeam).fill(bestCounters(hero, pool.size(), pool));
		
		return futureEnemyRoster.getPicked().stream()
				.mapToDouble((enemy) -> score(hero, enemy))
				.sum()
				/ (double) futureEnemyRoster.getFinalSize();
	}
	
	public Double score(Roster us, Roster them, Collection<Hero> pool) {
		Roster futureEnemyRoster = 
				new Roster(them).fill(bestCounters(us, pool.size(), pool));
		
		return us.getPicked()
				.stream()
				.mapToDouble((hero) -> score(hero, them, pool))
				.sum()
				/ (double) us.size();
		
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
	public List<Pick> optimalNextPicks(Roster enemyRoster, Collection<Hero> pool) {
		return pool.stream()
				.map((hero) -> new Pick(hero, score(hero, enemyRoster, pool)))
				.sorted()
				.collect(Collectors.toList());
	}
	
	/**
	 * @param enemyTeam
	 * @return
	 */
	public List<Pick> optimalOffensiveBan(Roster enemyTeam, List<Hero> pool) {
		List<Pick> bestBans = new ArrayList<>();
		
		for (Hero ban : pool) {
			List<Hero> possiblePool = new ArrayList<>(pool);
			possiblePool.remove(ban); // "if we ban this..."
			List<Pick> picks = optimalNextPicks(enemyTeam, possiblePool);
			Double futureHeroScore = picks.get(1).getScore(); // "...then our next hero is this good"
			bestBans.add(new Pick(ban, futureHeroScore));
		}
		Collections.sort(bestBans);
		return bestBans;
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
	private List<Pick> bestCounters(Roster roster, int nBest, Collection<Hero> pool) {
		return pool.stream()
				.map((counter) -> new Pick(counter, score(counter, roster, pool)))
				.sorted()
				.collect(Collectors.toList());
	}
}
