/*******************************************************************************
 * Copyright (c) 2010-2011 VIVO Harvester Team. For full list of contributors, please see the AUTHORS file provided.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the new BSD license which accompanies this distribution, and is available at http://www.opensource.org/licenses/bsd-license.html
 ******************************************************************************/
package org.vivoweb.harvester.fetch.nih;

import java.io.IOException;
import java.net.MalformedURLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vivoweb.harvester.util.InitLog;
import org.vivoweb.harvester.util.WebAide;
import org.vivoweb.harvester.util.args.ArgList;
import org.vivoweb.harvester.util.args.UsageException;
import org.vivoweb.harvester.util.repo.RecordHandler;
import org.vivoweb.harvester.util.repo.XMLRecordOutputStream;

/**
 * NLMJournalFetch
 */
public class NLMJournalFetch extends NIHFetch {
	/**
	 * SLF4J Logger
	 */
	private static Logger log = LoggerFactory.getLogger(NLMJournalFetch.class);
	/**
	 * The name of the NLM Journals Database
	 */
	private static String database = "nlmcatalog";
	/**
	 * a base xmlrecordoutputstream
	 */
	protected static XMLRecordOutputStream baseXMLROS = new XMLRecordOutputStream(new String[]{"NLMCatalogRecord"}, "<?xml version=\"1.0\" ?>\n<!DOCTYPE NLMCatalogRecordSet PUBLIC \"-//NLM//DTD NLMCatalogRecordSet, 1st June 2017//EN\" \"https://www.nlm.nih.gov/databases/dtd/nlmcatalogrecordset_170601.dtd\">\n<NLMCatalogRecordSet>", "\n</NLMCatalogRecordSet>", ".*?<[nN][lL][mM][uU][nN][iI][qQ][uU][eE][iI][dD].*?>(.*?)</[nN][lL][mM][uU][nN][iI][qQ][uU][eE][iI][dD]>.*?", null);
	
	/**
	 * Constructor: Primary method for running a Journal Fetch. The email address of the person responsible for this
	 * install of the program is required by NIH guidelines so the person can be contacted if there is a problem, such
	 * as sending too many queries too quickly.
	 * @param emailAddress contact email address of the person responsible for this install of the VIVO Harvester
	 * @param searchTerm query to run on journal data
	 * @param maxRecords maximum number of records to fetch
	 * @param batchSize number of records to fetch per batch
	 * @param rh record handler to write to
	 */
	public NLMJournalFetch(String emailAddress, String searchTerm, String maxRecords, String batchSize, RecordHandler rh) {
		super(emailAddress, searchTerm, maxRecords, batchSize, rh, database);
	}
	
	/**
	 * Constructor
	 * @param args commandline arguments
	 * @throws IOException error creating task
	 * @throws UsageException user requested usage message
	 */
	private NLMJournalFetch(String[] args) throws IOException, UsageException {
		this(getParser("NLMJournalFetch", database).parse(args));
	}
	
