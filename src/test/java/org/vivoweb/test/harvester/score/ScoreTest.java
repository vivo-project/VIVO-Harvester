/*******************************************************************************
 * Copyright (c) 2010-2011 VIVO Harvester Team. For full list of contributors, please see the AUTHORS file provided.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the new BSD license which accompanies this distribution, and is available at http://www.opensource.org/licenses/bsd-license.html
 ******************************************************************************/
package org.vivoweb.test.harvester.score;

import java.io.IOException;
import java.util.HashMap;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vivoweb.harvester.score.Match;
import org.vivoweb.harvester.score.Score;
import org.vivoweb.harvester.score.algorithm.Algorithm;
import org.vivoweb.harvester.score.algorithm.CaseInsensitiveInitialTest;
import org.vivoweb.harvester.score.algorithm.EqualityTest;
import org.vivoweb.harvester.score.algorithm.NormalizedDoubleMetaphoneDifference;
import org.vivoweb.harvester.util.InitLog;
import org.vivoweb.harvester.util.repo.JenaConnect;
import org.vivoweb.harvester.util.repo.MemJenaConnect;
import org.vivoweb.harvester.util.repo.SDBJenaConnect;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * @author Nicholas Skaggs (nskaggs@ctrip.ufl.edu)
 */
public class ScoreTest extends TestCase {
	/**
	 * SLF4J Logger
	 */
	private static Logger log = LoggerFactory.getLogger(ScoreTest.class);
	/**
	 * Score input test file
	 */
	protected static final String inputRDF = "" +
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
		"<rdf:RDF xmlns:bibo=\"http://purl.org/ontology/bibo/\" " +
				"xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" " +
				"xmlns:owlPlus=\"http://www.w3.org/2006/12/owl2-xml#\" " +
				"xmlns:xs=\"http://www.w3.org/2001/XMLSchema#\" " +
				"xmlns:skos=\"http://www.w3.org/2008/05/skos#\" " +
				"xmlns:owl=\"http://www.w3.org/2002/07/owl#\" " +
				"xmlns:vocab=\"http://purl.org/vocab/vann/\" " +
				"xmlns:swvocab=\"http://www.w3.org/2003/06/sw-vocab-status/ns#\" " +
				"xmlns:localVIVO=\"http://vivo.mydomain.edu/ontology/vivo-local/\" " +
				"xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" " +
				"xmlns:dc=\"http://purl.org/dc/elements/1.1/\" " +
				"xmlns:core=\"http://vivoweb.org/ontology/core#\" " +
				"xmlns:vitro=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#\" " +
				"xmlns:foaf=\"http://xmlns.com/foaf/0.1/\" " +
				"xmlns:score=\"http://vivoweb.org/ontology/score#\">" +
			"<rdf:Description rdf:about=\"http://vivoweb.org/pubmed/article/pmid23656776\">" +
				"<rdf:type rdf:resource=\"http://purl.org/ontology/bibo/Document\"/>" +
				"<rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>" +
				"<bibo:pmid>12345678</bibo:pmid>" +
				"<rdfs:label>Batch 5 effects in controlled human testing</rdfs:label>" +
				"<core:Title>Batch 5 effects in controlled human testing</core:Title>" +
				"<score:Affiliation>Room 5 Downing Street, London, England. v@mydomain.edu</score:Affiliation>" +
				"<bibo:volume>40</bibo:volume>" +
				"<bibo:number>2</bibo:number>" +
				"<core:Year>2010</core:Year>" +
				"<core:informationResourceInAuthorship rdf:resource=\"http://vivoweb.org/pubmed/article/pmid20113680/authorship1\"/>" +
				"<core:informationResourceInAuthorship rdf:resource=\"http://vivoweb.org/pubmed/article/pmid20113680/authorship2\"/>" +
				"<core:informationResourceInAuthorship rdf:resource=\"http://vivoweb.org/pubmed/article/pmid20113680/authorship3\"/>" +
				"<core:informationResourceInAuthorship rdf:resource=\"http://vivoweb.org/pubmed/article/pmid20113680/authorship4\"/>" +
				"<core:hasSubjectArea rdf:nodeID=\"pmid20113680mesh1\"/>" +
				"<core:hasSubjectArea rdf:nodeID=\"pmid20113680mesh2\"/>" +
				"<core:hasSubjectArea rdf:nodeID=\"pmid20113680mesh3\"/>" +
				"<core:hasSubjectArea rdf:nodeID=\"pmid20113680mesh4\"/>" +
				"<core:hasSubjectArea rdf:nodeID=\"pmid23656776mesh5\"/>" +
				"<core:hasSubjectArea rdf:nodeID=\"pmid23656776mesh6\"/>" +
				"<core:hasSubjectArea rdf:nodeID=\"pmid23656776mesh7\"/>" +
				"<core:hasPublicationVenue rdf:resource=\"http://vivoweb.org/pubmed/journal/j1558-4623\"/>" +
				"<score:hasCreateDate rdf:resource=\"http://vivoweb.org/pubmed/article/pmid23656776/dateCreated\"/>" +
				"<score:hasCompleteDate rdf:resource=\"http://vivoweb.org/pubmed/article/pmid23656776/dateCompleted\"/>" +
			"</rdf:Description>" +
			"<rdf:Description rdf:about=\"http://vivoweb.org/pubmed/article/pmid23656776/authorship1\">" +
				"<rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#Authorship\"/>" +
				"<rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>" +
				"<rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#DependentResource\"/>" +
				"<rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#DependentResource\"/>" +
				"<core:linkedAuthor rdf:resource=\"http://vivoweb.org/pubmed/article/pmid23656776/author1\"/>" +
				"<core:linkedInformationResource rdf:resource=\"http://vivoweb.org/pubmed/article/pmid23656776\"/>" +
				"<core:authorRank rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">1</core:authorRank>" +
			"</rdf:Description>" +
			"<rdf:Description rdf:about=\"http://vivoweb.org/pubmed/article/pmid23656776/author1\">" +
				"<rdf:type rdf:resource=\"http://xmlns.com/foaf/0.1/Person\"/>" +
				"<rdfs:label>Fawkes, Guy</rdfs:label>" +
				"<foaf:lastName>Fawkes</foaf:lastName>" +
				"<score:workEmail>v@mydomain.edu</score:workEmail>" +
				"<score:foreName>Guy</score:foreName>" +
				"<core:middleName>G</core:middleName>" +
				"<score:initials>GF</score:initials>" +
				"<score:suffix/>" +
				"<rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>" +
				"<core:authorInAuthorship rdf:resource=\"http://vivoweb.org/pubmed/article/pmid23656776/authorship1\"/>" +
			"</rdf:Description>" +
			"<rdf:Description rdf:about=\"http://vivoweb.org/pubmed/article/pmid23656776/authorship2\">" +
				"<rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#Authorship\"/>" +
				"<rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>" +
				"<rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#DependentResource\"/>" +
				"<rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#DependentResource\"/>" +
				"<core:linkedAuthor rdf:resource=\"http://vivoweb.org/pubmed/article/pmid23656776/author2\"/>" +
				"<core:linkedInformationResource rdf:resource=\"http://vivoweb.org/pubmed/article/pmid23656776\"/>" +
				"<core:authorRank rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">2</core:authorRank>" +
			"</rdf:Description>" +
			"<rdf:Description rdf:about=\"http://vivoweb.org/pubmed/article/pmid23656776/author2\">" +
				"<rdf:type rdf:resource=\"http://xmlns.com/foaf/0.1/Person\"/>" +
				"<rdfs:label>Mans, Dude</rdfs:label>" +
				"<foaf:lastName>Mans</foaf:lastName>" +
				"<score:workEmail>v@mydomain.edu</score:workEmail>" +
				"<score:foreName>Dude</score:foreName>" +
				"<core:middleName>G</core:middleName>" +
				"<score:initials>DM</score:initials>" +
				"<score:suffix/>" +
				"<rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>" +
				"<core:authorInAuthorship rdf:resource=\"http://vivoweb.org/pubmed/article/pmid23656776/authorship2\"/>" +
			"</rdf:Description>" +
			"<rdf:Description rdf:about=\"http://vivoweb.org/pubmed/article/pmid23656776/authorship3\">" +
				"<rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#Authorship\"/>" +
				"<rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>" +
				"<rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#DependentResource\"/>" +
				"<rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#DependentResource\"/>" +
				"<core:linkedAuthor rdf:resource=\"http://vivoweb.org/pubmed/article/pmid23656776/author3\"/>" +
				"<core:linkedInformationResource rdf:resource=\"http://vivoweb.org/pubmed/article/pmid23656776\"/>" +
				"<core:authorRank rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">3</core:authorRank>" +
			"</rdf:Description>" +
			"<rdf:Description rdf:about=\"http://vivoweb.org/pubmed/article/pmid23656776/author3\">" +
				"<rdf:type rdf:resource=\"http://xmlns.com/foaf/0.1/Person\"/>" +
				"<rdfs:label>Boogle, Oggle</rdfs:label>" +
				"<foaf:lastName>Boogle</foaf:lastName>" +
				"<score:workEmail>v@mydomain.edu</score:workEmail>" +
				"<score:foreName>Oggle</score:foreName>" +
				"<core:middleName>D</core:middleName>" +
				"<score:initials>ODB</score:initials>" +
				"<score:suffix/>" +
				"<rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>" +
				"<core:authorInAuthorship rdf:resource=\"http://vivoweb.org/pubmed/article/pmid23656776/authorship3\"/>" +
			"</rdf:Description>" +
			"<rdf:Description rdf:about=\"http://vivoweb.org/pubmed/article/pmid23656776/authorship4\">" +
				"<rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#Authorship\"/>" +
				"<rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>" +
				"<rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#DependentResource\"/>" +
				"<rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#DependentResource\"/>" +
				"<core:linkedAuthor rdf:resource=\"http://vivoweb.org/pubmed/article/pmid23656776/author4\"/>" +
				"<core:linkedInformationResource rdf:resource=\"http://vivoweb.org/pubmed/article/pmid23656776\"/>" +
				"<core:authorRank rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">3</core:authorRank>" +
			"</rdf:Description>" +
			"<rdf:Description rdf:about=\"http://vivoweb.org/pubmed/article/pmid23656776/author4\">" +
				"<rdf:type rdf:resource=\"http://xmlns.com/foaf/0.1/Person\"/>" +
				"<rdfs:label>Fox, Ralfe</rdfs:label>" +
				"<foaf:lastName>Fox</foaf:lastName>" +
				"<score:workEmail>v@mydomain.edu</score:workEmail>" +
				"<score:foreName>Ralfe</score:foreName>" +
				"<core:middleName>B</core:middleName>" +
				"<score:initials>RBF</score:initials>" +
				"<score:suffix/>" +
				"<rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>" +
				"<core:authorInAuthorship rdf:resource=\"http://vivoweb.org/pubmed/article/pmid23656776/authorship4\"/>" +
			"</rdf:Description>" +
			"<rdf:Description rdf:nodeID=\"pmid23656776mesh1\">" +
				"<rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>" +
				"<rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>" +
				"<core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubmed/article/pmid23656776\"/>" +
				"<rdfs:label>Animals</rdfs:label>" +
				"<rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>" +
				"<score:meshTermOf rdf:resource=\"http://vivoweb.org/pubmed/article/pmid23656776\"/>" +
				"<score:Descriptor>Animals</score:Descriptor>" +
				"<score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>" +
				"<score:Qualifier/>" +
				"<score:QualifierIsMajorTerm/>" +
			"</rdf:Description>" +
			"<rdf:Description rdf:nodeID=\"pmid23656776mesh2\">" +
				"<rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>" +
				"<rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>" +
				"<core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubmed/article/pmid23656776\"/>" +
				"<rdfs:label>Antibodies, Monoclonal</rdfs:label>" +
				"<rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>" +
				"<score:meshTermOf rdf:resource=\"http://vivoweb.org/pubmed/article/pmid23656776\"/>" +
				"<score:Descriptor>Antibodies, Monoclonal</score:Descriptor>" +
				"<score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>" +
				"<score:Qualifier>adverse effects therapeutic use</score:Qualifier>" +
				"<score:QualifierIsMajorTerm>N Y</score:QualifierIsMajorTerm>" +
			"</rdf:Description>" +
			"<rdf:Description rdf:nodeID=\"pmid23656776mesh3\">" +
				"<rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>" +
				"<rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>" +
				"<core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubmed/article/pmid23656776\"/>" +
				"<rdfs:label>Humans</rdfs:label>" +
				"<rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>" +
				"<score:meshTermOf rdf:resource=\"http://vivoweb.org/pubmed/article/pmid23656776\"/>" +
				"<score:Descriptor>Humans</score:Descriptor>" +
				"<score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>" +
				"<score:Qualifier/>" +
				"<score:QualifierIsMajorTerm/>" +
			"</rdf:Description>" +
			"<rdf:Description rdf:nodeID=\"pmid23656776mesh4\">" +
				"<rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>" +
				"<rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>" +
				"<core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubmed/article/pmid23656776\"/>" +
				"<rdfs:label>Lymphoma</rdfs:label>" +
				"<rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>" +
				"<score:meshTermOf rdf:resource=\"http://vivoweb.org/pubmed/article/pmid23656776\"/>" +
				"<score:Descriptor>Lymphoma</score:Descriptor>" +
				"<score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>" +
				"<score:Qualifier>radiotherapy therapy</score:Qualifier>" +
				"<score:QualifierIsMajorTerm>Y N</score:QualifierIsMajorTerm>" +
			"</rdf:Description>" +
			"<rdf:Description rdf:nodeID=\"pmid23656776mesh5\">" +
				"<rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>" +
				"<rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>" +
				"<core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubmed/article/pmid23656776\"/>" +
				"<rdfs:label>Nuclear Medicine</rdfs:label>" +
				"<rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>" +
				"<score:meshTermOf rdf:resource=\"http://vivoweb.org/pubmed/article/pmid23656776\"/>" +
				"<score:Descriptor>Nuclear Medicine</score:Descriptor>" +
				"<score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>" +
				"<score:Qualifier/>" +
				"<score:QualifierIsMajorTerm/>" +
			"</rdf:Description>" +
			"<rdf:Description rdf:nodeID=\"pmid23656776mesh6\">" +
				"<rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>" +
				"<rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>" +
				"<core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubmed/article/pmid23656776\"/>" +
				"<rdfs:label>Radioimmunotherapy</rdfs:label>" +
				"<rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>" +
				"<score:meshTermOf rdf:resource=\"http://vivoweb.org/pubmed/article/pmid23656776\"/>" +
				"<score:Descriptor>Radioimmunotherapy</score:Descriptor>" +
				"<score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>" +
				"<score:Qualifier>adverse effects methods</score:Qualifier>" +
				"<score:QualifierIsMajorTerm>N Y</score:QualifierIsMajorTerm>" +
			"</rdf:Description>" +
			"<rdf:Description rdf:nodeID=\"pmid23656776mesh7\">" +
				"<rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>" +
				"<rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>" +
				"<core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubmed/article/pmid23656776\"/>" +
				"<rdfs:label>Treatment Outcome</rdfs:label>" +
				"<rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>" +
				"<score:meshTermOf rdf:resource=\"http://vivoweb.org/pubmed/article/pmid23656776\"/>" +
				"<score:Descriptor>Treatment Outcome</score:Descriptor>" +
				"<score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>" +
				"<score:Qualifier/>" +
				"<score:QualifierIsMajorTerm/>" +
			"</rdf:Description>" +
			"<rdf:Description rdf:about=\"http://vivoweb.org/pubmed/journal/j1558-4623\">" +
				"<rdf:type rdf:resource=\"http://purl.org/ontology/bibo/Journal\"/>" +
				"<rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>" +
				"<core:Title>Seminars in nuclear medicine</core:Title>" +
				"<rdfs:label>Seminars in nuclear medicine</rdfs:label>" +
				"<bibo:ISSN>1558-4623</bibo:ISSN>" +
				"<core:publicationVenueFor rdf:resource=\"http://vivoweb.org/pubmed/article/pmid23656776\"/>" +
			"</rdf:Description>" +
			"<rdf:Description rdf:about=\"http://vivoweb.org/pubmed/article/pmid23656776/dateCreated\">" +
				"<core:Year>\"2010\"</core:Year>" +
				"<core:Month>\"02\"</core:Month>" +
				"<core:Day>\"01\"</core:Day>" +
			"</rdf:Description>" +
			"<rdf:Description rdf:about=\"http://vivoweb.org/pubmed/article/pmid23656776/dateCompleted\">" +
				"<core:Year>2010</core:Year>" +
				"<core:Month>07</core:Month>" +
				"<core:Day>08</core:Day>" +
			"</rdf:Description>" +
			"<rdf:Description rdf:about=\"http://vivoweb.org/harvester/people/uniqueid7821299012\">" +
				"<rdf:type rdf:resource=\"http://xmlns.com/foaf/0.1/Person\"/>" +
				"<rdfs:label>Fawkes, Guy</rdfs:label>" +
				"<localVIVO:harvestedBy>People-Harvester</localVIVO:harvestedBy>" +
				"<localVIVO:uniqueid>7821299012</localVIVO:uniqueid>" +
				"<foaf:firstName>Guy</foaf:firstName>" +
				"<foaf:lastName>Fawkes</foaf:lastName>" +
			"</rdf:Description>" +
			"<rdf:Description rdf:about=\"http://vivoweb.org/harvester/people/uniqueid8572293123\">" +
				"<rdf:type rdf:resource=\"http://xmlns.com/foaf/0.1/Person\"/>" +
				"<rdfs:label>Vendetta, Victoria</rdfs:label>" +
				"<localVIVO:harvestedBy>People-Harvester</localVIVO:harvestedBy>" +
				"<localVIVO:uniqueid>8572293123</localVIVO:uniqueid>" +
				"<foaf:firstName>Victoria</foaf:firstName>" +
				"<foaf:lastName>Vendetta</foaf:lastName>" +
			"</rdf:Description>" +
			"<rdf:Description rdf:about=\"http://vivoweb.org/harvester/org/deptid019283\">" +
				"<rdfs:label>Department of Medicine</rdfs:label>" +
				"<localVIVO:harvestedBy>Org-Harvester</localVIVO:harvestedBy>" +
				"<localVIVO:deptid>019283</localVIVO:deptid>" +
			"</rdf:Description>" +
			"<rdf:Description rdf:about=\"http://vivoweb.org/harvester/position/posFor7821299012in019283start20091203\">" +
				"<rdfs:label>Head of Nursing</rdfs:label>" +
				"<localVIVO:harvestedBy>Position-Harvester</localVIVO:harvestedBy>" +
				"<localVIVO:deptidForPosition>019283</localVIVO:deptidForPosition>" +
				"<core:positionForPerson rdf:resource=\"http://vivoweb.org/harvester/people/uniqueid7821299012\"/>" +
				"<core:positionInOrganization rdf:resource=\"http://vivoweb.org/harvester/org/deptid019283\"/>" +
			"</rdf:Description>" +
		"</rdf:RDF>";
	/**
	 * vivo rdf statements to load for test
	 */
	protected static final String vivoRDF = "" +
		"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
		"<rdf:RDF xmlns:j.0=\"http://aims.fao.org/aos/geopolitical.owl#\" " +
				"xmlns:skos=\"http://www.w3.org/2004/02/skos/core#\" " +
				"xmlns:event=\"http://purl.org/NET/c4dm/event.owl#\" " +
				"xmlns:dc=\"http://purl.org/dc/elements/1.1/\" " +
				"xmlns:owl2=\"http://www.w3.org/2006/12/owl2-xml#\" " +
				"xmlns:core=\"http://vivoweb.org/ontology/core#\" " +
				"xmlns:localVIVO=\"http://vivo.mydomain.edu/ontology/vivo-local/\" " +
				"xmlns:swrlb=\"http://www.w3.org/2003/11/swrlb#\" " +
				"xmlns:vann=\"http://purl.org/vocab/vann/\" " +
				"xmlns:j.1=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#\" " +
				"xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" " +
				"xmlns:bibo=\"http://purl.org/ontology/bibo/\" " +
				"xmlns:afn=\"http://jena.hpl.hp.com/ARQ/function#\" " +
				"xmlns:foaf=\"http://xmlns.com/foaf/0.1/\" " +
				"xmlns:swvs=\"http://www.w3.org/2003/06/sw-vocab-status/ns#\" " +
				"xmlns:owl=\"http://www.w3.org/2002/07/owl#\" " +
				"xmlns:dcterms=\"http://purl.org/dc/terms/\" " +
				"xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\" " +
				"xmlns:swrl=\"http://www.w3.org/2003/11/swrl#\" " +
				"xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\">" +
			"<rdf:Description rdf:about=\"http://vivo.mydomain.edu/individual/n3573\">" +
				"<rdf:type rdf:resource=\"http://xmlns.com/foaf/0.1/Person\"/>" +
				"<rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#FacultyMember\"/>" +
				"<rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1ValueThing\"/>" +
				"<localVIVO:uniqueid>5845425276</localVIVO:uniqueid>" +
				"<j.1:moniker rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">Faculty Member</j.1:moniker>" +
				"<j.1:modTime rdf:datatype=\"http://www.w3.org/2001/XMLSchema#dateTime\">2010-08-09T15:46:21</j.1:modTime>" +
				"<rdfs:label xml:lang=\"en-US\">Fawkes, Girl</rdfs:label>" +
				"<foaf:firstName>Girl</foaf:firstName>" +
				"<core:middleName>G</core:middleName>" +
				"<foaf:lastName>Fawkes</foaf:lastName>" +
				"<core:workEmail>vgirl@mydomain.edu</core:workEmail>" +
			"</rdf:Description>" +
			"<rdf:Description rdf:about=\"http://vivo.mydomain.edu/individual/n3574\">" +
				"<rdf:type rdf:resource=\"http://xmlns.com/foaf/0.1/Person\"/>" +
				"<rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#FacultyMember\"/>" +
				"<rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1ValueThing\"/>" +
				"<localVIVO:uniqueid>7821299012</localVIVO:uniqueid>" +
				"<j.1:moniker rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">Faculty Member</j.1:moniker>" +
				"<j.1:modTime rdf:datatype=\"http://www.w3.org/2001/XMLSchema#dateTime\">2010-08-09T15:46:21</j.1:modTime>" +
				"<rdfs:label xml:lang=\"en-US\">Fawkes, Guy</rdfs:label>" +
				"<foaf:firstName>Guy</foaf:firstName>" +
				"<core:middleName>G</core:middleName>" +
				"<foaf:lastName>Fawkes</foaf:lastName>" +
				"<core:workEmail>v@mydomain.edu</core:workEmail>" +
			"</rdf:Description>" +
			"<rdf:Description rdf:about=\"http://vivo.mydomain.edu/individual/n3576\">" +
				"<rdf:type rdf:resource=\"http://xmlns.com/foaf/0.1/Person\"/>" +
				"<rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#FacultyMember\"/>" +
				"<rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1ValueThing\"/>" +
				"<localVIVO:uniqueid>1721204153</localVIVO:uniqueid>" +
				"<j.1:moniker rdf:datatype=\"http://www.w3.org/2001/XMLSchema#string\">Faculty Member</j.1:moniker>" +
				"<j.1:modTime rdf:datatype=\"http://www.w3.org/2001/XMLSchema#dateTime\">2010-08-09T15:46:21</j.1:modTime>" +
				"<rdfs:label xml:lang=\"en-US\">Mans, Dude</rdfs:label>" +
				"<foaf:firstName>Dude</foaf:firstName>" +
				"<core:middleName>G</core:middleName>" +
				"<foaf:lastName>Mans</foaf:lastName>" +
				"<core:workEmail>dgm@mydomain.edu</core:workEmail>" +
			"</rdf:Description>" +
			"<rdf:Description rdf:about=\"http://vivo.mydomain.edu/individual/n821173458\">" +
				"<rdfs:label>Department of Medicine</rdfs:label>" +
				"<localVIVO:harvestedBy>Org-Harvester</localVIVO:harvestedBy>" +
				"<localVIVO:deptid>019283</localVIVO:deptid>" +
			"</rdf:Description>" +
			"<rdf:Description rdf:about=\"http://vivo.mydomain.edu/individual/n675720185\">" +
				"<rdfs:label>Head of Nursing</rdfs:label>" +
				"<localVIVO:harvestedBy>Position-Harvester</localVIVO:harvestedBy>" +
				"<localVIVO:deptidForPosition>019283</localVIVO:deptidForPosition>" +
				"<core:positionForPerson rdf:resource=\"http://vivo.mydomain.edu/individual/n3574\"/>" +
				"<core:positionInOrganization rdf:resource=\"http://vivo.mydomain.edu/individual/n821173458\"/>" +
			"</rdf:Description>" +
		"</rdf:RDF>";
	/** */
	private JenaConnect input;
	/** */
	private JenaConnect vivo;
	/** */
	private JenaConnect output;
	/** */
	private JenaConnect score;
	
