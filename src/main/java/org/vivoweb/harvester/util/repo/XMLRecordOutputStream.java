/*******************************************************************************
 * Copyright (c) 2010-2011 VIVO Harvester Team. For full list of contributors, please see the AUTHORS file provided.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the new BSD license which accompanies this distribution, and is available at http://www.opensource.org/licenses/bsd-license.html
 ******************************************************************************/
package org.vivoweb.harvester.util.repo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An Output Stream that breaks XML blobs into individual Records and writes to a RecordHandler
 * @author Christopher Haines (hainesc@ctrip.ufl.edu)
 */
public class XMLRecordOutputStream extends OutputStream implements Cloneable {
	/**
	 * Buffer to hold data until a complete record is formed
	 */
	private ByteArrayOutputStream buf;
	/**
	 * RecordStreamOrigin to give record back to
	 */
	private RecordStreamOrigin rso;

	/**
	 * the byte array that represent a closing record tag
	 */
	private byte[][] closeTags;
	/**
	 * the byte array that represent a opening record tag
	 */
	private byte[][] openTags;
	/**
	 * Regex to find the identifing data in the record data
	 */
	private Pattern idRegex;
	/**
	 * Prepend to each record
	 */
	private String header;
	/**
	 * Append to each record
	 */
	private String footer;
	/**
   * Store state of buffer comparison against tags to split on
   */
  private Boolean capturingRecord;

	/**
	 * Constructor
	 * @param tagsToSplitOn defines the record tag types
	 * @param headerInfo prepended to each record
	 * @param footerInfo appended to each record
	 * @param idLocationRegex regex to find the data to be used as ID
	 * @param rso RecordStreamOrigin to give record back to
	 */
	public XMLRecordOutputStream(String[] tagsToSplitOn, String headerInfo, String footerInfo, String idLocationRegex, RecordStreamOrigin rso) {
		this.capturingRecord = false;
    this.buf = new ByteArrayOutputStream();
		this.rso = rso;
		this.idRegex = Pattern.compile(idLocationRegex);
		this.closeTags = new byte[tagsToSplitOn.length][];
		this.openTags = new byte[tagsToSplitOn.length][];
		for(int x = 0; x < tagsToSplitOn.length; x++) {
			this.closeTags[x] = ("</" + tagsToSplitOn[x] + ">").getBytes();
			this.openTags[x] = ("<" + tagsToSplitOn[x] ).getBytes();
		}
		this.header = headerInfo;
		this.footer = footerInfo;
	}
	
	@Override
	public XMLRecordOutputStream clone() {
		XMLRecordOutputStream template = new XMLRecordOutputStream(new String[]{}, this.header, this.footer, this.idRegex.pattern(), this.rso);
		template.closeTags = this.closeTags;
		template.openTags = this.openTags;
		return template;
	}
	
	@Override
	public void write(int arg0) throws IOException {
	
    this.buf.write(arg0);
    byte[] a = this.buf.toByteArray();
    
    //store the state of when an opening tag is found
    if (!this.capturingRecord){
      for(int x = 0; x < this.openTags.length; x++) {
        if(compareByteArrays(a, this.openTags[x])) {
          this.capturingRecord = true;
          this.buf.reset();
          this.buf.write(this.openTags[x]);
          //System.out.println("capturing record");
        }
      }
    }

    //compare each closing tag - only if an opening tag has been found 
    if (this.capturingRecord){
      for(int x = 0; x < this.closeTags.length; x++) {
        if(compareByteArrays(a, this.closeTags[x])) {
          
          //Create the record
          String record = new String(a);
          Matcher m = this.idRegex.matcher(record);
          m.find();
          String id = m.group(1);
          
          //Write the record
          if(this.rso == null) {
            throw new IllegalArgumentException("Must provide a valid RecordStreamOrigin before writing!");
          }
          id = id.trim();
          this.rso.writeRecord(id, this.header + record.trim() + this.footer);
          this.buf.reset();
          this.capturingRecord = false;
        }
      }
    }

	}
	
	/**
	 * Compare two byte arrays
	 * @param arrayOne first to compare
	 * @param arrayTwo second to compare
	 * @return true if the last bytes in arrayOne is equivalent to arrayTwo, false otherwise
	 */
	private boolean compareByteArrays(byte[] arrayOne, byte[] arrayTwo) {
		if(arrayOne.length < arrayTwo.length) {
			return false;
		}
		int o = arrayOne.length - arrayTwo.length;
		for(int i = 0; i < arrayTwo.length; i++) {
			if(arrayOne[o + i] != arrayTwo[i]) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Set the RecordStreamOrigin
	 * @param rso the rso to use
	 * @return self
	 */
	public XMLRecordOutputStream setRso(RecordStreamOrigin rso) {
		this.rso = rso;
		return this;
	}
}
