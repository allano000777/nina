package edu.nd.nina.io;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

public class NINALogger {
	  static private java.util.logging.FileHandler fileTxt;
	  static private SimpleFormatter formatterTxt;

	  static public void setup() throws IOException {

	    // Get the global logger to configure it
	    java.util.logging.Logger logger = java.util.logging.Logger.getLogger("");

	    logger.setLevel(Level.INFO);
	    fileTxt = new java.util.logging.FileHandler("logging.txt");

	    // Create txt Formatter
	    formatterTxt = new SimpleFormatter();
	    fileTxt.setFormatter(formatterTxt);
	    logger.addHandler(fileTxt);

	  }
	} 