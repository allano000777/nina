package edu.nd.nina.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import edu.nd.nina.graph.TypedEdge;
import edu.nd.nina.graph.TypedSimpleGraph;
import edu.nd.nina.types.dblp.Author;
import edu.nd.nina.types.dblp.Paper;
import edu.nd.nina.types.dblp.Term;
import edu.nd.nina.types.dblp.Venue;

public class DBLP {
	
	private static Logger logger = Logger.getLogger(DBLP.class.getName());

	private static void loadDBLPGraphFromFile(InputStream is,
			TypedSimpleGraph tsg) {
		try {
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser parser = parserFactory.newSAXParser();
			ConfigHandler handler = new ConfigHandler(tsg);
			parser.getXMLReader().setFeature(
					"http://xml.org/sax/features/validation", true);
			parser.parse(is, handler);
		} catch (IOException e) {
			System.out.println("Error reading URI: " + e.getMessage());
		} catch (SAXException e) {
			System.out.println("Error in parsing: " + e.getMessage());
		} catch (ParserConfigurationException e) {
			System.out.println("Error in XML parser configuration: "
					+ e.getMessage());
		}
	}

	private static class ConfigHandler extends DefaultHandler {
		TypedSimpleGraph tsg = null;
		Set<String> paperType = new HashSet<String>();
		
		Set<String> venueType = new HashSet<String>();

		public ConfigHandler(TypedSimpleGraph tsg) {
			this.tsg = tsg;
			paperType.add("article");
			paperType.add("inproceedings");
			paperType.add("book");
			paperType.add("proceedings");
			paperType.add("phdthesis");
			paperType.add("mastersthesis");
			paperType.add("incollection");
			
			venueType.add("journal");
			venueType.add("booktitle");
			
			currentAuthors = new ArrayList<Author>();
			currentTerms = new ArrayList<Term>();
		}

		private Locator locator;

		private Paper current;
		private Venue currentVenue;
		private List<Author> currentAuthors;
		private List<Term> currentTerms;
		
		private String Value;
		private String key;
		private String recordTag;
		
		int total = 3391788;
		int perc = 0;
		int i=0;

		public void setDocumentLocator(Locator locator) {
			this.locator = locator;
		}

		public void startElement(String namespaceURI, String localName,
				String rawName, Attributes atts) throws SAXException {

			if(perc > 2) return;
			
			if(paperType.contains(rawName)){				
				if(perc < i++/(float)total * 100f){
					perc++;
					logger.info(perc + "% loaded");
				}
				current = new Paper("");
				currentVenue = null;
				currentAuthors.clear();
				currentTerms.clear();
			}else if(rawName.equals("www")){
				//eat
				current = new Paper("");
				currentVenue = null;
				currentAuthors.clear();
				currentTerms.clear();
			}else{			
				key = rawName;
			}
			Value = "";
		}

		public void endElement(String namespaceURI, String localName,
				String rawName) throws SAXException {			
			if(perc > 2) return;
			
			if(paperType.contains(rawName)){
				if(current.getName().equals("Convergence Results on Proximal Method of Multipliers in Nonconvex Programming")){
					System.out.println();
				}
				int i=0;
				while(tsg.containsVertex(current)){
					i++;
					current.setIdx(String.valueOf(i));
				}
				tsg.addVertex(current);
				if(currentVenue == null){
					currentVenue = new Venue(current.getAttribute("publisher"));					
				}
				tsg.addVertex(currentVenue);				
				tsg.addEdge(current, currentVenue);
				for(Author a : currentAuthors){
					tsg.addVertex(a);
					tsg.addEdge(current, a);
				}
				for(Term t : currentTerms){
					tsg.addVertex(t);
					tsg.addEdge(current, t);
				}				
			}else if(venueType.contains(rawName)){
				currentVenue = new Venue(Value);
			}else if(rawName.equalsIgnoreCase("title")){				
				current.setTitle(Value);
				String[] s = Value.split("\\W+");
				for(String t : s){
					currentTerms.add(new Term(t.toLowerCase()));
				}
			}else if(rawName.equalsIgnoreCase("author")){
				currentAuthors.add(new Author(Value));
			}else{
				current.addAttribute(rawName, Value);
			}
			
			
		}

		public void characters(char[] ch, int start, int length)
				throws SAXException {
			if(perc > 2) return;
			Value += new String(ch, start, length);
		}

		private void Message(String mode, SAXParseException exception) {
			System.out.println(mode + " Line: " + exception.getLineNumber()
					+ " URI: " + exception.getSystemId() + "\n" + " Message: "
					+ exception.getMessage());
		}

		public void warning(SAXParseException exception) throws SAXException {

			Message("**Parsing Warning**\n", exception);
			throw new SAXException("Warning encountered");
		}

		public void error(SAXParseException exception) throws SAXException {

			Message("**Parsing Error**\n", exception);
			throw new SAXException("Error encountered");
		}

		public void fatalError(SAXParseException exception) throws SAXException {

			Message("**Parsing Fatal Error**\n", exception);
			throw new SAXException("Fatal Error encountered");
		}
	}

	public static void main(String[] args) {
		File data = new File("./data/dblp/dblp.xml.gz");

		TypedSimpleGraph tsg = new TypedSimpleGraph(TypedEdge.class);
		try {
			NINALogger.setup();
			loadDBLPGraphFromFile(FileHandler.toInputStream(data), tsg);
			//PrintStatistics.PrintTypedGraphStatTable(tsg,
			//		"./data/dblp/testStats", "DBLPTypedGraph");
			//PrintStatistics.PrintCrazyCCF(tsg, "./data/dblp/testStats", "DBLPTypedGraph");
			//CalculateStatistics.calcAssortativity(tsg, -1);
			PrintStatistics.PrintCrazyAssortativity(tsg, "./data/dblp/testStats", "DBLPTypedGraph");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
