package edu.nd.nina.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import cc.mallet.classify.Classifier;
import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.classify.NaiveBayesTrainer;
import cc.mallet.pipe.Array2FeatureVector;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.Target2Label;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Labeling;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import edu.nd.nina.Graphs;
import edu.nd.nina.Type;
import edu.nd.nina.alg.ConstrainedRandomWalkWithRestart;
import edu.nd.nina.alg.MetaPath;
import edu.nd.nina.graph.TypedEdge;
import edu.nd.nina.graph.TypedSimpleGraph;
import edu.nd.nina.types.kddcup2013.Affiliation;
import edu.nd.nina.types.kddcup2013.Author;
import edu.nd.nina.types.kddcup2013.AuthorAlsoKnownAs;
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

	private static File dataFolder = new File("./data/kddcup2013/");
	private static File serial = new File(dataFolder, "serial.bin");
	private static File serialMap = new File(dataFolder, "serialMap.bin");
	
	private static File valid = new File(dataFolder.getAbsolutePath() + "\\" + "Valid.csv");
	private static File train = new File(dataFolder.getAbsolutePath() + "\\" + "Train.csv");
	
	private static Map<Integer, Author> authorMap = new HashMap<Integer, Author>();
	private static Map<Integer, Paper> paperMap = new HashMap<Integer, Paper>();
	private static Map<Integer, Venue> venueMap = new HashMap<Integer, Venue>();
	
	
	
	private static void loadKDDCupGraphFromFolder(File dataFolder,
			TypedSimpleGraph tsg) throws IOException {

		

		File author = new File(dataFolder.getAbsolutePath() + "\\"
				+ "Author.csv");
		File conference = new File(dataFolder.getAbsolutePath() + "\\"
				+ "Conference.csv");
		File journal = new File(dataFolder.getAbsolutePath() + "\\"
				+ "Journal.csv");
		File paper = new File(dataFolder.getAbsolutePath() + "\\" + "sanitizedPaper.csv");
		File paperAuthor = new File(dataFolder.getAbsolutePath() + "\\"
				+ "PaperAuthor.csv");
		
		
		valid = new File(dataFolder.getAbsolutePath() + "\\" + "Valid.csv");

				
		
		// load author
		BufferedReader br = new BufferedReader(new FileReader(author));
		String line = "";

		br.readLine(); //eat the first line

		while ((line = br.readLine()) != null) {
			String[] authorline = line.split(csvregex);
			Author a;
			if (authorline.length >= 2) {
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
				p = new Paper(Integer.parseInt(pline[0]), pline[1].replaceAll("\"", "").toLowerCase(), pline[5]);
			} else {
				p = new Paper(Integer.parseInt(pline[0]), pline[1].replaceAll("\"", "").toLowerCase(), null);
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

		count=12000000;
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
		

	}
	
	private static String ltrim(String s) {
		int i = 0;
		while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
			i++;
		}
		return s.substring(i);
	}

	public static void main(String[] args) {
		
		TypedSimpleGraph tsg = null;// = new TypedSimpleGraph(TypedEdge.class);
		List<Map<Integer, ? extends Type>> mapList = null;
		Kryo kryo = new Kryo();
		try {
			NINALogger.setup();
			
			if (serial.exists()) {
				try {
					Input input = new Input(new FileInputStream(serial));
					Input inputMap = new Input(new FileInputStream(serialMap));
					tsg = kryo.readObject(input, TypedSimpleGraph.class);
					mapList = kryo.readObject(inputMap, ArrayList.class);
					authorMap = (Map<Integer, Author>) mapList.get(0);
					paperMap = (Map<Integer, Paper>) mapList.get(1);
					venueMap = (Map<Integer, Venue>) mapList.get(2);
					input.close();
					inputMap.close();
				} catch (Exception e) {
					System.out
							.println("Exception during deserialization: " + e);
					System.exit(0);
				}
			} else {
				tsg = new TypedSimpleGraph(TypedEdge.class);
				loadKDDCupGraphFromFolder(dataFolder, tsg);
			}

			
			
			ConstrainedRandomWalkWithRestart crwr = new ConstrainedRandomWalkWithRestart(
					tsg, 0.15f);
			
			//for each training pair
			BufferedReader br = new BufferedReader(new FileReader(train));
			String line = "";
			
			br.readLine(); // eat the first line
			
			
			// Create the pipeline that will take as input {data = File, target
			// = String for classname}
			// and turn them into {data = FeatureVector, target = Label}
			Pipe instancePipe = new SerialPipes(new Pipe[] {
					new Target2Label(), 
					new Array2FeatureVector() }
			);

			// Create an empty list of the training instances
			InstanceList ilist = new InstanceList (instancePipe);
			
			int count=3740;
			int i=0;
			int perc = 0;
			
			// authorid, confirmedpaperid, deletedpaperid
			while ((line = br.readLine()) != null) {
				
				if(perc < (++i/(float)count)*100){
					logger.info(++perc + "%");
				}
				if(perc > 4) break;
				
				
				String[] tline = line.split(csvregex);				
				Author a = authorMap.get(Integer.parseInt(tline[0]));
				if(a == null) continue;
				
				MetaPath mp = new MetaPath(a);
				mp.addToPath(Paper.class);
				Map<Type, Integer> ap = crwr.pathCount(mp);
				
				mp = new MetaPath(a);			
				mp.addToPath(Paper.class);
				mp.addToPath(Term.class);
				mp.addToPath(Paper.class);
				Map<Type, Integer> aptp = crwr.pathCount(mp);
				
				
				for (String pid : tline[1].split(" ")) {
					Integer pi = Integer.parseInt(pid);					
					Paper p = paperMap.get(pi);
					if(p == null) continue;
					//a -> p = 1
					
					Integer apI = ap.get(p);								
					Integer aptpI = aptp.get(p);
					
					ilist.addThruPipe (new Instance(new double[] {apI, aptpI}, 1, "wtf", "wtfSource"));
					
				}

				for (String pid : tline[2].split(" ")) {
					Integer pi = Integer.parseInt(pid);
					Paper p = paperMap.get(pi);
					if(p == null) continue;
					//a -> p = 0
					
					Integer apI = ap.get(p);										
					Integer aptpI = aptp.get(p);
					
					ilist.addThruPipe (new Instance(new double[] {apI, aptpI}, 0, "wtf", "wtfSource"));
				}

			}
			br.close();
		
			// Create a classifier trainer, and use it to create a classifier
			ClassifierTrainer naiveBayesTrainer = new NaiveBayesTrainer ();
			Classifier classifier = naiveBayesTrainer.train (ilist);
			
			System.out.println ("The training accuracy is "+ classifier.getAccuracy (ilist));
			
	

			
			//
			//
			//
			//
			//
			//			
			
			
			
			
			//for each training pair
			br = new BufferedReader(new FileReader(valid));
			line = "";
			
			br.readLine(); // eat the first line
	
			count=1497;
			i=0;
			perc = 0;
			
			// authorid, confirmedpaperid, deletedpaperid
			while ((line = br.readLine()) != null) {				
				
				
				if(perc < (++i/(float)count)*100){
					logger.info(++perc + "%");
				}
				if(perc > 4) break;
				
				
				String[] tline = line.split(csvregex);				
				Author a = authorMap.get(Integer.parseInt(tline[0]));
				if (a == null)
					continue;
				
				MetaPath mp = new MetaPath(a);
				mp.addToPath(Paper.class);
				Map<Type, Integer> ap = crwr.pathCount(mp);

				mp = new MetaPath(a);
				mp.addToPath(Paper.class);
				mp.addToPath(Term.class);
				mp.addToPath(Paper.class);
				Map<Type, Integer> aptp = crwr.pathCount(mp);
				
				System.out.print(a.getUniqueIdentifier() + ",");
				List<Integer> negClass = new ArrayList<Integer>(); 
				
				for (Type t : Graphs.neighborListOf(tsg, a)) {
					if(! (t instanceof Paper)) continue;

					Paper p = (Paper)t;
				

					Integer apI = ap.get(p);								
					Integer aptpI = aptp.get(p);
					
					InstanceList iListTest = new InstanceList(instancePipe);
					iListTest.addThruPipe(new Instance(new double[] {apI, aptpI}, 0, "wtf", "wtfSource"));
					
					Labeling l = classifier.classify(iListTest).get(0).getLabeling();				
					if(l.getBestLabel().getEntry().equals(1)){
						System.out.print(p.getId() + " ");
					}else{
						negClass.add(p.getId());
					}
					
				}
				
				for(Integer neg : negClass){
					System.out.print(neg + " ");
				}
				System.out.println();
				
				

			}
			br.close();		
			
			
			
			
			if (!serial.exists()) {
				// Object serialization
				try {
					Output output = new Output(new FileOutputStream(serial));
					Output outputMap = new Output(new FileOutputStream(serialMap));
					kryo.writeObject(output, tsg);
					List<Map<Integer, ? extends Type>> o = new ArrayList<Map<Integer, ? extends Type>>();
					o.add(authorMap);
					o.add(paperMap);
					o.add(venueMap);
					kryo.writeObject(outputMap, o);
					output.close();
					outputMap.close();
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
