package draft;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

public class MetaMainframe {

	private static final Format FORMAT = Format.SINGLE_BAN;
	private static final String FILE_PATH = "VG8Matrix.xml";
	
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		HeroMatrix matrix;
		try {
			matrix = MatrixLoader.load(new File(FILE_PATH));
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
			System.out.println("File load failed! Continue with random data? (y/n)");
			if (scanner.next().equals("y")) {
				matrix = MatrixLoader.loadRandom();
			} else {
				scanner.close();
				return;
			}
		}
		DraftSession sesh = new DraftSession(FORMAT, matrix);

		
		System.out.println();
		System.out.println("======== Vainglory Meta Mainframe ========");

		for (int picked = 0; picked < FORMAT.size(); picked++) {
			
			// PROVIDE ADVICE
			System.out.println(String.format("Current odds blue wins: %.3f", sesh.currentOddsForBlue()));
			
			List<Pick> optimalNextPicks = sesh.suggestions();
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
		System.out.println(String.format("Final odds blue wins: %.3f", sesh.currentOddsForBlue()));
		scanner.close();
	}
}
