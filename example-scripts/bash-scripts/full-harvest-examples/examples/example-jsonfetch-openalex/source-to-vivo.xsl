<!--
  Copyright (c) 2010-2011 VIVO Harvester Team. For full list of contributors, please see the AUTHORS file provided.
  All rights reserved.
  This program and the accompanying materials are made available under the terms of the new BSD license which accompanies this distribution, and is available at http://www.opensource.org/licenses/bsd-license.html
-->
<!-- Header information for the Style Sheet
	The style sheet requires xmlns for each prefix you use in constructing
	the new elements
-->

<xsl:stylesheet version = "2.0"
                xmlns:xsl = 'http://www.w3.org/1999/XSL/Transform'
                xmlns:rdf = 'http://www.w3.org/1999/02/22-rdf-syntax-ns#'
                xmlns:rdfs = 'http://www.w3.org/2000/01/rdf-schema#'
                xmlns:core = 'http://vivoweb.org/ontology/core#'
                xmlns:bibo = 'http://purl.org/ontology/bibo/'
                xmlns:obo = 'http://purl.obolibrary.org/obo/'
                xmlns:vitro = 'http://vitro.mannlib.cornell.edu/ns/vitro/0.7#'
                xmlns:vcard = 'http://www.w3.org/2006/vcard/ns#'
                xmlns:node-publication='http://vivo.example.com/harvest/aims_users/fields/publication/'
                xmlns:fn='http://www.w3.org/2005/xpath-functions'
                xmlns:vivo-oa='http://lod.tib.eu/onto/vivo-oa/'
                xmlns:c4o='http://purl.org/spar/c4o/' >

    <xsl:output method = "xml" indent = "yes"/>
    <xsl:variable name = "baseURI">http://vivo.school.edu/individual/</xsl:variable>

    <xsl:template match = "rdf:RDF">
        <rdf:RDF xmlns:rdf = 'http://www.w3.org/1999/02/22-rdf-syntax-ns#'
                 xmlns:rdfs = 'http://www.w3.org/2000/01/rdf-schema#'
                 xmlns:core = 'http://vivoweb.org/ontology/core#'
                 xmlns:bibo = 'http://purl.org/ontology/bibo/'>
            <xsl:apply-templates select = "rdf:Description" />
        </rdf:RDF>
    </xsl:template>

<!--    <xsl:template match = "rdf:Description">-->
<!--        <xsl:variable name = "this" select = "." />-->
<!--        <xsl:call-template name = "t_People">-->
<!--            <xsl:with-param name = "this" select = "$this" />-->
<!--            <xsl:with-param name = "personid" select = "lower-case($this/db-CSV:CN)" />-->
<!--        </xsl:call-template>-->
<!--    </xsl:template>-->

    <xsl:template match = "rdf:Description">
        <xsl:variable name = "this" select = "." />
        <xsl:variable name = "title" select="$this/node-publication:title" />
        <xsl:if test="$title != ''">
            <xsl:variable name = "doi" select = "$this/node-publication:doi" />
            <xsl:variable name = "oaid" select="substring-after($this/node-publication:id,'org/')" />
            <!--        <xsl:variable name = "publisher" select="$this/node-publication:host_venue/publisher"/>-->
            <xsl:variable name = "host_organization_name" select="$this/node-publication:primary_location/source/host_organization_name"/>

            <xsl:variable name="venue_id">
                <xsl:call-template name="setVenueId">
                    <xsl:with-param name="issn_0" select="$this/node-publication:primary_location/source/issn/issn_0"/>
                    <xsl:with-param name="oa_id" select="substring-after($this/node-publication:primary_location/source/id,'org/')"/>
                </xsl:call-template>
            </xsl:variable>

            <!--        <xsl:if test="$publisher !=''">-->
            <!--            <xsl:call-template name="t_Publisher">-->
            <!--                <xsl:with-param name="publisher" select="$publisher"/>-->
            <!--                <xsl:with-param name="host_venue" select="$this/node-publication:host_venue"/>-->
            <!--            </xsl:call-template>-->
            <!--        </xsl:if>-->

            <xsl:call-template name="t_PublicationVenue">
                <!--            <xsl:with-param name="host_venue" select="$this/node-publication:host_venue"/>  &lt;!&ndash; TODO: deprecated, use primary location instead &ndash;&gt;-->
                <xsl:with-param name="primary_location" select="$this/node-publication:primary_location"/>
                <!--            <xsl:with-param name="pub_type" select="$this/node-publication:type"/> &lt;!&ndash;  TODO korrigieren, use type of venue!&ndash;&gt;-->
                <!--            <xsl:with-param name="publisher" select="$publisher"/>-->
                <xsl:with-param name="venue_id" select="$venue_id"/>
            </xsl:call-template>

            <xsl:call-template name = "t_Publications">
                <xsl:with-param name = "this" select = "$this" />
                <xsl:with-param name = "doi" select = "$doi" />
                <xsl:with-param name = "oaid" select = "$oaid" />
                <xsl:with-param name = "venue_id" select="$venue_id"/>
            </xsl:call-template>
            <xsl:for-each select="$this/node-publication:authorships/*">
                <xsl:variable name="i" select="position()" />

                <xsl:variable name = "affiliation" select = "raw_affiliation_string"/>

                <xsl:variable name = "org_id">
                    <xsl:call-template name = "setOrgID">
                        <xsl:with-param name = "ror" select="substring-after(institutions/institution_0/ror,'org/')"/>
                        <xsl:with-param name = "openAlex" select="substring-after(institutions/institution_0/id,'org/')"/>
                    </xsl:call-template>
                </xsl:variable>

                <xsl:call-template name = "t_Author">
                    <xsl:with-param name = "author" select = "author" />
                    <xsl:with-param name = "org_id" select = "$org_id" />
                    <xsl:with-param name = "doi" select = "$doi" />
                    <xsl:with-param name = "oaid" select = "$oaid" />
                    <xsl:with-param name = "position" select="$i"/>
                </xsl:call-template>
                <xsl:call-template name="t_Institution">
                    <!--            if multiple institutions are possible the next line has to be edited to handle institutions e.g. in a for-each block-->
                    <xsl:with-param name = "institution" select = "institutions/institution_0" />
                    <xsl:with-param name = "org_id" select = "$org_id" />
                    <xsl:with-param name = "affiliation" select = "$affiliation" />
                </xsl:call-template>
            </xsl:for-each>
        </xsl:if>
    </xsl:template>

