package edu.nd.nina.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import edu.nd.nina.graph.TypedEdge;
import edu.nd.nina.graph.TypedSimpleGraph;
import edu.nd.nina.types.kddcup2013.Affiliation;
import edu.nd.nina.types.kddcup2013.Author;
import edu.nd.nina.types.kddcup2013.AuthorAlsoKnownAs;
import edu.nd.nina.types.kddcup2013.Confirmed;
import edu.nd.nina.types.kddcup2013.Deleted;
import edu.nd.nina.types.kddcup2013.Paper;
import edu.nd.nina.types.kddcup2013.Term;
import edu.nd.nina.types.kddcup2013.Venue;
import edu.nd.nina.types.kddcup2013.VenueWebPage;
import edu.nd.nina.types.kddcup2013.Year;

public class KDDCup2013 {

	private static Logger logger = Logger.getLogger(KDDCup2013.class.getName());

	private static String otherThanQuote = " [^\"] ";
	private static String quotedString = String.format(" \" %s* \" ",
			otherThanQuote);
	private static String csvregex = String.format("(?x) " + // enable comments,
																// ignore white
																// spaces
			",                         " + // match a comma
			"(?=                       " + // start positive look ahead
			"  (                       " + // start group 1
			"    %s*                   " + // match 'otherThanQuote' zero or
											// more times
			"    %s                    " + // match 'quotedString'
			"  )*                      " + // end group 1 and repeat it zero or
											// more times
			"  %s*                     " + // match 'otherThanQuote'
			"  $                       " + // match the end of the string
			")                         ", // stop positive look ahead
			otherThanQuote, quotedString, otherThanQuote);

	private static void loadKDDCupGraphFromFolder(File dataFolder,
			TypedSimpleGraph tsg) throws IOException {

		Map<Integer, Paper> paperMap = new HashMap<Integer, Paper>();
		Map<Integer, Venue> venueMap = new HashMap<Integer, Venue>();
		Map<Integer, Author> authorMap = new HashMap<Integer, Author>();

		File author = new File(dataFolder.getAbsolutePath() + "\\"
				+ "Author.csv");
		File conference = new File(dataFolder.getAbsolutePath() + "\\"
				+ "Conference.csv");
		File journal = new File(dataFolder.getAbsolutePath() + "\\"
				+ "Journal.csv");
		File paper = new File(dataFolder.getAbsolutePath() + "\\" + "sanitizedPaper.csv");
		File paperAuthor = new File(dataFolder.getAbsolutePath() + "\\"
				+ "PaperAuthor.csv");
		File train = new File(dataFolder.getAbsolutePath() + "\\" + "Train.csv");

				
		
		// load author
		BufferedReader br = new BufferedReader(new FileReader(author));
		String line = "";

		br.readLine(); //eat the first line

		while ((line = br.readLine()) != null) {
			String[] authorline = line.split(csvregex);
			Author a;
			if (authorline.length == 2) {
				a = new Author(Integer.parseInt(authorline[0]), authorline[1]);
			} else {
				a = new Author(Integer.parseInt(authorline[0]), "");
			}
			tsg.addVertex(a);
			authorMap.put(Integer.parseInt(authorline[0]), a);
			if (authorline.length == 3) {
				Affiliation affil = new Affiliation(authorline[2]);
				tsg.addVertex(affil);
				tsg.addEdge(a, affil);
			}
		}
		br.close();

		// load conference
		br = new BufferedReader(new FileReader(conference));
		line = "";

		br.readLine(); // eat the first line
		int journalOffset = 0;
		// id shortname, fullname, homepage
		while ((line = br.readLine()) != null) {
			String[] confline = line.split(csvregex);
			Venue v = new Venue(Integer.parseInt(confline[0]),
					confline[1].replace("\"", ""),
					confline[2].replace("\"", ""));
			tsg.addVertex(v);
			journalOffset++;
			venueMap.put(Integer.parseInt(confline[0]), v);
			if (confline.length == 4) {

				URI url = null;
				try {
					url = new URI(confline[3].replace("\"", ""));
				} catch (URISyntaxException e) {
					// eat
				}
				if (url != null) {
					VenueWebPage u = new VenueWebPage(url);
					tsg.addVertex(u);
					tsg.addEdge(v, u);
				}
			}
		}
		br.close();

		// load journal
		br = new BufferedReader(new FileReader(journal));
		line = "";

		br.readLine(); // eat the first line
		// id shortname, fullname, homepage
		while ((line = br.readLine()) != null) {
			String[] jline = line.split(csvregex);
			Venue v = new Venue(Integer.parseInt(jline[0]) + journalOffset,
					jline[1].replace("\"", ""), jline[2].replace("\"", ""));
			tsg.addVertex(v);
			venueMap.put(Integer.parseInt(jline[0]) + journalOffset, v);

			if (jline.length == 4) {

				URI url = null;
				try {
					url = new URI(jline[3].replace("\"", ""));
				} catch (URISyntaxException e) {
					// eat
				}
				if (url != null) {
					VenueWebPage u = new VenueWebPage(url);
					tsg.addVertex(u);
					tsg.addEdge(v, u);
				}
			}
		}
		br.close();

		// load paper
		br = new BufferedReader(new FileReader(paper));
		line = "";

		int count=1000000;
		int i=0;
		int perc = 0;
		//br.readLine(); // eat the first line
		// Id Title Year ConferenceId JournalId Keyword
		while ((line = br.readLine()) != null) {			
			while(line.length() - line.replaceAll(csvregex, "").length() < 5){
				String newline = ltrim(br.readLine());
				
				line = line + " " + newline;
			}
			
			if(perc < (++i/(float)count)*100){
				logger.info(++perc + "%");
			}
			if(perc > 4) break;
			
			String[] pline = line.split(csvregex);
			Paper p;
			if (pline.length == 6) {
				p = new Paper(Integer.parseInt(pline[0]), pline[1].replaceAll("\"", ""), pline[5]);
			} else {
				p = new Paper(Integer.parseInt(pline[0]), pline[1].replaceAll("\"", ""), null);
			}
			paperMap.put(Integer.parseInt(pline[0]), p);
			tsg.addVertex(p);

			try{
			Integer year = Integer.parseInt(pline[2]);
			if (year != 0) {
				Year y = new Year(year);
				tsg.addVertex(y);
				tsg.addEdge(p, y);
			}
			}catch(Exception e){
				System.out.println(line);
			}

			Integer confid = Integer.parseInt(pline[3]);
			Integer jid = Integer.parseInt(pline[4]);
			if(!(confid == -1 || jid == -1)){
				if (confid == 0) {
					tsg.addEdge(p, venueMap.get(jid + journalOffset));
				} else {
					if(venueMap.containsKey(confid) == true){
						tsg.addEdge(p, venueMap.get(confid));
					}
				}
			}

			String[] terms = pline[1].replaceAll("\"", "").split("\\W++");
			for (String t : terms) {
				Term term = new Term(t.toLowerCase().trim());
				tsg.addVertex(term);
				tsg.addEdge(p, term);
			}

			if (pline.length == 6) {
				terms = pline[5].split("\\W++");
				for (String t : terms) {
					Term term = new Term(t.toLowerCase().trim());
					tsg.addVertex(term);
					tsg.addEdge(p, term);
				}
			}

		}
		br.close();

		// load paper-Author
		br = new BufferedReader(new FileReader(paperAuthor));
		line = "";

		count=1000000;
		i=0;
		perc = 0;

		br.readLine(); // eat the first line
		// paperid, authorid, authorname, authoraffil
		while ((line = br.readLine()) != null) {
			while(line.length() - line.replaceAll(csvregex, "").length() < 3){
				String newline = ltrim(br.readLine());
				line = line + " " + newline;
			}
			
			if(perc < (++i/(float)count)*100){
				logger.info(++perc + "%");
			}
			if(perc > 4) break;

			String[] paline = line.split(csvregex);
			
			Paper p = paperMap.get(Integer.parseInt(paline[0]));
			if (p == null)continue;
			Author a = authorMap.get(Integer.parseInt(paline[1]));
			if (a == null)continue;
			
			tsg.addEdge(p,a);

			if(paline.length == 3){
			AuthorAlsoKnownAs aka = new AuthorAlsoKnownAs(paline[2]);
			tsg.addVertex(aka);
			tsg.addEdge(a, aka);
			}
			if(paline.length == 4){
			Affiliation affil = new Affiliation(paline[3]);
			tsg.addVertex(affil);
			tsg.addEdge(a, affil);
			}
		}
		br.close();

		// load train
		br = new BufferedReader(new FileReader(train));
		line = "";

		br.readLine(); // eat the first line
		Confirmed c = new Confirmed();
		Deleted d = new Deleted();
		tsg.addVertex(c);
		tsg.addVertex(d);
		// authorid, confirmedpaperid, deletedpaperid
		while ((line = br.readLine()) != null) {
			String[] tline = line.split(csvregex);
			Author a = authorMap.get(Integer.parseInt(tline[0]));
			if(a == null) continue;
			
			for (String pid : tline[1].split(" ")) {
				Integer p = Integer.parseInt(pid);
				if(!paperMap.containsKey(p)) continue;
				tsg.addEdge(a, c);
				tsg.addEdge(c, paperMap.get(p));
			}

			for (String pid : tline[2].split(" ")) {
				Integer p = Integer.parseInt(pid);
				if(!paperMap.containsKey(p)) continue;
				tsg.addEdge(a, d);
				tsg.addEdge(d, paperMap.get(p));
			}

		}
		br.close();

	}
	