	/**
	 * Test URI EqualityTest Algorithm
	 * @throws IOException error
	 */
	public void testURIEqualityTest() throws IOException {
		log.info("BEGIN testURIEqualityTest");
		// prep org arguments
		HashMap<String, Class<? extends Algorithm>> algorithms = new HashMap<String, Class<? extends Algorithm>>();
		algorithms.put("deptid", EqualityTest.class);
		HashMap<String, String> inputPredicates = new HashMap<String, String>();
		inputPredicates.put("deptid", "http://vivo.mydomain.edu/ontology/vivo-local/deptid");
		HashMap<String, String> vivoPredicates = new HashMap<String, String>();
		vivoPredicates.put("deptid", "http://vivo.mydomain.edu/ontology/vivo-local/deptid");
		HashMap<String, Float> weights = new HashMap<String, Float>();
		weights.put("deptid", Float.valueOf(1f));
		String namespace = "http://vivoweb.org/harvester/org/";
		// run org score
		log.info("Score: Start");
		new Score(this.input, this.vivo, this.score, null, algorithms, inputPredicates, vivoPredicates, namespace, weights, null, 1, false, false).execute();
		log.info("Score: End");
		// run org match
		log.info("Match: Start");
		new Match(this.input, this.score, null, true, 1f, null, false, 500).execute();
		log.info("Match: End");
		
		assertFalse(this.input.executeAskQuery("ASK { <http://vivoweb.org/harvester/org/deptid019283> ?p ?o }"));
		assertTrue(this.input.executeAskQuery("ASK { <http://vivo.mydomain.edu/individual/n821173458> ?p ?o }"));
		
		// prep people arguments
		this.score.truncate();
		algorithms.clear();
		algorithms.put("uid", EqualityTest.class);
		inputPredicates.clear();
		inputPredicates.put("uid", "http://vivo.mydomain.edu/ontology/vivo-local/uniqueid");
		vivoPredicates.clear();
		vivoPredicates.put("uid", "http://vivo.mydomain.edu/ontology/vivo-local/uniqueid");
		weights.clear();
		weights.put("uid", Float.valueOf(1f));
		namespace = "http://vivoweb.org/harvester/people/";
		// run people score
		log.info("Score: Start");
		new Score(this.input, this.vivo, this.score, null, algorithms, inputPredicates, vivoPredicates, namespace, weights, null, 20, false, false).execute();
		log.info("Score: End");
		// run people match
		log.info("Match: Start");
		new Match(this.input, this.score, null, true, 1f, null, false, 500).execute();
		log.info("Match: End");
		
		assertFalse(this.input.executeAskQuery("ASK { <http://vivoweb.org/harvester/people/uniqueid7821299012> ?p ?o }"));
		assertTrue(this.input.executeAskQuery("ASK { <http://vivo.mydomain.edu/individual/n3574> ?p ?o }"));
		
		// prep position arguments
		this.score.truncate();
		algorithms.clear();
		algorithms.put("deptForPos", EqualityTest.class);
		algorithms.put("posForPer", EqualityTest.class);
		algorithms.put("posInOrg", EqualityTest.class);
		inputPredicates.clear();
		inputPredicates.put("deptForPos", "http://vivo.mydomain.edu/ontology/vivo-local/deptidForPosition");
		inputPredicates.put("posForPer", "http://vivoweb.org/ontology/core#positionForPerson");
		inputPredicates.put("posInOrg", "http://vivoweb.org/ontology/core#positionInOrganization");
		vivoPredicates.clear();
		vivoPredicates.put("deptForPos", "http://vivo.mydomain.edu/ontology/vivo-local/deptidForPosition");
		vivoPredicates.put("posForPer", "http://vivoweb.org/ontology/core#positionForPerson");
		vivoPredicates.put("posInOrg", "http://vivoweb.org/ontology/core#positionInOrganization");
		weights.clear();
		weights.put("deptForPos", Float.valueOf(1/3f));
		weights.put("posForPer", Float.valueOf(1/3f));
		weights.put("posInOrg", Float.valueOf(1/3f));
		namespace = "http://vivoweb.org/harvester/position/";
		// run position score
		log.info("Score: Start");
		new Score(this.input, this.vivo, this.score, null, algorithms, inputPredicates, vivoPredicates, namespace, weights, null, 50, true, false).execute();
		log.info("Score: End");
		// run position match
		log.info("Match: Start");
		new Match(this.input, this.score, null, true, 1f, null, false, 500).execute();
		log.info("Match: End");
		
		assertTrue(this.input.executeAskQuery("ASK { <http://vivo.mydomain.edu/individual/n675720185> ?p ?o }"));
		assertFalse(this.input.executeAskQuery("ASK { <http://vivoweb.org/harvester/position/posFor7821299012in019283start20091203> ?p ?o }"));
		
		log.info("END testURIEqualityTest");
	}
	
