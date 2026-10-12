/*******************************************************************************
 * Copyright (c) 2010-2011 VIVO Harvester Team. For full list of contributors, please see the AUTHORS file provided.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the new BSD license which accompanies this distribution, and is available at http://www.opensource.org/licenses/bsd-license.html
 ******************************************************************************/
package org.vivoweb.harvester.fetch.nih;

import gov.nih.nlm.ncbi.www.soap.eutils.EUtilsServiceStub;
import gov.nih.nlm.ncbi.www.soap.eutils.EUtilsServiceStub.IdListType;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vivoweb.harvester.util.WebAide;
import org.vivoweb.harvester.util.args.ArgDef;
import org.vivoweb.harvester.util.args.ArgList;
import org.vivoweb.harvester.util.args.ArgParser;
import org.vivoweb.harvester.util.repo.RecordHandler;
import org.vivoweb.harvester.util.repo.RecordStreamOrigin;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Shared code for modules for fetching NIH data using the SOAP or HTML Interface Based on the example code available at
 * the NIH Website.
 * @author Stephen V. Williams (swilliams@ctrip.ufl.edu)
 * @author Dale R. Scheppler (dscheppler@ctrip.ufl.edu)
 * @author Christopher Haines (hainesc@ctrip.ufl.edu)
 */
public abstract class NIHFetch implements RecordStreamOrigin {
	/**
	 * SLF4J Logger
	 */
	private static Logger log = LoggerFactory.getLogger(NIHFetch.class);
	/**
	 * Email address to contact in case of issues
	 */
	private String emailAddress;
	/**
	 * tool identifier
	 */
	private final String toolName = "VIVO_Harvester_(vivo.sourceforge.net)";
	/**
	 * Writer for our output stream
	 */
	private OutputStreamWriter osWriter;
	/**
	 * Query to run on data
	 */
	private String searchTerm;
	/**
	 * Maximum number of records to fetch
	 */
	private String maxRecords;
	/**
	 * Number of records to fetch per batch
	 */
	private String batchSize;
	/**
	 * Database name
	 */
	private String databaseName;
	/**
	 * The Record Handler to write to
	 */
	private RecordHandler rh;
	
	/**
	 * Constructor: Primary method for running an NIH Fetch. The email address of the person responsible for this
	 * install of the program is required by NIH guidelines so the person can be contacted if there is a problem, such
	 * as sending too many queries too quickly.
	 * @param emailAddress contact email address of the person responsible for this install of the VIVO Harvester
	 * @param searchTerm query to run on data
	 * @param maxRecords maximum number of records to fetch
	 * @param batchSize number of records to fetch per batch
	 * @param rh record handler to write to
	 * @param database database name
	 */
	protected NIHFetch(String emailAddress, String searchTerm, String maxRecords, String batchSize, RecordHandler rh, String database) {
		this.emailAddress = emailAddress; // NIH Will email this person if there is a problem
		this.searchTerm = searchTerm;
		this.maxRecords = maxRecords;
		this.batchSize = batchSize;
		this.databaseName = database;
		this.rh = rh;
		//TODO Erroroneous input checking
	}
	
	/**
	 * Constructor
	 * @param argList parsed argument list
	 * @param database database name
	 * @throws IOException error creating task
	 */
	protected NIHFetch(ArgList argList, String database) throws IOException {
		this(argList.get("m"), argList.get("t"), argList.get("n"), argList.get("b"), RecordHandler.parseConfig(argList.get("o"), argList.getValueMap("O")), database);
	}
	
	/**
	 * Performs an ESearch against NIH database and returns the query web environment/query key data
	 * @param term search term to run against database
	 * @return String[] = {WebEnv, QueryKey, number of records found, first record ID} from the search
	 * @throws IOException error processing search
	 */
	public String[] runESearch(String term) throws IOException {
		return runESearch(term, true);
	}
	
