package draft;

public class Matchup implements Cloneable {

	private Roster blue;
	private Roster red;
	
	public Matchup(Roster blue, Roster red) {
		this.blue = blue;
		this.red = red;
	}
	
	public Matchup whatIf(Hero pick, boolean bluePicked) {
		Matchup newMatchup = this.clone();
		newMatchup.pick(pick, bluePicked);
		return newMatchup;
	}
	
	public boolean isFull() {
		return blue.isFull() && red.isFull();
	}
	
	public Roster getBlue() { return blue; }
	public Roster getRed() { return red; }
	
	public void pick(Hero pick, boolean bluePicked) {
		if (bluePicked)
			blue.add(pick);
		else
			red.add(pick);
	}

	@Override
	public Matchup clone() {
		return new Matchup (blue.clone(), red.clone());
	}
	
}
