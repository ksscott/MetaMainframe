package draft;

import static draft.Phase.*;
import static draft.Strategy.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

enum Format {
	SINGLE_BAN(BLUE_BAN,
			   RED_BAN,
			   BLUE_PICK,
			   RED_PICK,
			   RED_PICK,
			   BLUE_PICK,
			   BLUE_PICK,
			   RED_PICK),
	DOUBLE_BAN(BLUE_BAN,
			   RED_BAN,
			   BLUE_PICK,
			   RED_PICK,
			   RED_BAN,
			   BLUE_BAN,
			   RED_PICK,
			   BLUE_PICK,
			   BLUE_PICK,
			   RED_PICK);
	
	private List<Phase> phases;
	
	Format(Phase... phases) {
		this.phases = Arrays.asList(phases);
	}
	
	public Phase get(int phase) {
		return phases.get(phase);
	}
	
	public Strategy strategy(int phase) {
		Phase one = phases.get(phase);
		if (one.isPick()) {
			return PICK;
		} else { // bans
			String message = "Offensive ban requires format [our-ban, their-ban, our-pick, ...]";
			Phase two = phases.get(phase+1);
			if (one.isBlue() == two.isBlue())
				throw new IllegalStateException(message);
			if (two.isPick())
				return DEFENSIVE_BAN;
			Phase three = phases.get(phase+2);
			if (!three.isPick() || !(one.isBlue() == three.isBlue()))
				throw new IllegalStateException(message);
			return OFFENSIVE_BAN;
		}
	}
	
	public int size() { return phases.size(); }
	public int blueRoster() { return Collections.frequency(phases, BLUE_PICK); }
	public int redRoster() { return Collections.frequency(phases, RED_PICK); }
}