package draft;

import java.util.List;
import java.util.Scanner;

public class MetaMainframe {

	private static Format format = Format.SINGLE_BAN;
	private static DraftSession sesh = new DraftSession(format, new HeroMatrix());
	private static Scanner scanner = new Scanner(System.in);
	
	public static void main(String[] args) {
		int picked = 0;
		System.out.println("======== Vainglory Meta Mainframe ========");
		while (picked < format.size()) {
			System.out.println(String.format("Current odds blue wins: %.3f", sesh.currentOddsForBlue()));
			
			List<Pick> optimalNextPicks = sesh.suggestions();
			String advice = "Optimal next picks: ";
			for (Pick p : optimalNextPicks)
				advice += p + " ";
			System.out.println(advice);
			
			System.out.println("Choose next hero for " + sesh.currentPhase().name() + ":");
			
//			String input = "best";
			String input = scanner.next();
			if (input.equals("quit"))
				return;
			Hero hero = null;
			if (input.equals("best")) {
				hero = optimalNextPicks.get(0).getCandidate();
			} else if (input.equals("worst")) {
				hero = optimalNextPicks.get(optimalNextPicks.size() - 1).getCandidate();
			} else if (input.equals("none")) {
				// leave null to skip
			} else {
				hero = Hero.fromName(input);
			}
			
			sesh.pickOrBan(hero);
			picked++;
		}
		System.out.println(String.format("Final odds blue wins: %.3f", sesh.currentOddsForBlue()));
	}

}
