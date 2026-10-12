/*******************************************************************************
 * Copyright (c) 2010-2011 VIVO Harvester Team. For full list of contributors, please see the AUTHORS file provided.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the new BSD license which accompanies this distribution, and is available at http://www.opensource.org/licenses/bsd-license.html
 ******************************************************************************/
package org.vivoweb.test.harvester.qualify;

import java.io.IOException;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vivoweb.harvester.qualify.ChangeNamespace;
import org.vivoweb.harvester.util.InitLog;
import org.vivoweb.harvester.util.repo.JenaConnect;
import org.vivoweb.harvester.util.repo.MemJenaConnect;
//import org.vivoweb.harvester.util.repo.RDBJenaConnect;
import org.vivoweb.harvester.util.repo.SDBJenaConnect;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/** */
public class ChangeNamespaceTest extends TestCase {
	/**
	 * SLF4J Logger
	 */
	private static Logger log = LoggerFactory.getLogger(ChangeNamespaceTest.class);
	/** */
	private JenaConnect model;
	/** */
	private JenaConnect vivo;
	/** */
	private String namespace;
	/** */
	private String newNamespace;
	
	@Override
	protected void setUp() throws Exception {
		InitLog.initLogger(null, null);
		this.namespace = "http://testChNS.vivoweb.org/individual/";
		this.newNamespace = "http://vivo.test.edu/individual/";
		this.model = new SDBJenaConnect("jdbc:h2:mem:testChNSh2change", "sa", "", "H2", "org.h2.Driver", "layout2", "testChNSchange");
		this.vivo = new MemJenaConnect();
		//this.vivo = new RDBJenaConnect("jdbc:h2:mem:testChNSh2vivo;MODE=HSQLDB", "sa", "", "HSQLDB", "org.h2.Driver", "testChNSvivo");
		String vivoData = "" +
			"<rdf:RDF" +
			"\n xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"" +
			"\n xmlns:bibo=\"http://purl.org/ontology/bibo/\"" +
			"\n xmlns:vitro=\"http://vitro.mannlib.cornell.edu/ns/vitro/public#\"" +
			"\n xmlns:localVivo=\"http://vivo.test.edu/ontology/vivo-test/\"" +
			"\n xmlns:core=\"http://vivoweb.org/ontology/core#\"" +
			"\n xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"" +
			"\n xmlns:foaf=\"http://xmlns.com/foaf/0.1/\">" +
			"\n    <rdf:Description rdf:about=\"http://vivo.test.edu/individual/n123456\">" +
			"\n        <bibo:prefixName>Dr.</bibo:prefixName>" +
			"\n        <localVivo:loginName>john.smith</localVivo:loginName>" +
			"\n        <localVivo:uniqueId>1234567890</localVivo:uniqueId>" +
			"\n        <core:workEmail>john.smith@test.edu</core:workEmail>" +
			"\n        <core:workFax>123 098 7654</core:workFax>" +
			"\n        <core:workPhone>123 456 7890</core:workPhone>" +
			"\n        <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#FacultyMember\"/>" +
			"\n        <rdf:type rdf:resource=\"http://www.w3.org/2002/07/owl#Thing\"/>" +
			"\n        <rdf:type rdf:resource=\"http://xmlns.com/foaf/0.1/Agent\"/>" +
			"\n        <rdf:type rdf:resource=\"http://xmlns.com/foaf/0.1/Person\"/>" +
			"\n        <rdfs:label xml:lang=\"en-US\">Smith, John</rdfs:label>" +
			"\n        <foaf:firstName>Smith</foaf:firstName>" +
			"\n        <foaf:lastName>John</foaf:lastName>" +
			"\n    </rdf:Description>" +
			"\n    <rdf:Description rdf:about=\"http://vivo.test.edu/individual/n654321\">" +
			"\n        <bibo:prefixName>Dr.</bibo:prefixName>" +
			"\n        <localVivo:loginName>bob.johnson</localVivo:loginName>" +
			"\n        <localVivo:uniqueId>2345678901</localVivo:uniqueId>" +
			"\n        <core:workEmail>bob.johnson@test.edu</core:workEmail>" +
			"\n        <core:workFax>123 987 6543</core:workFax>" +
			"\n        <core:workPhone>123 345 6789</core:workPhone>" +
			"\n        <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#FacultyMember\"/>" +
			"\n        <rdf:type rdf:resource=\"http://www.w3.org/2002/07/owl#Thing\"/>" +
			"\n        <rdf:type rdf:resource=\"http://xmlns.com/foaf/0.1/Agent\"/>" +
			"\n        <rdf:type rdf:resource=\"http://xmlns.com/foaf/0.1/Person\"/>" +
			"\n        <rdfs:label xml:lang=\"en-US\">Johnson, Bob</rdfs:label>" +
			"\n        <foaf:firstName>Johnson</foaf:firstName>" +
			"\n        <foaf:lastName>Robert</foaf:lastName>" +
			"\n    </rdf:Description>" +
			"\n    <rdf:Description rdf:about=\"http://vivo.test.edu/individual/n321456\">" +
			"\n        <localVivo:uniqueId>2345678901</localVivo:uniqueId>" +
			"\n        <rdfs:label xml:lang=\"en-US\">Johnson, Rob</rdfs:label>" +
			"\n    </rdf:Description>" +
			"\n    <rdf:Description rdf:about=\"http://vivobad.test.edu/wrong/n987456\">" +
			"\n        <localVivo:uniqueId>2345678901</localVivo:uniqueId>" +
			"\n        <rdfs:label xml:lang=\"en-US\">Johnson, Rob</rdfs:label>" +
			"\n    </rdf:Description>" +
			"\n</rdf:RDF>";
		this.vivo.loadRdfFromString(vivoData, null, null);
		Resource test123 = ResourceFactory.createResource(this.namespace + "test123");
		this.model.getJenaModel().add(test123, ResourceFactory.createProperty("http://www.w3.org/2000/01/rdf-schema#", "label"), "Johnson, Robert");
		this.model.getJenaModel().add(test123, ResourceFactory.createProperty("http://vivo.test.edu/ontology/vivo-test/", "uniqueId"), "2345678901");
		this.model.getJenaModel().add(test123, ResourceFactory.createProperty("http://vivoweb.org/ontology/core#", "workEmail"), "robert.johnson@sci.test.edu");
		Resource test321 = ResourceFactory.createResource(this.namespace + "test321");
		this.model.getJenaModel().add(test321, ResourceFactory.createProperty("http://www.w3.org/2000/01/rdf-schema#", "label"), "Laplace, Philip");
		this.model.getJenaModel().add(test321, ResourceFactory.createProperty("http://vivo.test.edu/ontology/vivo-test/", "uniqueId"), "1234567891");
		this.model.getJenaModel().add(test321, ResourceFactory.createProperty("http://vivoweb.org/ontology/core#", "workEmail"), "philip.laplace@test.edu");
		Resource testBad = ResourceFactory.createResource("http://norename.blah.com/blah/testBad");
		this.model.getJenaModel().add(testBad, ResourceFactory.createProperty("http://www.w3.org/2000/01/rdf-schema#", "label"), "Not Renamed");
		this.model.getJenaModel().add(testBad, ResourceFactory.createProperty("http://vivo.test.edu/ontology/vivo-test/", "uniqueId"), "1234567892");
		this.model.getJenaModel().add(testBad, ResourceFactory.createProperty("http://vivoweb.org/ontology/core#", "workEmail"), "bad@blah.com");
		log.info("testing Start");
	}
	
