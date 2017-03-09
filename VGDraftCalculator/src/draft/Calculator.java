package draft;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class Calculator {

	private HeroMatrix meta;
	
	public Calculator(HeroMatrix meta) {
		this.meta = meta;
	}
	
	public Double score(Hero hero, Hero adversary) {
		return meta.get(hero, adversary);
	}
	
	public Double score(Hero hero, Roster enemyTeam, Collection<Hero> pool) {
		double score = 0.0;
		int numFoes = enemyTeam.getFinalSize();
		List<Hero> picked = enemyTeam.getPicked();
		for (Hero enemy : picked) {
			score += score(hero, enemy);
		}
		List<Pick> bestCounters = bestCounters(hero, numFoes - picked.size(), pool);
		for (Pick pick : bestCounters) {
			score += score(hero, pick.getCandidate());
		}
		score /= numFoes;
		return new Double(score);
	}
	
	public Double score(Roster us, Roster them, Collection<Hero> pool) {
		double score = 0.0;
		for (Hero hero : us.getPicked()) {
			score += score(hero, them, pool);
		}
		score /= us.size();
		return score;
	}
	
	/**
	 * @return list of all possible hero picks, decreasing in strength
	 */
	public List<Pick> optimalNextPicks(Roster enemyRoster, Collection<Hero> pool) {
		ArrayList<Pick> optimalPicks = new ArrayList<Pick>();
		for (Hero h : pool) {
			optimalPicks.add(new Pick(h, score(h, enemyRoster, pool)));
		}
		Collections.sort(optimalPicks);
		Collections.reverse(optimalPicks);
		return optimalPicks;
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
		Collections.reverse(bestBans);
		return bestBans;
	}

	/**
	 * @return list of N best counters, decreasing in strength
	 */
	private List<Pick> bestCounters(Hero hero, int nBest, Collection<Hero> pool) {
		ArrayList<Pick> bestCounters = new ArrayList<Pick>();
		for (Hero h : pool) {
			bestCounters.add(new Pick(h, score(hero, h)));
		}
		Collections.sort(bestCounters);
		Collections.reverse(bestCounters);
		return bestCounters.subList(0, nBest);
	}
}
