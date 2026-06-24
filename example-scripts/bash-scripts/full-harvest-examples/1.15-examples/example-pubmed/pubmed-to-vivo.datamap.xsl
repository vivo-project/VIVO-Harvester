<!--
  Copyright (c) 2010-2011 VIVO Harvester Team. For full list of contributors, please see the AUTHORS file provided.
  All rights reserved.
  This program and the accompanying materials are made available under the terms of the new BSD license which accompanies this distribution, and is available at http://www.opensource.org/licenses/bsd-license.html
-->
<!-- <?xml version="1.0"?> -->
<!-- Header information for the Style Sheet
	The style sheet requires xmlns for each prefix you use in constructing
	the new elements
-->
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:xs="http://www.w3.org/2001/XMLSchema"
	xmlns:fn="http://www.w3.org/2005/xpath-functions"
	xmlns:functx="http://www.functx.com"
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
	xmlns:core="http://vivoweb.org/ontology/core#"
	xmlns:vitro = "http://vitro.mannlib.cornell.edu/ns/vitro/0.7#"
	xmlns:foaf="http://xmlns.com/foaf/0.1/"
	xmlns:score='http://vivoweb.org/ontology/score#'
	xmlns:bibo='http://purl.org/ontology/bibo/'
	xmlns:obo = "http://purl.obolibrary.org/obo/"
	xmlns:vcard = "http://www.w3.org/2006/vcard/ns#"
	xmlns:rdfs='http://www.w3.org/2000/01/rdf-schema#'
	xmlns:localVivo='http://vivo.sample.edu/ontology/'>

	<!-- This will create indenting in xml readers -->
	<xsl:output method="xml" indent="yes"/>
	<xsl:variable name="baseURI">http://vivoweb.org/harvest/pubmed/</xsl:variable>
    <xsl:function name="functx:substring-before-last-match" as="xs:string?"
              xmlns:functx="http://www.functx.com">
     <xsl:param name="arg" as="xs:string?"/>
     <xsl:param name="regex" as="xs:string"/>

     <xsl:sequence select="replace($arg,concat('^(.*)',$regex,'.*'),'$1')
    "/>