<!--    <xsl:template name="t_Publisher">-->
<!--        <xsl:param name = 'publisher' />-->
<!--        <xsl:param name = 'host_venue' />-->
<!--        <rdf:Description rdf:about="{$baseURI}{$publisher}">-->
<!--            <rdf:type rdf:resource="http://vivoweb.org/ontology/core#Publisher" />-->
<!--            <rdfs:label><xsl:value-of select="$publisher"/></rdfs:label>-->
<!--            <core:publisherOf rdf:resource="{$baseURI}{$host_venue/issn/issn_0}"/>-->
<!--        </rdf:Description>-->
<!--    </xsl:template>-->



    <xsl:template name="t_PublicationVenue">
        <xsl:param name = 'venue_id'/>
        <xsl:param name = "primary_location"/>

        <xsl:variable name="issn_0" select="$primary_location/source/issn/issn_0"/>

        <rdf:Description rdf:about="{$baseURI}{$venue_id}">
            <xsl:call-template name='getPublicationType'>
                <xsl:with-param name = "pbType" select = "$primary_location/source/type" />
            </xsl:call-template>

            <xsl:if test="$issn_0 != ''"><bibo:issn><xsl:value-of select="$issn_0"/></bibo:issn></xsl:if>
            <rdfs:label><xsl:value-of select="$primary_location/source/display_name"/></rdfs:label>

            <!--            <core:publisher rdf:resource="{$baseURI}{$publisher}"/>-->
        </rdf:Description>
    </xsl:template>

    <xsl:template name="t_Publications">
        <xsl:param name = 'this' />
        <xsl:param name = 'doi' />
        <xsl:param name = 'oaid' />
        <xsl:param name = 'venue_id'/>

        <xsl:variable name="title" select="$this/node-publication:title" />
        <xsl:variable name="publication_date" select="$this/node-publication:publication_date" />
        <xsl:variable name="type" select="$this/node-publication:type" />
        <xsl:variable name="type_crossref" select="$this/node-publication:type_crossref" />
<!--        <xsl:variable name="host_venue" select="$this/node-publication:host_venue" />-->
        <xsl:variable name="primary_location" select="$this/node-publication:primary_location"/>
        <xsl:variable name="volume" select="$this/node-publication:biblio/volume" />
        <xsl:variable name="issue" select="$this/node-publication:biblio/issue" />
        <xsl:variable name="pageStart" select="$this/node-publication:biblio/first_page" />
        <xsl:variable name="pageEnd" select="$this/node-publication:biblio/last_page" />
        <xsl:variable name="is_oa" select="$this/node-publication:open_access/is_oa" />
        <xsl:variable name="oa_status" select="$this/node-publication:open_access/oa_status" />
        <xsl:variable name="cited_by_count" select="$this/node-publication:cited_by_count" />
        <xsl:variable name="updated_date" select="$this/node-publication:updated_date" />


<!--        Tob be added later:-->
<!--        host_venue.issn-->
<!--        host_venue.display_name-->
<!--        host_venue.publisher-->

