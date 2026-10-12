/*******************************************************************************
 * Copyright (c) 2010-2011 VIVO Harvester Team. For full list of contributors, please see the AUTHORS file provided.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the new BSD license which accompanies this distribution, and is available at http://www.opensource.org/licenses/bsd-license.html
 ******************************************************************************/
package org.vivoweb.test.harvester.fetch.nih;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vivoweb.harvester.fetch.nih.PubmedFetch;
import org.vivoweb.harvester.fetch.nih.PubmedHTTPFetch;
import org.vivoweb.harvester.util.InitLog;
import org.vivoweb.harvester.util.repo.JenaRecordHandler;
import org.vivoweb.harvester.util.repo.MemJenaConnect;
import org.vivoweb.harvester.util.repo.Record;
import org.vivoweb.harvester.util.repo.RecordHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Dale Scheppler (dscheppler@ctrip.ufl.edu)
 * @author Christopher Haines (hainesc@ctrip.ufl.edu)
 */
public class PubmedFetchTest extends TestCase {
	/**
	 * SLF4J Logger
	 */
	private static Logger log = LoggerFactory.getLogger(PubmedFetchTest.class);
	/** */
	private RecordHandler rh;
	
	@Override
	protected void setUp() throws Exception {
		InitLog.initLogger(null, null);
		this.rh = new JenaRecordHandler(new MemJenaConnect(), "http://vivoweb.org/harvester/test/datatype");
	}
	
	@Override
	protected void tearDown() throws Exception {
		if(this.rh != null) {
			this.rh.close();
		}
	}
	
	/**
	 * Test method for
	 * {@link org.vivoweb.harvester.fetch.nih.PubmedFetch#PubmedFetch(java.lang.String, java.lang.String, java.lang.String, java.lang.String, org.vivoweb.harvester.util.repo.RecordHandler)
	 * PubmedFetch(String emailAddress, String searchTerm, String maxRecords, String batchSize, RecordHandler rh)}.
	 * @throws IOException error
	 */
	public final void testPubmedFetchNoRecordQuery() throws IOException {
		log.info("BEGIN testPubmedFetchNoRecordQuery");
		//test no records
		boolean hasRecords = true;
		try {
			new PubmedHTTPFetch("test@test.com", "", "100", "100", this.rh).execute();
		} catch(IllegalArgumentException e) {
			hasRecords = false;
		}
		// Eliza: Don't understand why it had to throw an exception for empty records
		// Suppressed the following for now.
		assertFalse(hasRecords);
		assertFalse(this.rh.iterator().hasNext());
		log.info("END testPubmedFetchNoRecordQuery");
	}
	
	/**
	 * Test method for
	 * {@link org.vivoweb.harvester.fetch.nih.PubmedFetch#PubmedFetch(java.lang.String, java.lang.String, java.lang.String, java.lang.String, org.vivoweb.harvester.util.repo.RecordHandler)
	 * PubmedFetch(String emailAddress, String searchTerm, String maxRecords, String batchSize, RecordHandler rh)}.
	 * @throws IOException error
	 * @throws ParserConfigurationException error
	 * @throws SAXException error
	 */
	public final void testPubmedFetchOneRecord() throws IOException, ParserConfigurationException, SAXException {
		log.info("BEGIN testPubmedFetchOneRecord");
		//test 1 record
		new PubmedHTTPFetch("test@test.com", "1:8000[dp]", "1", "1", this.rh).execute();
		assertTrue(this.rh.iterator().hasNext());
		DocumentBuilder docB = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		int count = 0;
		for(Record r : this.rh) {
			log.debug("record ID: "+r.getID());
			Document doc = docB.parse(new ByteArrayInputStream(r.getData().getBytes("UTF-8")));
			Element elem = doc.getDocumentElement();
			traverseNodes(elem.getChildNodes());
			count++;
		}
		assertEquals(1, count);
		log.info("END testPubmedFetchOneRecord");
	}
	
	/**
	 * Test method for
	 * {@link org.vivoweb.harvester.fetch.nih.PubmedFetch#PubmedFetch(java.lang.String, java.lang.String, java.lang.String, java.lang.String, org.vivoweb.harvester.util.repo.RecordHandler)
	 * PubmedFetch(String emailAddress, String searchTerm, String maxRecords, String batchSize, RecordHandler rh)}.
	 * @throws IOException error
	 */
	public final void testPubmedFetchManyRecords() throws IOException {
		log.info("BEGIN testPubmedFetchManyRecords");
		//test 1200 records, batch 500
		new PubmedHTTPFetch("test@test.com", "1:8000[dp]", "1200", "500", this.rh).execute();
		assertTrue(this.rh.iterator().hasNext());
		int count = 0;
		for(Record r : this.rh) {
			log.debug("record ID: "+r.getID());
			count++;
		}
		assertEquals(1200, count);
		log.info("END testPubmedFetchManyRecords");
	}
	
	/**
	 * @param nodeList the nodes
	 */
	private void traverseNodes(NodeList nodeList) {
		for(int x = 0; x < nodeList.getLength(); x++) {
			Node child = nodeList.item(x);
			String name = child.getNodeName();
			if(!name.contains("#text")) {
				log.trace(name);
				traverseNodes(child.getChildNodes());
			}
		}
	}
}