</xsl:function>
	<!-- The main Article Set of all pubmed citations loaded 
		This serves as the header of the RDF file produced
	 -->
	<xsl:template match="/PubmedArticleSet">
		<rdf:RDF xmlns:owlPlus='http://www.w3.org/2006/12/owl2-xml#'
			xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'
			xmlns:skos='http://www.w3.org/2008/05/skos#'
			xmlns:rdfs='http://www.w3.org/2000/01/rdf-schema#'
			xmlns:owl='http://www.w3.org/2002/07/owl#'
			xmlns:vocab='http://purl.org/vocab/vann/'
			xmlns:swvocab='http://www.w3.org/2003/06/sw-vocab-status/ns#'
			xmlns:dc='http://purl.org/dc/elements/1.1/'
			xmlns:vitro='http://vitro.mannlib.cornell.edu/ns/vitro/0.7#' 
			xmlns:core='http://vivoweb.org/ontology/core#'
			xmlns:foaf='http://xmlns.com/foaf/0.1/'
			xmlns:obo = "http://purl.obolibrary.org/obo/"
			xmlns:vcard = "http://www.w3.org/2006/vcard/ns#"
			xmlns:score='http://vivoweb.org/ontology/score#'
			xmlns:xs='http://www.w3.org/2001/XMLSchema#'
			xmlns:fn="http://www.w3.org/2005/xpath-functions"
			xmlns:functx="http://www.functx.com"
			xmlns:localVivo='http://vivo.sample.edu/ontology/'>
			<xsl:apply-templates select="PubmedArticle" />
			<xsl:apply-templates select="PubmedBookArticle" />
		</rdf:RDF>
	</xsl:template>
	
	<!-- The Article -->
	<xsl:template match="PubmedArticle">
		
        <xsl:variable name="pmid" ><xsl:value-of select="MedlineCitation/PMID" /></xsl:variable>
        
		<rdf:Description rdf:about="{$baseURI}pub/pmid{child::MedlineCitation/PMID}">
			<xsl:apply-templates select='MedlineCitation/Article/PublicationTypeList/PublicationType' />
			 
			<localVivo:harvestedBy>PubMed-Harvester</localVivo:harvestedBy>
			<bibo:pmid><xsl:value-of select="MedlineCitation/PMID" /></bibo:pmid>
			<rdfs:label><xsl:value-of select="MedlineCitation/Article/ArticleTitle" /></rdfs:label>
			 
			<!--  <score:Affiliation><xsl:value-of select="MedlineCitation/Article/Affiliation" /></score:Affiliation> -->
			<bibo:volume><xsl:value-of select="MedlineCitation/Article/Journal/JournalIssue/Volume"/></bibo:volume>
			<bibo:number><xsl:value-of select="MedlineCitation/Article/Journal/JournalIssue/Issue"/></bibo:number>
			<bibo:abstract><xsl:value-of select="MedlineCitation/Article/Abstract"/></bibo:abstract>
			
			<xsl:apply-templates select="MedlineCitation/ChemicalList/Chemical" />
			<!-- keywords -->
			<xsl:apply-templates select="MedlineCitation/KeywordList/Keyword" />
			<xsl:apply-templates  select="MedlineCitation/MeshHeadingList/MeshHeading" mode="termAsKeyword" />
			
			<xsl:choose>
				<xsl:when test='string(PubmedData/ArticleIdList/ArticleId[@IdType="doi"])'>
					<bibo:doi><xsl:value-of select='PubmedData/ArticleIdList/ArticleId[@IdType="doi"]' /></bibo:doi>
				</xsl:when>
			</xsl:choose>
			<xsl:variable name="MonthNumber">
				<xsl:choose>
					<xsl:when test="string(MedlineCitation/Article/Journal/JournalIssue/PubDate/Month)=Jan">01</xsl:when>
					<xsl:when test="string(MedlineCitation/Article/Journal/JournalIssue/PubDate/Month)=Feb">02</xsl:when>
					<xsl:when test="string(MedlineCitation/Article/Journal/JournalIssue/PubDate/Month)=Mar">03</xsl:when>
					<xsl:when test="string(MedlineCitation/Article/Journal/JournalIssue/PubDate/Month)=Apr">04</xsl:when>
					<xsl:when test="string(MedlineCitation/Article/Journal/JournalIssue/PubDate/Month)=May">05</xsl:when>
					<xsl:when test="string(MedlineCitation/Article/Journal/JournalIssue/PubDate/Month)=Jun">06</xsl:when>
					<xsl:when test="string(MedlineCitation/Article/Journal/JournalIssue/PubDate/Month)=Jul">07</xsl:when>
					<xsl:when test="string(MedlineCitation/Article/Journal/JournalIssue/PubDate/Month)=Aug">08</xsl:when>
					<xsl:when test="string(MedlineCitation/Article/Journal/JournalIssue/PubDate/Month)=Sep">09</xsl:when>
					<xsl:when test="string(MedlineCitation/Article/Journal/JournalIssue/PubDate/Month)=Oct">10</xsl:when>
					<xsl:when test="string(MedlineCitation/Article/Journal/JournalIssue/PubDate/Month)=Nov">11</xsl:when>
					<xsl:when test="string(MedlineCitation/Article/Journal/JournalIssue/PubDate/Month)=Dec">12</xsl:when>
				</xsl:choose>
			</xsl:variable>
			<xsl:choose>
				<xsl:when test="string(MedlineCitation/Article/Journal/JournalIssue/PubDate/Year)">
				    <core:dateTimeValue>
                        <rdf:Description rdf:about="{$baseURI}pub/year{MedlineCitation/Article/Journal/JournalIssue/PubDate/Year}">
					        <rdf:type rdf:resource="http://vivoweb.org/ontology/core#DateTimeValue"/>
					        <core:dateTimePrecision rdf:resource="http://vivoweb.org/ontology/core#yearPrecision"/>
	                        <core:dateTime rdf:datatype="http://www.w3.org/2001/XMLSchema#dateTime"><xsl:value-of select="MedlineCitation/Article/Journal/JournalIssue/PubDate/Year"/>-01-01T00:00:00</core:dateTime>
				        </rdf:Description>
                    </core:dateTimeValue>
					<!-- <core:year rdf:datatype="http://www.w3.org/2001/XMLSchema#gYear"><xsl:value-of select="MedlineCitation/Article/Journal/JournalIssue/PubDate/Year"/></core:year> -->
				</xsl:when>
				<xsl:when test="string(MedlineCitation/Article/Journal/JournalIssue/PubDate/Month) and string(MedlineCitation/Article/Journal/JournalIssue/PubDate/Year)">
                    <core:dateTimeValue>
                        <rdf:Description rdf:about="{$baseURI}pub/monthyear{$MonthNumber}{MedlineCitation/Article/Journal/JournalIssue/PubDate/Year}">
	                        <rdf:type rdf:resource="http://vivoweb.org/ontology/core#DateTimeValue"/>
	                        <core:dateTimePrecision rdf:resource="http://vivoweb.org/ontology/core#yearMonthPrecision"/>
	                        <core:dateTime rdf:datatype="http://www.w3.org/2001/XMLSchema#dateTime"><xsl:value-of select="MedlineCitation/Article/Journal/JournalIssue/PubDate/Year"/>-<xsl:copy-of select="$MonthNumber"/>-01T00:00:00</core:dateTime>
                        </rdf:Description>
                    </core:dateTimeValue>
					<!-- <core:yearMonth rdf:datatype="http://www.w3.org/2001/XMLSchema#gYearMonth"><xsl:value-of select="MedlineCitation/Article/Journal/JournalIssue/PubDate/Year"/>-<xsl:copy-of select="$MonthNumber" /></core:yearMonth> -->
				</xsl:when>
				<xsl:when test="string(MedlineCitation/Article/Journal/JournalIssue/PubDate/Day) and string(MedlineCitation/Article/Journal/JournalIssue/PubDate/Month) and string(MedlineCitation/Article/Journal/JournalIssue/PubDate/Year)">
					<core:dateTimeValue>
                        <rdf:Description rdf:about="{$baseURI}pub/daymonthyear{MedlineCitation/Article/Journal/JournalIssue/PubDate/Day}{$MonthNumber}{MedlineCitation/Article/Journal/JournalIssue/PubDate/Year}">
	                        <rdf:type rdf:resource="http://vivoweb.org/ontology/core#DateTimeValue"/>
	                        <core:dateTimePrecision rdf:resource="http://vivoweb.org/ontology/core#yearMonthDayPrecision"/>
	                        <core:dateTime rdf:datatype="http://www.w3.org/2001/XMLSchema#dateTime"><xsl:value-of select="MedlineCitation/Article/Journal/JournalIssue/PubDate/Year"/>-<xsl:copy-of select="$MonthNumber"/>-<xsl:value-of select="MedlineCitation/Article/Journal/JournalIssue/PubDate/Day"/>T00:00:00</core:dateTime>
                        </rdf:Description>
                    </core:dateTimeValue>
					<!-- <core:date rdf:datatype="http://www.w3.org/2001/XMLSchema#date"><xsl:value-of select="MedlineCitation/Article/Journal/JournalIssue/PubDate/Year"/>-<xsl:copy-of select="$MonthNumber" />-<xsl:value-of select="MedlineCitation/Article/Journal/JournalIssue/PubDate/Day"/></core:date> -->
				</xsl:when>
			</xsl:choose>
			<!-- parse page numbers: START -->
			<bibo:pageStart><xsl:value-of select="substring-before(MedlineCitation/Article/Pagination/MedlinePgn, '-')" /></bibo:pageStart>
			<xsl:variable name="pageStart"><xsl:value-of select="substring-before(MedlineCitation/Article/Pagination/MedlinePgn, '-')" /></xsl:variable>
			<xsl:variable name="pageEnd"><xsl:value-of select="substring-after(MedlineCitation/Article/Pagination/MedlinePgn, '-')" /></xsl:variable>
			<xsl:choose>
				<xsl:when test="string-length($pageStart) &gt; string-length($pageEnd)">
					<xsl:variable name="cutoff"><xsl:value-of select="(string-length($pageStart))-(string-length($pageEnd))+1" /></xsl:variable>
					<bibo:pageEnd><xsl:value-of select="concat(substring($pageStart,0,$cutoff),$pageEnd)"/></bibo:pageEnd>
				</xsl:when>
				<xsl:when test="string-length($pageStart) &lt;= string-length($pageEnd)">
					<bibo:pageEnd><xsl:value-of select="$pageEnd"/></bibo:pageEnd>
				</xsl:when>
			</xsl:choose>
			<!-- parse page numbers: END -->
			<xsl:apply-templates select="MedlineCitation/Article/AuthorList/Author" mode="authorRef" />
			<xsl:apply-templates select="MedlineCitation/Article/Journal" mode="journalRef"/>
		</rdf:Description>
		
		<xsl:apply-templates select="MedlineCitation/Article/AuthorList/Author" mode="fullAuthor" /> 
		 
		<xsl:apply-templates select="MedlineCitation/Article/Journal" mode="fullJournal" />
	</xsl:template>
	
	<!-- The Book Article -->
	<xsl:template match="PubmedBookArticle">
	    <xsl:variable name="lastName" select="normalize-space(LastName)"/>
        <xsl:variable name="firstName" select="normalize-space(ForeName)"/>    
        <xsl:variable name="fullName"><xsl:value-of select = "$lastName" />, <xsl:value-of select = "$firstName" /></xsl:variable>
        <xsl:variable name="pmid" select="ancestor::BookDocument/PMID" />
        
        <xsl:comment>book pmid is <xsl:value-of select="$pmid" /></xsl:comment>
        
		<rdf:Description rdf:about="{$baseURI}pub/pmid{child::BookDocument/PMID}">
			<rdf:type rdf:resource="http://purl.org/ontology/bibo/Article" />
			<rdf:type rdf:resource="http://purl.org/ontology/bibo/Book" /> 
			<localVivo:harvestedBy>PubMed-Harvester</localVivo:harvestedBy>
			<bibo:pmid><xsl:value-of select="BookDocument/PMID" /></bibo:pmid>
			<rdfs:label><xsl:value-of select="BookDocument/Book/BookTitle" /></rdfs:label>
			<core:relatedBy rdf:resource="{$baseURI}authorship/pmid{$pmid}authorship{position()}" />  
			<score:Affiliation><xsl:value-of select="BookDocument/Book/CollectionTitle" /></score:Affiliation>
			<bibo:abstract><xsl:value-of select="BookDocument/Abstract"/></bibo:abstract>
			<xsl:apply-templates select="BookDocument/KeywordList/Keyword" />
			<xsl:choose>
				<xsl:when test='string(PubmedData/ArticleIdList/ArticleId[@IdType="doi"])'>
					<bibo:doi><xsl:value-of select='PubmedData/ArticleIdList/ArticleId[@IdType="doi"]' /></bibo:doi>
				</xsl:when>
			</xsl:choose>
			<xsl:choose>
				<xsl:when test='string(PubmedBookData/ArticleIdList/ArticleId[@IdType="doi"])'>
					<bibo:doi><xsl:value-of select='PubmedBookData/ArticleIdList/ArticleId[@IdType="doi"]' /></bibo:doi>
				</xsl:when>
			</xsl:choose>
			<xsl:variable name="MonthNumber">
				<xsl:choose>
					<xsl:when test="string(BookDocument/Book/PubDate/Month)=Jan">01</xsl:when>
					<xsl:when test="string(BookDocument/Book/PubDate/Month)=Feb">02</xsl:when>
					<xsl:when test="string(BookDocument/Book/PubDate/Month)=Mar">03</xsl:when>
					<xsl:when test="string(BookDocument/Book/PubDate/Month)=Apr">04</xsl:when>
					<xsl:when test="string(BookDocument/Book/PubDate/Month)=May">05</xsl:when>
					<xsl:when test="string(BookDocument/Book/PubDate/Month)=Jun">06</xsl:when>
					<xsl:when test="string(BookDocument/Book/PubDate/Month)=Jul">07</xsl:when>
					<xsl:when test="string(BookDocument/Book/PubDate/Month)=Aug">08</xsl:when>
					<xsl:when test="string(BookDocument/Book/PubDate/Month)=Sep">09</xsl:when>
					<xsl:when test="string(BookDocument/Book/PubDate/Month)=Oct">10</xsl:when>
					<xsl:when test="string(BookDocument/Book/PubDate/Month)=Nov">11</xsl:when>
					<xsl:when test="string(BookDocument/Book/PubDate/Month)=Dec">12</xsl:when>
				</xsl:choose>
			</xsl:variable>
			<xsl:choose>
				<xsl:when test="string(BookDocument/Book/PubDate/Year)">
                    <core:dateTimeValue>
                        <rdf:Description rdf:about="{$baseURI}pub/year{BookDocument/Book/PubDate/Year}">
                            <rdf:type rdf:resource="http://vivoweb.org/ontology/core#DateTimeValue"/>
                            <core:dateTimePrecision rdf:resource="http://vivoweb.org/ontology/core#yearPrecision"/>
                            <core:dateTime rdf:datatype="http://www.w3.org/2001/XMLSchema#dateTime"><xsl:value-of select="BookDocument/Book/PubDate/Year"/>-01-01T00:00:00</core:dateTime>
                        </rdf:Description>
                    </core:dateTimeValue>