<!--        <xsl:variable name="url" select="$this/node-publication:"-->
<!--        <xsl:variable name="type" select="tibf:publication-type($this/node-publication:type)" />-->
<!--        <xsl:variable name="type" select="$this/node-publication:type" />-->
<!--        host_venue.type/-->
<!--        <xsl:variable name="author" select="$this/node-publication:author" />-->

        <!--    Creating a Publication   -->
        <rdf:Description rdf:about = "{$baseURI}{$oaid}">
            <xsl:choose>
                <xsl:when test="$type_crossref != ''">
                    <xsl:call-template name='getPublicationType'>
                        <xsl:with-param name = "pbType" select = "$type_crossref" />
                    </xsl:call-template>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:call-template name='getPublicationType'>
                        <xsl:with-param name = "pbType" select = "$type" />
                    </xsl:call-template>
                </xsl:otherwise>
            </xsl:choose>

            <obo:ARG_2000028 rdf:resource="{$baseURI}vc_{$oaid}"/>
            <rdfs:label><xsl:value-of select = "$title" /></rdfs:label> <!-- TODO add language ?-->

            <xsl:if test="$doi !=''">
                <bibo:doi><xsl:value-of select = "$doi" /></bibo:doi>
            </xsl:if>

            <core:dateTimeValue rdf:resource="{$baseURI}dtv_{$oaid}"/>

            <xsl:if test="$primary_location !=''">
                <core:hasPublicationVenue rdf:resource="{$baseURI}{$venue_id}"/>
            </xsl:if>
            <xsl:if test="$volume != ''">
                <bibo:volume><xsl:value-of select="$volume" /></bibo:volume>
            </xsl:if>
            <xsl:if test="$issue != ''">
                <bibo:issue><xsl:value-of select="$issue" /></bibo:issue>
            </xsl:if>
            <xsl:if test="$pageStart != ''">
                <bibo:pageStart><xsl:value-of select="$pageStart" /></bibo:pageStart>
            </xsl:if>
            <xsl:if test="$pageEnd != ''">
                <bibo:pageEnd><xsl:value-of select="$pageEnd" /></bibo:pageEnd>
            </xsl:if>
<!--            Open access ontology is needed to do display open access type and status-->
            <xsl:if test="$is_oa != ''">
                <xsl:choose>
                    <xsl:when test="$is_oa='true'">
                        <vivo-oa:has_access rdf:resource="http://lod.tib.eu/onto/vivo-oa/Open_Access"/>
                    </xsl:when>
                    <xsl:when test="$is_oa='false'">
                        <vivo-oa:has_access rdf:resource="http://lod.tib.eu/onto/vivo-oa/Non_Open_Access"/>
                    </xsl:when>
                </xsl:choose>
            </xsl:if>
            <xsl:if test="$oa_status != ''">
                <xsl:choose>
                    <xsl:when test="$oa_status='green'">
                        <vivo-oa:has_access rdf:resource="http://lod.tib.eu/onto/vivo-oa/Open_Access_Green"/>
                    </xsl:when>
                    <xsl:when test="$oa_status='gold'">
                        <vivo-oa:has_access rdf:resource="http://lod.tib.eu/onto/vivo-oa/Open_Access_Gold"/>
                    </xsl:when>
                </xsl:choose>
            </xsl:if>
            <xsl:if test="$cited_by_count != ''">
                <c4o:hasGlobalCitationFrequency rdf:resource="{$baseURI}gcf_{$oaid}"/>
            </xsl:if>

            <xsl:for-each select="$this/node-publication:authorships/*">
                <xsl:variable name="i" select="position()" />
                <core:relatedBy rdf:resource="{$baseURI}authorship_{$oaid}-{$i}"/>
            </xsl:for-each>

            <xsl:for-each select="$this/node-publication:concepts/*">
                <xsl:call-template name='getConcept'>
                    <xsl:with-param name = "concept" select = "id" />
                </xsl:call-template>
            </xsl:for-each>

        </rdf:Description>

        <!-- date time value -->
        <xsl:if test="normalize-space( $publication_date )">
            <rdf:Description rdf:about="{$baseURI}dtv_{$oaid}">
                <rdf:type rdf:resource="http://vivoweb.org/ontology/core#DateTimeValue"/>
                <core:dateTime rdf:datatype="http://www.w3.org/2001/XMLSchema#dateTime"><xsl:value-of select="$publication_date"/>T00:00:00</core:dateTime>
                <core:dateTimePrecision rdf:resource="http://vivoweb.org/ontology/core#yearMonthDayPrecision"/>
            </rdf:Description>
        </xsl:if>

        <!-- global citation count -->
        <xsl:if test="normalize-space( $cited_by_count )">
            <rdf:Description rdf:about="{$baseURI}gcf_{$oaid}">
                <rdf:type rdf:resource="http://purl.org/spar/c4o/GlobalCitationCount"/>
                <c4o:hasGlobalCountSource rdf:resource="https://forschungsatlas01.develop.service.tib.eu/individual/n4885"/>
                <c4o:hasGlobalCountDate rdf:datatype="http://www.w3.org/2001/XMLSchema#dateTime"><xsl:value-of select="substring-before($updated_date,'.')"/></c4o:hasGlobalCountDate>
                <c4o:hasGlobalCountValue><xsl:value-of select="$cited_by_count"/></c4o:hasGlobalCountValue>
            </rdf:Description>
        </xsl:if>

    </xsl:template>

    <!-- Create Institution -->
    <xsl:template name="t_Institution">
        <xsl:param name = 'institution'/>
        <xsl:param name = "org_id"/>
        <xsl:param name = "affiliation"/>
        <xsl:variable name="ror" select="$institution/ror"/>
        <xsl:variable name="label" select="$institution/display_name"/>

        <xsl:variable name = "org_label" select="substring-before($affiliation, ', ')"/>
        <xsl:variable name = "org_address" select="substring-after($affiliation, ', ')"/>
        <xsl:variable name = "org_street" select="substring-before($org_address, ', ')"/>
        <xsl:variable name = "org_cityCountry" select="substring-after($org_address, ', ')"/>
        <xsl:variable name = "org_city" select="substring-before($org_cityCountry, ', ')"/>
        <xsl:variable name = "org_country" select="substring-after($org_cityCountry, ', ')"/>

        <!-- Institution -->
        <rdf:Description rdf:about="{$baseURI}organization_{$org_id}">
            <rdf:type rdf:resource = "http://xmlns.com/foaf/0.1/Organization"/>
            <vitro:mostSpecificType rdf:resource="http://xmlns.com/foaf/0.1/Organization"/>
            <rdfs:label rdf:datatype="http://www.w3.org/2001/XMLSchema#string"><xsl:value-of select = "$label" /></rdfs:label>
            <rdf:type rdf:resource="http://purl.obolibrary.org/obo/ARG_2000028"/>
        </rdf:Description>

        <!-- vcard -->