	private static String ltrim(String s) {
		int i = 0;
		while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
			i++;
		}
		return s.substring(i);
	}

	public static void main(String[] args) {
		File dataFolder = new File("./data/kddcup2013/");
		File serial = new File("serial.bin");
		TypedSimpleGraph tsg = null;// = new TypedSimpleGraph(TypedEdge.class);
		Kryo kryo = new Kryo();
		try {
			NINALogger.setup();
			
			if (serial.exists()) {
				try {
					Input input = new Input(new FileInputStream(serial));
					tsg = kryo.readObject(input, TypedSimpleGraph.class);
					input.close();
				} catch (Exception e) {
					System.out
							.println("Exception during deserialization: " + e);
					System.exit(0);
				}
			} else {
				tsg = new TypedSimpleGraph(TypedEdge.class);
				loadKDDCupGraphFromFolder(dataFolder, tsg);
			}
			// PrintStatistics.PrintTypedGraphStatTable(tsg,
			// "./data/dblp/testStats", "DBLPTypedGraph");
			// PrintStatistics.PrintCrazyCCF(tsg, "./data/dblp/testStats",
			// "DBLPTypedGraph");
			// CalculateStatistics.calcAssortativity(tsg, -1);
			PrintStatistics.PrintCrazyAssortativity(tsg,
					"./data/kddcup2013/testStats", "DBLPTypedGraph");

			if (!serial.exists()) {
				// Object serialization
				try {
					Output output = new Output(new FileOutputStream(serial));
					kryo.writeObject(output, tsg);
					output.close();
				} catch (Exception e) {
					System.out.println("Exception during serialization: " + e);
					System.exit(0);
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
