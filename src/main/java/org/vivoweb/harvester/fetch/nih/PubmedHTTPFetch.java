/*******************************************************************************
 * Copyright (c) 2010-2011 VIVO Harvester Team. For full list of contributors, please see the AUTHORS file provided.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the new BSD license which accompanies this distribution, and is available at http://www.opensource.org/licenses/bsd-license.html
 ******************************************************************************/
package org.vivoweb.harvester.fetch.nih;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vivoweb.harvester.util.InitLog;
import org.vivoweb.harvester.util.WebAide;
import org.vivoweb.harvester.util.args.ArgList;
import org.vivoweb.harvester.util.args.UsageException;
import org.vivoweb.harvester.util.repo.RecordHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Module for fetching PubMed Citations using the PubMed HTTP Interface Based on the example code available at the
 * PubMed Website.
 * @author Stephen V. Williams (swilliams@ctrip.ufl.edu)
 * @author Dale R. Scheppler (dscheppler@ctrip.ufl.edu)
 * @author Christopher Haines (hainesc@ctrip.ufl.edu)
 */
public class PubmedHTTPFetch extends NIHFetch {
	/**
	 * SLF4J Logger
	 */
	private static Logger log = LoggerFactory.getLogger(PubmedHTTPFetch.class);
	/**
	 * The name of the PubMed database
	 */
	private static String database = "pubmed";
	
	/**
	 * Constructor: Primary method for running a PubMed Fetch. The email address of the person responsible for this
	 * install of the program is required by NIH guidelines so the person can be contacted if there is a problem, such
	 * as sending too many queries too quickly.
	 * @param emailAddress contact email address of the person responsible for this install of the VIVO Harvester
	 * @param searchTerm query to run on pubmed data
	 * @param maxRecords maximum number of records to fetch
	 * @param batchSize number of records to fetch per batch
	 * @param rh output stream to write to
	 */
	public PubmedHTTPFetch(String emailAddress, String searchTerm, String maxRecords, String batchSize, RecordHandler rh) {
		super(emailAddress, searchTerm, maxRecords, batchSize, rh, database);
	}
	
	/**
	 * Constructor
	 * @param args commandline arguments
	 * @throws IOException error creating task
	 * @throws UsageException user requested usage message
	 */
	private PubmedHTTPFetch(String[] args) throws IOException, UsageException {
		this(getParser("PubmedHTTPFetch", database).parse(args));
	}
	
	/**
	 * Constructor
	 * @param argList parsed argument list
	 * @throws IOException error creating task
	 */
	private PubmedHTTPFetch(ArgList argList) throws IOException {
		super(argList, database);
	}
	
	@Override
	public String[] runESearch(String term, boolean logMessage) throws IOException {
		String[] env = new String[4];
		try {
			StringBuilder urlSb = new StringBuilder();
			urlSb.append("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?");
			urlSb.append("&db=");
			urlSb.append(database);
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
	
	@Override
	public void fetchRecords(String WebEnv, String QueryKey, String retStart, String numRecords) throws IOException {
		StringBuilder urlSb = new StringBuilder();
		urlSb.append("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?");
		urlSb.append("&db=");
		urlSb.append(database);
		urlSb.append("&query_key=");
		urlSb.append(QueryKey);
		urlSb.append("&WebEnv=");
		urlSb.append(WebEnv);
		urlSb.append("&tool=");
		urlSb.append(getToolName());
		urlSb.append("&email=");
		urlSb.append(getEmailAddress());
		urlSb.append("&retmode=xml");
		// set max number of records to return from search
		urlSb.append("&retmax=" + numRecords);
		// set number to start at
		urlSb.append("&retstart=" + retStart);
		//		log.debug(urlSb.toString());
		int retEnd = Integer.parseInt(retStart) + Integer.parseInt(numRecords);
		log.info("Fetching " + retStart + " to " + retEnd + " records from search");
		try {
			sanitizeXML(WebAide.getURLContents(urlSb.toString()));
		} catch(MalformedURLException e) {
			throw new IOException("Query URL incorrectly formatted", e);
		}
	}
	
	/**
	 * Sanitizes XML in preparation for writing to output stream
	 * <ol>
	 * <li>Removes xml namespace attributes</li>
	 * <li>Removes XML wrapper tag</li>
	 * <li>Splits each record on a new line</li>
	 * <li>Writes to outputstream writer</li>
	 * </ol>
	 * @param strInput The XML to Sanitize.
	 * @throws IOException Unable to write XML to record
	 */
	private void sanitizeXML(String strInput) throws IOException {
		//used to remove header from xml
		String headerRegEx = "<\\?xml.*?PubmedArticleSet>";
		//used to remove footer from xml
		String footerRegEx = "</PubmedArticleSet>";
		log.debug("Sanitizing Output");
		log.debug("XML File Length - Pre Sanitize: " + strInput.length());
//		log.debug("====== PRE-SANITIZE ======\n"+strInput);
		String newS = strInput.replaceAll(" xmlns=\".*?\"", "");
		newS = newS.replaceAll("</?RemoveMe>", "");
		//TODO: this seems really hacky here... revise somehow?
		newS = newS.replaceAll("</PubmedArticle>.*?<PubmedArticle", "</PubmedArticle>\n<PubmedArticle");
		newS = newS.replaceAll("</PubmedBookArticle>.*?<PubmedBookArticle", "</PubmedBookArticle>\n<PubmedBookArticle");
		newS = newS.replaceAll("</PubmedArticle>.*?<PubmedBookArticle", "</PubmedArticle>\n<PubmedBookArticle");
		newS = newS.replaceAll("</PubmedBookArticle>.*?<PubmedArticle", "</PubmedBookArticle>\n<PubmedArticle");
		newS = newS.replaceAll(headerRegEx, "");
		newS = newS.replaceAll(footerRegEx, "");
		log.debug("XML File Length - Post Sanitze: " + newS.length());
//		log.debug("====== POST-SANITIZE ======\n"+newS);
		log.debug("Sanitization Complete");
		log.trace("Writing to output");
		if(getOsWriter() == null) {
			setOs(PubmedFetch.baseXMLROS.clone().setRso(this));
		}
		getOsWriter().write(newS);
		//file close statements.  Warning, not closing the file will leave incomplete xml files and break the translate method
		getOsWriter().write("\n");
		getOsWriter().flush();
		log.trace("Writing complete");
	}
	
	@Override
	protected int getLatestRecord() throws IOException {
		return Integer.parseInt(runESearch("1:8000[dp]", false)[3]);
	}

	@Override
	public void writeRecord(String id, String data) throws IOException {
		log.trace("Adding Record "+id);
		getRh().addRecord(id, data, getClass());
	}
	
	/**
	 * Main method
	 * @param args commandline arguments
	 */
	public static void main(String... args) {
		Exception error = null;
		try {
			InitLog.initLogger(args, getParser("PubmedHTTPFetch", database));
			log.info("PubmedHTTPFetch: Start");
			new PubmedHTTPFetch(args).execute();
		} catch(IllegalArgumentException e) {
			log.error(e.getMessage());
			log.debug("Stacktrace:",e);
			System.out.println(getParser("PubmedHTTPFetch", database).getUsage());
			error = e;
		} catch(UsageException e) {
			log.info("Printing Usage:");
			System.out.println(getParser("PubmedHTTPFetch", database).getUsage());
			error = e;
		} catch(Exception e) {
			log.error(e.getMessage());
			log.debug("Stacktrace:",e);
			error = e;
		} finally {
			log.info("PubmedHTTPFetch: End");
			if(error != null) {
				System.exit(1);
			}
		}
	}
}