<!-- 					<core:year rdf:datatype="http://www.w3.org/2001/XMLSchema#gYear"><xsl:value-of select="BookDocument/Book/PubDate/Year"/></core:year> -->
				</xsl:when>
				<xsl:when test="string(BookDocument/Book/PubDate/Month) and string(BookDocument/Book/PubDate/Year)">
                    <core:dateTimeValue>
                        <rdf:Description rdf:about="{$baseURI}pub/monthyear{$MonthNumber}{BookDocument/Book/PubDate/Year}">
                            <rdf:type rdf:resource="http://vivoweb.org/ontology/core#DateTimeValue"/>
                            <core:dateTimePrecision rdf:resource="http://vivoweb.org/ontology/core#yearMonthPrecision"/>
                            <core:dateTime rdf:datatype="http://www.w3.org/2001/XMLSchema#dateTime"><xsl:value-of select="BookDocument/Book/PubDate/Year"/>-<xsl:copy-of select="$MonthNumber"/>-01T00:00:00</core:dateTime>
                        </rdf:Description>
                    </core:dateTimeValue>
<!-- 					<core:yearMonth rdf:datatype="http://www.w3.org/2001/XMLSchema#gYearMonth"><xsl:value-of select="BookDocument/Book/PubDate/Year"/>-<xsl:copy-of select="$MonthNumber" /></core:yearMonth> -->
				</xsl:when>
				<xsl:when test="string(BookDocument/Book/PubDate/Day) and string(BookDocument/Book/PubDate/Month) and string(BookDocument/Book/PubDate/Year)">
                    <core:dateTimeValue>
                        <rdf:Description rdf:about="{$baseURI}pub/daymonthyear{MedlineCitation/Article/Journal/JournalIssue/PubDate/Day}{$MonthNumber}{BookDocument/Book/PubDate/Year}">
                            <rdf:type rdf:resource="http://vivoweb.org/ontology/core#DateTimeValue"/>
                            <core:dateTimePrecision rdf:resource="http://vivoweb.org/ontology/core#yearMonthDayPrecision"/>
                            <core:dateTime rdf:datatype="http://www.w3.org/2001/XMLSchema#dateTime"><xsl:value-of select="BookDocument/Book/PubDate/Year"/>-<xsl:copy-of select="$MonthNumber"/>-<xsl:value-of select="BookDocument/Book/PubDate/Day"/>T00:00:00</core:dateTime>
                        </rdf:Description>
                    </core:dateTimeValue>
