/*******************************************************************************
 * Copyright (c) 2010-2011 VIVO Harvester Team. For full list of contributors, please see the AUTHORS file provided.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the new BSD license which accompanies this distribution, and is available at http://www.opensource.org/licenses/bsd-license.html
 ******************************************************************************/
package org.vivoweb.test.harvester.translate;

import java.io.IOException;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vivoweb.harvester.translate.XSLTranslator;
import org.vivoweb.harvester.util.FileAide;
import org.vivoweb.harvester.util.InitLog;
import org.vivoweb.harvester.util.repo.Record;
import org.vivoweb.harvester.util.repo.RecordHandler;
import org.vivoweb.harvester.util.repo.TextFileRecordHandler;

/**
 * @author Stephen Williams (swilliams@ctrip.ufl.edu)
 * @author Christopher Haines (hainesc@ctrip.ufl.edu)
 */
public class TranslateTest extends TestCase {
	/**
	 * SLF4J Logger
	 */
	private static Logger log = LoggerFactory.getLogger(TranslateTest.class);
	/** */
	private static final String inputDataArticle = "" +
			"<?xml version=\"1.0\"?>\n" +
			"<!DOCTYPE PubmedArticleSet PUBLIC \"-//NLM//DTD PubMedArticle, 1st January 2011//EN\" \"http://www.ncbi.nlm.nih.gov/entrez/query/DTD/pubmed_110101.dtd\">\n" +
			"<PubmedArticleSet>\n" +
			"  <PubmedArticle>" +
			"    <MedlineCitation Owner=\"NLM\" Status=\"MEDLINE\">" +
			"      <PMID>19885433</PMID>" +
			"      <DateCreated>" +
			"        <Year>2009</Year>" +
			"        <Month>11</Month>" +
			"        <Day>03</Day>" +
			"      </DateCreated>" +
			"      <DateCompleted>" +
			"        <Year>2010</Year>" +
			"        <Month>02</Month>" +
			"        <Day>02</Day>" +
			"      </DateCompleted>" +
			"      <Article PubModel=\"Print\">" +
			"        <Journal>" +
			"          <ISSN IssnType=\"Print\">0882-2786</ISSN>" +
			"          <JournalIssue CitedMedium=\"Print\">" +
			"            <Volume>24 Suppl</Volume>" +
			"            <PubDate>" +
			"              <Year>2009</Year>" +
			"            </PubDate>" +
			"          </JournalIssue>" +
			"          <Title>The International journal of oral &amp; maxillofacial implants</Title>" +
			"          <ISOAbbreviation>Int J Oral Maxillofac Implants</ISOAbbreviation>" +
			"        </Journal>" +
			"        <ArticleTitle>Local risk factors for implant therapy.</ArticleTitle>" +
			"        <Pagination>" +
			"          <MedlinePgn>28-38</MedlinePgn>" +
			"        </Pagination>" +
			"        <Abstract>" +
			"          <AbstractText>PURPOSE: The aim of this review was to determine the effect of several potential local risk factors on implant survival and success (primary outcomes) as well as on mucosal recession, bleeding on probing, and proximal marginal bone loss (secondary outcomes). MATERIALS AND METHODS: A comprehensive review of the literature was conducted. The selection of publications reporting on human clinical studies was based on predetermined inclusion criteria and was agreed upon by three reviewers. After title and abstract screening of 2,681 publications obtained from the search, 19 articles were deemed to be relevant to the topic and the search criteria. RESULTS: Limited data show that when an implant is placed within 3 mm of the neighboring tooth, proximal bone is at risk. The data regarding the placement of implants into infected sites are still insufficient, but studies have shown that this may be possible. Soft tissue thickness has not been shown to be a risk factor in implant survival. There is also no evidence to support a relationship between the width of keratinized tissue and implant survival. No studies were found that directly related bone density to implant survival. Implant stability was also difficult to examine due to the lack of validated stability measures. DISCUSSION AND CONCLUSION: One critical factor that faced the group during the review of the literature and interpretation of the data was the multifactorial nature of implant therapy. This makes isolation of specific risk factors difficult. Conclusions are limited by the current lack of quality clinical trials in this area.</AbstractText>" +
			"        </Abstract>" +
			"        <Affiliation>Department of Oral and Maxillofacial Surgery, University of Florida College of Dentistry, 1600 SW Archer Road, Room D7-6, Gainesville, FL 32610, USA. WMartin102@Dental1.ufl.edu</Affiliation>" +
			"        <AuthorList CompleteYN=\"Y\">" +
			"          <Author ValidYN=\"Y\">" +
			"            <LastName>Martin</LastName>" +
			"            <ForeName>William Joe</ForeName>" +
			"            <Initials>WJ</Initials>" +
			"          </Author>" +
			"          <Author ValidYN=\"Y\">" +
			"            <LastName>Lewis</LastName>" +
			"            <ForeName>Emma</ForeName>" +
			"            <Initials>E</Initials>" +
			"          </Author>" +
			"          <Author ValidYN=\"Y\">" +
			"            <LastName>Nicol</LastName>" +
			"            <ForeName>Ailsa</ForeName>" +
			"            <Initials>A</Initials>" +
			"          </Author>" +
			"        </AuthorList>" +
			"        <Language>eng</Language>" +
			"        <PublicationTypeList>" +
			"          <PublicationType>Journal Article</PublicationType>" +
			"          <PublicationType>Review</PublicationType>" +
			"        </PublicationTypeList>" +
			"      </Article>" +
			"      <MedlineJournalInfo>" +
			"        <Country>United States</Country>" +
			"        <MedlineTA>Int J Oral Maxillofac Implants</MedlineTA>" +
			"        <NlmUniqueID>8611905</NlmUniqueID>" +
			"        <ISSNLinking>0882-2786</ISSNLinking>" +
			"      </MedlineJournalInfo>" +
			"      <ChemicalList>" +
			"        <Chemical>" +
			"          <RegistryNumber>0</RegistryNumber>" +
			"          <NameOfSubstance>Dental Implants</NameOfSubstance>" +
			"        </Chemical>" +
			"      </ChemicalList>" +
			"      <CitationSubset>D</CitationSubset>" +
			"      <CitationSubset>IM</CitationSubset>" +
			"      <MeshHeadingList>" +
			"        <MeshHeading>" +
			"          <DescriptorName MajorTopicYN=\"N\">Bone Density</DescriptorName>" +
			"        </MeshHeading>" +
			"        <MeshHeading>" +
			"          <DescriptorName MajorTopicYN=\"N\">Dental Arch</DescriptorName>" +
			"          <QualifierName MajorTopicYN=\"N\">anatomy &amp; histology</QualifierName>" +
			"        </MeshHeading>" +
			"        <MeshHeading>" +
			"          <DescriptorName MajorTopicYN=\"N\">Dental Implantation, Endosseous</DescriptorName>" +
			"          <QualifierName MajorTopicYN=\"Y\">contraindications</QualifierName>" +
			"        </MeshHeading>" +
			"        <MeshHeading>" +
			"          <DescriptorName MajorTopicYN=\"N\">Dental Implants</DescriptorName>" +
			"          <QualifierName MajorTopicYN=\"Y\">contraindications</QualifierName>" +
			"        </MeshHeading>" +
			"        <MeshHeading>" +
			"          <DescriptorName MajorTopicYN=\"N\">Dental Restoration Failure</DescriptorName>" +
			"        </MeshHeading>" +
			"        <MeshHeading>" +
			"          <DescriptorName MajorTopicYN=\"N\">Gingiva</DescriptorName>" +
			"          <QualifierName MajorTopicYN=\"N\">anatomy &amp; histology</QualifierName>" +
			"        </MeshHeading>" +
			"        <MeshHeading>" +
			"          <DescriptorName MajorTopicYN=\"N\">Humans</DescriptorName>" +
			"        </MeshHeading>" +
			"        <MeshHeading>" +
			"          <DescriptorName MajorTopicYN=\"N\">Infection</DescriptorName>" +
			"        </MeshHeading>" +
			"        <MeshHeading>" +
			"          <DescriptorName MajorTopicYN=\"N\">Periodontitis</DescriptorName>" +
			"        </MeshHeading>" +
			"        <MeshHeading>" +
			"          <DescriptorName MajorTopicYN=\"N\">Risk Factors</DescriptorName>" +
			"        </MeshHeading>" +
			"      </MeshHeadingList>" +
			"      <NumberOfReferences>72</NumberOfReferences>" +
			"    </MedlineCitation>" +
			"    <PubmedData>" +
			"      <History>" +
			"        <PubMedPubDate PubStatus=\"entrez\">" +
			"          <Year>2009</Year>" +
			"          <Month>11</Month>" +
			"          <Day>4</Day>" +
			"          <Hour>6</Hour>" +
			"          <Minute>0</Minute>" +
			"        </PubMedPubDate>" +
			"        <PubMedPubDate PubStatus=\"pubmed\">" +
			"          <Year>2009</Year>" +
			"          <Month>12</Month>" +
			"          <Day>4</Day>" +
			"          <Hour>6</Hour>" +
			"          <Minute>0</Minute>" +
			"        </PubMedPubDate>" +
			"        <PubMedPubDate PubStatus=\"medline\">" +
			"          <Year>2010</Year>" +
			"          <Month>2</Month>" +
			"          <Day>3</Day>" +
			"          <Hour>6</Hour>" +
			"          <Minute>0</Minute>" +
			"        </PubMedPubDate>" +
			"      </History>" +
			"      <PublicationStatus>ppublish</PublicationStatus>" +
			"      <ArticleIdList>" +
			"        <ArticleId IdType=\"doi\">10.3949/ccjm.77.s3.07</ArticleId>" +
			"        <ArticleId IdType=\"pubmed\">19885433</ArticleId>" +
			"      </ArticleIdList>" +
			"    </PubmedData>" +
			"  </PubmedArticle>\n" +
			"</PubmedArticleSet>";
	/** */
	private static final String inputDataBook = "" +
			"<?xml version=\"1.0\"?>\n" +
			"<!DOCTYPE PubmedArticleSet PUBLIC \"-//NLM//DTD PubMedArticle, 1st January 2011//EN\" \"http://www.ncbi.nlm.nih.gov/entrez/query/DTD/pubmed_110101.dtd\">\n" +
			"<PubmedArticleSet>\n" +
			"  <PubmedBookArticle>" +
			"    <BookDocument>" +
			"      <PMID>21391348</PMID>" +
			"      <ArticleIdList>" +
			"        <ArticleId IdType=\"bookaccession\">NBK52867</ArticleId>" +
			"      </ArticleIdList>" +
			"      <Book>" +
			"        <Publisher>" +
			"          <PublisherName>National Academies Press (US)</PublisherName>" +
			"          <PublisherLocation>Washington (DC)</PublisherLocation>" +
			"        </Publisher>" +
			"        <BookTitle book=\"nap11996\">Global Infectious Disease Surveillance and Detection: Assessing the Challenges—Finding Solutions, Workshop Summary</BookTitle>" +
			"        <PubDate>" +
			"          <Year>2007</Year>" +
			"        </PubDate>" +
			"        <AuthorList Type=\"authors\">" +
			"          <Author>" +
			"            <CollectiveName>Institute of Medicine (US) Forum on Microbial Threats</CollectiveName>" +
			"          </Author>" +
			"        </AuthorList>" +
			"        <CollectionTitle book=\"napcollect\">The" +
			"                    National Academies Collection: Reports funded by National Institutes of" +
			"                Health</CollectionTitle>" +
			"        <Isbn>9780309111140</Isbn>" +
			"        <Isbn>0309111145</Isbn>" +
			"      </Book>" +
			"      <Language>eng</Language>" +
			"      <Abstract>" +
			"        <AbstractText>Early detection is essential to the control of emerging, reemerging, and novel" +
			"                infectious diseases, whether naturally occurring or intentionally introduced. Containing" +
			"                the spread of such diseases in a profoundly interconnected world requires" +
			"                active vigilance for signs of an outbreak, rapid recognition of its presence, and" +
			"                diagnosis of its microbial cause, in addition to strategies and resources for an" +
			"                appropriate and efficient response. Although these actions are often viewed in" +
			"                terms of human public health, they also challenge the plant and animal health" +
			"                communities. The Forum on Microbial Threats of the Institute of Medicine hosted a public" +
			"                workshop in Washington, DC, on December 12 and 13, 2006, to consider the" +
			"                scientific and policy issues—some of them long standing, others more recently" +
			"                arisen—relevant to the practice of disease surveillance and detection. Through" +
			"                invited presentations and discussions, participants examined current and emerging" +
			"                methods and strategies for the surveillance and detection of human, animal," +
			"                and plant diseases, and assessed the resource needs and opportunities for improving" +
			"                and coordinating infectious disease surveillance, detection, and reporting.</AbstractText>" +
			"            <CopyrightInformation>Copyright © 2007, National Academy of Sciences</CopyrightInformation>" +
			"      </Abstract>" +
			"      <Sections>" +
			"        <Section>" +
			"          <SectionTitle book=\"nap11996\" part=\"nap11996.front.s1\">The National Academies</SectionTitle>" +
			"        </Section>" +
			"        <Section>" +
			"          <SectionTitle book=\"nap11996\" part=\"nap11996.front.s2\">Forum on Microbial Threats</SectionTitle>" +
			"        </Section>" +
			"        <Section>" +
			"          <SectionTitle book=\"nap11996\" part=\"nap11996.front.s4\">Board on Global Health</SectionTitle>" +
			"        </Section>" +
			"        <Section>" +
			"          <SectionTitle book=\"nap11996\" part=\"nap11996.reviewers\">Reviewers</SectionTitle>" +
			"        </Section>" +
			"        <Section>" +
			"          <SectionTitle book=\"nap11996\" part=\"nap11996.preface\">Preface</SectionTitle>" +
			"        </Section>" +
			"        <Section>" +
			"          <SectionTitle book=\"nap11996\" part=\"summary\">Summary and Assessment</SectionTitle>" +
			"        </Section>" +
			"        <Section>" +
			"          <LocationLabel Type=\"chapter\">1</LocationLabel>" +
			"          <SectionTitle book=\"nap11996\" part=\"ch1\">Surveillance Strategies</SectionTitle>" +
			"        </Section>" +
			"        <Section>" +
			"          <LocationLabel Type=\"chapter\">2</LocationLabel>" +
			"          <SectionTitle book=\"nap11996\" part=\"ch2\">Surveillance Networks</SectionTitle>" +
			"        </Section>" +
			"        <Section>" +
			"          <LocationLabel Type=\"chapter\">3</LocationLabel>" +
			"          <SectionTitle book=\"nap11996\" part=\"ch3\">Detection and Diagnostics</SectionTitle>" +
			"        </Section>" +
			"        <Section>" +
			"          <LocationLabel Type=\"chapter\">4</LocationLabel>" +
			"          <SectionTitle book=\"nap11996\" part=\"ch4\">Resource Needs and Opportunities</SectionTitle>" +
			"        </Section>" +
			"        <Section>" +
			"          <SectionTitle book=\"nap11996\" part=\"nap11996.appgroup1\">Appendixes</SectionTitle>" +
			"        </Section>" +
			"      </Sections>" +
			"    </BookDocument>" +
			"    <PubmedBookData>" +
			"      <History>" +
			"        <PubMedPubDate PubStatus=\"pubmed\">" +
			"          <Year>2011</Year>" +
			"          <Month>3</Month>" +
			"          <Day>11</Day>" +
			"          <Hour>6</Hour>" +
			"          <Minute>0</Minute>" +
			"        </PubMedPubDate>" +
			"        <PubMedPubDate PubStatus=\"medline\">" +
			"          <Year>2011</Year>" +
			"          <Month>3</Month>" +
			"          <Day>11</Day>" +
			"          <Hour>6</Hour>" +
			"          <Minute>0</Minute>" +
			"        </PubMedPubDate>" +
			"        <PubMedPubDate PubStatus=\"entrez\">" +
			"          <Year>2011</Year>" +
			"          <Month>3</Month>" +
			"          <Day>11</Day>" +
			"          <Hour>6</Hour>" +
			"          <Minute>0</Minute>" +
			"        </PubMedPubDate>" +
			"      </History>" +
			"      <PublicationStatus>ppublish</PublicationStatus>" +
			"      <ArticleIdList>" +
			"        <ArticleId IdType=\"pubmed\">21391348</ArticleId>" +
			"      </ArticleIdList>" +
			"    </PubmedBookData>" +
			"  </PubmedBookArticle>\n" +
			"</PubmedArticleSet>";
	/** */
	//private static final String outputData = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<rdf:RDF xmlns:bibo=\"http://purl.org/ontology/bibo/\"\n         xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n         xmlns:owlPlus=\"http://www.w3.org/2006/12/owl2-xml#\"\n         xmlns:xs=\"http://www.w3.org/2001/XMLSchema#\"\n         xmlns:skos=\"http://www.w3.org/2008/05/skos#\"\n         xmlns:owl=\"http://www.w3.org/2002/07/owl#\"\n         xmlns:vocab=\"http://purl.org/vocab/vann/\"\n         xmlns:swvocab=\"http://www.w3.org/2003/06/sw-vocab-status/ns#\"\n         xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n         xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n         xmlns:core=\"http://vivoweb.org/ontology/core#\"\n         xmlns:vitro=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#\"\n         xmlns:foaf=\"http://xmlns.com/foaf/0.1/\"\n         xmlns:score=\"http://vivoweb.org/ontology/score#\">\n   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/article/pmid19885433\">\n      <rdf:type rdf:resource=\"http://purl.org/ontology/bibo/AcademicArticle\"/>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n      <bibo:pmid>19885433</bibo:pmid>\n      <rdfs:label>Local risk factors for implant therapy.</rdfs:label>\n      <core:Title>Local risk factors for implant therapy.</core:Title>\n      <score:Affiliation>Department of Oral and Maxillofacial Surgery, University of Florida College of Dentistry, 1600 SW Archer Road, Room D7-6, Gainesville, FL 32610, USA. WMartin102@Dental1.ufl.edu</score:Affiliation>\n      <bibo:volume>24 Suppl</bibo:volume>\n      <bibo:number/>\n      <bibo:doi>10.3949/ccjm.77.s3.07</bibo:doi>\n      <score:workEmail>WMartin102@Dental1.ufl.edu</score:workEmail>\n      <core:informationResourceInAuthorship rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/authorship1\"/>\n      <core:informationResourceInAuthorship rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/authorship2\"/>\n      <core:informationResourceInAuthorship rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/authorship3\"/>\n      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh1\"/>\n      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh2\"/>\n      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh3\"/>\n      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh4\"/>\n      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh5\"/>\n      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh6\"/>\n      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh7\"/>\n      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh8\"/>\n      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh9\"/>\n      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh10\"/>\n      <core:hasPublicationVenue rdf:resource=\"http://vivoweb.org/pubMed/journal/j0882-2786\"/>\n      <score:hasCreateDate rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/dateCreated\"/>\n      <score:hasCompleteDate rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/dateCompleted\"/>\n   </rdf:Description>\n   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/article/pmid19885433/authorship1\">\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#Authorship\"/>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#DependentResource\"/>\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#DependentResource\"/>\n      <core:linkedAuthor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/author1\"/>\n      <core:linkedInformationResource rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <rdfs:label>Authorship for Martin, William</rdfs:label>\n      <core:authorRank rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">1</core:authorRank>\n   </rdf:Description>\n   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/article/pmid19885433/authorship2\">\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#Authorship\"/>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#DependentResource\"/>\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#DependentResource\"/>\n      <core:linkedAuthor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/author2\"/>\n      <core:linkedInformationResource rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <rdfs:label>Authorship for Lewis, Emma</rdfs:label>\n      <core:authorRank rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">2</core:authorRank>\n   </rdf:Description>\n   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/article/pmid19885433/authorship3\">\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#Authorship\"/>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#DependentResource\"/>\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#DependentResource\"/>\n      <core:linkedAuthor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/author3\"/>\n      <core:linkedInformationResource rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <rdfs:label>Authorship for Nicol, Ailsa</rdfs:label>\n      <core:authorRank rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">3</core:authorRank>\n   </rdf:Description>\n   <rdf:Description rdf:nodeID=\"pmid19885433mesh1\">\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <rdfs:label>Bone Density</rdfs:label>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <score:Descriptor>Bone Density</score:Descriptor>\n      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n      <score:Qualifier/>\n      <score:QualifierIsMajorTerm/>\n   </rdf:Description>\n   <rdf:Description rdf:nodeID=\"pmid19885433mesh2\">\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <rdfs:label>Dental Arch</rdfs:label>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <score:Descriptor>Dental Arch</score:Descriptor>\n      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n      <score:Qualifier>anatomy &amp; histology</score:Qualifier>\n      <score:QualifierIsMajorTerm>N</score:QualifierIsMajorTerm>\n   </rdf:Description>\n   <rdf:Description rdf:nodeID=\"pmid19885433mesh3\">\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <rdfs:label>Dental Implantation, Endosseous</rdfs:label>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <score:Descriptor>Dental Implantation, Endosseous</score:Descriptor>\n      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n      <score:Qualifier>contraindications</score:Qualifier>\n      <score:QualifierIsMajorTerm>Y</score:QualifierIsMajorTerm>\n   </rdf:Description>\n   <rdf:Description rdf:nodeID=\"pmid19885433mesh4\">\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <rdfs:label>Dental Implants</rdfs:label>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <score:Descriptor>Dental Implants</score:Descriptor>\n      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n      <score:Qualifier>contraindications</score:Qualifier>\n      <score:QualifierIsMajorTerm>Y</score:QualifierIsMajorTerm>\n   </rdf:Description>\n   <rdf:Description rdf:nodeID=\"pmid19885433mesh5\">\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <rdfs:label>Dental Restoration Failure</rdfs:label>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <score:Descriptor>Dental Restoration Failure</score:Descriptor>\n      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n      <score:Qualifier/>\n      <score:QualifierIsMajorTerm/>\n   </rdf:Description>\n   <rdf:Description rdf:nodeID=\"pmid19885433mesh6\">\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <rdfs:label>Gingiva</rdfs:label>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <score:Descriptor>Gingiva</score:Descriptor>\n      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n      <score:Qualifier>anatomy &amp; histology</score:Qualifier>\n      <score:QualifierIsMajorTerm>N</score:QualifierIsMajorTerm>\n   </rdf:Description>\n   <rdf:Description rdf:nodeID=\"pmid19885433mesh7\">\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <rdfs:label>Humans</rdfs:label>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <score:Descriptor>Humans</score:Descriptor>\n      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n      <score:Qualifier/>\n      <score:QualifierIsMajorTerm/>\n   </rdf:Description>\n   <rdf:Description rdf:nodeID=\"pmid19885433mesh8\">\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <rdfs:label>Infection</rdfs:label>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <score:Descriptor>Infection</score:Descriptor>\n      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n      <score:Qualifier/>\n      <score:QualifierIsMajorTerm/>\n   </rdf:Description>\n   <rdf:Description rdf:nodeID=\"pmid19885433mesh9\">\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <rdfs:label>Periodontitis</rdfs:label>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <score:Descriptor>Periodontitis</score:Descriptor>\n      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n      <score:Qualifier/>\n      <score:QualifierIsMajorTerm/>\n   </rdf:Description>\n   <rdf:Description rdf:nodeID=\"pmid19885433mesh10\">\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <rdfs:label>Risk Factors</rdfs:label>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n      <score:Descriptor>Risk Factors</score:Descriptor>\n      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n      <score:Qualifier/>\n      <score:QualifierIsMajorTerm/>\n   </rdf:Description>\n   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/journal/j0882-2786\">\n      <rdf:type rdf:resource=\"http://purl.org/ontology/bibo/Journal\"/>\n      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n      <core:Title>The International journal of oral &amp; maxillofacial implants</core:Title>\n      <rdfs:label>The International journal of oral &amp; maxillofacial implants</rdfs:label>\n      <bibo:ISSN>0882-2786</bibo:ISSN>\n      <core:publicationVenueFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n   </rdf:Description>\n   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/article/pmid19885433/dateCreated\">\n      <core:Year>\"2009\"</core:Year>\n      <core:Month>\"11\"</core:Month>\n      <core:Day>\"03\"</core:Day>\n   </rdf:Description>\n   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/article/pmid19885433/dateCompleted\">\n      <core:Year>2010</core:Year>\n      <core:Month>02</core:Month>\n      <core:Day>02</core:Day>\n   </rdf:Description>\n</rdf:RDF>";
	@SuppressWarnings("unused")
	private static final String outputData = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<rdf:RDF xmlns:bibo=\"http://purl.org/ontology/bibo/\"\n"
			+ "         xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\"\n"
			+ "         xmlns:owlPlus=\"http://www.w3.org/2006/12/owl2-xml#\"\n"
			+ "         xmlns:xs=\"http://www.w3.org/2001/XMLSchema#\"\n"
			+ "         xmlns:skos=\"http://www.w3.org/2008/05/skos#\"\n"
			+ "         xmlns:owl=\"http://www.w3.org/2002/07/owl#\"\n"
			+ "         xmlns:vocab=\"http://purl.org/vocab/vann/\"\n"
			+ "         xmlns:swvocab=\"http://www.w3.org/2003/06/sw-vocab-status/ns#\"\n"
			+ "         xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
			+ "         xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
			+ "         xmlns:core=\"http://vivoweb.org/ontology/core#\"\n"
			+ "         xmlns:vitro=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#\"\n"
			+ "         xmlns:foaf=\"http://xmlns.com/foaf/0.1/\"\n"
			+ "         xmlns:score=\"http://vivoweb.org/ontology/score#\">\n"
			+ "   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/article/pmid19885433\">\n"
			+ "      <rdf:type rdf:resource=\"http://purl.org/ontology/bibo/AcademicArticle\"/>\n"
			+ "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
			+ "      <bibo:pmid>19885433</bibo:pmid>\n"
			+ "      <rdfs:label>Local risk factors for implant therapy.</rdfs:label>\n"
			+ "      <core:Title>Local risk factors for implant therapy.</core:Title>\n"
			+ "      <score:Affiliation>Department of Oral and Maxillofacial Surgery, University of Florida College of Dentistry, 1600 SW Archer Road, Room D7-6, Gainesville, FL 32610, USA. WMartin102@Dental1.ufl.edu</score:Affiliation>\n"
			+ "      <bibo:volume>24 Suppl</bibo:volume>\n"
			+ "      <bibo:number/>\n"
			+ "      <bibo:doi>10.3949/ccjm.77.s3.07</bibo:doi>\n"
			+ "      <score:workEmail>WMartin102@Dental1.ufl.edu</score:workEmail>\n"
			+ "      <core:informationResourceInAuthorship rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/authorship1\"/>\n"
			+ "      <core:informationResourceInAuthorship rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/authorship2\"/>\n"
			+ "      <core:informationResourceInAuthorship rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/authorship3\"/>\n"
			+ "      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh1\"/>\n"
			+ "      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh2\"/>\n"
			+ "      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh3\"/>\n"
			+ "      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh4\"/>\n"
			+ "      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh5\"/>\n"
			+ "      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh6\"/>\n"
			+ "      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh7\"/>\n"
			+ "      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh8\"/>\n"
			+ "      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh9\"/>\n"
			+ "      <core:hasSubjectArea rdf:nodeID=\"pmid19885433mesh10\"/>\n"
			+ "      <core:hasPublicationVenue rdf:resource=\"http://vivoweb.org/pubMed/journal/j0882-2786\"/>\n"
			+ "      <score:hasCreateDate rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/dateCreated\"/>\n"
			+ "      <score:hasCompleteDate rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/dateCompleted\"/>\n"
			+ "   </rdf:Description>\n"
			+ "   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/article/pmid19885433/authorship1\">\n"
			+ "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#Authorship\"/>\n"
			+ "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
			+ "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#DependentResource\"/>\n"
			+ "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#DependentResource\"/>\n"
			+ "      <core:linkedAuthor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/author1\"/>\n"
			+ "      <core:linkedInformationResource rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
			+ "      <rdfs:label>Authorship for Martin, William</rdfs:label>\n"
			+ "      <core:authorRank rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">1</core:authorRank>\n"
			+ "   </rdf:Description>\n"
			+ "   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/article/pmid19885433/author1\">\n"
			+ "      <rdf:type rdf:resource=\"http://xmlns.com/foaf/0.1/Person\"/>\n"
			+ "      <rdfs:label>Martin, William</rdfs:label>\n"
			+ "      <foaf:lastName>Martin</foaf:lastName>\n"
			+ "      <score:foreName>William</score:foreName>\n"
			+ "      <score:initials>W</score:initials>\n"
			+ "      <score:suffix/>\n"
			+ "      <rdf:type rdf:resource=\"http://vivoweb.org/harvester/excludeEntity\"/>\n"
			+ "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
			+ "      <core:authorInAuthorship rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/authorship1\"/>\n"
			+ "   </rdf:Description>\n"
			+ "   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/article/pmid19885433/authorship2\">\n"
			+ "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#Authorship\"/>\n"
			+ "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
			+ "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#DependentResource\"/>\n"
			+ "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#DependentResource\"/>\n"
			+ "      <core:linkedAuthor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/author2\"/>\n"
			+ "      <core:linkedInformationResource rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
			+ "      <rdfs:label>Authorship for Lewis, Emma</rdfs:label>\n"
			+ "      <core:authorRank rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">2</core:authorRank>\n"
			+ "   </rdf:Description>\n"
			+ "   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/article/pmid19885433/author2\">\n"
			+ "      <rdf:type rdf:resource=\"http://xmlns.com/foaf/0.1/Person\"/>\n"
			+ "      <rdfs:label>Lewis, Emma</rdfs:label>\n"
			+ "      <foaf:lastName>Lewis</foaf:lastName>\n"
			+ "      <score:foreName>Emma</score:foreName>\n"
			+ "      <score:initials>E</score:initials>\n"
			+ "      <score:suffix/>\n"
			+ "      <rdf:type rdf:resource=\"http://vivoweb.org/harvester/excludeEntity\"/>\n"
			+ "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
			+ "      <core:authorInAuthorship rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/authorship2\"/>\n"
			+ "   </rdf:Description>\n"
			+ "   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/article/pmid19885433/authorship3\">\n"
			+ "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#Authorship\"/>\n"
			+ "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
			+ "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#DependentResource\"/>\n"
			+ "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#DependentResource\"/>\n"
			+ "      <core:linkedAuthor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/author3\"/>\n"
			+ "      <core:linkedInformationResource rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
			+ "      <rdfs:label>Authorship for Nicol, Ailsa</rdfs:label>\n"
			+ "      <core:authorRank rdf:datatype=\"http://www.w3.org/2001/XMLSchema#int\">3</core:authorRank>\n"
			+ "   </rdf:Description>\n"
			+ "   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/article/pmid19885433/author3\">\n"
			+ "      <rdf:type rdf:resource=\"http://xmlns.com/foaf/0.1/Person\"/>\n"
			+ "      <rdfs:label>Nicol, Ailsa</rdfs:label>\n"
			+ "      <foaf:lastName>Nicol</foaf:lastName>\n"
			+ "      <score:foreName>Ailsa</score:foreName>\n"
			+ "      <score:initials>A</score:initials>\n"
			+ "      <score:suffix/>\n"
			+ "      <rdf:type rdf:resource=\"http://vivoweb.org/harvester/excludeEntity\"/>\n"
			+ "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
			+ "      <core:authorInAuthorship rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433/authorship3\"/>\n"
			+ "   </rdf:Description>\n"
			+ "   <rdf:Description rdf:nodeID=\"pmid19885433mesh1\">\n"
			+ "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n"
			+ "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n"
			+ "      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
			+ "      <rdfs:label>Bone Density</rdfs:label>\n"
			+ "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
			+ "      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
			+ "      <score:Descriptor>Bone Density</score:Descriptor>\n"
			+ "      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n"
			+ "      <score:Qualifier/>\n"
			+ "      <score:QualifierIsMajorTerm/>\n"
			+ "   </rdf:Description>\n"
			+ "   <rdf:Description rdf:nodeID=\"pmid19885433mesh2\">\n"
			+ "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n"
			+ "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n"
			+ "      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
			+ "      <rdfs:label>Dental Arch</rdfs:label>\n"
			+ "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
			+ "      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
			+ "      <score:Descriptor>Dental Arch</score:Descriptor>\n"
			+ "      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n"
			+ "      <score:Qualifier>anatomy &amp; histology</score:Qualifier>\n"
			+ "      <score:QualifierIsMajorTerm>N</score:QualifierIsMajorTerm>\n"
			+ "   </rdf:Description>\n"
			+ "   <rdf:Description rdf:nodeID=\"pmid19885433mesh3\">\n"
			+ "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n"
			+ "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n"
			+ "      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
			+ "      <rdfs:label>Dental Implantation, Endosseous</rdfs:label>\n"
			+ "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
			+ "      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
			+ "      <score:Descriptor>Dental Implantation, Endosseous</score:Descriptor>\n"
			+ "      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n"
			+ "      <score:Qualifier>contraindications</score:Qualifier>\n"
			+ "      <score:QualifierIsMajorTerm>Y</score:QualifierIsMajorTerm>\n"
			+ "   </rdf:Description>\n"
			+ "   <rdf:Description rdf:nodeID=\"pmid19885433mesh4\">\n"
			+ "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n"
			+ "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n"
			+ "      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
			+ "      <rdfs:label>Dental Implants</rdfs:label>\n"
			+ "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
			+ "      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
			+ "      <score:Descriptor>Dental Implants</score:Descriptor>\n"
			+ "      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n"
			+ "      <score:Qualifier>contraindications</score:Qualifier>\n"
			+ "      <score:QualifierIsMajorTerm>Y</score:QualifierIsMajorTerm>\n"
			+ "   </rdf:Description>\n"
			+ "   <rdf:Description rdf:nodeID=\"pmid19885433mesh5\">\n"
			+ "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n"
			+ "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n"
			+ "      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
			+ "      <rdfs:label>Dental Restoration Failure</rdfs:label>\n"
			+ "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
			+ "      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
			+ "      <score:Descriptor>Dental Restoration Failure</score:Descriptor>\n"
			+ "      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n"
			+ "      <score:Qualifier/>\n"
			+ "      <score:QualifierIsMajorTerm/>\n"
			+ "   </rdf:Description>\n"
			+ "   <rdf:Description rdf:nodeID=\"pmid19885433mesh6\">\n"
			+ "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n"
			+ "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n"
			+ "      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
			+ "      <rdfs:label>Gingiva</rdfs:label>\n"
			+ "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
			+ "      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
			+ "      <score:Descriptor>Gingiva</score:Descriptor>\n"
			+ "      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n"
			+ "      <score:Qualifier>anatomy &amp; histology</score:Qualifier>\n"
			+ "      <score:QualifierIsMajorTerm>N</score:QualifierIsMajorTerm>\n"
			+ "   </rdf:Description>\n"
			+ "   <rdf:Description rdf:nodeID=\"pmid19885433mesh7\">\n"
			+ "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n"
			+ "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n"
			+ "      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
			+ "      <rdfs:label>Humans</rdfs:label>\n"
			+ "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
			+ "      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
			+ "      <score:Descriptor>Humans</score:Descriptor>\n"
			+ "      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n"
			+ "      <score:Qualifier/>\n"
			+ "      <score:QualifierIsMajorTerm/>\n"
			+ "   </rdf:Description>\n"
			+ "   <rdf:Description rdf:nodeID=\"pmid19885433mesh8\">\n"
			+ "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n"
			+ "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n"
			+ "      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
			+ "      <rdfs:label>Infection</rdfs:label>\n"
			+ "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
			+ "      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
			+ "      <score:Descriptor>Infection</score:Descriptor>\n"
			+ "      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n"
			+ "      <score:Qualifier/>\n"
			+ "      <score:QualifierIsMajorTerm/>\n"
			+ "   </rdf:Description>\n"
			+ "   <rdf:Description rdf:nodeID=\"pmid19885433mesh9\">\n"
			+ "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n"
			+ "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n"
			+ "      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
			+ "      <rdfs:label>Periodontitis</rdfs:label>\n"
			+ "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
			+ "      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
			+ "      <score:Descriptor>Periodontitis</score:Descriptor>\n"
			+ "      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n"
			+ "      <score:Qualifier/>\n"
			+ "      <score:QualifierIsMajorTerm/>\n"
			+ "   </rdf:Description>\n"
			+ "   <rdf:Description rdf:nodeID=\"pmid19885433mesh10\">\n"
			+ "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/score#MeshTerm\"/>\n"
			+ "      <rdf:type rdf:resource=\"http://vivoweb.org/ontology/core#SubjectArea\"/>\n"
			+ "      <core:SubjectAreaFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
			+ "      <rdfs:label>Risk Factors</rdfs:label>\n"
			+ "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
			+ "      <score:meshTermOf rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
			+ "      <score:Descriptor>Risk Factors</score:Descriptor>\n"
			+ "      <score:DescriptorIsMajorTerm>N</score:DescriptorIsMajorTerm>\n"
			+ "      <score:Qualifier/>\n"
			+ "      <score:QualifierIsMajorTerm/>\n"
			+ "   </rdf:Description>\n"
			+ "   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/journal/j0882-2786\">\n"
			+ "      <rdf:type rdf:resource=\"http://purl.org/ontology/bibo/Journal\"/>\n"
			+ "      <rdf:type rdf:resource=\"http://vitro.mannlib.cornell.edu/ns/vitro/0.7#Flag1Value1Thing\"/>\n"
			+ "      <core:Title>The International journal of oral &amp; maxillofacial implants</core:Title>\n"
			+ "      <rdfs:label>The International journal of oral &amp; maxillofacial implants</rdfs:label>\n"
			+ "      <bibo:ISSN>0882-2786</bibo:ISSN>\n"
			+ "      <core:publicationVenueFor rdf:resource=\"http://vivoweb.org/pubMed/article/pmid19885433\"/>\n"
			+ "   </rdf:Description>\n"
			+ "   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/article/pmid19885433/dateCreated\">\n"
			+ "      <core:Year>\"2009\"</core:Year>\n"
			+ "      <core:Month>\"11\"</core:Month>\n"
			+ "      <core:Day>\"03\"</core:Day>\n"
			+ "   </rdf:Description>\n"
			+ "   <rdf:Description rdf:about=\"http://vivoweb.org/pubMed/article/pmid19885433/dateCompleted\">\n"
			+ "      <core:Year>2010</core:Year>\n"
			+ "      <core:Month>02</core:Month>\n"
			+ "      <core:Day>02</core:Day>\n"
			+ "   </rdf:Description>\n"
			+ "</rdf:RDF>";
	
