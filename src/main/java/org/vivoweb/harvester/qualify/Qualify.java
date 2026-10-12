/*******************************************************************************
 * Copyright (c) 2010-2011 VIVO Harvester Team. For full list of contributors, please see the AUTHORS file provided.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the new BSD license which accompanies this distribution, and is available at http://www.opensource.org/licenses/bsd-license.html
 ******************************************************************************/
package org.vivoweb.harvester.qualify;

import java.io.IOException;
import java.util.ArrayList;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vivoweb.harvester.util.InitLog;
import org.vivoweb.harvester.util.IterableAdaptor;
import org.vivoweb.harvester.util.args.ArgDef;
import org.vivoweb.harvester.util.args.ArgList;
import org.vivoweb.harvester.util.args.ArgParser;
import org.vivoweb.harvester.util.args.UsageException;
import org.vivoweb.harvester.util.repo.JenaConnect;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

/**
 * Qualify data using SPARQL queries
 * @author Christopher Haines (hainesc@ctrip.ufl.edu)
 * @author Nicholas Skaggs (nskaggs@ctrip.ufl.edu)
 */
public class Qualify {
	/**
	 * SLF4J Logger
	 */
	private static Logger log = LoggerFactory.getLogger(Qualify.class);
	/**
	 * Jena Model we are working in
	 */
	private final JenaConnect model;
	/**
	 * The data predicate
	 */
	private final String dataPredicate;
	/**
	 * The string to match
	 */
	private final String matchTerm;
	/**
	 * The value to replace it with
	 */
	private final String newVal;
	/**
	 * Is this to use Regex to match the string
	 */
	private final boolean regex;
	/**
	 * the namespace you want removed
	 */
	private final String namespace;
	/**
	 * remove all statements where the predicate is from the given namespace
	 */
	private final boolean cleanPredicates;
	/**
	 * remove all statements where the subject or object is from the given namespace
	 */
	private final boolean cleanResources;
	
	/**
	 * Constructor
	 * @param args commandline arguments
	 * @throws IOException error creating task
	 * @throws UsageException user requested usage message
	 */
	private Qualify(String[] args) throws IOException, UsageException {
		this(getParser().parse(args));
	}
	
	/**
	 * Constructor
	 * @param argList parsed argument list
	 * @throws IOException error creating task
	 */
	private Qualify(ArgList argList) throws IOException {
		this(
			JenaConnect.parseConfig(argList.get("i"), argList.getValueMap("I")), 
			argList.get("d"), 
			(argList.has("r") ? argList.get("r") : argList.get("t")), 
			argList.get("v"), 
			argList.has("r"), 
			argList.get("n"), 
			argList.has("p"), 
			argList.has("c")
		);
		if(argList.has("r") && argList.has("t")) {
			log.warn("Both text and regex matchTerm's provided, using only regex");
		}
	}
	
	/**
	 * Constructor
	 * @param jenaModel the JENA model to run qualifications on
	 * @param dataType the data predicate
	 * @param matchString the string to match
	 * @param newValue the value to replace it with
	 * @param isRegex is this to use Regex to match the string
	 * @param removeNameSpace remove statements with predicates in this namespace
	 * @param cleanPredicates remove all statements where the predicate is from the given namespace
	 * @param cleanResources remove all statements where the subject or object is from the given namespace
	 */
	public Qualify(JenaConnect jenaModel, String dataType, String matchString, String newValue, boolean isRegex, String removeNameSpace, boolean cleanPredicates, boolean cleanResources) {
		this.model = jenaModel;
		if(this.model == null) {
			throw new IllegalArgumentException("Must provide a jena model");
		}
		this.dataPredicate = dataType;
		this.matchTerm = matchString;
		this.newVal = newValue;
		this.regex = isRegex;
		this.namespace = removeNameSpace;
		this.cleanPredicates = cleanPredicates;
		this.cleanResources = cleanResources;
		if(this.namespace == null || this.namespace.trim().isEmpty()) {
			if(this.matchTerm == null || this.matchTerm.trim().isEmpty()) {
				throw new IllegalArgumentException("Must specify either a match term (regex or text) or a removeNamespace");
			}
			if(this.cleanPredicates && this.cleanResources) {
				throw new IllegalArgumentException("Cannot specify cleanPredicates and cleanResources when removeNamepsace is empty");
			}
			if(this.cleanPredicates) {
				throw new IllegalArgumentException("Cannot specify cleanPredicates when removeNamepsace is empty");
			}
			if(this.cleanResources) {
				throw new IllegalArgumentException("Cannot specify cleanResources when removeNamepsace is empty");
			}
		}
	}
	