<!-- 					<core:date rdf:datatype="http://www.w3.org/2001/XMLSchema#date"><xsl:value-of select="BookDocument/Book/PubDate/Year"/>-<xsl:copy-of select="$MonthNumber" />-<xsl:value-of select="BookDocument/Book/PubDate/Day"/></core:date> -->
				</xsl:when>
			</xsl:choose>
			<xsl:apply-templates select="BookDocument/Book/AuthorList/Author" mode="authorRef" />
		</rdf:Description>
		<xsl:apply-templates select="BookDocument/Book/AuthorList/Author" mode="fullAuthor" />
	</xsl:template>

	<xsl:template match="MedlineCitation/Article/Journal" mode="journalRef">
		<core:hasPublicationVenue rdf:resource="{$baseURI}journal/journal{child::ISSN}" />
	</xsl:template>

<!-- The Main Journal Entity -->
	<xsl:template match="MedlineCitation/Article/Journal" mode="fullJournal">
		<rdf:Description rdf:about="{$baseURI}journal/journal{child::ISSN}" >
			<localVivo:harvestedBy>PubMed-Harvester</localVivo:harvestedBy>
			<rdf:type rdf:resource="http://purl.org/ontology/bibo/Journal" /> 
			<rdfs:label><xsl:value-of select="Title" /></rdfs:label>
			<bibo:issn><xsl:value-of select="ISSN"/></bibo:issn>
			<core:publicationVenueFor rdf:resource="{$baseURI}pub/pmid{ancestor::MedlineCitation/PMID}"/>
		</rdf:Description>	
	</xsl:template>

	<!-- Links to From the Article to the Terms and Authors -->
	<xsl:template match="MedlineCitation/Article/AuthorList/Author" mode="authorRef"> 
		<core:relatedBy rdf:resource="{$baseURI}authorship/pmid{ancestor::MedlineCitation/PMID}authorship{position()}" />
	</xsl:template> 

	<!-- Article Author List Navigation -->
	<xsl:template match="MedlineCitation/Article/AuthorList/Author" mode="fullAuthor">
	    
	   <xsl:variable name="authorEmail" >
	       <xsl:apply-templates select="AffiliationInfo/Affiliation" />
		</xsl:variable>
	    
	    <xsl:variable name="lastName" select="normalize-space(LastName)"/>
        <xsl:variable name="firstName" select="normalize-space(ForeName)"/>    
        <xsl:variable name="fullName"><xsl:value-of select = "$lastName" />, <xsl:value-of select = "$firstName" /></xsl:variable>
        <xsl:variable name="pmid" select="ancestor::MedlineCitation/PMID" />
         
	    <!-- authorship -->
		<rdf:Description rdf:about="{$baseURI}authorship/pmid{ancestor::MedlineCitation/PMID}authorship{position()}">
		   <localVivo:harvestedBy>PubMed-Harvester</localVivo:harvestedBy>
			<rdf:type rdf:resource="http://vivoweb.org/ontology/core#Authorship" />
			<vitro:mostSpecificType rdf:resource="http://vivoweb.org/ontology/core#Authorship" />
			<rdf:type rdf:resource="http://vivoweb.org/ontology/core#Relationship" />
			<rdf:type rdf:resource="http://www.w3.org/2002/07/owl#Thing"/>
			<rdf:type rdf:resource="http://purl.obolibrary.org/obo/BFO_0000020" />
			<rdf:type rdf:resource="http://purl.obolibrary.org/obo/BFO_0000001" />
			<rdf:type rdf:resource="http://purl.obolibrary.org/obo/BFO_0000002" />
			<xsl:choose>
				<xsl:when test="$firstName">
					<rdfs:label>Authorship for <xsl:value-of select = "$firstName" />, <xsl:value-of select = "$lastName" /></rdfs:label>
				</xsl:when>
				<xsl:when test="$lastName">
					<rdfs:label>Authorship for <xsl:value-of select="$lastName" /></rdfs:label>
				</xsl:when> 
			</xsl:choose>
			<core:relates rdf:resource="{$baseURI}author/pmid{$pmid}author{position()}" />
			<core:relates rdf:resource="{$baseURI}pub/pmid{$pmid}" />
			<core:authorRank rdf:datatype="http://www.w3.org/2001/XMLSchema#int"><xsl:value-of select="position()" /></core:authorRank>
		</rdf:Description>
		
		<!-- author -->
		<rdf:Description rdf:about="{$baseURI}author/pmid{ancestor::MedlineCitation/PMID}author{position()}">
		   <rdf:type rdf:resource = "http://xmlns.com/foaf/0.1/Person"/>
           <vitro:mostSpecificType rdf:resource="http://xmlns.com/foaf/0.1/Person"/>
           <rdfs:label rdf:datatype="http://www.w3.org/2001/XMLSchema#string"><xsl:value-of select = "$fullName" /></rdfs:label>
           <obo:ARG_2000028 rdf:resource="{$baseURI}vcard/pmid{ancestor::MedlineCitation/PMID}vcard{position()}"/>
		</rdf:Description>
		
		<!-- vcard -->
		<rdf:Description rdf:about="{$baseURI}vcard/pmid{ancestor::MedlineCitation/PMID}vcard{position()}">
		   <obo:ARG_2000029 rdf:resource="{$baseURI}author/pmid{ancestor::MedlineCitation/PMID}author{position()}"/>
           <vitro:mostSpecificType rdf:resource="http://www.w3.org/2006/vcard/ns#Individual"/>
           <rdf:type rdf:resource="http://www.w3.org/2006/vcard/ns#Individual"/>
           <rdf:type rdf:resource="http://purl.obolibrary.org/obo/ARG_2000379"/>
           <rdfs:label rdf:datatype="http://www.w3.org/2001/XMLSchema#string">vCard for: <xsl:value-of select = "$fullName" /></rdfs:label>
           <vcard:hasName rdf:resource="{$baseURI}vcard/pmid{ancestor::MedlineCitation/PMID}vcardName{position()}"/>
           <xsl:if test="normalize-space($authorEmail)" >
           <vcard:hasEmail rdf:resource="{$baseURI}vcard/pmid{ancestor::MedlineCitation/PMID}vcardEmail{position()}"/>
           </xsl:if>
		</rdf:Description>
		
		<!-- vcard name -->
		<rdf:Description rdf:about="{$baseURI}vcard/pmid{ancestor::MedlineCitation/PMID}vcardName{position()}">
		   <rdf:type rdf:resource="http://www.w3.org/2006/vcard/ns#Name"/> 
           <vitro:mostSpecificType rdf:resource="http://www.w3.org/2006/vcard/ns#Name"/>
           <rdfs:label rdf:datatype="http://www.w3.org/2001/XMLSchema#string">vCard name for: <xsl:value-of select = "$fullName" /></rdfs:label>
           <vcard:givenName rdf:datatype="http://www.w3.org/2001/XMLSchema#string"><xsl:value-of select = "$firstName" /></vcard:givenName>     
           <vcard:familyName rdf:datatype="http://www.w3.org/2001/XMLSchema#string"><xsl:value-of select = "$lastName" /></vcard:familyName>
		</rdf:Description>
		
		<!-- vcard email -->
		 <xsl:if test="normalize-space($authorEmail)" >
	     <rdf:Description rdf:about="{$baseURI}vcard/pmid{ancestor::MedlineCitation/PMID}vcardEmail{position()}">
	       <rdf:type rdf:resource="http://www.w3.org/2006/vcard/ns#Email"/>
	       <rdf:type rdf:resource="http://www.w3.org/2006/vcard/ns#Work"/>
	       <vitro:mostSpecificType rdf:resource="http://www.w3.org/2006/vcard/ns#Email"/>
	       <rdfs:label rdf:datatype="http://www.w3.org/2001/XMLSchema#string">vCard email for: <xsl:value-of select = "$fullName" /></rdfs:label>
	       <vcard:email rdf:datatype="http://www.w3.org/2001/XMLSchema#string"><xsl:value-of select = "$authorEmail" /></vcard:email>  
	     </rdf:Description>
	     </xsl:if>
	</xsl:template> 
	
	<xsl:template match="AffiliationInfo/Affiliation">
	   <xsl:choose>
			<xsl:when test="normalize-space(.)" >
				<xsl:analyze-string select="string(.)" regex="\s*([a-zA-Z\d\.]*@[a-zA-Z\d\.]*)"> 
				  
					<xsl:matching-substring>
					<xsl:choose>
					<xsl:when test="fn:ends-with(regex-group(1), '.')">
					<xsl:value-of select="functx:substring-before-last-match(regex-group(1), '.')" />  
					</xsl:when> 
					<xsl:otherwise><xsl:value-of select="regex-group(1)" /></xsl:otherwise> 
					</xsl:choose>   
					</xsl:matching-substring>
								
				</xsl:analyze-string>
			</xsl:when>
			<xsl:otherwise></xsl:otherwise>
			</xsl:choose>
	</xsl:template>
	
	<!-- Links to From the Book to the Terms and Authors -->
	<xsl:template match="BookDocument/Book/AuthorList/Author" mode="authorRef">
		<core:relatedBy rdf:resource="{$baseURI}authorship/pmid{ancestor::MedlineCitation/PMID}authorship{position()}" />
	</xsl:template>

	<!-- Book Author List Navigation --> 
	<xsl:template match="BookDocument/Book/AuthorList/Author" mode="fullAuthor">
	    <xsl:variable name="lastName" select="normalize-space(LastName)"/>
        <xsl:variable name="firstName" select="normalize-space(ForeName)"/>    
        <xsl:variable name="fullName"><xsl:value-of select = "$lastName" />, <xsl:value-of select = "$firstName" /></xsl:variable>
        <xsl:variable name="pmid" select="ancestor::MedlineCitation/PMID" />
        <xsl:variable name="authorEmail" >
	       <xsl:apply-templates select="AffiliationInfo/Affiliation" />
		</xsl:variable>
		
		<!-- authorship -->
		<rdf:Description rdf:about="{$baseURI}authorship/pmid{ancestor::BookDocument/PMID}authorship{position()}">
			<localVivo:harvestedBy>PubMed-Harvester</localVivo:harvestedBy>
			<rdf:type rdf:resource="http://vivoweb.org/ontology/core#Authorship" />
			<vitro:mostSpecificType rdf:resource="http://vivoweb.org/ontology/core#Authorship" />
			<rdf:type rdf:resource="http://vivoweb.org/ontology/core#Relationship" />
			<rdf:type rdf:resource="http://www.w3.org/2002/07/owl#Thing"/>
			<rdf:type rdf:resource="http://purl.obolibrary.org/obo/BFO_0000020" />
			<rdf:type rdf:resource="http://purl.obolibrary.org/obo/BFO_0000001" />
			<rdf:type rdf:resource="http://purl.obolibrary.org/obo/BFO_0000002" />
			<xsl:choose>
				<xsl:when test="$firstName">
					<rdfs:label>Authorship for <xsl:value-of select = "$firstName" />, <xsl:value-of select = "$lastName" /></rdfs:label>
				</xsl:when>
				<xsl:when test="$lastName">
					<rdfs:label>Authorship for <xsl:value-of select="$lastName" /></rdfs:label>
				</xsl:when> 
			</xsl:choose>
			<core:relates rdf:resource="{$baseURI}author/pmid{$pmid}author{position()}" />
			<core:relates rdf:resource="{$baseURI}pub/pmid{$pmid}" />
			<core:authorRank rdf:datatype="http://www.w3.org/2001/XMLSchema#int"><xsl:value-of select="position()" /></core:authorRank>			
		</rdf:Description>
		
		<!-- author -->
		<rdf:Description rdf:about="{$baseURI}author/pmid{ancestor::MedlineCitation/PMID}author{position()}">
		   <rdf:type rdf:resource = "http://xmlns.com/foaf/0.1/Person"/>
           <vitro:mostSpecificType rdf:resource="http://xmlns.com/foaf/0.1/Person"/>
           <rdfs:label rdf:datatype="http://www.w3.org/2001/XMLSchema#string"><xsl:value-of select = "$fullName" /></rdfs:label>
           <obo:ARG_2000028 rdf:resource="{$baseURI}vcard/pmid{ancestor::MedlineCitation/PMID}vcard{position()}"/>
		</rdf:Description>
		
		<!-- vcard -->
		<rdf:Description rdf:about="{$baseURI}vcard/pmid{ancestor::MedlineCitation/PMID}vcard{position()}">
		   <obo:ARG_2000029 rdf:resource="{$baseURI}author/pmid{ancestor::MedlineCitation/PMID}author{position()}"/>
           <vitro:mostSpecificType rdf:resource="http://www.w3.org/2006/vcard/ns#Individual"/>
           <rdf:type rdf:resource="http://www.w3.org/2006/vcard/ns#Individual"/>
           <rdf:type rdf:resource="http://purl.obolibrary.org/obo/ARG_2000379"/>
           <rdfs:label rdf:datatype="http://www.w3.org/2001/XMLSchema#string">vCard for: <xsl:value-of select = "$fullName" /></rdfs:label>
           <vcard:hasName rdf:resource="{$baseURI}vcard/pmid{ancestor::MedlineCitation/PMID}vcardName{position()}"/>
           <xsl:if test="normalize-space($authorEmail)" >
           <vcard:hasEmail rdf:resource="{$baseURI}vcard/pmid{ancestor::MedlineCitation/PMID}vcardEmail{position()}"/>
           </xsl:if>
		</rdf:Description>
		
		<!-- vcard name -->
		<rdf:Description rdf:about="{$baseURI}vcard/pmid{ancestor::MedlineCitation/PMID}vcardName{position()}">
		   <rdf:type rdf:resource="http://www.w3.org/2006/vcard/ns#Name"/> 
           <vitro:mostSpecificType rdf:resource="http://www.w3.org/2006/vcard/ns#Name"/>
           <rdfs:label rdf:datatype="http://www.w3.org/2001/XMLSchema#string">vCard name for: <xsl:value-of select = "$fullName" /></rdfs:label>
           <vcard:givenName rdf:datatype="http://www.w3.org/2001/XMLSchema#string"><xsl:value-of select = "$firstName" /></vcard:givenName>     
           <vcard:familyName rdf:datatype="http://www.w3.org/2001/XMLSchema#string"><xsl:value-of select = "$lastName" /></vcard:familyName>
		</rdf:Description>
		
		<!-- vcard email -->
		 <xsl:if test="normalize-space($authorEmail)" >
	     <rdf:Description rdf:about="{$baseURI}vcard/pmid{ancestor::MedlineCitation/PMID}vcardEmail{position()}">
	       <rdf:type rdf:resource="http://www.w3.org/2006/vcard/ns#Email"/>
	       <rdf:type rdf:resource="http://www.w3.org/2006/vcard/ns#Work"/>
	       <vitro:mostSpecificType rdf:resource="http://www.w3.org/2006/vcard/ns#Email"/>
	       <rdfs:label rdf:datatype="http://www.w3.org/2001/XMLSchema#string">vCard email for: <xsl:value-of select = "$fullName" /></rdfs:label>
	       <vcard:email rdf:datatype="http://www.w3.org/2001/XMLSchema#string"><xsl:value-of select = "$authorEmail" /></vcard:email>  
	     </rdf:Description>
	     </xsl:if>
			
			<!-- <rdf:type rdf:resource="http://vivoweb.org/harvester/excludeEntity" /> 
			<core:authorInAuthorship rdf:resource="{$baseURI}authorship/pmid{ancestor::BookDocument/PMID}authorship{position()}" /> -->
		 
	</xsl:template>
	
	<xsl:template match="MedlineCitation/MeshHeadingList/MeshHeading" mode="termAsKeyword">
		<xsl:choose>
			<xsl:when test="string(DescriptorName)">
				<core:freetextKeyword><xsl:value-of select="string(DescriptorName)" /></core:freetextKeyword>  
			</xsl:when>
		</xsl:choose>	
	</xsl:template>	
	
	<!-- Chemical List -->
	<xsl:template match="MedlineCitation/ChemicalList/Chemical">
		<xsl:if test="normalize-space(.)">
			<core:freetextKeyword><xsl:value-of select="." /></core:freetextKeyword>
		</xsl:if>
	</xsl:template>
	
	<!-- Article Keyword -->
	 
	
	<xsl:template match="MedlineCitation/KeywordList/Keyword">
	    <xsl:if test="normalize-space(.)" > 
		<core:freetextKeyword><xsl:value-of select="." /></core:freetextKeyword>
		</xsl:if>
	</xsl:template>
	
	<!-- Book Keyword -->
	<xsl:template match="BookDocument/KeywordList/Keyword">
		<xsl:if test="normalize-space(.)">
			<core:freetextKeyword><xsl:value-of select="." /></core:freetextKeyword>  
		</xsl:if>
	</xsl:template>
	
	<!-- Types -->
	<xsl:template match="MedlineCitation/Article/PublicationTypeList/PublicationType">
		<xsl:variable name="up" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ '"/>
		<xsl:variable name="lo" select="'abcdefghijklmnopqrstuvwxyz '"/>
		<xsl:variable name="pbType" select="string(self::PublicationType)" />
		<xsl:choose>
			<xsl:when test="translate(string($pbType),$up,$lo)='addresses'">
				<rdf:type rdf:resource="http://vivoweb.org/ontology/core#ConferencePaper" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='atlases'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/ReferenceSource" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='bibliography'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/AcademicArticle" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='biobibliography'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/AcademicArticle" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='biography'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Book" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='book reviews'">
				<rdf:type rdf:resource="http://vivoweb.org/ontology/core#Review" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='case reports'">
				<rdf:type rdf:resource="http://vivoweb.org/ontology/core#CaseStudy" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='charts'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Image" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='classical article'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/AcademicArticle" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='clinical conference'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/AcademicArticle" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='clinical trial'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/AcademicArticle" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='clinical trial, phase i'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/AcademicArticle" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='clinical trial, phase ii'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/AcademicArticle" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='clinical trial, phase iii'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/AcademicArticle" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='clinical trial, phase iv'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/AcademicArticle" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='collected correspondence'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Article" />
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Book" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='collected works'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Article" />
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Book" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='comment'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/AcademicArticle" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='comparative study'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/AcademicArticle" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='congresses'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Proceedings" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='consensus development conference'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Proceedings" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='consensus development conference, nih'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Proceedings" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='controlled clinical trial'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/AcademicArticle" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='cookbooks'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Book" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='corrected and republished article'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/AcademicArticle" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='database'">
				<rdf:type rdf:resource="http://vivoweb.org/ontology/core#Database" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='diaries'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Book" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='dictionary'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/ReferenceSource" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='directory'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/ReferenceSource" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='documentaries and factual films'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Film" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='drawings'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Image" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='duplicate publication'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Book" />
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Article" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='editorial'">
				<rdf:type rdf:resource="http://vivoweb.org/ontology/core#EditorialArticle" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='encyclopedias'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/ReferenceSource" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='essays'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Book" />
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Article" />
			</xsl:when>			
			<xsl:when test="translate(string($pbType),$up,$lo)='evaluation studies'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/AcademicArticle" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='festschrift'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Book" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='fictional work'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Book" />
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Article" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='formularies'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Book" />
			</xsl:when>
			<!-- Government Publications -->
			<xsl:when test="translate(string($pbType),$up,$lo)='guidebooks'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/ReferenceSource" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='guideline'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Standard" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='handbooks'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/ReferenceSource" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='historical article'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Article" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='incunabula'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Book" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='indexes'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/ReferenceSource" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='instruction'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/AudioVisualDocument" />
			</xsl:when>
			<!-- Interactive Tutorial -->
			<xsl:when test="translate(string($pbType),$up,$lo)='interview'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Book" />
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Article" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='introductory journal article'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Article" />
			</xsl:when>
			<xsl:when test="translate($pbType,$up,$lo)='journal article'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/AcademicArticle" />
			</xsl:when>
			<!-- Lectures -->
			<xsl:when test="translate(string($pbType),$up,$lo)='legal cases'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/LegalCaseDocument" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='legislation'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Legislation" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='maps'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Map" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='meeting abstracts'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/ConferenceProceedings" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='meta-analysis'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/AcademicArticle" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='multicenter study'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/AcademicArticle" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='news'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Article" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='newspaper article'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Article" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='patents'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Patent" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='patient education handout'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Document" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='periodical index'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/ReferenceSource" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='periodicals'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Periodicals" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='pharmacopoeias'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/ReferenceSource" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='pictorial works'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Book" />
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Article" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='portraits'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Image" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='practice guideline'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Standard" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='randomized controlled trial'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/AcademicArticle" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='review'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Book" />
				<rdf:type rdf:resource="http://vivoweb.org/ontology/core#Review" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='statistics'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Article" />
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Book" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='study characteristics'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Article" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='technical report'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Report" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='textbooks'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Book" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='twin study'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/AcademicArticle" />
			</xsl:when>
			<xsl:when test="translate(string($pbType),$up,$lo)='validation studies'">
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/AcademicArticle" />
			</xsl:when>
			<!--  Webcasts -->
			<xsl:otherwise>
				<rdf:type rdf:resource="http://purl.org/ontology/bibo/Document" />
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	 

</xsl:stylesheet>
