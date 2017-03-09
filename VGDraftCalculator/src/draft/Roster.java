package draft;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Roster implements Iterable<Hero> {

	private int finalSize;
	private List<Hero> picked;
	
	public Roster(int finalSize, List<Hero> picked) {
		this.finalSize = finalSize;
		this.picked = picked;
	}
	
	public Roster(Roster cloned) {
		this.finalSize = cloned.finalSize;
		this.picked = new ArrayList<Hero>(cloned.picked);
	}
	
	public int getFinalSize() {
		return finalSize;
	}
	
	public List<Hero> getPicked() {
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
