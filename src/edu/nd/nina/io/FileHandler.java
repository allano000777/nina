package edu.nd.nina.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

enum Ext {TXT, BZ2, GZ};

public class FileHandler {
	
	public static InputStream toInputStream(File data) throws IOException {
		if (data.isFile() && data.canRead()) {
			String extension = data.getName().substring(
					data.getName().lastIndexOf(".") + 1);
			Ext x = Ext.valueOf(extension.toUpperCase());
			switch (x) {
			case TXT:
				return txt(data);
			case BZ2:
				return bzip2(data);
			case GZ:
				return gz(data);
			default:
				throw new UnsupportedOperationException("Cannot process "
						+ extension);
			}
		} else {
			throw new IOException("Not a readable file");
		}
	}

	private static InputStream bzip2(File f){
		BZip2CompressorInputStream bz2 = null;
		try {
			bz2 = new BZip2CompressorInputStream(
					new FileInputStream(f));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return bz2;
	}
	
	private static InputStream gz(File f){
		GzipCompressorInputStream bz2 = null;
		try {
			bz2 = new GzipCompressorInputStream(
					new FileInputStream(f));
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return bz2;
	}

	private static InputStream txt(File f){
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(f);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}
		return fis;
	}
	
}