	/**
	 * Constructor
	 * @param argList parsed argument list
	 * @throws IOException error creating task
	 */
	private NLMJournalFetch(ArgList argList) throws IOException {
		super(argList, database);
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
		String headerRegEx = "<\\?xml.*?NLMCatalogRecordSet>";
		//used to remove footer from xml
		String footerRegEx = "</NLMCatalogRecordSet>";
		log.debug("Sanitizing Output");
		log.debug("XML File Length - Pre Sanitize: " + strInput.length());
//		log.debug("====== PRE-SANITIZE ======\n"+strInput);
		String newS = strInput.replaceAll(" xmlns=\".*?\"", "");
		newS = newS.replaceAll("</?RemoveMe>", "");
		//TODO: this seems really hacky here... revise somehow?
		newS = newS.replaceAll("</NLMCatalogRecord>.*?<NLMCatalogRecord", "</NLMCatalogRecord>\n<NLMCatalogRecord");
		newS = newS.replaceAll(headerRegEx, "");
		newS = newS.replaceAll(footerRegEx, "");
		log.debug("XML File Length - Post Sanitze: " + newS.length());
//		log.debug("====== POST-SANITIZE ======\n"+newS);
		log.debug("Sanitization Complete");
		log.trace("Writing to output");
		if(getOsWriter() == null) {
			setOs(baseXMLROS.clone().setRso(this));
		}
		getOsWriter().write(newS);
		//file close statements.  Warning, not closing the file will leave incomplete xml files and break the translate method
		getOsWriter().write("\n");
		getOsWriter().flush();
		log.trace("Writing complete");
	}
	
/*	Non-HTTP Fetch Version... broken, but keep around incase it ever works again
	@Override
	public void fetchRecords(String WebEnv, String QueryKey, String retStart, String numRecords) throws IOException {
		EFetchJournalsServiceStub.EFetchRequest req = new EFetchJournalsServiceStub.EFetchRequest();
		req.setQuery_key(QueryKey);
		req.setWebEnv(WebEnv);
		req.setEmail(getEmailAddress());
		req.setTool(getToolName());
		req.setRetstart(retStart);
		req.setRetmax(numRecords);
		int retEnd = Integer.parseInt(retStart) + Integer.parseInt(numRecords);
		log.info("Fetching " + retStart + " to " + retEnd + " records from search");
		try {
			serializeFetchRequest(req);
		} catch(RemoteException e) {
			throw new IOException("Could not run search", e);
		}
	}
	
	/ **
	 * Runs, sanitizes, and outputs the results of a EFetch request to the xmlWriter
	 * @param req the request to run and output results
	 * @throws RemoteException error running EFetch
	 * /
	private void serializeFetchRequest(EFetchJournalsServiceStub.EFetchRequest req) throws RemoteException {
		//Create buffer for raw, pre-sanitized output
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		//Connect to NLM
		EFetchJournalsServiceStub service = new EFetchJournalsServiceStub();
		//Run the EFetch request
		EFetchResult result = service.run_eFetch(req);
		//Get the article set
		SerialsSet_type0 serialSet = result.getSerialsSet();
		XMLStreamWriter writer;
		try {
			//Create a temporary xml writer to our buffer
			writer = XMLOutputFactory.newInstance().createXMLStreamWriter(buffer);
			MTOMAwareXMLSerializer serial = new MTOMAwareXMLSerializer(writer);
			log.debug("Buffering records");
			//Output data
			serialSet.serialize(new QName("RemoveMe"), null, serial);
			serial.flush();
			log.debug("Buffering complete");
			log.debug("buffer size: " + buffer.size());
			//Dump buffer to String
			String iString = buffer.toString("UTF-8");
			//Sanitize string (which writes it to xmlWriter)
			sanitizeXML(iString);
		} catch(XMLStreamException e) {
			log.error("Unable to write to output", e);
		} catch(UnsupportedEncodingException e) {
			log.error("Cannot get xml from buffer", e);
		}
	}
	
	/ **
	 * Sanitizes XML in preparation for writing to output stream
	 * <ol>
	 * <li>Removes xml namespace attributes</li>
	 * <li>Removes XML wrapper tag</li>
	 * <li>Splits each record on a new line</li>
	 * <li>Writes to outputstream writer</li>
	 * </ol>
	 * @param strInput The XML to Sanitize.
	 * @throws IOException Unable to write XML to record
	 * /
	private void sanitizeXML(String strInput) {
		log.debug("Sanitizing Output");
		log.debug("XML File Length - Pre Sanitize: " + strInput.length());
		String newS = strInput.replaceAll(" xmlns=\".*?\"", "").replaceAll("</?RemoveMe>", "").replaceAll("</Serial>.*?<Serial", "</Serial>\n<Serial");
		log.debug("XML File Length - Post Sanitze: " + newS.length());
		log.debug("Sanitization Complete");
		try {
			log.trace("Writing to output");
			getOsWriter().write(newS);
			//file close statements.  Warning, not closing the file will leave incomplete xml files and break the translate method
			getOsWriter().write("\n");
			getOsWriter().flush();
			log.trace("Writing complete");
		} catch(IOException e) {
			log.error("Unable to write XML to file.", e);
		}
	}*/
	
	@Override
	protected int getLatestRecord() throws IOException {
		//FIXME: make this work for NLM Journal Fetch? Is relevant? if not, try to move out of NIHFetch
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
			InitLog.initLogger(args, getParser("NLMJournalFetch", database));
			log.info("NLMJournalFetch: Start");
			new NLMJournalFetch(args).execute();
		} catch(IllegalArgumentException e) {
			log.error(e.getMessage());
			log.debug("Stacktrace:",e);
			System.out.println(getParser("NLMJournalFetch", database).getUsage());
			error = e;
		} catch(UsageException e) {
			log.info("Printing Usage:");
			System.out.println(getParser("NLMJournalFetch", database).getUsage());
			error = e;
		} catch(Exception e) {
			log.error(e.getMessage());
			log.debug("Stacktrace:",e);
			error = e;
		} finally {
			log.info("NLMJournalFetch: End");
			if(error != null) {
				System.exit(1);
			}
		}
	}
	
}
