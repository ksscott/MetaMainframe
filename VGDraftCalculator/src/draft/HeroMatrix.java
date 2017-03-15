package draft;

import java.util.HashMap;

public class HeroMatrix {

	private HashMap<Hero, HashMap<Hero,Double>> chances;
	
	public HeroMatrix() {
		chances = new HashMap<>();
		for (Hero h : Hero.values()) {
			chances.put(h, new HashMap<Hero,Double>());
		}
	}
	
	public void put(Hero pro, Hero con, double odds) {
		chances.get(pro).put(con, odds);
	}
	
	public Double get(Hero pro, Hero con) {
		return chances.get(pro).get(con);
	}
}