<!--        <rdf:Description rdf:about="{$baseURI}vcard/vcard_{$org_id}">-->
<!--            <obo:ARG_2000029 rdf:resource="{$baseURI}organization/{$org_id}"/>-->
<!--            <vitro:mostSpecificType rdf:resource="http://www.w3.org/2006/vcard/ns#Individual"/>-->
<!--            <rdf:type rdf:resource="http://www.w3.org/2006/vcard/ns#Individual"/>-->
<!--            <rdf:type rdf:resource="http://purl.obolibrary.org/obo/ARG_2000379"/>-->
<!--            <rdf:type rdf:resource="http://purl.obolibrary.org/obo/BFO_0000001"/>-->
<!--            <rdf:type rdf:resource="http://purl.obolibrary.org/obo/BFO_0000002"/>-->
<!--            <rdf:type rdf:resource="http://purl.obolibrary.org/obo/BFO_0000031"/>-->
<!--            <rdf:type rdf:resource="http://purl.obolibrary.org/obo/IAO_0000030"/>-->
<!--            <vcard:hasAddress rdf:resource="{$baseURI}vcard/vcardAddr_{$org_id}"/>-->
<!--        </rdf:Description>-->

        <!-- vcard address -->
<!--        <rdf:Description rdf:about="{$baseURI}vcard/vcardAddr_{$org_id}">-->
<!--            <rdf:type rdf:resource="http://www.w3.org/2006/vcard/ns#Address"/>-->
<!--            <vitro:mostSpecificType rdf:resource="http://www.w3.org/2006/vcard/ns#Address"/>-->
<!--            <vcard:country><xsl:value-of select = "$org_country" /></vcard:country>-->
<!--            <vcard:locality><xsl:value-of select = "substring-before($org_city, ' ')" /></vcard:locality>-->
<!--            <vcard:postalCode><xsl:value-of select = "substring-after($org_city, ' ')" /></vcard:postalCode>-->
<!--            <vcard:streetAddress><xsl:value-of select = "$org_street" /></vcard:streetAddress>-->
<!--        </rdf:Description>-->
    </xsl:template>

    <!-- Create Author -->
    <xsl:template name="t_Author">
        <xsl:param name = 'author'/>
        <xsl:param name = 'doi'/>
        <xsl:param name = 'oaid'/>
        <xsl:param name = "org_id"/>
        <xsl:param name = "position"/>

        <xsl:variable name="fullName" select="$author/display_name"/>
        <xsl:variable name="id" select="substring-after($author/id,'org/')"/>
        <xsl:variable name="orcid" select="replace($author/orcid,' ','')"/>

        <xsl:variable name="firstName">
            <xsl:choose>
                <xsl:when test="fn:contains($fullName, ' ')">
                    <xsl:value-of select="substring-before($fullName,' ')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="''"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <xsl:variable name="lastName">
            <xsl:choose>
                <xsl:when test="fn:contains($fullName, ' ')">
                    <xsl:value-of select="substring-after($fullName,' ')"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$fullName"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