	/**
	 * Performs an ESearch against NIH database and returns the query web environment/query key data
	 * @param term search term to run against database
	 * @param logMessage do we write log messages
	 * @return String[] = {WebEnv, QueryKey, number of records found, first record ID} from the search
	 * @throws IOException error processing search
	 */
	public String[] runESearch(String term, boolean logMessage) throws IOException {
		String[] env = new String[4];
		try {
			StringBuilder urlSb = new StringBuilder();
			urlSb.append("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?");
			urlSb.append("&db=");
			urlSb.append(databaseName);
			urlSb.append("&tool=");
			urlSb.append(urlEncode(getToolName()));
			urlSb.append("&email=");
			urlSb.append(getEmailAddress());
			urlSb.append("&usehistory=y");
			urlSb.append("&retmode=xml");
			urlSb.append("&term=");
			urlSb.append(urlEncode(term));
			if(logMessage) {
				//				log.debug(urlSb.toString());
			}
			
			DocumentBuilderFactory docBuildFactory = DocumentBuilderFactory.newInstance();
			docBuildFactory.setIgnoringComments(true);

			String xmlResponse = WebAide.getURLContents(urlSb.toString());
			Document doc = docBuildFactory.newDocumentBuilder().parse(new InputSource(new ByteArrayInputStream(xmlResponse.getBytes(
				StandardCharsets.UTF_8))));

			Node webEnv = doc.getElementsByTagName("WebEnv").item(0);
			if (webEnv == null) {
				throw new IllegalArgumentException();
			}
			env[0] = webEnv.getTextContent();
			env[1] = doc.getElementsByTagName("QueryKey").item(0).getTextContent();
			env[2] = doc.getElementsByTagName("Count").item(0).getTextContent();
			env[3] = doc.getElementsByTagName("Id").item(0).getTextContent();
			if(logMessage) {
				log.info("Query resulted in a total of " + env[2] + " records.");
			}
		} catch(MalformedURLException e) {
			throw new IOException(e);
		} catch(SAXException e) {
			throw new IOException(e);
		} catch(ParserConfigurationException e) {
			throw new IOException(e);
		}
		return env;
	}