	@Override
	protected void tearDown() throws Exception {
		log.info("testing End");
		this.model.close();
		this.model = null;
		this.vivo.close();
		this.vivo = null;
		this.namespace = null;
		this.newNamespace = null;
		System.gc();
	}
	
	/**
	 * Test method for
	 * {@link org.vivoweb.harvester.qualify.ChangeNamespace#getUnusedURI(java.lang.String, org.vivoweb.harvester.util.repo.JenaConnect...)
	 * getUnusedURI(String namespace, JenaConnect... models)}.
	 * @throws IOException error connecting
	 */
	public void testGetUnusedURI() throws IOException {
		log.info("BEGIN testGetUnusedURI");
		String uri = ChangeNamespace.getUnusedURI(this.newNamespace, this.vivo);
		assertFalse(this.vivo.containsURI(uri));
		log.info("END testGetUnusedURI");
	}
	
	/**
	 * Test method for
	 * {@link org.vivoweb.harvester.qualify.ChangeNamespace#changeNS(org.vivoweb.harvester.util.repo.JenaConnect, org.vivoweb.harvester.util.repo.JenaConnect, java.lang.String, java.lang.String, boolean)
	 * changeNS(JenaConnect model, JenaConnect vivo, String oldNamespace, String newNamespace, boolean errorLog)}.
	 * @throws IOException error connecting
	 */
	public void testChangeNS() throws IOException {
		log.info("BEGIN testObjChangeNS");
		log.debug("VIVO");
		log.debug(this.model.exportRdfToString());
		new ChangeNamespace(this.model, this.vivo, this.namespace, this.newNamespace, false).execute();
		log.debug("Changed VIVO");
		log.debug(this.model.exportRdfToString());
		assertFalse(this.model.containsURI(this.namespace));
		String query = "" +
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" + "\n" +
			"PREFIX localVivo: <http://vivo.test.edu/ontology/vivo-test/>" + "\n" +
			"SELECT ?uri" + "\n" +
			"WHERE {" + "\n\t" +
			"?uri localVivo:uniqueId ?id ." + "\n\t" +
			"}";
		ResultSet rs = this.model.executeSelectQuery(query);
		assertTrue(rs.hasNext());
		while(rs.hasNext()) {
			String ns = rs.next().getResource("uri").getNameSpace();
			assertTrue(ns.equals(this.newNamespace) || ns.equals("http://norename.blah.com/blah/"));
		}
		System.out.println(this.model.exportRdfToString());
		log.info("END testObjChangeNS");
	}
}
