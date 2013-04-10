package edu.nd.nina.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

public class DBLP {

	/**
	 * . Within this archive, you will find one plain text file. Each line
	 * begins with an identifier for the data found on that line, as is
	 * described at the Arnetminer dataset website:
	 * 
	 * #* --- paperTitle 

	 * #@ --- Authors
	 * 
	 * #year ---- Year
	 * 
	 * #conf --- publication venue
	 * 
	 * #citation --- number of citations for this paper
	 * 
	 * #index ---- index id of this paper
	 * 
	 * #% ---- the id of references of this paper (there are multiple lines,
	 * with each indicating a reference)
	 * 
	 * @param dblpGraphFile
	 */
	public static void loadDBLPGraphFromFile(File dblpGraphFile){
		BZip2CompressorInputStream bz2 = null;
		try {
			bz2 = new BZip2CompressorInputStream(
					new FileInputStream(dblpGraphFile));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(bz2));

		String line = "";
		try {
			
			while((line = br.readLine() ) != null){
				if(line.trim().isEmpty()){
					//store
					//addPaper();
				}else if(line.startsWith("#*")){
					//paper title					
				}else if(line.startsWith("#*")){
					//paper title
				}else if(line.startsWith("#*")){
					//paper title
				}else if(line.startsWith("#*")){
					//paper title
				}else if(line.startsWith("#*")){
					//paper title
				}else if(line.startsWith("#*")){
					//paper title
				}else if(line.startsWith("#*")){
					//paper title
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args){
		String data = "/mnt/fcroot/full-arnetminer/arnetminer_full.txt.bz2";
		
	}
}
