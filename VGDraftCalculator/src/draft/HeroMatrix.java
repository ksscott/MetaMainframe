package draft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class HeroMatrix {

	private HashMap<Hero, HashMap<Hero,Double>> chances;
	
	public HeroMatrix() {
		chances = new HashMap<>();
		for (Hero h : Hero.values()) {
			chances.put(h, new HashMap<Hero,Double>());
		}
		
		// generate random win rates
		// (.5) (1/x) (1/y)
		// (x)  (.5)  (1/z)
		// (y)   (z)   (.5) etc...
		double standardDeviation = .15;
		ArrayList<Hero> axis2 = new ArrayList<>();
		Random rand = new Random();
		for (Hero one : Hero.values()) {
			for (Hero two : axis2) {
				double score = rand.nextGaussian();
				// scale and shift:
				score *= standardDeviation;
				score += .5;
				// bound outliers within probability:
				score = score > 1 ? 1 : score;
				score = score < 0 ? 0 : score;
				put(one, two, score);
				put(two, one, 1 - score);
			}
			put(one, one, .5);
			axis2.add(one);
		}
		// some consistent numbers
//		put(CATHERINE, CATHERINE, .5);
//		put(CATHERINE, RINGO, .2);
//		put(CATHERINE, ADAGIO, .6);
//		put(CATHERINE, KOSHKA, .35);
//		put(RINGO, CATHERINE, .8);
//		put(RINGO, RINGO, .5);
//		put(RINGO, ADAGIO, .3);
//		put(RINGO, KOSHKA, .45);
//		put(ADAGIO, CATHERINE, .4);
//		put(ADAGIO, RINGO, .7);
//		put(ADAGIO, ADAGIO, .5);
//		put(ADAGIO, KOSHKA, .55);
//		put(KOSHKA, CATHERINE, .65);
//		put(KOSHKA, RINGO, .55);
//		put(KOSHKA, ADAGIO, .45);
//		put(KOSHKA, KOSHKA, .5);
	}
	
	public void put(Hero pro, Hero con, double odds) {
		chances.get(pro).put(con, odds);
	}
	
	public Double get(Hero pro, Hero con) {
		return chances.get(pro).get(con);
	}
}
