<xsl:stylesheet version="2.0"
                xmlns:xsl = "http://www.w3.org/1999/XSL/Transform"
                xmlns:xs = "http://www.w3.org/2001/XMLSchema"
                xmlns:xsi = "http://www.w3.org/2001/XMLSchema-instance"
                xmlns:rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:rdfs = "http://www.w3.org/2000/01/rdf-schema#"
                xmlns:vivo = "http://vivoweb.org/ontology/core#"
                xmlns:score = "http://vivoweb.org/ontology/score#"
                xmlns:foaf = "http://xmlns.com/foaf/0.1/"
                xmlns:bibo = "http://purl.org/ontology/bibo/"
                xmlns:obo = "http://purl.obolibrary.org/obo/"
                xmlns:vitro = "http://vitro.mannlib.cornell.edu/ns/vitro/0.7#"
                xmlns:vcard = "http://www.w3.org/2006/vcard/ns#"
                xmlns:geo = "http://aims.fao.org/aos/geopolitical.owl#"
                xmlns:mets = "http://www.loc.gov/METS/"
                xmlns:mods="http://www.loc.gov/mods/v3"
                xmlns:xlink="http://www.w3.org/1999/xlink"
                xmlns:node-oai = "http://vivo.example.com/harvest/fields/oai/"
                xmlns:localVivo = "http://vivo.sample.edu/ontology/"
                xmlns:oai_dc = "http://www.openarchives.org/OAI/2.0/oai_dc/"
                xmlns:dc = "http://purl.org/dc/elements/1.1/"
                xmlns:core = 'http://vivoweb.org/ontology/core#'
                xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd http://purl.org/dc/terms/ http://dublincore.org/schemas/xmls/qdc/2008/02/11/dcterms.xsdhttp://www.loc.gov/METS/ http://www.loc.gov/standards/mets/mets.xsd http://www.loc.gov/mods/v3 http://www.loc.gov/standards/mods/v3/mods-3-3.xsd"
                xmlns:stringhash="java:org.vivoweb.harvester.util.xslt.StringHash"
                extension-element-prefixes = "stringhash"