<!--        <xsl:variable name="fullName"><xsl:value-of select = "$lastName" />, <xsl:value-of select = "$firstName" /></xsl:variable>-->
<!--        <xsl:variable name="authorEmail" select=""/>-->


        <!-- authorship -->
        <rdf:Description rdf:about="{$baseURI}authorship_{$oaid}-{$position}">
            <rdf:type rdf:resource="http://vivoweb.org/ontology/core#Authorship" />
            <vitro:mostSpecificType rdf:resource="http://vivoweb.org/ontology/core#Authorship" />
            <rdf:type rdf:resource="http://vivoweb.org/ontology/core#Relationship" />
            <rdf:type rdf:resource="http://www.w3.org/2002/07/owl#Thing"/>
            <rdf:type rdf:resource="http://purl.obolibrary.org/obo/BFO_0000020" />
            <rdf:type rdf:resource="http://purl.obolibrary.org/obo/BFO_0000001" />
            <rdf:type rdf:resource="http://purl.obolibrary.org/obo/BFO_0000002" />
            <xsl:choose>
                <xsl:when test="$fullName">
                    <rdfs:label>Authorship for <xsl:value-of select = "$fullName" /></rdfs:label>
                </xsl:when>
                <xsl:when test="$lastName">
                    <rdfs:label>Authorship for <xsl:value-of select="$lastName" /></rdfs:label>
                </xsl:when>
            </xsl:choose>
            <core:relates rdf:resource="{$baseURI}author_{$id}"/>
            <core:relates rdf:resource="{$baseURI}{$oaid}"/>
            <core:authorRank rdf:datatype="http://www.w3.org/2001/XMLSchema#int"><xsl:value-of select="position()" /></core:authorRank>
        </rdf:Description>

        <!-- author -->
        <rdf:Description rdf:about="{$baseURI}author_{$id}">
            <rdf:type rdf:resource = "http://xmlns.com/foaf/0.1/Person"/>
            <vitro:mostSpecificType rdf:resource="http://xmlns.com/foaf/0.1/Person"/>
            <rdfs:label rdf:datatype="http://www.w3.org/2001/XMLSchema#string"><xsl:value-of select = "$lastName" />, <xsl:value-of select = "$firstName" /></rdfs:label>
            <obo:ARG_2000028 rdf:resource="{$baseURI}vcard_{$id}"/>
            <core:orcidId rdf:resource="{$orcid}"/>
            <core:relatedBy rdf:resource="{$baseURI}position/pos_{$id}"/>
        </rdf:Description>

        <!-- vcard -->
        <rdf:Description rdf:about="{$baseURI}vcard_{$id}">
            <obo:ARG_2000029 rdf:resource="{$baseURI}author_{$id}"/>
            <vitro:mostSpecificType rdf:resource="http://www.w3.org/2006/vcard/ns#Individual"/>
            <rdf:type rdf:resource="http://www.w3.org/2006/vcard/ns#Individual"/>
            <rdf:type rdf:resource="http://purl.obolibrary.org/obo/ARG_2000379"/>
            <rdfs:label rdf:datatype="http://www.w3.org/2001/XMLSchema#string">vCard for: <xsl:value-of select = "$fullName" /></rdfs:label>
            <vcard:hasName rdf:resource="{$baseURI}vcardName_{$id}"/>
<!--            <xsl:if test="normalize-space($authorEmail)" >-->
<!--                <vcard:hasEmail rdf:resource="{$baseURI}vcard/vcardEmail_{$id}"/>-->
<!--            </xsl:if>-->
        </rdf:Description>

        <!-- vcard name -->
        <rdf:Description rdf:about="{$baseURI}vcardName_{$id}">
            <rdf:type rdf:resource="http://www.w3.org/2006/vcard/ns#Name"/>
            <vitro:mostSpecificType rdf:resource="http://www.w3.org/2006/vcard/ns#Name"/>
            <rdfs:label rdf:datatype="http://www.w3.org/2001/XMLSchema#string">vCard name for: <xsl:value-of select = "$fullName" /></rdfs:label>
            <vcard:givenName><xsl:value-of select = "$firstName" /></vcard:givenName>
            <vcard:familyName><xsl:value-of select = "$lastName" /></vcard:familyName>
        </rdf:Description>

        <!-- Position-->
        <rdf:Description rdf:about="{$baseURI}position/pos_{$id}">
            <rdf:type rdf:resource="http://vivoweb.org/ontology/core#Position"/>
            <core:relates rdf:resource="{$baseURI}organization_{$org_id}"/>
        </rdf:Description>


        <!-- vcard email -->