	/** */
	private static final String mapFilePath = "example-scripts/bash-scripts/full-harvest-examples/1.6-examples/example-pubmed/pubmed-to-vivo.datamap.xsl";
	/** */
	private RecordHandler inRH;
	/** */
	private RecordHandler outRH;
	
	@Override
	protected void setUp() throws Exception {
		InitLog.initLogger(null, null);
		this.inRH = new TextFileRecordHandler("tmp://TranslateTestXML");
		this.outRH = new TextFileRecordHandler("tmp://TranslateTestRDF");
	}
	
	/**
	 * Test method for {@link org.vivoweb.harvester.translate.XSLTranslator#main(java.lang.String[]) main(String...
	 * args)}.
	 * @throws IOException error
	 */
	public void testPubMedXSLTArticle() throws IOException {
		log.info("BEGIN testPubMedXSLTArticle");
		// add a record to the record handler
		this.inRH.addRecord("Article", inputDataArticle, this.getClass());
		
		// call the xlsTranslate
		new XSLTranslator(this.inRH, this.outRH, FileAide.getInputStream(mapFilePath), false, false).execute();
		
		// verify that output record handler has some records
		assertTrue(this.outRH.iterator().hasNext());
		
		// compare the record data with expected output
		log.debug("Translation Output:\n" + this.outRH.getRecordData("Article"));
		
//			assertEquals(outputData, this.outRH.getRecordData("Article"));
		log.info("END testPubMedXSLTArticle");
	}
	