	/**
	 * Test EqualityTest Algorithm
	 * @throws IOException error
	 */
	public void testEmailLastNameEqualityTest() throws IOException {
		log.info("BEGIN testEmailLastNameEqualityTest");
		// prep arguments
		HashMap<String, Class<? extends Algorithm>> algorithms = new HashMap<String, Class<? extends Algorithm>>();
		algorithms.put("wEmail", EqualityTest.class);
		algorithms.put("lName", NormalizedDoubleMetaphoneDifference.class);
		algorithms.put("fName", NormalizedDoubleMetaphoneDifference.class);
		
		HashMap<String, String> inputPredicates = new HashMap<String, String>();
		inputPredicates.put("wEmail", "http://vivoweb.org/ontology/score#workEmail");
		inputPredicates.put("lName", "http://xmlns.com/foaf/0.1/lastName");
		inputPredicates.put("fName", "http://vivoweb.org/ontology/score#foreName");
		
		HashMap<String, String> vivoPredicates = new HashMap<String, String>();
		vivoPredicates.put("wEmail", "http://vivoweb.org/ontology/core#workEmail");
		vivoPredicates.put("lName", "http://xmlns.com/foaf/0.1/lastName");
		vivoPredicates.put("fName", "http://xmlns.com/foaf/0.1/firstName");
		
		HashMap<String, Float> weights = new HashMap<String, Float>();
		weights.put("wEmail", Float.valueOf(1 / 2f));
		weights.put("lName", Float.valueOf(1 / 3f));
		weights.put("fName", Float.valueOf(1 / 6f));
		
//		log.debug("Input Dump Pre-Score\n" + this.input.exportRdfToString());
		
		// run score
		log.info("Score: Start");
		new Score(this.input, this.vivo, this.score, null, algorithms, inputPredicates, vivoPredicates, "http://vivoweb.org/pubmed/article/", weights, null, 50, false, false).execute();
		log.info("Score: End");
		//log.info("Score Dump Post-Score\n" + this.vivo.exportRdfToString());
		log.info("Match: Start");
		new Match(this.input, this.score, this.output, true, 0.75f, null, true, 500).execute();
		log.info("Match: End");
		//log.info("Match Dump Post-Match\n" + this.input.exportRdfToString());
		
		// check score model
		//Check for matched person authorship
		assertTrue(this.input.executeAskQuery("ASK { <http://vivo.mydomain.edu/individual/n3574> <http://vivoweb.org/ontology/core#authorInAuthorship> <http://vivoweb.org/pubmed/article/pmid23656776/authorship1> }"));
		//Check to make sure pub doesn't have matched authorship
		assertFalse(this.input.executeAskQuery("ASK { <http://vivoweb.org/pubmed/article/pmid20113680author1> <http://vivoweb.org/ontology/core#authorInAuthorship> <http://vivoweb.org/pubmed/article/pmid23656776/authorship1> }"));
		//Check to make sure information authorship exists on publication
		assertFalse(this.input.executeAskQuery("ASK { <http://vivoweb.org/pubmed/article/pmid20113680author1> <http://vivoweb.org/ontology/core#informationResourceInAuthorship> <http://vivoweb.org/pubmed/article/pmid23656776/authorship1> }"));
		//check for records in output model records
		Property rdfType = ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Resource authorshipType = ResourceFactory.createProperty("http://vivoweb.org/ontology/core#Authorship");
		Resource journalType = ResourceFactory.createProperty("http://purl.org/ontology/bibo/Journal");
		//Resource publicationType = ResourceFactory.createProperty("");

		assertTrue(!this.output.isEmpty());
//		System.out.println(this.output.exportRdfToString());
		//assertTrue(this.output.getJenaModel().contains(null, rdfType, publicationType));
		assertTrue(this.output.getJenaModel().contains(null, rdfType, journalType));
		assertTrue(this.output.getJenaModel().contains(null, rdfType, authorshipType));
		
		//assertTrue(this.input.executeAskQuery("ASK { <http://vivoweb.org/pubmed/article/pmid20113680author1> <http://vivoweb.org/ontology/core#informationResourceInAuthorship <http://vivoweb.org/harvest/pubmedPub/pmid20374097/vivoAuthorship/l1> }"));
		
		//assertFalse(this.input.containsURI("http://vivoweb.org/pubmed/article/pmid20113680author1"));
		//assertTrue(this.input.containsURI("http://vivoweb.org/pubmed/pmid201138680/vivoAuthorship/l1"));
		
		assertFalse(this.input.executeAskQuery("ASK { <http://vivo.mydomain.edu/individual/n3574> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?type }"));
		assertFalse(this.input.executeAskQuery("ASK { <http://vivo.mydomain.edu/individual/n3574> ?p ?lit . FILTER(isLiteral(?lit)) }"));
		
		//pub has 
		//<j.1:informationResourceInAuthorship rdf:resource="http://vivoweb.org/harvest/pubmedPub/pmid20374097/vivoAuthorship/l1"/>
		
		//person has
		//<j.3:authorInAuthorship rdf:resource="http://vivoweb.org/harvest/pubmedPub/pmid20374097/vivoAuthorship/l1"/>
		
		//http://vivoweb.org/pubmed/article/pmid20113680/authorship1
		log.info("END testEmailLastNameEqualityTest");
	}
	
