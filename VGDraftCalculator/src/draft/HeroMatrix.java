package draft;

import java.util.HashMap;

public class HeroMatrix {

	private HashMap<Hero, HashMap<Hero,Double>> versus;
	private HashMap<Hero, HashMap<Hero,Double>> synergy;
	
	public HeroMatrix() {
		versus = new HashMap<>();
		synergy = new HashMap<>();
		for (Hero h : Hero.values()) {
			versus.put(h, new HashMap<Hero,Double>());
			synergy.put(h, new HashMap<Hero,Double>());
		}
	}
	
	public void put(Hero one, Hero other, double odds, boolean withOrAgainst) {
		if (withOrAgainst)
			synergy.get(one).put(other, odds);
		else
			versus.get(one).put(other, odds);
			
	}
	
	public Double get(Hero one, Hero other, boolean withOrAgainst) {
		if (withOrAgainst)
			return synergy.get(one).get(other);
		else
			return versus.get(one).get(other);
	}
}
