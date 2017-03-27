package draft;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import algorithm.Engine;
import data.Calculator;
import data.Hero;
import data.HeroMatrix;
import data.MatrixLoader;
import data.Pick;

/**
 * Entrance to the Meta Mainframe
 */
public class MetaMainframe {

	private static final Format FORMAT = Format.DOUBLE_BAN;
	private static final String VS_FILE_PATH = "VG8VersusMatrix.xml";
	private static final String SYNERGY_FILE_PATH = "VG8SynergyMatrix.xml";
	private static HeroMatrix matrix;
	static {
		try {
			matrix = MatrixLoader.load(Paths.get(VS_FILE_PATH), Paths.get(SYNERGY_FILE_PATH));
		} catch (ParserConfigurationException | SAXException | IOException | IllegalArgumentException e) {
			e.printStackTrace();
			System.out.println("File load failed! Continuing with random simulated data.");
			matrix = MatrixLoader.loadRandom();
		}
	}
	private static Engine engine = new Engine(new Calculator(matrix));
	
	/**
	 * Command line use of the application
	 */
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		DraftSession sesh = new DraftSession(FORMAT);
		
		System.out.println();
		System.out.println("======== Vainglory Meta Mainframe ========");

		for (int phase = 0; phase < FORMAT.size(); phase++) {
			
			// PROVIDE ADVICE
			List<Pick> optimalNextPicks = engine.suggestions(sesh);
			Double score = optimalNextPicks.get(0).getScore();
			System.out.println(String.format("Current odds blue wins: %.3f   %s vs. %s", 
					score, sesh.getBlue(), sesh.getRed()));
			String advice = "Optimal next picks: ";
			for (Pick p : optimalNextPicks)
				advice += p + " ";
			System.out.println(advice);

			
			// PARSE INPUTS
			Hero hero = null;
			while (true) {
				System.out.println("Choose next hero for " + sesh.currentPhase().name() + ":");
				
				// String input = "best"; // for debugging
				String input = scanner.next();
				if (input.equals("quit")) {
					scanner.close();
					return;
				}
				if (input.equals("best")) {
					hero = optimalNextPicks.get(0).getCandidate();
					break; // success
				} else if (input.equals("worst")) {
					hero = optimalNextPicks.get(optimalNextPicks.size() - 1).getCandidate();
					break; // success
				} else if (input.equals("none")) {
					// leave null to skip
					break; // success
				} else {
					try {
						hero = Hero.fromName(input);
					} catch (IllegalArgumentException e) {
						System.out.println(e.getMessage());
						continue; // try again
					}
					break; // success
				}
			}
			
			sesh.pickOrBan(hero);
		}
		System.out.println(String.format("Final odds blue wins: %.3f   %s vs. %s", 
				engine.currentOddsForBlue(sesh), sesh.getBlue(), sesh.getRed()));
		scanner.close();
	}
	
	/**
	 * Stateless entrance into the application
	 */
	public static Map<String, Integer> coachMeSenpai (String draftFormat, final List<String> selected) {
		return engine.coachMeSenpai(draftFormat, selected);
	}
}
