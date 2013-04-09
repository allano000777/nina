package edu.nd.nina.graph.load;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import edu.nd.nina.types.Instance;


/**
* An iterator that generates instances from an initial
* directory or set of directories. The iterator will recurse through sub-directories.
* Each filename becomes the data field of an instance, and the result of
* a user-specified regular expression pattern applied to the filename becomes
* the target value of the instance.
* <p>
* In document classification it is common that the file name in the data field
* will be subsequently processed by one or more pipes until it contains a feature vector.
* The pattern applied to the file name is often
* used to extract a directory name
* that will be used as the true label of the instance; this label is kept in the target
* field.
*
*
*  @author Tim Weninger
*/
public class LineIterator implements Iterator<Instance>
{
	ArrayList<String> lineArray;
	Iterator<String> subIterator;
	int lineCount;	

	/** Special value that means to use the directories[i].getPath() as the target name */

	/** Use as label names the directories specified in the constructor,
	 * optionally removing common prefix of all starting directories
	 */

	// added by Fuchun Peng	
	public ArrayList<String> getLineArray()
	{
		return lineArray;
	}

	/**
	 * Construct a FileIterator that will supply filenames within initial directories
	 * as instances
	 * @param directories  Array of directories to collect files from
	 * @param fileFilter   class implementing interface FileFilter that will decide which names to accept.
	 *                     May be null.
	 * @param targetPattern  regex Pattern applied to the filename whose first parenthesized group
	 *                       on matching is taken to be the target value of the generated instance. The pattern is applied to
	 *                       the directory with the matcher.find() method. If null, then all instances
*                       will have target null.
	 * @param removeCommonPrefix boolean that modifies the behavior of the STARTING_DIRECTORIES pattern,
	 *                           removing the common prefix of all initially specified directories,
	 *                          leaving the remainder of each filename as the target value.
	 *
	 */
	public LineIterator(File file) {
		this.lineArray = new ArrayList<String> ();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		String line = "";
		try {
			while((line = br.readLine()) != null){
				line = line.trim();
				if(line.isEmpty()){
					continue;
				}
				if(line.startsWith("#")){
					continue;
				}
				lineArray.add(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		subIterator = lineArray.iterator();
		
	}

	// The PipeInputIterator interface
	public Instance next ()
	{
		String nextLine = subIterator.next();		
		String targetName = null;
	
		lineCount++;
		return new Instance (nextLine, targetName, lineCount, null);
	}
	
	public void remove () {
		throw new IllegalStateException ("This Iterator<Instance> does not support remove().");
	}

	public boolean hasNext ()	{	
		return subIterator.hasNext();	
		}
	
}