	/**
	 * Test Tiered Scoring
	 * @throws IOException error
	 */
	public void testTieredScore() throws IOException {
		log.info("BEGIN testTieredScore");
		// prep arguments
		HashMap<String, Class<? extends Algorithm>> algorithms = new HashMap<String, Class<? extends Algorithm>>();
		algorithms.put("lName", NormalizedDoubleMetaphoneDifference.class);
		algorithms.put("type", EqualityTest.class);
		
		HashMap<String, String> inputPredicates = new HashMap<String, String>();
		inputPredicates.put("lName", "http://xmlns.com/foaf/0.1/lastName");
		inputPredicates.put("type", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		
		HashMap<String, String> vivoPredicates = new HashMap<String, String>();
		vivoPredicates.put("lName", "http://xmlns.com/foaf/0.1/lastName");
		vivoPredicates.put("type", "http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		
		HashMap<String, Float> weights = new HashMap<String, Float>();
		weights.put("lName", Float.valueOf(8 / 16f));
		weights.put("type", Float.valueOf(0 / 16f));
		
		//log.debug("Input Dump Pre-Score\n" + this.input.exportRdfToString());
		
		// run score
		log.info("Score: Start");
		new Score(this.input, this.vivo, this.score, null, algorithms, inputPredicates, vivoPredicates, "http://vivoweb.org/pubmed/article/", weights, null, 250, false, false).execute();
		log.info("Score: End");
		//log.info("Score Dump Post-Score\n" + this.vivo.exportRdfToString());
		
		algorithms.clear();
		algorithms.put("fName", NormalizedDoubleMetaphoneDifference.class);
		algorithms.put("mName", CaseInsensitiveInitialTest.class);
		
		inputPredicates.clear();
		inputPredicates.put("fName", "http://vivoweb.org/ontology/score#foreName");
		inputPredicates.put("mName", "http://vivoweb.org/ontology/core#middleName");
		
		vivoPredicates.clear();
		vivoPredicates.put("fName", "http://xmlns.com/foaf/0.1/firstName");
		vivoPredicates.put("mName", "http://vivoweb.org/ontology/core#middleName");
		
		weights.clear();
		weights.put("fName", Float.valueOf(7 / 16f));
		weights.put("mName", Float.valueOf(1 / 16f));
		
		// run filter score
		log.info("Score: Start");
		new Score(this.input, this.vivo, this.score, null, algorithms, inputPredicates, vivoPredicates, "http://vivoweb.org/pubmed/article/", weights, Float.valueOf(7/16f), 250, false, false).execute();
		log.info("Score: End");
		//log.info("Score Dump Post-Score\n" + this.vivo.exportRdfToString());
		log.info("Match: Start");
		new Match(this.input, this.score, this.output, true, 13/16f, null, true, 500).execute();
		log.info("Match: End");
		//log.info("Match Dump Post-Match\n" + this.input.exportRdfToString());
		
		// check score model
		//Check for matched person authorship
		assertTrue(this.input.executeAskQuery("ASK { <http://vivo.mydomain.edu/individual/n3574> <http://vivoweb.org/ontology/core#authorInAuthorship> <http://vivoweb.org/pubmed/article/pmid23656776/authorship1> }"));
		//Check to make sure pub doesn't have matched authorship
		assertFalse(this.input.executeAskQuery("ASK { <http://vivoweb.org/pubmed/article/pmid20113680author1> <http://vivoweb.org/ontology/core#authorInAuthorship> <http://vivoweb.org/pubmed/article/pmid23656776/authorship1> }"));
		//Check to make sure information authorship exists on publication
		assertFalse(this.input.executeAskQuery("ASK { <http://vivoweb.org/pubmed/article/pmid20113680author1> <http://vivoweb.org/ontology/core#informationResourceInAuthorship> <http://vivoweb.org/pubmed/article/pmid23656776/authorship1> }"));
		//check for records in output model records
		Property rdfType = ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Resource authorshipType = ResourceFactory.createProperty("http://vivoweb.org/ontology/core#Authorship");
		Resource journalType = ResourceFactory.createProperty("http://purl.org/ontology/bibo/Journal");
		//Resource publicationType = ResourceFactory.createProperty("");

		assertTrue(!this.output.isEmpty());
//		System.out.println(this.output.exportRdfToString());
		//assertTrue(this.output.getJenaModel().contains(null, rdfType, publicationType));
		assertTrue(this.output.getJenaModel().contains(null, rdfType, journalType));
		assertTrue(this.output.getJenaModel().contains(null, rdfType, authorshipType));
		
		//assertTrue(this.input.executeAskQuery("ASK { <http://vivoweb.org/pubmed/article/pmid20113680author1> <http://vivoweb.org/ontology/core#informationResourceInAuthorship <http://vivoweb.org/harvest/pubmedPub/pmid20374097/vivoAuthorship/l1> }"));
		
		//assertFalse(this.input.containsURI("http://vivoweb.org/pubmed/article/pmid20113680author1"));
		//assertTrue(this.input.containsURI("http://vivoweb.org/pubmed/pmid201138680/vivoAuthorship/l1"));
		
		assertFalse(this.input.executeAskQuery("ASK { <http://vivo.mydomain.edu/individual/n3574> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?type }"));
		assertFalse(this.input.executeAskQuery("ASK { <http://vivo.mydomain.edu/individual/n3574> ?p ?lit . FILTER(isLiteral(?lit)) }"));
		
		//pub has 
		//<j.1:informationResourceInAuthorship rdf:resource="http://vivoweb.org/harvest/pubmedPub/pmid20374097/vivoAuthorship/l1"/>
		
		//person has
		//<j.3:authorInAuthorship rdf:resource="http://vivoweb.org/harvest/pubmedPub/pmid20374097/vivoAuthorship/l1"/>
		
		//http://vivoweb.org/pubmed/article/pmid20113680/authorship1
		log.info("END testTieredScore");
	}
	
	@Override
	protected void setUp() throws Exception {
		InitLog.initLogger(null, null);
		// load input models
		this.input = new MemJenaConnect();
		this.input.loadRdfFromString(inputRDF, null, null);
		
		this.vivo = this.input.neighborConnectClone("vivo");
		this.vivo.loadRdfFromString(vivoRDF, null, null);
		
		this.output = this.input.neighborConnectClone("output");
		
		this.score = this.input.neighborConnectClone("score");
	}
	
	@Override
	protected void tearDown() {
		log.debug("Tear Down");
		if(this.input != null) {
			try {
				this.input.truncate();
			} catch(Exception e) {
				//Ignore
			} finally {
				try {
					this.input.close();
				} catch(Exception e) {
					//Ignore
				}
			}
			this.input = null;
		}
		if(this.vivo != null) {
			try {
				this.vivo.truncate();
			} catch(Exception e) {
				//Ignore
			} finally {
				try {
					this.vivo.close();
				} catch(Exception e) {
					//Ignore
				}
			}
			this.vivo = null;
		}
		if(this.output != null) {
			try {
				this.output.truncate();
			} catch(Exception e) {
				//Ignore
			} finally {
				try {
					this.output.close();
				} catch(Exception e) {
					//Ignore
				}
			}
			this.output = null;
		}
		if(this.score != null) {
			try {
				this.score.truncate();
			} catch(Exception e) {
				//Ignore
			} finally {
				try {
					this.score.close();
				} catch(Exception e) {
					//Ignore
				}
			}
			this.score = null;
		}
	}
}
