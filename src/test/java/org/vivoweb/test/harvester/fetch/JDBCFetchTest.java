/*******************************************************************************
 * Copyright (c) 2010-2011 VIVO Harvester Team. For full list of contributors, please see the AUTHORS file provided.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the new BSD license which accompanies this distribution, and is available at http://www.opensource.org/licenses/bsd-license.html
 ******************************************************************************/
package org.vivoweb.test.harvester.fetch;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vivoweb.harvester.fetch.JDBCFetch;
import org.vivoweb.harvester.util.InitLog;
import org.vivoweb.harvester.util.repo.JDBCRecordHandler;
import org.vivoweb.harvester.util.repo.Record;
import org.vivoweb.harvester.util.repo.RecordHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Christopher Haines (hainesc@ctrip.ufl.edu)
 */
public class JDBCFetchTest extends TestCase {
	/**
	 * SLF4J Logger
	 */
	private static Logger log = LoggerFactory.getLogger(JDBCFetchTest.class);
	/** */
	private Connection conn;
	/** */
	private RecordHandler rh;
	
	@Override
	protected void setUp() throws Exception {
		InitLog.initLogger(null, null);
		Class.forName("org.h2.Driver");
		this.conn = DriverManager.getConnection("jdbc:h2:mem:TestJDBCFetchDB", "sa", "");
		this.conn.setAutoCommit(false);
		Statement cursor = this.conn.createStatement();
		cursor.executeUpdate("CREATE TABLE department (dep_id INT NOT NULL AUTO_INCREMENT, `name` text NOT NULL, description text NOT NULL, PRIMARY KEY (dep_id))");
		cursor.executeUpdate("CREATE TABLE paylevel (id INT NOT NULL AUTO_INCREMENT, `name` text NOT NULL, low INT NOT NULL, high INT NOT NULL, PRIMARY KEY (id))");
		cursor.executeUpdate("CREATE TABLE faculty (fac_id INT NOT NULL AUTO_INCREMENT, badge_num INT NOT NULL, fname text NOT NULL, mname text NOT NULL, lname text NOT NULL, jobtitle text NOT NULL, salary INT NOT NULL, paygrade_id INT NOT NULL, dept_id INT NOT NULL, PRIMARY KEY (fac_id), CONSTRAINT faculty_ibfk_1 FOREIGN KEY (paygrade_id) REFERENCES paylevel(id), CONSTRAINT faculty_ibfk_2 FOREIGN KEY (dept_id) REFERENCES department(dep_id))");
		cursor.executeUpdate("INSERT INTO paylevel (id, name, low, high) VALUES (1, 'IT Expert', 100000, 300000), (2, 'IT Noob', 20000, 25000);");
		cursor.executeUpdate("INSERT INTO department (dep_id, name, description) VALUES (1, 'CTRIP', 'UF Clinical & Translational Research Informatics Program');");
		cursor.executeUpdate("INSERT INTO faculty (fac_id, badge_num, fname, mname, lname, jobtitle, salary, paygrade_id, dept_id) VALUES (1, 12345678, 'Bob', 'Alfred', 'Johnson', 'Software Engineer', 156000, 1, 1), (2, 98765432, 'Fredrick', 'Markus', 'Brown', 'Junior Software Engineer', 22500, 2, 1);");
		this.conn.commit();
	}
	
	@Override
	protected void tearDown() throws Exception {
		if(this.conn != null) {
			this.conn.close();
		}
		if(this.rh != null) {
			this.rh.close();
		}
		this.conn = null;
		this.rh = null;
	}
	
	/**
	 * Test method for
	 * {@link org.vivoweb.harvester.fetch.JDBCFetch#JDBCFetch(java.sql.Connection, org.vivoweb.harvester.util.repo.RecordHandler, java.lang.String)
	 * JDBCFetch(Connection dbConn, RecordHandler output, String uriNameSpace)}.
	 * @throws IOException error
	 * @throws ParserConfigurationException error
	 * @throws SAXException error
	 */
	public final void testJDBCFetchConstRunDBALL() throws IOException, ParserConfigurationException, SAXException {
		log.info("BEGIN testJDBCFetchConstRunDBALL");
		this.rh = new JDBCRecordHandler("org.h2.Driver", "jdbc:h2:mem:TestJDBCFetchRHDB", "sa", "", "recordTable", "dataField");
		runConstTest(new JDBCFetch(this.conn, this.rh, "jdbc:h2:mem:TestJDBCFetchDB/"));
		log.info("END testJDBCFetchConstRunDBALL");
	}
	
