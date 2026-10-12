/*******************************************************************************
 * Copyright (c) 2010-2011 VIVO Harvester Team. For full list of contributors, please see the AUTHORS file provided.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the new BSD license which accompanies this distribution, and is available at http://www.opensource.org/licenses/bsd-license.html
 ******************************************************************************/
package org.vivoweb.test.harvester.qualify;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vivoweb.harvester.qualify.Smush;
import org.vivoweb.harvester.util.InitLog;
import org.vivoweb.harvester.util.repo.JenaConnect;
import org.vivoweb.harvester.util.repo.MemJenaConnect;
//import org.vivoweb.harvester.util.repo.RDBJenaConnect;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Testing the Ported and altered Smush Resources
 * @author jrpence
 *
 */
public class SmushTest extends TestCase {
	/**
	 * SLF4J Logger
	 */
	private static Logger log = LoggerFactory.getLogger(SmushTest.class);
	/** */
	private JenaConnect inputModel;
	/** */
	//private JenaConnect outputModel;
	/** */
	private String namespace;
	 
	/**
	 * @throws Exception passing along excpetions
	 */
	@Override
	public void setUp() throws Exception {
		InitLog.initLogger(null, null);
		this.namespace = "http://vivo.test.edu/individual/";
		this.inputModel = new MemJenaConnect();
//		this.outputModel = new SDBJenaConnect("jdbc:h2:mem:testSmushoutput", "sa", "", "H2", "org.h2.Driver", "layout2", "testSmushoutput");
		//this.inputModel = new RDBJenaConnect("jdbc:h2:mem:testSmushinput;MODE=HSQLDB", "sa", "", "HSQLDB", "org.h2.Driver", "testSmushinput");
		String testData = "" +
			"<rdf:RDF" +
			"\n xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"" +
			"\n xmlns:bibo=\"http://purl.org/ontology/bibo/\"" +
			"\n xmlns:vitro=\"http://vitro.mannlib.cornell.edu/ns/vitro/public#\"" +
			"\n xmlns:localVivo=\"http://vivo.test.edu/ontology/vivo-test/\"" +
			"\n xmlns:core=\"http://vivoweb.org/ontology/core#\"" +
			"\n xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"" +
			"\n xmlns:foaf=\"http://xmlns.com/foaf/0.1/\">" +
			"\n    <rdf:Description rdf:about=\"http://vivo.test.edu/individual/n1231456\">" +
			"\n        <localVivo:uniqueId>1234567890</localVivo:uniqueId>" +
			"\n        <bibo:prefixName>Dr.</bibo:prefixName>" +
			"\n        <localVivo:loginName>john.smith</localVivo:loginName>" +
			"\n    </rdf:Description>" +
			"\n    <rdf:Description rdf:about=\"http://vivo.test.edu/individual/n1232456\">" +
			"\n        <localVivo:uniqueId>1234567890</localVivo:uniqueId>" +
			"\n        <core:workEmail>john.smith@test.edu</core:workEmail>" +
			"\n        <core:workFax>123 098 7654</core:workFax>" +
			"\n    </rdf:Description>" +
			"\n    <rdf:Description rdf:about=\"http://vivo.test.edu/individual/n1233456\">" +
			"\n        <localVivo:uniqueId>1234567890</localVivo:uniqueId>" +
			"\n        <core:workPhone>123 456 7890</core:workPhone>" +
			"\n        <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#FacultyMember\"/>" +
			"\n        <rdf:type rdf:resource=\"http://www.w3.org/2002/07/owl#Thing\"/>" +
			"\n        <rdfs:label xml:lang=\"en-US\">Smith, John</rdfs:label>" +
			"\n    </rdf:Description>" +
			"\n    <rdf:Description rdf:about=\"http://vivo.test.edu/individual/n1234456\">" +
			"\n        <localVivo:uniqueId>1234567890</localVivo:uniqueId>" +
			"\n        <rdf:type rdf:resource=\"http://xmlns.com/foaf/0.1/Agent\"/>" +
			"\n        <rdf:type rdf:resource=\"http://xmlns.com/foaf/0.1/Person\"/>" +
			"\n    </rdf:Description>" +
			"\n    <rdf:Description rdf:about=\"http://vivo.test.edu/individual/n1235456\">" +
			"\n        <localVivo:uniqueId>1234567890</localVivo:uniqueId>" +
			"\n        <rdfs:label xml:lang=\"en-US\">Smith, John</rdfs:label>" +
			"\n        <foaf:firstName>Smith</foaf:firstName>" +
			"\n    </rdf:Description>" +
			"\n    <rdf:Description rdf:about=\"http://vivo.test.edu/individual/n1236456\">" +
			"\n        <localVivo:uniqueId>1234567890</localVivo:uniqueId>" +
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
		this.inputModel.loadRdfFromStream(new ByteArrayInputStream(testData.getBytes()), null, null);
		Resource test123 = ResourceFactory.createResource(this.namespace + "test123");
		this.inputModel.getJenaModel().add(test123, ResourceFactory.createProperty("http://www.w3.org/2000/01/rdf-schema#", "label"), "Johnson, Robert");
		this.inputModel.getJenaModel().add(test123, ResourceFactory.createProperty("http://vivo.test.edu/ontology/vivo-test/", "uniqueId"), "2345678901");
		this.inputModel.getJenaModel().add(test123, ResourceFactory.createProperty("http://vivoweb.org/ontology/core#", "workEmail"), "robert.johnson@sci.test.edu");
		Resource test321 = ResourceFactory.createResource(this.namespace + "test321");
		this.inputModel.getJenaModel().add(test321, ResourceFactory.createProperty("http://www.w3.org/2000/01/rdf-schema#", "label"), "Laplace, Philip");
		this.inputModel.getJenaModel().add(test321, ResourceFactory.createProperty("http://vivo.test.edu/ontology/vivo-test/", "uniqueId"), "1234567891");
		this.inputModel.getJenaModel().add(test321, ResourceFactory.createProperty("http://vivoweb.org/ontology/core#", "workEmail"), "philip.laplace@test.edu");
		Resource testBad = ResourceFactory.createResource("http://norename.blah.com/blah/testBad");
		this.inputModel.getJenaModel().add(testBad, ResourceFactory.createProperty("http://www.w3.org/2000/01/rdf-schema#", "label"), "Not Renamed");
		this.inputModel.getJenaModel().add(testBad, ResourceFactory.createProperty("http://vivo.test.edu/ontology/vivo-test/", "uniqueId"), "1234567892");
		this.inputModel.getJenaModel().add(testBad, ResourceFactory.createProperty("http://vivoweb.org/ontology/core#", "workEmail"), "bad@blah.com");
		log.info("testing Start");
	}
	
