<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
                xmlns:vivo="http://vivoweb.org/ontology/core#"
                xmlns:score="http://vivoweb.org/ontology/score#"
                xmlns:foaf="http://xmlns.com/foaf/0.1/"
                xmlns:bibo="http://purl.org/ontology/bibo/"
                xmlns:obo="http://purl.obolibrary.org/obo/"
                xmlns:vitro="http://vitro.mannlib.cornell.edu/ns/vitro/0.7#"
                xmlns:vcard="http://www.w3.org/2006/vcard/ns#"
                xmlns:geo="http://aims.fao.org/aos/geopolitical.owl#"
                xmlns:mets="http://www.loc.gov/METS/"
                xmlns:mods="http://www.loc.gov/mods/v3"
                xmlns:xlink="http://www.w3.org/1999/xlink"
                xmlns:node-oai="http://vivo.example.com/harvest/fields/oai/"
                xmlns:localVivo="http://vivo.sample.edu/ontology/"
                xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
                xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns:core='http://vivoweb.org/ontology/core#'
                xmlns:stringhash="java:org.vivoweb.harvester.util.xslt.StringHash"
                extension-element-prefixes="stringhash">

    <!-- Import the main stylesheet -->
    <xsl:import href="../common/datamaps/oaifetch-dc.datamap.xsl"/>

    <!-- Type -->
    <xsl:template match="dc:type">
        <xsl:choose>
            <xsl:when test="ends-with(lower-case(.), 'article')">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Article" />
            </xsl:when>

            <xsl:when test="ends-with(lower-case(.), 'book')">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Book" />
            </xsl:when>

            <xsl:when test="ends-with(lower-case(.), 'chapter')">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/chapter" />
            </xsl:when>

            <xsl:when test="ends-with(lower-case(.), 'dataset')">
                <rdf:type rdf:resource="http://vivoweb.org/ontology/core#Dataset" />
            </xsl:when>

            <xsl:when test="ends-with(lower-case(.), 'presentation')">
                <rdf:type rdf:resource="http://vivoweb.org/ontology/core#Presentation" />
            </xsl:when>

            <xsl:when test="contains(lower-case(.), 'recording')">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/AudioDocument" />
            </xsl:when>

            <xsl:when test="contains(lower-case(.), 'software')">
                <rdf:type rdf:resource="http://purl.obolibrary.org/obo/ERO_0000071" />
            </xsl:when>

            <xsl:when test="contains(lower-case(.), 'thesis')">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Thesis" />
            </xsl:when>

            <xsl:when test="contains(lower-case(.), 'report')">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Report" />
            </xsl:when>

            <xsl:when test="contains(lower-case(.), 'video')">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/AudioVisualDocument" />
            </xsl:when>

            <xsl:when test="contains(lower-case(.), 'paper')">
                <rdf:type rdf:resource="http://vivoweb.org/ontology/core#WorkingPaper" />
            </xsl:when>

            <xsl:otherwise>
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Article" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>