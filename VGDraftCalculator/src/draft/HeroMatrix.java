package draft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class HeroMatrix {

	// for direct access by hero
	private HashMap<Hero, HashMap<Hero,Double>> versus;
	private HashMap<Hero, HashMap<Hero,Double>> synergy;
	// for a pre-sorted list
	private HashMap<Hero, ArrayList<Pick>> versusPicks;
	private HashMap<Hero, ArrayList<Pick>> synergyPicks;
	
	public HeroMatrix() {
		versus = new HashMap<>();
		synergy = new HashMap<>();
		versusPicks = new HashMap<>();
		synergyPicks = new HashMap<>();
		
		for (Hero h : Hero.values()) {
			versus.put(h, new HashMap<Hero,Double>());
			synergy.put(h, new HashMap<Hero,Double>());
			versusPicks.put(h, new ArrayList<>());
			synergyPicks.put(h,  new ArrayList<>());
		}
	}
	
	public void put(Hero one, Hero other, double odds, boolean withOrAgainst) {
		if (withOrAgainst) {
			synergy.get(one).put(other, odds);
			ArrayList<Pick> list = synergyPicks.get(one);
			list.add(new Pick(other, odds));
			Collections.sort(list); // ends up being > O(N^2); find a smarter way
		}
		else {
			versus.get(one).put(other, odds);
			ArrayList<Pick> list = versusPicks.get(one);
			list.add(new Pick(other, odds));
			Collections.sort(list); // ends up being > O(N^2); find a smarter way
		}
	}
	
	public Double get(Hero one, Hero other, boolean withOrAgainst) {
		if (withOrAgainst)
			return synergy.get(one).get(other);
		else
			return versus.get(one).get(other);
	}
	
	public ArrayList<Pick> get(Hero hero, boolean withOrAgainst) {
		if (withOrAgainst)
			return synergyPicks.get(hero);
		else
			return versusPicks.get(hero);
	}
}