<!--        <xsl:if test="normalize-space($authorEmail)" >-->
<!--            <rdf:Description rdf:about="{$baseURI}vcard/vcardEmail_{$id}">-->
<!--                <rdf:type rdf:resource="http://www.w3.org/2006/vcard/ns#Email"/>-->
<!--                <rdf:type rdf:resource="http://www.w3.org/2006/vcard/ns#Work"/>-->
<!--                <vitro:mostSpecificType rdf:resource="http://www.w3.org/2006/vcard/ns#Email"/>-->
<!--                <rdfs:label rdf:datatype="http://www.w3.org/2001/XMLSchema#string">vCard email for: <xsl:value-of select = "$fullName" /></rdfs:label>-->
<!--                <vcard:email rdf:datatype="http://www.w3.org/2001/XMLSchema#string"><xsl:value-of select = "$authorEmail" /></vcard:email>-->
<!--            </rdf:Description>-->
<!--        </xsl:if>-->
    </xsl:template>

    <xsl:template name="setOrgID">
        <xsl:param name = 'ror'/>
        <xsl:param name = 'openAlex'/>
        <xsl:choose>
            <xsl:when test="$ror != ''">
                <xsl:value-of select="$ror"/>
            </xsl:when>
            <xsl:when test="$openAlex != ''">
                <xsl:value-of select="$openAlex"/>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="setVenueId">
        <xsl:param name="issn_0"/>
        <xsl:param name="oa_id"/>
        <xsl:choose>
            <xsl:when test="$issn_0 != ''">
                <xsl:value-of select="$issn_0"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$oa_id"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="getPublicationType">
        <xsl:param name = 'pbType'>journal-article</xsl:param>
        <xsl:variable name="up" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZ '"/>
        <xsl:variable name="lo" select="'abcdefghijklmnopqrstuvwxyz '"/>
        <xsl:choose>
            <xsl:when test="translate(string($pbType),$up,$lo)='article'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/AcademicArticle" />
            </xsl:when>
            <xsl:when test="translate(string($pbType),$up,$lo)='book-section'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/BookSection" />
            </xsl:when>
            <xsl:when test="translate(string($pbType),$up,$lo)='monograph'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Book" />
            </xsl:when>
            <xsl:when test="translate(string($pbType),$up,$lo)='report-component'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/DocumentPart" />
            </xsl:when>
            <xsl:when test="translate(string($pbType),$up,$lo)='report'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Report" />
            </xsl:when>
            <xsl:when test="translate(string($pbType),$up,$lo)='journal-article'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/AcademicArticle" />
            </xsl:when>
            <xsl:when test="translate(string($pbType),$up,$lo)='book-part'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/BookSection" />
            </xsl:when>
            <xsl:when test="translate(string($pbType),$up,$lo)='book'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Book" />
            </xsl:when>
            <xsl:when test="translate(string($pbType),$up,$lo)='book-set'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/AcademicArticle" />
            </xsl:when>
            <xsl:when test="translate(string($pbType),$up,$lo)='reference-entry'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/ReferenceSource" />
            </xsl:when>
            <xsl:when test="translate(string($pbType),$up,$lo)='proceedings-article'">
                <rdf:type rdf:resource="http://vivoweb.org/ontology/core#ConferencePaper" />
            </xsl:when>
            <xsl:when test="translate(string($pbType),$up,$lo)='journal'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Journal" />
            </xsl:when>
            <xsl:when test="translate(string($pbType),$up,$lo)='component'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/DocumentPart" />
            </xsl:when>
            <xsl:when test="translate(string($pbType),$up,$lo)='book-chapter'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Chapter" />
            </xsl:when>
            <xsl:when test="translate(string($pbType),$up,$lo)='proceedings-series'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Series" />
            </xsl:when>
            <xsl:when test="translate(string($pbType),$up,$lo)='report-series'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Series" />
            </xsl:when>
            <xsl:when test="translate(string($pbType),$up,$lo)='proceedings'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Proceedings" />
            </xsl:when>
            <xsl:when test="translate(string($pbType),$up,$lo)='database'">
                <rdf:type rdf:resource="http://vivoweb.org/ontology/core#Database" />
            </xsl:when>
            <xsl:when test="translate(string($pbType),$up,$lo)='standard'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Standard" />
            </xsl:when>
            <xsl:when test="translate(string($pbType),$up,$lo)='reference-book'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/ReferenceSource" />
            </xsl:when>
            <xsl:when test="translate(string($pbType),$up,$lo)='posted-content'">
                <rdf:type rdf:resource="http://vivoweb.org/ontology/core#BlogPosting" />
            </xsl:when>
            <xsl:when test="translate(string($pbType),$up,$lo)='journal-issue'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Issue" />
            </xsl:when>
            <xsl:when test="translate(string($pbType),$up,$lo)='dissertation'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Thesis" />
            </xsl:when>
            <xsl:when test="translate(string($pbType),$up,$lo)='dataset'">
                <rdf:type rdf:resource="http://vivoweb.org/ontology/core#Dataset" />
            </xsl:when>
            <xsl:when test="translate(string($pbType),$up,$lo)='book-series'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Series" />
            </xsl:when>
            <xsl:when test="translate(string($pbType),$up,$lo)='edited-book'">
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/EditedBook" />
            </xsl:when>
            <xsl:otherwise>
                <rdf:type rdf:resource="http://purl.org/ontology/bibo/Document" />
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template name="getConcept">
        <xsl:param name = 'concept'></xsl:param>
        <xsl:choose>
            <xsl:when test="$concept='https://openalex.org/C88230418'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21306714546_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C154611951'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11365612424_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C50128577'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11806796543_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C107054158'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21976259210_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C127454912'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21108351632_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2778348673'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R22049467594_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C19159745'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21657927165_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2777394807'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21126014518_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C180198813'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21181970389_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2777877159'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20257497633_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2994410616'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20789680115_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C148803439'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21401764070_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C34447519'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11643535431_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C559116025'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20383486008_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2781353297'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10656440916_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C3020690005'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21772601359_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C86432685'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10249999444_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C110872660'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11357426155_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C1631582'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10339148687_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C71864017'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10524615458_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2994266286'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11227365538_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C190831278'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20339388564_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2779184092'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11434895487_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C52438962'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11080617381_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C547646559'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R12091564190_3"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2776175706'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R22119722052_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C82576440'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11521343997_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C153294291'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10469539698_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C3019616415'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11675558039_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2993877802'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11212646931_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C502990516'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R22074562421_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C71827079'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10329727946_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C120441037'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10067875322_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C4311470'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21596097020_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2991968081'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11109695162_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2779755355'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20456162297_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2988954192'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10035959677_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C54750564'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R12123719627_3"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2778852317'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R12107779314_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C199733313'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11482114250_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C117455697'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21119000368_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2779635184'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21466573540_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C1631582'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20342424453_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C158071213'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21400611376_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C41458344'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20892531185_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2776154427'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10936780837_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2776542561'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20597362058_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C127313418'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21666958044_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2994544150'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10090838520_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C95831776'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20848412882_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C16678853'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11028188780_2"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C204983608'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10918418790_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C109948328'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10154550125_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2776510970'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20246528336_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2778719706'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20397061674_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C56095865'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21066445109_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C104002121'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20525772596_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C152877465'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11780355670_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C127413603'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20719591921_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C32235935'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21260135128_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2778449271'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10019664563_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C143910263'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21208498100_3"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2777351106'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21710102699_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2779201158'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10257174043_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2985695025'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20272834845_3"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C82753439'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11520175789_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C518851703'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10071689810_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C71376074'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11810502432_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C49204034'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20044969474_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C138885662'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11068098425_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C199539241'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10077851633_1"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C1631582'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11052106899_2"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2994464924'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21838445461_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C138816342'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11363764110_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2776825979'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21734220057_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2993443034'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11599961734_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C41008148'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10548596211_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C110872660'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21378899234_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C4792198'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10585848086_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C131046424'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11803293762_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C48233174'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R12088228029_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2777413173'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10725821060_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C84265765'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11981733736_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2988676352'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10292232665_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C86432685'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10168537769_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2992697980'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11826973867_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C44154836'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20806677380_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2779732396'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10785024235_3"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C32763512'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21562900649_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C205649164'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10013526254_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C52622258'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21893620258_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C149782125'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11427733730_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2778053677'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10082474865_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C501299471'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11550165632_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C123657996'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21144849761_2"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2780958017'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10633584452_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C91375879'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10453205068_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C19417346'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R12082792165_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C178895491'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21539000171_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C143128703'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20175620728_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C37129596'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10253523053_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2781374353'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21604189794_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C71043370'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R12089325758_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C190831278'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10728681156_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C41045048'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R22069181738_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C165786947'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11121116874_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C28901747'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R22091461619_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C542628504'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10260978363_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2781440851'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10750151305_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2777917351'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R22140044218_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C148383697'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11386092678_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C206836424'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11194673298_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2779134260'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21375822636_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2776949292'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21574793358_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C130064352'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R22014133992_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C61980418'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R12095269328_3"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C36289849'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21129818147_2"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C95389739'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11828185802_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C88463610'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20732926524_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C144024400'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10450981137_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C83864248'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10832030476_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2992537118'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21099737297_2"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2993267355'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21025923750_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C537397655'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21254671490_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C126255220'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20118146428_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2993624421'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10606149036_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C528095902'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10447875765_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C75586009'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R22082460470_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2777364373'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21275210525_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2781283787'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21993371835_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C26271046'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21981049546_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C115901376'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10778940072_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2780396716'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21211107111_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2991746682'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20750891397_2"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2779576378'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11317182150_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C97053079'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20869915834_3"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2984309096'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20091249387_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2986442093'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20954911058_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C149923435'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20203351007_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C526734887'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10369505798_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C52119013'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10328195327_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2549261'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21174281825_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2994455448'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21830386323_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2983406839'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11949182252_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C3018369621'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10000002839_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2778348119'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10460242295_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C555826173'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20262631282_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C88862950'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R12142364286_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C541104983'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20393980707_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C112149622'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11959378370_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C39853841'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11417381790_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C116681429'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11870070600_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2781259832'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21536870954_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2779201158'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20344259458_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2777538416'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11900975607_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2988166257'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20671496642_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2777973936'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11488656433_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C189326681'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10062549072_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2994058677'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21790729232_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C15744967'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20488350687_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C74363100'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10761175854_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2781137002'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11207426288_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C531593650'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21759215799_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2992325636'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20441139478_2"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C93692415'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10930345245_3"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2780060422'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20583726163_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C8625798'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10154313028_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2994493120'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11940865504_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C105639569'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11570030183_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2985879086'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21383563223_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2987985974'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11685037446_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C39549134'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10676251851_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2908647359'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10657727406_2"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C33923547'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21875582790_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C118416809'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10874191971_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C110269972'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11892292587_3"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2780328347'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11073274478_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C67141207'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10725922667_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C12404463'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R22103737565_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C74294582'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20817216607_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C206139338'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10415405816_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C202565627'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10183544763_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2778647717'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21363350966_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C116822448'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10875042401_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2994309752'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10617488519_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C137403100'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R12134883531_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2777347956'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20828641775_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C94625758'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10319999407_2"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C18574340'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11135834099_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2994141135'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20513125884_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C53571324'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11277232714_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C87427459'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11898244452_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C56666940'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21082027241_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C104151175'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20244670025_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C62649853'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21143733125_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2992770910'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10734123785_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2985380958'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20513125884_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C176864760'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10973112113_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2986427349'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R22067143042_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2780155792'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10937505789_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2992184231'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20620453615_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C49271019'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21536796218_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C23213687'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11213249961_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C177986884'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11039262486_2"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C10879293'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10409620888_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C18650270'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10495469870_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C154193497'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10958563611_2"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C48327123'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10842060339_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C180872759'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10752527142_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C82685317'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10541388936_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C548895740'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R12053244692_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2778302417'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10133405151_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C121017731'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11878926871_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C166957645'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11966162496_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2780944931'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11022806199_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2988016980'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10249970275_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2781463953'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10009542917_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2778348171'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R12040936856_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C160934017'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21827393480_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C7879346'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R12074619444_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C165895018'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21598343907_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C70160891'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20626304665_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C33613203'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21331772638_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C16160715'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21898606269_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C58640448'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11058379816_3"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C139019045'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11948627224_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2777171531'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11618414691_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2779614062'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10075885427_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2982944804'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11288635562_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2986045029'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10959220951_1"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C76155785'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20809473087_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2778451013'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11511431464_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C146278227'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10002602355_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C129047720'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10299871047_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2775999810'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11434341667_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C204441458'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10000069132_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C78600449'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20764515782_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2780816530'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10533403994_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2778324724'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11585251942_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C141121606'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10903273502_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2780510313'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21250083827_2"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C99476002'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10198479869_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C53657456'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10002581604_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C44493715'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11288707643_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C3017489713'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10369569520_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C71116409'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11011596617_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C82279013'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10198817614_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C199539241'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R12098934955_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C61661205'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21642257976_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C71924100'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11658781751_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2780631588'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11887264842_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C70036468'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11342808204_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C61441594'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10211798536_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C17744445'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20003482335_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C125471540'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21510726611_2"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2994183059'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11070354222_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C104988666'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R12075906186_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C118518473'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10026568203_3"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2984157484'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10499022259_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2988151755'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10257378828_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C19159745'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11156680953_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2777967926'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11279303216_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C46312422'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20037231829_3"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2781469121'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20488799202_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C177002080'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10899115465_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C136264566'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R12140734944_1"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2776190866'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R22107424836_3"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C508466165'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R22111226377_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2776081408'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21198480759_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C22787982'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20230711381_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C542127796'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10531184497_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C87690585'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20015695286_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C487182'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20345584141_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C97137747'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R22069584342_3"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C105795698'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10705334189_2"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2992918391'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10214691016_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2986817661'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20198006041_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C206658404'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20449714594_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C524878704'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11752249521_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C50522688'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10942292645_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C184235594'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21682841947_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C86627976'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20518217068_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C57442070'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21580520479_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C77805123'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R12103338603_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C205300905'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20402156503_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C505870484'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20956479909_3"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C123157820'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20763660948_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C122098685'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20751996447_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C160934017'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21827375008_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C18918823'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20996857533_2"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C129216166'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21395972139_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2780427980'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11848861228_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2780165032'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10417431924_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2778206487'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20289324773_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C90195498'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11867594779_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C193235544'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21035536571_2"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C994546'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21349340901_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2776554220'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20655861297_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C3020591692'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20052332735_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C533735693'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20844149770_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C518936366'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11814777244_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2775835988'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11874456019_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C18903297'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20882538437_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2779977313'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10971599009_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2778873432'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10998786636_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2779610281'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R22025070831_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C59427239'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R12053236887_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2777364373'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10387651247_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C158049464'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20617358207_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C514928085'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R21211660442_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C73495956'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11057543064_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C122284674'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10002372986_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2989271921'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10549101185_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C4988496'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10067168537_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2984717066'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20902199568_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2992625000'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20242136859_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C2985722716'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11819156093_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C27564746'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R20647932179_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C73555534'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R10470148014_4"/>
            </xsl:when>
            <xsl:when test="$concept='https://openalex.org/C190048596'">
                <core:hasSubjectArea rdf:resource="https://terminology.fraunhofer.de/voc/raum#R11510714254_4"/>
            </xsl:when>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>