	/**
	 * 
	 */
	@Override
	public void tearDown() {
		log.info("testing End");
		this.inputModel.close();
		this.inputModel = null;
//		this.outputModel.close();
//		this.outputModel = null;
		this.namespace = null;
	}
	
	/**
	 * @throws IOException incase there is an ioerror from exportRdfToString
	 * 
	 */
	public void testExecSmushResources() throws IOException {
		log.info("BEGIN testExecSmushResources");
		List<String> predicates = new ArrayList<String>();
		predicates.add("http://vivo.test.edu/ontology/vivo-test/uniqueId");
		
		Smush testSubject = new Smush(this.inputModel,predicates,this.namespace);

		testSubject.execute();
		{
			log.trace("The output model :\n" + this.inputModel.exportRdfToString());
			StringBuilder query = new StringBuilder();
	
			query.append("PREFIX rdf:	<http://www.w3.org/1999/02/22-rdf-syntax-ns#>");
			query.append("PREFIX bibo:	<http://purl.org/ontology/bibo/>");
			query.append("PREFIX vitro:	<http://vitro.mannlib.cornell.edu/ns/vitro/public#>");
			query.append("PREFIX localVivo:	<http://vivo.test.edu/ontology/vivo-test/>");
			query.append("PREFIX core:	<http://vivoweb.org/ontology/core#>");
			query.append("PREFIX rdfs:	<http://www.w3.org/2000/01/rdf-schema#>");
			query.append("PREFIX foaf:	<http://xmlns.com/foaf/0.1/>");
			query.append("SELECT ?uri WHERE{");
			query.append("?uri localVivo:uniqueId \"1234567890\" .");
			query.append("}");
			ResultSet rs = this.inputModel.executeSelectQuery(query.toString());
			log.debug("query result set :\n");
			ArrayList<String> list = new ArrayList<String>();
			for(String var : rs.getResultVars()){
				while(rs.hasNext()){
					String line = rs.next().get(var).toString();
					list.add(line);
					log.debug(line);
				}
			}
			assertTrue(list.size() == 1);//Node of the proper namespace is reduced
		}
		
		log.info("END testExecSmushResources");
	}
	
	/**
	 * @throws IOException incase there is an ioerror from exportRdfToString
	 * 
	 */
	public void tesExectNotSmushResources() throws IOException {
		log.info("BEGIN testExecNotSmushResources");
		List<String> predicates = new ArrayList<String>();
		predicates.add("http://vivo.test.edu/ontology/vivo-test/uniqueId");
		
		Smush testSubject = new Smush(this.inputModel,predicates,this.namespace);

		testSubject.execute();

		{
			log.debug("The output model :\n" + this.inputModel.exportRdfToString());
			StringBuilder query = new StringBuilder();
	
			query.append("PREFIX rdf:	<http://www.w3.org/1999/02/22-rdf-syntax-ns#>");
			query.append("PREFIX bibo:	<http://purl.org/ontology/bibo/>");
			query.append("PREFIX vitro:	<http://vitro.mannlib.cornell.edu/ns/vitro/public#>");
			query.append("PREFIX localVivo:	<http://vivo.test.edu/ontology/vivo-test/>");
			query.append("PREFIX core:	<http://vivoweb.org/ontology/core#>");
			query.append("PREFIX rdfs:	<http://www.w3.org/2000/01/rdf-schema#>");
			query.append("PREFIX foaf:	<http://xmlns.com/foaf/0.1/>");
			query.append("SELECT ?uri WHERE{");
			query.append("?uri localVivo:uniqueId ?lbl .");
			query.append("}");
			ResultSet rs = this.inputModel.executeSelectQuery(query.toString());
			log.debug("query result set :\n");
			ArrayList<String> list = new ArrayList<String>();
			for(String var : rs.getResultVars()){
				while(rs.hasNext()){
					String line = rs.next().get(var).toString();
					list.add(line);
					log.debug(line);
				}
			}
			assertTrue(list.size() == 5);//Nodes of the improper namespace is not reduced
		}

		log.info("END testExecNotSmushResources");
	}
	
}