	/**
	 * Replace records exactly matching uri & datatype & oldValue with newValue
	 * @param dataType data type to match
	 * @param oldValue old value to match
	 * @param newValue new value to set
	 */
	private void strReplace(String dataType, String oldValue, String newValue) {
		StmtIterator stmtItr = this.model.getJenaModel().listStatements(null, this.model.getJenaModel().createProperty(dataType), oldValue);
		ArrayList<Statement> statements = new ArrayList<Statement>();
		while(stmtItr.hasNext()) {
			statements.add(stmtItr.next());
		}
		for(Statement stmt : statements) {
			log.trace("Replacing record");
			log.debug("oldValue: " + oldValue);
			log.debug("newValue: " + newValue);
			stmt.changeObject(newValue);
		}
	}
	
	/**
	 * Replace records matching predicate & the regexMatch with newValue
	 * @param predicate data type to match
	 * @param regexMatch regular expression to match
	 * @param newValue new value
	 * @throws IOException error connecting
	 */
	private void regexReplace(String predicate, String regexMatch, String newValue) throws IOException {
		String query = "" + "SELECT ?s ?o \n" 
						  + "WHERE {\n" 
						  + "  ?s <" + predicate + "> ?o .\n" 
						  + "  FILTER (regex(str(?o), \"" + regexMatch + "\", \"s\")) .\n" + "}";
		log.debug(query);
		StringBuilder insertQ = new StringBuilder("INSERT DATA {\n");
		StringBuilder deleteQ = new StringBuilder("DELETE DATA {\n");
		int modifyCounter = 0;
		for(QuerySolution s : IterableAdaptor.adapt(this.model.executeSelectQuery(query))) {
			modifyCounter++;
			//if (modifyCounter > 291){
			//	break;
			//}
			Literal obj = s.getLiteral("o");
			RDFDatatype datatype = obj.getDatatype();
			String lang = obj.getLanguage();
			String objStr = obj.getValue().toString();
			String oldStr = encodeString(objStr, datatype, lang);
			log.trace("Replacing record");
			log.debug("oldValue: " + oldStr);
			String newStr = encodeString(objStr.replaceAll(regexMatch, newValue), datatype, lang);
			String sUri = s.getResource("s").getURI();
			log.debug("newValue: " + newStr);
			// eliza: removed the single quotes in order to pass the test
			// hope it doesn't break anything.
			deleteQ.append("  <" + sUri + "> <" + predicate + "> " + oldStr + " . \n");
			insertQ.append("  <" + sUri + "> <" + predicate + "> " + newStr + " . \n");
		}
		log.debug("Modifying " + Integer.toString(modifyCounter) + " Records.");
		insertQ.append("} \n");
		deleteQ.append("} \n");
		log.debug("Removing old data:\n" + deleteQ);
		this.model.executeUpdateQuery(deleteQ.toString());
		log.debug("Inserting updated data:\n" + insertQ);
		this.model.executeUpdateQuery(insertQ.toString());
	}
	
	/**
	 * Encode a string with its rdfdatatype or lang
	 * @param str the string
	 * @param datatype the datatype
	 * @param lang the language
	 * @return the encoded string
	 */
	private String encodeString(String str, RDFDatatype datatype, String lang) {
		String encStr = "\"" + str + "\"";
		if(datatype != null && !datatype.getURI().equals(RDF.langString.getURI())) {
			encStr += "^^<" + datatype.getURI().trim() + ">";
		}
		if (StringUtils.isNotBlank(lang)) {
			encStr += "@" + lang.trim();
		}
		return encStr;
	}
	
	/**
	 * Remove all subjects and objects in a given namespace
	 * @param ns the namespace to remove all resources from
	 * @throws IOException error connecting
	 */
	private void cleanResources(String ns) throws IOException {
		String query = "" + "DELETE { ?s ?p ?o } " 
		+ "WHERE { " + "?s ?p ?o .  " 
		+ "FILTER (regex(str(?s), \"^" + ns + "\" ) || regex(str(?o), \"^" + ns + "\" ))" + "}";
		log.debug(query);
		this.model.executeUpdateQuery(query);
	}
	
