package draft;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public class MetaMainframe {

	private static final Format FORMAT = Format.SINGLE_BAN;
	private static final String VS_FILE_PATH = "VG8Matrix.xml";
	private static final String SYNERGY_FILE_PATH = "VG8SynergyMatrix.xml";
	private static HeroMatrix matrix;
	static {
		try {
			matrix = MatrixLoader.load(new File(VS_FILE_PATH), new File(SYNERGY_FILE_PATH));
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
			System.out.println("File load failed! Continuing with random simulated data.");
			matrix = MatrixLoader.loadRandom();
		}
	}
	private static Calculator scorer = new Calculator(matrix);
	
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		DraftSession sesh = new DraftSession(FORMAT);
		
		System.out.println();
		System.out.println("======== Vainglory Meta Mainframe ========");

		for (int phase = 0; phase < FORMAT.size(); phase++) {
			
			// PROVIDE ADVICE
			List<Pick> optimalNextPicks = scorer.suggestions(sesh);
			Double score = optimalNextPicks.get(0).getScore();
			System.out.println(String.format("Current odds blue wins: %.3f", score));
			String advice = "Optimal next picks: ";
			for (Pick p : optimalNextPicks)
				advice += p + " ";
			System.out.println(advice);
			
			System.out.println("Choose next hero for " + sesh.currentPhase().name() + ":");
			
			
			// PARSE INPUTS
//			String input = "best"; // for debugging
			String input = scanner.next();
			if (input.equals("quit")) {
				scanner.close();
				return;
			}
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
		}
		System.out.println(String.format("Final odds blue wins: %.3f", 
				scorer.currentOddsForBlue(sesh)));
		scanner.close();
	}
	
	public static Map<String, Integer> coachMeSenpai (String draftFormat, final List<String> selected) {
		return scorer.coachMeSenpai(draftFormat, selected);
	}
}
