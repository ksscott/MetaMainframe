package draft;

public class Matchup implements Cloneable {

	private Roster blue;
	private Roster red;
	private Calculator scorer;
	double oddsForBlue = 0.0;
	
	public Matchup(Roster blue, Roster red, Calculator scorer) {
		this.blue = blue;
		this.red = red;
		this.scorer = scorer;
	}
	
	@Override
	public Matchup clone() {
		return new Matchup (blue.clone(), red.clone(), scorer);
	}
	
	public Matchup whatIf(Hero pick, boolean bluePicked) {
		Roster newBlue = blue;
		Roster newRed = red;
		double newOddsForBlue = oddsForBlue;
//		Matchup hypothetical = new Matchup(newBlue, newRed, scorer);
		if (bluePicked) {
			Double current = oddsForBlue * blue.size();
			newBlue = blue.whatIf(pick);
			Double heroScore = 0.0;
			for (Hero enemy : red.getPicked()) {
				heroScore += scorer.score(pick, enemy);
			}
			int divisor = red.size() == 0 ? 1 : red.size();
			heroScore /= divisor;
			newOddsForBlue = (current + heroScore) / newBlue.size();
		} else {
			Double current = oddsForBlue * red.size();
			newRed = red.whatIf(pick);
			Double scoreAgainstEnemy = 0.0;
			for (Hero hero : blue.getPicked()) {
				scoreAgainstEnemy += scorer.score(hero, pick);
			}
			int divisor = blue.size() == 0 ? 1 : blue.size();
			scoreAgainstEnemy /= divisor;
			newOddsForBlue = (current + scoreAgainstEnemy) / newRed.size();
		}
		Matchup hypothetical = new Matchup(newBlue, newRed, scorer);
		hypothetical.oddsForBlue = newOddsForBlue;
		System.out.println(String.format("%.3f", oddsForBlue) + " " + newBlue + " vs. " + newRed);
		return hypothetical;
	}
	
	/**
	 * Makes no attempt at extrapolation; only returns currently cached score.
	 */
	public Double oddsForBlue() {
		return oddsForBlue;
	}
	
	public boolean isFull() {
//		System.out.println("Matchup full? " + (blue.isFull() && red.isFull()));
//		System.out.println("Blue full? " + blue.isFull() + " -- " + blue.toString());
//		System.out.println("Red full? " + red.isFull() + " -- " + red.toString());
		return blue.isFull() && red.isFull();
	}
	
	/**
	 * @return current odds blue wins
	 */
	public Double pick(Hero pick, boolean bluePicked) {
		if (bluePicked) {
			Double current = oddsForBlue * blue.size();
			blue.add(pick);
			Double heroScore = 0.0;
			for (Hero enemy : red.getPicked()) {
				heroScore += scorer.score(pick, enemy);
			}
			int divisor = red.size() == 0 ? 1 : red.size();
			heroScore /= divisor;
			oddsForBlue = (current + heroScore) / blue.size();
		} else {
			Double current = oddsForBlue * red.size();
			red.add(pick);
			Double scoreAgainstEnemy = 0.0;
			for (Hero hero : blue.getPicked()) {
				scoreAgainstEnemy += scorer.score(hero, pick);
			}
			int divisor = blue.size() == 0 ? 1 : blue.size();
			scoreAgainstEnemy /= divisor;
			oddsForBlue = (current + scoreAgainstEnemy) / red.size();
		}
//		System.out.println((bluePicked ? "Blue" : "Red") + " picked " + pick);
//		System.out.println("Blue: " + blue);
//		System.out.println("Red: " + red);
//		System.out.println("Odds: " + oddsForBlue);
		return oddsForBlue;
	}
	
}