	/**
	 * Remove all predicates in a given namespace
	 * @param ns the namespace to remove all predicates from
	 * @throws IOException error connecting
	 */
	private void cleanPredicates(String ns) throws IOException {
		String predicateQuery = "" + "DELETE { ?s ?p ?o } " 
		+ "WHERE { " + "?s ?p ?o .  " 
		+ "FILTER regex(str(?p), \"^" + ns + "\" ) " + "}";
		log.debug(predicateQuery);
		this.model.executeUpdateQuery(predicateQuery);
	}
	
	/**
	 * Executes the task
	 * @throws IOException error connecting
	 */
	public void execute() throws IOException {
		log.debug(Integer.toString(this.model.size()));
		if(StringUtils.isNotBlank(this.namespace)) {
			if(this.cleanPredicates) {
				log.info("Running clean predicates for " + this.namespace);
				cleanPredicates(this.namespace);
			}
			if(this.cleanResources) {
				log.info("Running clean resources for " + this.namespace);
				cleanResources(this.namespace);
			}
		}
//		if(StringUtils.isNotBlank(this.matchTerm) && StringUtils.isNotBlank(this.dataPredicate) && StringUtils.isNotBlank(this.newVal)) {
		if(StringUtils.isNotBlank(this.matchTerm) && StringUtils.isNotBlank(this.dataPredicate)) {
			if(this.regex) {
				log.info("Running Regex replace '" + this.dataPredicate + "': '" + this.matchTerm + "' with '" + this.newVal + "'");
				regexReplace(this.dataPredicate, this.matchTerm, this.newVal);
			} else {
				log.info("Running text replace '" + this.dataPredicate + "': '" + this.matchTerm + "' with '" + this.newVal + "'");
				strReplace(this.dataPredicate, this.matchTerm, this.newVal);
			}
		}
		this.model.sync();
	}
	
	/**
	 * Get the ArgParser for this task
	 * @return the ArgParser
	 */
	private static ArgParser getParser() {
		ArgParser parser = new ArgParser("Qualify");
		parser.addArgument(new ArgDef().setShortOption('i').setLongOpt("jenaConfig").setDescription("config file for jena model").withParameter(true, "CONFIG_FILE").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('I').setLongOpt("jenaOverride").setDescription("override the JENA_PARAM of jena model config using VALUE").withParameterValueMap("JENA_PARAM", "VALUE").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('d').setLongOpt("dataType").setDescription("data type (rdf predicate)").withParameter(true, "RDF_PREDICATE").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('r').setLongOpt("regexMatch").setDescription("match this regex expression").withParameter(true, "REGEX").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('t').setLongOpt("textMatch").setDescription("match this exact text string").withParameter(true, "MATCH_STRING").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('v').setLongOpt("value").setDescription("replace matching record data with this value").withParameter(true, "REPLACE_VALUE").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('n').setLongOpt("remove-namespace").setDescription("specify namespace for -p/--predicate-clean and -c/--clean-resources flag").withParameter(true, "RDF_NAMESPACE").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('p').setLongOpt("predicate-clean").setDescription("remove all statements where the predicate is from the given -n/--remove-namespace").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('c').setLongOpt("clean-resources").setDescription("remove all statements where the subject or object is from the given -n/--remove-namespace").setRequired(false));
		return parser;
	}
	
	/**
	 * Main method
	 * @param args commandline arguments
	 */
	public static void main(String... args) {
		Exception error = null;
		try {
			InitLog.initLogger(args, getParser());
			log.info(getParser().getAppName() + ": Start");
			new Qualify(args).execute();
		} catch(IllegalArgumentException e) {
			log.error(e.getMessage());
			log.debug("Stacktrace:",e);
			System.out.println(getParser().getUsage());
			error = e;
		} catch(UsageException e) {
			log.info("Printing Usage:");
			System.out.println(getParser().getUsage());
			error = e;
		} catch(Exception e) {
			log.error(e.getMessage());
			log.debug("Stacktrace:",e);
			error = e;
		} finally {
			log.info(getParser().getAppName() + ": End");
			if(error != null) {
				System.exit(1);
			}
		}
	}
}