>
    <xsl:output method="xml" indent="yes"/>

    <!-- Authorship keys -->
    <xsl:key name="authorships"
             match="dc:creator"
             use="concat('authorship_author/', translate(encode-for-uri(translate(normalize-space(.), ' ', '')), '%', ''), $documentId)" />

    <!-- Global variables -->
    <xsl:variable name="baseURI">http://vivo.example.com/harvest/</xsl:variable>
    <xsl:variable name="documentId" select="translate(encode-for-uri(translate(/oai_dc:dc/dc:identifier[1], ' ', '')), '%', '')" />
    <xsl:variable name="documentURI" select="concat($baseURI, 'oai/', translate(encode-for-uri(translate(/oai_dc:dc/dc:identifier[1], ' ', '')), '%', ''))" />

    <xsl:template match="/oai_dc:dc">
        <rdf:RDF>
            <rdf:Description rdf:about="{$documentURI}">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Document" />
                <rdf:type rdf:resource="http://www.w3.org/2002/07/owl#Thing" />
                <rdf:type rdf:resource="http://purl.obolibrary.org/obo/BFO_0000002" />
                <rdf:type rdf:resource="http://purl.obolibrary.org/obo/BFO_0000001" />
                <rdf:type rdf:resource="http://purl.obolibrary.org/obo/BFO_0000031" />
                <rdf:type rdf:resource="http://purl.obolibrary.org/obo/IAO_0000030" />


                <!-- Add vivo:relatedBy for all Authorships -->
                <xsl:for-each select="dc:creator">
                    <xsl:variable name="authorshipId" select="concat('authorship_author/', translate(encode-for-uri(translate(normalize-space(.), ' ', '')), '%', ''), $documentId)" />
                    <vivo:relatedBy rdf:resource="{$baseURI}{$authorshipId}" />
                </xsl:for-each>

                <xsl:apply-templates select="*[not(self::dc:creator or self::dc:contributor)]" />
            </rdf:Description>

            <!-- Process authors and contributors -->
            <xsl:apply-templates select="dc:creator" />
            <xsl:apply-templates select="dc:contributor" />
        </rdf:RDF>
    </xsl:template>

    <!-- Title -->
    <xsl:template match="dc:title">
        <rdfs:label><xsl:value-of select="normalize-space(.)"/></rdfs:label>
    </xsl:template>

    <!-- Subject -->
    <xsl:template match="dc:subject">
        <vivo:hasSubjectArea>
            <rdf:Description rdf:about="{$baseURI}contributor/{translate(encode-for-uri(translate(normalize-space(.), ' ', '')), '%', '')}">
                <rdf:type rdf:resource="http://www.w3.org/2004/02/skos/core#Concept" />
                <rdfs:label><xsl:value-of select="normalize-space(.)"/></rdfs:label>
            </rdf:Description>
        </vivo:hasSubjectArea>
    </xsl:template>

    <!-- Description -->
    <xsl:template match="dc:description">
        <bibo:abstract><xsl:value-of select="normalize-space(.)"/></bibo:abstract>
    </xsl:template>

    <!-- Date - Only first dc:date sibling -->
    <xsl:template match="dc:date[not(preceding-sibling::dc:date)]">
        <xsl:variable name="dateVal" select="normalize-space(.)"/>

        <vivo:dateTimeValue>
            <rdf:Description rdf:about="{$baseURI}oai/{$documentId}">
                <rdf:type rdf:resource="http://vivoweb.org/ontology/core#dateTimeValue"/>
                <vivo:dateTime rdf:datatype="http://www.w3.org/2001/XMLSchema#dateTime">
                    <xsl:choose>
                        <xsl:when test="string-length(translate(substring($dateVal,1,4),'0123456789',''))=0">
                            <xsl:value-of select="concat(substring($dateVal,1,4),'-01-01T00:00:00')"/>
                        </xsl:when>
                        <xsl:otherwise>0000-01-01T00:00:00</xsl:otherwise>
                    </xsl:choose>
                </vivo:dateTime>
                <vivo:dateTimePrecision rdf:resource="http://vivoweb.org/ontology/core#yearPrecision"/>
            </rdf:Description>
        </vivo:dateTimeValue>
    </xsl:template>

    <!-- Template for subsequent dc:date elements - do nothing -->
    <xsl:template match="dc:date[preceding-sibling::dc:date]">
        <!-- Skip all but first dc:date -->
    </xsl:template>

    <!-- Type -->
    <xsl:template match="dc:type">
        <xsl:param name="type-value" select="."/>
        <xsl:choose>
            <xsl:when test="$type-value = 'article' or $type-value = 'journal article'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/AcademicArticle" />
            </xsl:when>
            <xsl:otherwise>
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Article" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- Identifier -->
    <xsl:template match="dc:identifier">
        <bibo:uri><xsl:value-of select="normalize-space(.)"/></bibo:uri>
    </xsl:template>

    <!-- Publisher -->
    <xsl:template match="dc:publisher">
        <vivo:publisher>
            <rdf:Description rdf:about="{$baseURI}publisher/{translate(encode-for-uri(translate(normalize-space(.), ' ', '')), '%', '')}">
                <rdf:type rdf:resource="http://xmlns.com/foaf/0.1/Organization" />
                <rdfs:label><xsl:value-of select="normalize-space(.)"/></rdfs:label>

                <vivo:publisherOf rdf:resource="{$documentURI}"/>
            </rdf:Description>
        </vivo:publisher>
    </xsl:template>

    <!-- Language -->
    <xsl:template match="dc:language">
        <vivo:language><xsl:value-of select="normalize-space(.)"/></vivo:language>
    </xsl:template>

    <!-- Relation -->
    <xsl:template match="dc:relation">
        <vivo:relatedLink>
            <rdf:Description>
                <rdf:type rdf:resource="http://www.w3.org/2006/vcard/ns#URL" />
                <vcard:url><xsl:value-of select="normalize-space(.)"/></vcard:url>
            </rdf:Description>
        </vivo:relatedLink>
    </xsl:template>

    <!-- Rights -->
    <xsl:template match="dc:rights">
        <bibo:rights><xsl:value-of select="normalize-space(.)"/></bibo:rights>
    </xsl:template>

    <!-- Format -->
    <xsl:template match="dc:format">
        <vivo:format rdf:datatype="http://www.w3.org/2001/XMLSchema#string">
            <xsl:value-of select="normalize-space(.)"/>
        </vivo:format>
    </xsl:template>

    <!-- Source -->
    <xsl:template match="dc:source">
        <bibo:source><xsl:value-of select="normalize-space(.)"/></bibo:source>
    </xsl:template>

    <!-- Coverage -->
    <xsl:template match="dc:coverage">
        <vivo:coverage><xsl:value-of select="normalize-space(.)"/></vivo:coverage>
    </xsl:template>

    <!-- Creator -->
    <xsl:template match="dc:creator">
        <!-- Output Authorship and Person separately -->
        <xsl:variable name="authorId" select="concat('author/', translate(encode-for-uri(translate(normalize-space(.), ' ', '')), '%', ''))"/>
        <xsl:variable name="authorURI" select="concat($baseURI, $authorId)"/>
        <xsl:variable name="authorshipId" select="concat('authorship_', $authorId, $documentId)"/>
        <xsl:variable name="authorName" select="normalize-space(.)"/>

        <!-- Authorship -->
        <rdf:Description rdf:about="{$baseURI}{$authorshipId}">
            <rdf:type rdf:resource="http://vivoweb.org/ontology/core#Authorship" />
            <rdfs:label>Authorship for <xsl:value-of select="$authorName"/></rdfs:label>

            <core:relates rdf:resource="{$documentURI}"/>
            <core:relates rdf:resource="{$authorURI}" />

            <core:authorRank rdf:datatype="http://www.w3.org/2001/XMLSchema#int">
                <xsl:value-of select="position()" />
            </core:authorRank>
        </rdf:Description>

        <!-- Person -->
        <rdf:Description rdf:about="{$authorURI}">
            <rdf:type rdf:resource="http://xmlns.com/foaf/0.1/Person" />
            <rdfs:label><xsl:value-of select="$authorName"/></rdfs:label>

            <vivo:relatedBy rdf:resource="{$baseURI}{$authorshipId}" />
            <core:relates rdf:resource="{$documentURI}"/>
        </rdf:Description>

        <xsl:variable name="vcardURI" select="concat($baseURI, 'vcard_', $authorshipId)"/>
        <xsl:variable name="vcardNameURI" select="concat($baseURI, 'vcardName_', $authorshipId)"/>

        <!-- vcard -->
        <rdf:Description rdf:about="{$vcardURI}">
            <obo:ARG_2000029 rdf:resource="{$authorURI}"/>
            <rdf:type rdf:resource="http://www.w3.org/2006/vcard/ns#Individual"/>
            <rdfs:label rdf:datatype="http://www.w3.org/2001/XMLSchema#string">vCard for: <xsl:value-of select="$authorName" /></rdfs:label>
            <vcard:hasName rdf:resource="{$vcardNameURI}"/>
        </rdf:Description>

        <!-- vcard name -->
        <rdf:Description rdf:about="{$vcardNameURI}">
            <rdf:type rdf:resource="http://www.w3.org/2006/vcard/ns#Name"/>
            <rdfs:label rdf:datatype="http://www.w3.org/2001/XMLSchema#string">vCard name for: <xsl:value-of select="$authorName" /></rdfs:label>
            <vcard:givenName><xsl:value-of select = "$authorName" /></vcard:givenName>
        </rdf:Description>
    </xsl:template>

    <!-- Contributor -->
    <xsl:template match="dc:contributor">
        <xsl:variable name="contributorName" select="normalize-space(.)"/>
        <xsl:variable name="isPerson" select="contains($contributorName, ',')"/>

        <xsl:choose>
            <!-- If contributor is a person -->
            <xsl:when test="$isPerson">
                <xsl:variable name="contributorId" select="concat('author/', translate(encode-for-uri(translate($contributorName, ' ', '')), '%', ''))"/>
                <xsl:variable name="contributorURI" select="concat($baseURI, $contributorId)"/>
                <xsl:variable name="authorshipId" select="concat('authorship_', $contributorId, $documentId)"/>

                <!-- Authorship -->
                <rdf:Description rdf:about="{$baseURI}{$authorshipId}">
                    <rdf:type rdf:resource="http://vivoweb.org/ontology/core#Authorship" />
                    <core:relates rdf:resource="{$documentURI}" />
                    <core:relates rdf:resource="{$contributorURI}" />
                </rdf:Description>

                <!-- Person -->
                <rdf:Description rdf:about="{$contributorURI}">
                    <rdf:type rdf:resource="http://xmlns.com/foaf/0.1/Person" />
                    <rdfs:label><xsl:value-of select="$contributorName"/></rdfs:label>

                    <vivo:relatedBy rdf:resource="{$baseURI}{$authorshipId}" />
                    <core:relates rdf:resource="{$documentURI}"/>
                </rdf:Description>

                <rdf:Description rdf:about="{$documentURI}">
                    <vivo:relatedBy rdf:resource="{$contributorURI}" />
                </rdf:Description>
            </xsl:when>

            <!-- If contributor is a keyword or organization -->
            <xsl:otherwise>
<!--            Nothing for now-->
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
</xsl:stylesheet>