	private String urlEncode(String value) {
		try {
			return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e); // Should never happen
		}
	}
	/**
	 * Executes the task
	 * @throws IOException error processing search
	 */
	public void execute() throws IOException {
		int recToFetch;
		if(getMaxRecords().equalsIgnoreCase("all")) {
			recToFetch = getLatestRecord();
		} else {
			recToFetch = Integer.parseInt(this.maxRecords);
		}
		int intBatchSize = Integer.parseInt(this.batchSize);
//		log.debug("recToFetch: "+recToFetch);
//		log.debug("intBatchSize: "+intBatchSize);
		String[] env = null;
		env = runESearch(this.searchTerm);

    	// eliza: handles null value that may be returned from the getCount() method
		boolean isNumeric = true;
        for (int i = 0; i < env[2].length(); i++) {
            if (!Character.isDigit(env[2].charAt(i))) {
            	isNumeric = false;
            	break;
            }
        }
        if (!isNumeric) { 
        	env[2] = "0";
        }
        // ends

		int resultsCount = Integer.parseInt(env[2]);

		// Eliza: comment out this line
		// if(env != null) {
		// Eliza: use this instead
		if(env != null && resultsCount > 0) {
			if(recToFetch <= intBatchSize) {
				fetchRecords(env, "0", "" + recToFetch);
			} else {
				String WebEnv = env[0];
				String QueryKey = env[1];
				// sanity check for max records
				if(Integer.parseInt(env[2]) < recToFetch) {
					recToFetch = Integer.parseInt(env[2]);
				}
//				log.debug("recToFetch: "+recToFetch);
				log.info("Fetching " + recToFetch + " records from search");
				for(int x = recToFetch; x > 0; x -= intBatchSize) {
					int maxRec = (x <= intBatchSize) ? x : intBatchSize;
					int startRec = recToFetch - x;
//					log.debug("maxRec: "+maxRec);
//					log.debug("startRec: "+startRec);
					fetchRecords(WebEnv, QueryKey, startRec + "", maxRec + "");
				}
			}
		}
	}
	
	/**
	 * Get latest NIH result
	 * @return latest record
	 * @throws IOException error processing search
	 */
	protected abstract int getLatestRecord() throws IOException;
	
	/**
	 * Performs an NIH Fetch using a previously defined esearch environment and querykey
	 * @param WebEnv web environment from an ESearch
	 * @param QueryKey query key from an ESearch
	 * @param retStart record number (out of the total - eg: '1200' out of 15000 records), not the record ID
	 * @param numRecords The number of records to fetch
	 * @throws IOException error fetching records
	 */
	protected abstract void fetchRecords(String WebEnv, String QueryKey, String retStart, String numRecords) throws IOException;
	
	/**
	 * Performs an NIH Fetch using a previously defined esearch environment and querykey
	 * @param env {WebEnv, QueryKey, number of records found} - from ESearch
	 * @throws IOException error fetching records
	 */
	public void fetchRecords(String[] env) throws IOException {
		if(env.length < 3) {
			throw new IllegalArgumentException("Invalid env. Must contain {WebEnv, QueryKey, number of records found}");
		}
		fetchRecords(env[0], env[1], "0", env[2]);
	}
	
	/**
	 * Performs an NIH Fetch using a previously defined esearch environment and querykey
	 * @param env {WebEnv, QueryKey, number of records found} - from ESearch
	 * @param retStart record number (out of the total - eg: '1200' out of 15000 records), not the record ID
	 * @param numRecords The number of records to fetch
	 * @throws IOException error fetching records
	 */
	public void fetchRecords(String[] env, String retStart, String numRecords) throws IOException {
		if(env.length < 2) {
			throw new IllegalArgumentException("Invalid env. Must contain {WebEnv, QueryKey}");
		}
		fetchRecords(env[0], env[1], retStart, numRecords);
	}
	
	/**
	 * Get the ArgParser for this task
	 * @param appName the application name
	 * @param database the database name
	 * @return the ArgParser
	 */
	protected static ArgParser getParser(String appName, String database) {
		ArgParser parser = new ArgParser(appName);
		parser.addArgument(new ArgDef().setShortOption('m').setLongOpt("email").setDescription("your contact email address").withParameter(true, "EMAIL_ADDRESS"));
		parser.addArgument(new ArgDef().setShortOption('o').setLongOpt("output").setDescription("RecordHandler config file path").withParameter(true, "CONFIG_FILE"));
		parser.addArgument(new ArgDef().setShortOption('O').setLongOpt("outputOverride").withParameterValueMap("RH_PARAM", "VALUE").setDescription("override the RH_PARAM of output recordhandler using VALUE").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('t').setLongOpt("termSearch").setDescription("term to search against "+database+" repository").withParameter(true, "SEARCH_STRING").setDefaultValue("1:8000[dp]"));
		parser.addArgument(new ArgDef().setShortOption('n').setLongOpt("numRecords").setDescription("maximum records to return").withParameter(true, "NUMBER").setDefaultValue("100"));
		parser.addArgument(new ArgDef().setShortOption('b').setLongOpt("batchSize").setDescription("number of records to fetch per batch").withParameter(true, "NUMBER").setDefaultValue("1000"));
		return parser;
	}
	
	/**
	 * @return the emailAddress
	 */
	protected String getEmailAddress() {
		return this.emailAddress;
	}
	
	/**
	 * @param emailAddress the emailAddress to set
	 */
	protected void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}
	
	/**
	 * @return the osWriter
	 */
	protected OutputStreamWriter getOsWriter() {
		return this.osWriter;
	}
	
	/**
	 * Set the outputstream
	 * @param os the outputstream to write to
	 */
	protected void setOs(OutputStream os) {
		this.osWriter = new OutputStreamWriter(os);
	}
	
	/**
	 * @return the searchTerm
	 */
	protected String getSearchTerm() {
		return this.searchTerm;
	}
	
	/**
	 * @param searchTerm the searchTerm to set
	 */
	protected void setSearchTerm(String searchTerm) {
		this.searchTerm = searchTerm;
	}
	
	/**
	 * @return the maxRecords
	 */
	protected String getMaxRecords() {
		return this.maxRecords;
	}
	
	/**
	 * @param maxRecords the maxRecords to set
	 */
	protected void setMaxRecords(String maxRecords) {
		this.maxRecords = maxRecords;
	}
	
	/**
	 * @return the batchSize
	 */
	protected String getBatchSize() {
		return this.batchSize;
	}
	
	/**
	 * @param batchSize the batchSize to set
	 */
	protected void setBatchSize(String batchSize) {
		this.batchSize = batchSize;
	}
	
	/**
	 * @return the toolName
	 */
	protected String getToolName() {
		return this.toolName;
	}
	
	/**
	 * Get the recordhandler
	 * @return the recordhandler
	 */
	protected RecordHandler getRh() {
		return this.rh;
	}
}
