package data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import draft.Hero;
import model.Pick;

public class HeroMatrix {

	// for direct access by hero
	private HashMap<Hero, HashMap<Hero,Double>> versus;
	private HashMap<Hero, HashMap<Hero,Double>> synergy;
	// for a pre-sorted list // TODO write protect
	private HashMap<Hero, List<Pick>> versusPicks;
	private HashMap<Hero, List<Pick>> synergyPicks;
	
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
			List<Pick> list = synergyPicks.get(one);
			list.add(new Pick(other, odds));
			if (list.size() == Hero.values().length) {
				// assuming proper use, time to lock up
				Collections.sort(list); // ends up being > O(N^2); find a smarter way
				synergyPicks.put(one, Collections.unmodifiableList(list));
			}
		}
		else {
			versus.get(one).put(other, odds);
			List<Pick> list = versusPicks.get(one);
			list.add(new Pick(other, odds));
			if (list.size() == Hero.values().length) {
				// assuming proper use, time to lock up
				Collections.sort(list); // ends up being > O(N^2); find a smarter way
				versusPicks.put(one, Collections.unmodifiableList(list));
			}
		}
	}
	
	public Double get(Hero one, Hero other, boolean withOrAgainst) {
		if (withOrAgainst)
			return synergy.get(one).get(other);
		else
			return versus.get(one).get(other);
	}
	
	public List<Pick> get(Hero hero, boolean withOrAgainst) {
		if (withOrAgainst)
			return new ArrayList<>(synergyPicks.get(hero));
		else
			return new ArrayList<>(versusPicks.get(hero));
	}
}