	/**
	 * Test method for
	 * {@link org.vivoweb.harvester.fetch.JDBCFetch#JDBCFetch(java.sql.Connection, org.vivoweb.harvester.util.repo.RecordHandler, java.lang.String)
	 * JDBCFetch(Connection dbConn, RecordHandler output, String uriNameSpace)}.
	 * @throws IOException error
	 * @throws ParserConfigurationException error
	 * @throws SAXException error
	 */
	public final void testJDBCFetchConstRunDBSpecifyID() throws IOException, ParserConfigurationException, SAXException {
		log.info("BEGIN testJDBCFetchConstRunDBSpecifyID");
		this.rh = new JDBCRecordHandler("org.h2.Driver", "jdbc:h2:mem:TestJDBCFetchRHSpecifyID", "sa", "", "recordTable", "dataField");
		HashMap<String, List<String>> idFields = new HashMap<String, List<String>>();
		idFields.put("faculty", Arrays.asList("fac_id"));
		runConstTest(new JDBCFetch(this.conn, this.rh, "jdbc:h2:mem:TestJDBCFetchDB/", null, null, null, null, null, idFields, null, null, null));
		log.info("END testJDBCFetchConstRunDBSpecifyID");
	}
	
	/**
	 * Test method for
	 * {@link org.vivoweb.harvester.fetch.JDBCFetch#JDBCFetch(java.sql.Connection, org.vivoweb.harvester.util.repo.RecordHandler, java.lang.String)
	 * JDBCFetch(Connection dbConn, RecordHandler output, String uriNameSpace)}.
	 * @throws IOException error
	 * @throws ParserConfigurationException error
	 * @throws SAXException error
	 */
	public final void testJDBCFetchConstRunQuery() throws IOException, ParserConfigurationException, SAXException {
		log.info("BEGIN testJDBCFetchConstRunQuery");
		this.rh = new JDBCRecordHandler("org.h2.Driver", "jdbc:h2:mem:TestJDBCFetchRHQuery", "sa", "", "recordTable", "dataField");
		HashMap<String, String> queryStrings = new HashMap<String, String>();
		queryStrings.put("people", "SELECT faculty.badge_num, faculty.fac_id, faculty.fname, faculty.lname, faculty.mname, faculty.jobtitle, paylevel.name, department.name, department.description FROM faculty, paylevel, department WHERE faculty.dept_id = department.dep_id AND faculty.paygrade_id = paylevel.id");
		HashMap<String, List<String>> idFields = new HashMap<String, List<String>>();
		idFields.put("people", Arrays.asList("faculty.fac_id"));
		runConstTest(new JDBCFetch(this.conn, this.rh, "jdbc:h2:mem:TestJDBCFetchDB/", null, null, null, null, null, idFields, null, null, queryStrings));
		log.info("END testJDBCFetchConstRunQuery");
	}
	
	/**
	 * run the test
	 * @param jdbcFetch the fetch to run
	 * @throws IOException error
	 * @throws ParserConfigurationException error
	 * @throws SAXException error
	 */
	private void runConstTest(JDBCFetch jdbcFetch) throws IOException, ParserConfigurationException, SAXException {
		jdbcFetch.execute();
		assertTrue(this.rh.iterator().hasNext());
		DocumentBuilder docC = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		for(Record r : this.rh) {
			log.trace("Record '" + r.getID() + "':\n" + r.getData());
			Document doc = docC.parse(new ByteArrayInputStream(r.getData().getBytes()));
			Element elem = doc.getDocumentElement();
			if(!traverseNodes(elem.getChildNodes())) {
				fail("Never encountered a non-rdf tag in record '"+r.getID()+"'");
			}
		}
	}
	
	/**
	 * @param nodeList the nodes
	 * @return whether we hit a non-rdf tag
	 */
	private boolean traverseNodes(NodeList nodeList) {
		boolean hitNonRDFtag = false;
		for(int x = 0; x < nodeList.getLength(); x++) {
			Node child = nodeList.item(x);
			String name = child.getNodeName().trim();
			if(!name.contains("#text")) {
				if(!name.startsWith("rdf:")) {
					hitNonRDFtag = true;
				}
				log.trace(name);
				hitNonRDFtag |= traverseNodes(child.getChildNodes());
			}
		}
		return hitNonRDFtag;
	}
}
