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
            <xsl:when test="lower-case(.) = 'article' or lower-case(.) = 'journal_article'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/AcademicArticle" />
            </xsl:when>

            <xsl:when test="lower-case(.) = 'book' or lower-case(.) = 'monograph'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Book" />
            </xsl:when>

            <xsl:when test="lower-case(.) = 'book_section' or lower-case(.) = 'book_chapter'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Chapter" />
            </xsl:when>

            <xsl:when test="lower-case(.) = 'conference_item' or lower-case(.) = 'conference_paper'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/ConferencePaper" />
            </xsl:when>

            <xsl:when test="lower-case(.) = 'conference_proceedings'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Proceedings" />
            </xsl:when>

            <xsl:when test="lower-case(.) = 'thesis' or lower-case(.) = 'doctoral_thesis' or lower-case(.) = 'phd_thesis'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Thesis" />
                <!-- Specific thesis type for PhD -->
                <xsl:if test="lower-case(.) = 'doctoral_thesis' or lower-case(.) = 'phd_thesis'">
                    <rdf:type rdf:resource="http://vivoweb.org/ontology/core#PhdThesis" />
                </xsl:if>
            </xsl:when>

            <xsl:when test="lower-case(.) = 'masters_thesis' or lower-case(.) = 'masters_dissertation'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Thesis" />
            </xsl:when>

            <xsl:when test="lower-case(.) = 'report' or lower-case(.) = 'technical_report'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Report" />
            </xsl:when>

            <xsl:when test="lower-case(.) = 'dataset' or lower-case(.) = 'research_data'">
                <rdf:type rdf:resource="http://vivoweb.org/ontology/core#Dataset" />
            </xsl:when>

            <xsl:when test="lower-case(.) = 'patent'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Patent" />
            </xsl:when>

            <xsl:when test="lower-case(.) = 'software'">
                <rdf:type rdf:resource="http://purl.obolibrary.org/obo/ERO_0000071" />
            </xsl:when>

            <xsl:when test="lower-case(.) = 'poster' or lower-case(.) = 'conference_poster'">
                <rdf:type rdf:resource="http://vivoweb.org/ontology/core#Poster" />
            </xsl:when>

            <xsl:when test="lower-case(.) = 'presentation' or lower-case(.) = 'slides'">
                <rdf:type rdf:resource="http://vivoweb.org/ontology/core#Presentation" />
            </xsl:when>

            <xsl:when test="lower-case(.) = 'working_paper' or lower-case(.) = 'discussion_paper'">
                <rdf:type rdf:resource="http://vivoweb.org/ontology/core#WorkingPaper" />
            </xsl:when>

            <xsl:when test="lower-case(.) = 'video' or lower-case(.) = 'audio_video'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/AudioVisualDocument" />
            </xsl:when>

            <xsl:when test="lower-case(.) = 'audio' or lower-case(.) = 'sound_recording'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/AudioDocument" />
            </xsl:when>

            <xsl:when test="lower-case(.) = 'image' or lower-case(.) = 'photograph'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Image" />
            </xsl:when>

            <xsl:when test="lower-case(.) = 'map' or lower-case(.) = 'cartographic_material'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Map" />
            </xsl:when>

            <xsl:when test="lower-case(.) = 'letter' or lower-case(.) = 'correspondence'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Letter" />
            </xsl:when>

            <xsl:when test="lower-case(.) = 'manual' or lower-case(.) = 'guide'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Manual" />
            </xsl:when>

            <xsl:when test="lower-case(.) = 'performance' or lower-case(.) = 'artistic_performance'">
                <rdf:type rdf:resource="http://vivoweb.org/ontology/core#Performance" />
            </xsl:when>

            <xsl:when test="lower-case(.) = 'exhibition'">
                <rdf:type rdf:resource="http://vivoweb.org/ontology/core#Exhibition" />
            </xsl:when>

            <xsl:when test="lower-case(.) = 'composition'">
                <rdf:type rdf:resource="http://vivoweb.org/ontology/core#Composition" />
            </xsl:when>

            <xsl:when test="lower-case(.) = 'editorial'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Article" />
                <vivo:editorialNote>Editorial</vivo:editorialNote>
            </xsl:when>

            <xsl:when test="lower-case(.) = 'review' or lower-case(.) = 'book_review'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Article" />
                <vivo:editorialNote>Review</vivo:editorialNote>
            </xsl:when>

            <xsl:when test="lower-case(.) = 'teaching_resource' or lower-case(.) = 'learning_object'">
                <rdf:type rdf:resource="http://vivoweb.org/ontology/core#TeachingResource" />
            </xsl:when>

            <!-- Fallback for any type that contains these keywords -->
            <xsl:when test="contains(lower-case(.), 'article')">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/AcademicArticle" />
            </xsl:when>

            <xsl:when test="contains(lower-case(.), 'thesis')">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Thesis" />
            </xsl:when>

            <xsl:when test="contains(lower-case(.), 'conference')">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/ConferencePaper" />
            </xsl:when>

            <xsl:otherwise>
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Article" />
                <vivo:editorialNote>Original type: <xsl:value-of select="."/></vivo:editorialNote>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>