	/**
	 * Test method for {@link org.vivoweb.harvester.translate.XSLTranslator#main(java.lang.String[]) main(String...
	 * args)}.
	 * @throws IOException error
	 */
	public void testPubMedXSLTBook() throws IOException {
		log.info("BEGIN testPubMedXSLTBook");
		// add a record to the record handler
		this.inRH.addRecord("Book", inputDataBook, this.getClass());
		
		// call the xlsTranslate
		new XSLTranslator(this.inRH, this.outRH, FileAide.getInputStream(mapFilePath), false, false).execute();
		
		// verify that output record handler has some records
		assertTrue(this.outRH.iterator().hasNext());
		
		// compare the record data with expected output
		log.debug("Translation Output:\n" + this.outRH.getRecordData("Book"));
		
//			assertEquals(outputData, this.outRH.getRecordData("Book"));
		log.info("END testPubMedXSLTBook");
	}
	
	/**
	 * Test method for {@link org.vivoweb.harvester.translate.XSLTranslator#main(java.lang.String[]) main(String...
	 * args)}.
	 * @throws IOException error
	 */
	public void testPubMedXSLTArticleNoAffiliation() throws IOException {
		log.info("BEGIN testPubMedXSLTArticleNoAffiliation");
		// add a record to the record handler
		this.inRH.addRecord("ArticleNoAffiliation", inputDataArticle.replaceAll("<Affiliation.*?/Affiliation>", ""), this.getClass());
		
		// call the xlsTranslate
		new XSLTranslator(this.inRH, this.outRH, FileAide.getInputStream(mapFilePath), false, false).execute();
		
		// verify that output record handler has some records
		assertTrue(this.outRH.iterator().hasNext());
		
		// compare the record data with expected output
		log.debug("Translation Output:\n" + this.outRH.getRecordData("ArticleNoAffiliation"));
		
//			assertEquals(outputData, this.outRH.getRecordData("Article"));
		log.info("END testPubMedXSLTArticleNoAffiliation");
	}
	
	@Override
	public void tearDown() {
		try {
			for(Record r : this.inRH) {
				this.inRH.delRecord(r.getID());
			}
			this.inRH.close();
		} catch(IOException e) {
			log.debug(e.getMessage(), e);
		}
		this.inRH = null;
		try {
			for(Record r : this.outRH) {
				this.outRH.delRecord(r.getID());
			}
			this.outRH.close();
		} catch(IOException e) {
			log.debug(e.getMessage(), e);
		}
		this.outRH = null;
	}
}
