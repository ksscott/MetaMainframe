package draft;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Roster implements Iterable<Hero> {

	private int finalSize;
	private Set<Hero> picked;
	
	public Roster(int finalSize) {
		this.finalSize = finalSize;
		this.picked = new HashSet<Hero>();
	}
	
	public Roster(Roster cloned) {
		this.finalSize = cloned.finalSize;
		this.picked = new HashSet<Hero>(cloned.picked);
	}
	
	public Roster whatIf(Hero hero) {
		Roster newRoster = new Roster(this);
		newRoster.add(hero);
		return newRoster;
	}
	
	public int fullSize() {
		return finalSize;
	}
	
	public Set<Hero> getPicked() {
		return picked;
	}
	
	public Roster add(Hero newHero) {
		if (picked.size() < finalSize)
			picked.add(newHero);
		return this;
	}
	
	/**
	 * Adds heroes from the given list in order until this roster is full
	 */
	public Roster fill(List<Pick> recruits) {
		for (Pick pick : recruits) {
			if (size() < finalSize)
				add(pick.getCandidate());
		}
		return this;
	}
	
	public int size() {
		return picked.size();
	}

	public boolean isEmpty() {
		return picked.isEmpty();
	}

	@Override
	public Iterator<Hero> iterator() {
		return picked.iterator();
	}
}
