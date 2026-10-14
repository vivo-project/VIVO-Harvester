/*******************************************************************************
 * Copyright (c) 2010-2011 VIVO Harvester Team. For full list of contributors, please see the AUTHORS file provided.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the new BSD license which accompanies this distribution, and is available at http://www.opensource.org/licenses/bsd-license.html
 ******************************************************************************/
package org.vivoweb.harvester.util.repo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.jena.graph.GraphEvents;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.RDFWriter;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.Lock;
import org.apache.jena.sparql.resultset.ResultsFormat;
import org.apache.jena.update.UpdateAction;
import org.apache.jena.update.UpdateFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vivoweb.harvester.util.FileAide;
import org.vivoweb.harvester.util.InitLog;
import org.vivoweb.harvester.util.args.ArgDef;
import org.vivoweb.harvester.util.args.ArgList;
import org.vivoweb.harvester.util.args.ArgParser;
import org.vivoweb.harvester.util.args.UsageException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Connection Helper for Jena Models
 * @author Christopher Haines (hainesc@ctrip.ufl.edu)
 */
public abstract class JenaConnect {
	/**
	 * SLF4J Logger
	 */
	private static Logger log = LoggerFactory.getLogger(JenaConnect.class);
	/**
	 * Model we are connecting to
	 */
	private Model jenaModel;
	/**
	 * The modelname
	 */
	private String modelName;
	
	/**
	 * Factory (connects to the same jena triple store as another jena connect, but uses a different named model)
	 * @param newModelName the model name to use
	 * @return the new jenaconnect
	 * @throws IOException unable to secure db connection
	 */
	public abstract JenaConnect neighborConnectClone(String newModelName) throws IOException;
	
	/**
	 * Config Stream Based Factory that overrides parameters
	 * @param configStream the config input stream
	 * @param overrideParams the parameters to override the file with
	 * @return JenaConnect instance
	 * @throws IOException error connecting
	 */
	public static JenaConnect parseConfig(InputStream configStream, Map<String, String> overrideParams) throws IOException {
		Map<String, String> paramList = new JenaConnectConfigParser().parseConfig(configStream);
		if(overrideParams != null) {
			for(String key : overrideParams.keySet()) {
				paramList.put(key, overrideParams.get(key));
			}
		}
		for(String param : paramList.keySet()) {
			if(!param.equalsIgnoreCase("dbUser") && !param.equalsIgnoreCase("dbPass")) {
				log.trace("'" + param + "' - '" + paramList.get(param) + "'");
			}
		}
		return build(paramList);
	}
	
	/**
	 * Config File Based Factory
	 * @param configFileName the config file path
	 * @return JenaConnect instance
	 * @throws IOException xml parse error
	 */
	public static JenaConnect parseConfig(String configFileName) throws IOException {
		return parseConfig(configFileName, null);
	}
	
	/**
	 * Config File Based Factory
	 * @param configFileName the config file path
	 * @param overrideParams the parameters to override the file with
	 * @return JenaConnect instance
	 * @throws IOException xml parse error
	 */
	public static JenaConnect parseConfig(String configFileName, Map<String, String> overrideParams) throws IOException {
		InputStream confStream = (configFileName == null) ? null : FileAide.getInputStream(configFileName);
		return parseConfig(confStream, overrideParams);
	}
	
	/**
	 * Build a JenaConnect based on the given parameter set
	 * @param params the value map
	 * @return the JenaConnect
	 * @throws IOException error connecting to jena model
	 */
	private static JenaConnect build(Map<String, String> params) throws IOException {
		// for(String param : params.keySet()) {
		// log.debug(param+" => "+params.get(param));
		// }
		if((params == null) || params.isEmpty()) {
			return null;
		}
		if(!params.containsKey("type")) {
			throw new IllegalArgumentException("Must specify 'type' parameter {'rdb','sdb','tdb','file','mem'}");
		}
		String type = params.get("type");
		JenaConnect jc;
		if(type.equalsIgnoreCase("mem")) {
			jc = new MemJenaConnect(params.get("modelName"));
		} else if(type.equalsIgnoreCase("sdb")) {
			jc = new SDBJenaConnect(params.get("dbUrl"), params.get("dbUser"), params.get("dbPass"), params.get("dbType"), params.get("dbClass"), params.get("dbLayout"), params.get("modelName"));
		} else if(type.equalsIgnoreCase("tdb")) {
			jc = new TDBJenaConnect(params.get("dbDir"), params.get("modelName"));			 
		} else if(type.equalsIgnoreCase("file")) {
			jc = new FileJenaConnect(params.get("file"), params.get("rdfLang"));
		} else {
			throw new IllegalArgumentException("unknown type: " + type);
		}
		if((params.containsKey("checkEmpty") && (params.get("checkEmpty").toLowerCase() == "true")) && jc.isEmpty()) {
			StringBuilder emptyWarn = new StringBuilder("jena model empty! ");
			emptyWarn.append(type);
			emptyWarn.append(": ");
			if(!type.equalsIgnoreCase("mem")) {
				if(type.equalsIgnoreCase("tdb")) {
					emptyWarn.append("dbDir: ");
					emptyWarn.append(params.get("dbDir"));
					emptyWarn.append(" ");
				} else {
					emptyWarn.append("dbDir: ");
					emptyWarn.append(params.get("dbDir"));
					emptyWarn.append(" ");
				}
			}
			emptyWarn.append("modelName: ");
			emptyWarn.append(jc.getModelName());
			JenaConnect.log.warn(emptyWarn.toString());
		}
		return jc;
	}
	
	/**
	 * Get the size of a jena model
	 * @return the number of statement in this model
	 * @throws IOException error connecting
	 */
	public int size() throws IOException {
		ResultSet resultSet = executeSelectQuery("SELECT (count(?s) as ?size) WHERE { ?s ?p ?o }");
		// read first result
		if(resultSet.hasNext()) {
			//Display count
			return resultSet.next().get("size").asLiteral().getInt();
		}
		return 0;
	}
	
	/**
	 * Get the dataset for this connection Can be very expensive when using RDB connections (SDB, TDB, and Mem are fine)
	 * @return the database connection's dataset
	 * @throws IOException error connecting
	 */
	public abstract Dataset getDataset() throws IOException;
	
	/**
	 * Load in RDF
	 * @param in input stream to read rdf from
	 * @param namespace the base uri to use for imported uris
	 * @param language the language the rdf is in. Predefined values for lang are "RDF/XML", "N-TRIPLE", "TURTLE" (or
	 *        "TTL") and "N3". null represents the default language, "RDF/XML". "RDF/XML-ABBREV" is a synonym for
	 *        "RDF/XML"
	 */
	public void loadRdfFromStream(InputStream in, String namespace, String language) {
		getJenaModel().read(in, namespace, language);
	}
	
	/**
	 * Load the RDF from a file
	 * @param fileName the file to read from
	 * @param namespace the base uri to use for imported uris
	 * @param language the language the rdf is in. Predefined values for lang are "RDF/XML", "N-TRIPLE", "TURTLE" (or
	 *        "TTL") and "N3". null represents the default language, "RDF/XML". "RDF/XML-ABBREV" is a synonym for
	 *        "RDF/XML"
	 * @throws IOException error accessing file
	 */
	public void loadRdfFromFile(String fileName, String namespace, String language) throws IOException {
		loadRdfFromStream(FileAide.getInputStream(fileName), namespace, language);
	}
	
	/**
	 * Load in RDF
	 * @param rdf rdf string
	 * @param namespace the base uri to use for imported uris
	 * @param language the language the rdf is in. Predefined values for lang are "RDF/XML", "N-TRIPLE", "TURTLE" (or
	 *        "TTL") and "N3". null represents the default language, "RDF/XML". "RDF/XML-ABBREV" is a synonym for
	 *        "RDF/XML"
	 */
	public void loadRdfFromString(String rdf, String namespace, String language) {
		loadRdfFromStream(new ByteArrayInputStream(rdf.getBytes()), namespace, language);
	}
	
	/**
	 * Load in RDF from a model
	 * @param jc the model to load in
	 */
	public void loadRdfFromJC(JenaConnect jc) {
		getJenaModel().add(jc.getJenaModel());
	}
	
	/**
	 * Export all RDF
	 * @param out output stream to write rdf to
	 * @throws IOException error writing to stream
	 */
	public void exportRdfToStream(OutputStream out) throws IOException {
	        log.debug("exportRdfToStream:language not set, defaulting to RDF/XML");
		exportRdfToStream(out, "RDF/XML");
	}
	
	/**
	 * Export all RDF
	 * @param out output stream to write rdf to
	 * @param language the language the rdf is in. Predefined values for lang are "RDF/XML", "N-TRIPLE", "TURTLE" (or
	 *        "TTL") and "N3". null represents the default language, "RDF/XML". "RDF/XML-ABBREV" is a synonym for
	 *        "RDF/XML"
	 * @throws IOException error writing to stream
	 */
	public void exportRdfToStream(OutputStream out, String language) throws IOException {
	        log.debug("exportRdfToStream:language=" + language);
		exportRdfToStream(this.jenaModel, out, language);
	}
	
	/**
	 * Export all RDF
	 * @param m the model to export from
	 * @param out output stream to write rdf to
	 * @param language the language the rdf is in. Predefined values for lang are "RDF/XML", "N-TRIPLE", "TURTLE" (or
	 *        "TTL") and "N3". null represents the default language, "RDF/XML". "RDF/XML-ABBREV" is a synonym for
	 *        "RDF/XML"
	 * @throws IOException error writing to stream
	 */
	private static void exportRdfToStream(Model m, OutputStream out, String language) throws IOException {
		
	        language = (language != null) ? language : "RDF/XML";
		RDFWriter fasterWriter = m.getWriter(language);
		fasterWriter.setProperty("showXmlDeclaration", "true");
		fasterWriter.setProperty("allowBadURIs", "true");
		fasterWriter.setProperty("relativeURIs", "");
		OutputStreamWriter osw = new OutputStreamWriter(out, Charset.availableCharsets().get("UTF-8"));
		fasterWriter.write(m, osw, "");
		osw.flush();
		out.flush();		
	}
	
	/**
	 * Export all RDF
	 * @return the rdf
	 * @throws IOException error writing to string
	 */
	public String exportRdfToString() throws IOException {
		return exportRdfToString(null);
	}
	
	/**
	 * Export all RDF
	 * @param language the language the rdf is in. Predefined values for lang are "RDF/XML", "N-TRIPLE", "TURTLE" (or
	 *        "TTL") and "N3". null represents the default language, "RDF/XML". "RDF/XML-ABBREV" is a synonym for
	 *        "RDF/XML"
	 * @return the rdf
	 * @throws IOException error writing to string
	 */
	public String exportRdfToString(String language) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		exportRdfToStream(baos, language);
		return baos.toString();
	}
	
	/**
	 * Export the RDF to a file
	 * @param fileName the file to write to
	 * @throws IOException error writing to file
	 */
	public void exportRdfToFile(String fileName) throws IOException {
		exportRdfToFile(fileName, "RDF/XML");
	}
	
	/**
	 * Export the RDF to a file
	 * @param fileName the file to write to
	 * @param language the language the rdf is in. Predefined values for lang are "RDF/XML", "N-TRIPLE", "TURTLE" (or
	 *        "TTL") and "N3". null represents the default language, "RDF/XML". "RDF/XML-ABBREV" is a synonym for
	 *        "RDF/XML"
	 * @throws IOException error writing to file
	 */
	public void exportRdfToFile(String fileName, String language) throws IOException {
		exportRdfToFile(fileName, language, false);
	}
	
	/**
	 * Export the RDF to a file
	 * @param fileName the file to write to
	 * @param append append to the file
	 * @throws IOException error writing to file
	 */
	public void exportRdfToFile(String fileName, boolean append) throws IOException {
		exportRdfToFile(fileName, null, append);
	}
	
	/**
	 * Export the RDF to a file
	 * @param fileName the file to write to
	 * @param language the language the rdf is in. Predefined values for lang are "RDF/XML", "N-TRIPLE", "TURTLE" (or
	 *        "TTL") and "N3". null represents the default language, "RDF/XML". "RDF/XML-ABBREV" is a synonym for
	 *        "RDF/XML"
	 * @param append append to the file
	 * @throws IOException error writing to file
	 */
	public void exportRdfToFile(String fileName, String language, boolean append) throws IOException {
		exportRdfToStream(FileAide.getOutputStream(fileName, append), language);
	}
	
	/**
	 * Remove RDF from another JenaConnect
	 * @param inputJC the Model to read from
	 */
	public void removeRdfFromJC(JenaConnect inputJC) {
		this.jenaModel.remove(inputJC.getJenaModel());
	}
	
	/**
	 * Remove RDF from an input stream
	 * @param in input stream to read rdf from
	 * @param namespace the base uri to use for imported uris
	 * @param language the language the rdf is in. Predefined values for lang are "RDF/XML", "N-TRIPLE", "TURTLE" (or
	 *        "TTL") and "N3". null represents the default language, "RDF/XML". "RDF/XML-ABBREV" is a synonym for
	 *        "RDF/XML"
	 */
	public void removeRdfFromStream(InputStream in, String namespace, String language) {
		removeRdfFromJC(new MemJenaConnect(in, namespace, language));
	}
	
	/**
	 * Remove the RDF from a file
	 * @param fileName the file to read from
	 * @param namespace the base uri to use for imported uris
	 * @param language the language the rdf is in. Predefined values for lang are "RDF/XML", "N-TRIPLE", "TURTLE" (or
	 *        "TTL") and "N3". null represents the default language, "RDF/XML". "RDF/XML-ABBREV" is a synonym for
	 *        "RDF/XML"
	 * @throws IOException error connecting
	 */
	public void removeRdfFromFile(String fileName, String namespace, String language) throws IOException {
		removeRdfFromStream(FileAide.getInputStream(fileName), namespace, language);
	}
	
	/**
	 * Removes all records in a RecordHandler from the model
	 * @param rh the RecordHandler to pull records from
	 * @param namespace the base uri to use for imported uris
	 * @param language the rdf syntax language (RDF/XML, N3, TTL, etc). null = RDF/XML
	 * @return number of records removed
	 */
	public int removeRdfFromRH(RecordHandler rh, String namespace, String language) {
		int processCount = 0;
		for(Record r : rh) {
			log.trace("removing record: " + r.getID());
			if(namespace != null) {
				// log.trace("using namespace '"+namespace+"'");
			}
			ByteArrayInputStream bais = new ByteArrayInputStream(r.getData().getBytes());
			getJenaModel().remove(new MemJenaConnect(bais, namespace, language).getJenaModel());
			try {
				bais.close();
			} catch(IOException e) {
				// ignore
			}
			processCount++;
		}
		return processCount;
	}
	
	/**
	 * Adds all records in a RecordHandler to the model
	 * @param rh the RecordHandler to pull records from
	 * @param namespace the base uri to use for imported uris
	 * @param language the rdf syntax language (RDF/XML, N3, TTL, etc).  null = RDF/XML
	 * @return number of records added
	 */
	public int loadRdfFromRH(RecordHandler rh, String namespace, String language) {
		int processCount = 0;
		for(Record r : rh) {
			log.trace("loading record: " + r.getID());
			if (namespace != null) {
				log.trace("using namespace '"+namespace+"'");
			}
			ByteArrayInputStream bais = new ByteArrayInputStream(
				r.getData().getBytes(StandardCharsets.UTF_8)
			);
			getJenaModel().read(bais, namespace, language);
			try {
				bais.close();
			} catch(IOException e) {
				// ignore
			}
			processCount++;
		}
		return processCount;
	}
	
	/**
	 * Closes the model
	 */
	public void close() {
		sync();
	}
	
	/**
	 * Syncronizes the model to the datastore
	 */
	public abstract void sync();
	
	/**
	 * Build a QueryExecution from a queryString
	 * @param queryString the query to build execution for
	 * @param datasetMode execute against dataset
	 * @return the QueryExecution
	 * @throws IOException error connecting
	 */
	private QueryExecution buildQueryExec(String queryString, boolean datasetMode) throws IOException {
		QueryExecution qe;
		if(datasetMode) {
			qe = QueryExecutionFactory.create(QueryFactory.create(queryString, Syntax.syntaxARQ), getDataset());
		} else {
			qe = QueryExecutionFactory.create(QueryFactory.create(queryString, Syntax.syntaxARQ), getJenaModel());
		}
		return qe;
	}
	
	/**
	 * Executes a sparql select query against the JENA model and returns the selected result set
	 * @param queryString the query to execute against the model
	 * @return the executed query result set
	 * @throws IOException error connecting
	 */
	public ResultSet executeSelectQuery(String queryString) throws IOException {
		return executeSelectQuery(queryString, false, false);
	}
	
	/**
	 * Executes a sparql select query against the JENA model and returns the selected result set
	 * @param queryString the query to execute against the model
	 * @param datasetMode execute against dataset
	 * @return the executed query result set
	 * @throws IOException error connecting
	 */
	public ResultSet executeSelectQuery(String queryString, boolean datasetMode) throws IOException {
		return executeSelectQuery(queryString, false, datasetMode);
	}
	
	/**
	 * Executes a sparql select query against the JENA model and returns the selected result set
	 * @param queryString the query to execute against the model
	 * @param copyResultSet copy the resultset
	 * @param datasetMode execute against dataset
	 * @return the executed query result set
	 * @throws IOException error connecting
	 */
	public ResultSet executeSelectQuery(String queryString, boolean copyResultSet, boolean datasetMode) throws IOException {
		QueryExecution qexec = buildQueryExec(queryString, datasetMode);
		ResultSet rs = qexec.execSelect();
		if(copyResultSet) {
			rs = ResultSetFactory.copyResults(rs);
			qexec.close();
		}
		return rs;
	}
	
	/**
	 * Executes a sparql construct query against the JENA model and returns the constructed result model
	 * @param queryString the query to execute against the model
	 * @return the executed query result model
	 * @throws IOException error connecting
	 */
	public JenaConnect executeConstructQuery(String queryString) throws IOException {
		return executeConstructQuery(queryString, false);
	}
	
	/**
	 * Executes a sparql construct query against the JENA model and returns the constructed result model
	 * @param queryString the query to execute against the model
	 * @param datasetMode execute against dataset
	 * @return the executed query result model
	 * @throws IOException error connecting
	 */
	public JenaConnect executeConstructQuery(String queryString, boolean datasetMode) throws IOException {
		JenaConnect jc = new MemJenaConnect();
		jc.getJenaModel().add(buildQueryExec(queryString, datasetMode).execConstruct());
		return jc;
	}
	
	/**
	 * Executes a sparql describe query against the JENA model and returns the description result model
	 * @param queryString the query to execute against the model
	 * @return the executed query result model
	 * @throws IOException error connecting
	 */
	public JenaConnect executeDescribeQuery(String queryString) throws IOException {
		return executeDescribeQuery(queryString, false);
	}
	
	/**
	 * Executes a sparql describe query against the JENA model and returns the description result model
	 * @param queryString the query to execute against the model
	 * @param datasetMode execute against dataset
	 * @return the executed query result model
	 * @throws IOException error connecting
	 */
	public JenaConnect executeDescribeQuery(String queryString, boolean datasetMode) throws IOException {
		JenaConnect jc = new MemJenaConnect();
		jc.getJenaModel().add(buildQueryExec(queryString, datasetMode).execDescribe());
		return jc;
	}
	
	/**
	 * Executes a sparql describe query against the JENA model and returns the description result model
	 * @param queryString the query to execute against the model
	 * @return the executed query result model
	 * @throws IOException error connecting
	 */
	public boolean executeAskQuery(String queryString) throws IOException {
		return executeAskQuery(queryString, false);
	}
	
	/**
	 * Executes a sparql describe query against the JENA model and returns the description result model
	 * @param queryString the query to execute against the model
	 * @param datasetMode execute against dataset
	 * @return the executed query result model
	 * @throws IOException error connecting
	 */
	public boolean executeAskQuery(String queryString, boolean datasetMode) throws IOException {
		return buildQueryExec(queryString, datasetMode).execAsk();
	}
	
	/**
	 * Executes a sparql update query against the JENA model
	 * @param queryString the query to execute against the model
	 * @throws IOException error connecting
	 */
	public void executeUpdateQuery(String queryString) throws IOException {
		executeUpdateQuery(queryString, false);
	}
	
	/**
	 * Executes a sparql update query against the JENA model
	 * @param queryString the query to execute against the model
	 * @param datasetMode execute against dataset
	 * @throws IOException error connecting
	 */
	public void executeUpdateQuery(String queryString, boolean datasetMode) throws IOException {
		log.debug("datasetMode: "+ datasetMode); 
		if (datasetMode) {
			if (this.jenaModel.supportsTransactions()) this.jenaModel.begin();
			this.jenaModel.notifyEvent(GraphEvents.startRead);
            boolean committed = false;
			try {
	 			log.trace("query:\n" + queryString);
 				log.trace("Executing query against dataset");
				UpdateAction.execute(UpdateFactory.create(queryString), getDataset());
				if (this.jenaModel.supportsTransactions()) {
					this.jenaModel.commit();
					committed = true;
				}
			} catch(Exception e){
				log.error(e.getMessage(), e);
				if (this.jenaModel.supportsTransactions() && !committed) {
					this.jenaModel.abort();
				}
				throw e;
			} finally {
				this.jenaModel.notifyEvent(GraphEvents.finishRead);
			}
		} else {
			this.jenaModel.notifyEvent(GraphEvents.startRead);
			log.trace("Executing query against model");
			UpdateAction.parseExecute(queryString, getJenaModel());
			this.jenaModel.notifyEvent(GraphEvents.finishRead);
		}
		
	}

	/**
	 * RDF formats
	 */
	 
	/*protected static HashMap<String, ResultSetFormat> formatSymbols = new HashMap<String, ResultSetFormat>();
    static {
            formatSymbols.put(ResultSetFormat.syntaxXML.getSymbol(), ResultSetFormat.syntaxXML);
            formatSymbols.put(ResultSetFormat.syntaxRDF_XML.getSymbol(), ResultSetFormat.syntaxRDF_XML);
            formatSymbols.put(ResultSetFormat.syntaxRDF_N3.getSymbol(), ResultSetFormat.syntaxRDF_N3);
            formatSymbols.put(ResultSetFormat.syntaxCSV.getSymbol(), ResultSetFormat.syntaxCSV);
            formatSymbols.put(ResultSetFormat.syntaxText.getSymbol(), ResultSetFormat.syntaxText);
            formatSymbols.put(ResultSetFormat.syntaxJSON.getSymbol(), ResultSetFormat.syntaxJSON);
    }*/
    
    
    
    protected static HashMap<String, ResultsFormat> formatSymbols = new HashMap<String, ResultsFormat>();
    static { 
    	    
            formatSymbols.put(ResultsFormat.FMT_RS_XML.getSymbol(), ResultsFormat.FMT_RS_XML);
            formatSymbols.put(ResultsFormat.FMT_RDF_XML.getSymbol(), ResultsFormat.FMT_RDF_XML);
            formatSymbols.put(ResultsFormat.FMT_RDF_N3.getSymbol(), ResultsFormat.FMT_RDF_N3);
            formatSymbols.put(ResultsFormat.FMT_RS_CSV.getSymbol(), ResultsFormat.FMT_RS_CSV);
            formatSymbols.put(ResultsFormat.FMT_TEXT.getSymbol(), ResultsFormat.FMT_TEXT);
            formatSymbols.put(ResultsFormat.FMT_RS_JSON.getSymbol(), ResultsFormat.FMT_RS_JSON);
    }

	 
	/**
	 * Execute a Query and output result to System.out
	 * @param queryParam the query
	 * @param resultFormatParam the format to return the results in ('RS_RDF',etc for select queries / 'RDF/XML',etc for
	 *        construct/describe queries)
	 * @param datasetMode run against dataset rather than model
	 * @throws IOException error writing to output
	 */
	public void executeQuery(String queryParam, String resultFormatParam, boolean datasetMode) throws IOException {
		executeQuery(queryParam, resultFormatParam, null, datasetMode);
	}
	
	/**
	 * Execute a Query
	 * @param queryParam the query
	 * @param resultFormatParam the format to return the results in ('RS_TEXT' default for select queries / 'RDF/XML'
	 *        default for construct/describe queries)
	 * @param output output stream to write to - null uses System.out
	 * @param datasetMode run against dataset rather than model
	 * @throws IOException error writing to output
	 */
	public void executeQuery(String queryParam, String resultFormatParam, OutputStream output, boolean datasetMode) throws IOException {
		OutputStream out;
		if(output != null) {
			out = output;
			log.info("Outputting to the specified location");
		} else {
			out = System.out;
		}
		QueryExecution qe = null;
		try {
			Query query = QueryFactory.create(queryParam, Syntax.syntaxARQ);
			if(datasetMode) {
 				log.trace("Executing query against dataset");
				qe = QueryExecutionFactory.create(query, getDataset());
			} else {
 				log.trace("Executing query against model");
				qe = QueryExecutionFactory.create(query, getJenaModel());
			}
			if(query.isSelectType()) {
				log.trace("output formatted results"); 
				/*ResultSetFormat rsf = formatSymbols.get(resultFormatParam);
                if (rsf == null) {
                     rsf = ResultSetFormat.syntaxText;
                }*/
                
                ResultsFormat rsf = formatSymbols.get(resultFormatParam);
                if (rsf == null) {
                     rsf = ResultsFormat.FMT_TEXT;
                }
                log.trace("resultSetFormat: "+ rsf.toString());
                ResultSet rs = qe.execSelect();
                if (rs.hasNext()) {
                   ResultSetFormatter.output(out, rs, rsf);
                } else {
                	log.warn("empty result set");
                }
				// this changed in newer versions of Jena
				 
				/*ResultsFormat rsf = ResultsFormat.lookup(resultFormatParam);				 
				if (rsf == null) {
					ResultSetFormatter.out(qe.execSelect());					 
				} else {				 
				   ResultSetFormatter.output(out, qe.execSelect(), rsf);
				}*/
				
			} else if(query.isAskType()) {
				out.write((Boolean.toString(qe.execAsk())+"\n").getBytes());
			} else {
				MemJenaConnect resultModel = new MemJenaConnect();
				if(query.isConstructType()) {
					qe.execConstruct(resultModel.getJenaModel());
				} else if(query.isDescribeType()) {
					qe.execDescribe(resultModel.getJenaModel());
				} else {
					throw new IllegalArgumentException("Query Invalid: Not Select, Construct, Ask, or Describe");
				}
				log.trace("export result model using format: "+ resultFormatParam);
				resultModel.exportRdfToStream(out, resultFormatParam);
			}
		} catch(QueryParseException e1) {
			try {
				executeUpdateQuery(queryParam, datasetMode);
				log.info("Update Successfully Applied");
			} catch(QueryParseException e2) {
				log.error("Invalid Query:\n"+queryParam);
				log.trace("Attempted Query Exception:",e1);
				log.trace("Attempted Update Exception:",e2);
			}
		} finally {
			if(qe != null) {
				qe.close();
			}
		}
	}
	
	/**
	 * Accessor for Jena Model
	 * @return the Jena Model
	 */
	public Model getJenaModel() {
		return this.jenaModel;
	}
	
	/**
	 * Setter
	 * @param jena the new model
	 */
	protected void setJenaModel(Model jena) {
		this.jenaModel = jena;
	}
	
	/**
	 * Checks if the model contains the given uri
	 * @param uri the uri to check for
	 * @return true if found, false otherwise
	 * @throws IOException error connecting
	 */
	public boolean containsURI(String uri) throws IOException {
		return executeAskQuery("ASK { <" + uri + "> ?p ?o }");
	}
	
	/**
	 * Get the ArgParser for this task
	 * @return the ArgParser
	 */
	private static ArgParser getParser() {
		ArgParser parser = new ArgParser("JenaConnect");
		parser.addArgument(new ArgDef().setShortOption('j').setLongOpt("jena").withParameter(true, "CONFIG_FILE").setDescription("config file for jena model").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('J').setLongOpt("jenaOverride").withParameterValueMap("JENA_PARAM", "VALUE").setDescription("override the JENA_PARAM of jena model config using VALUE").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('q').setLongOpt("query").withParameter(true, "SPARQL_QUERY").setDescription("sparql query to execute").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('Q').setLongOpt("queryResultFormat").withParameter(true, "RESULT_FORMAT").setDescription("the format to return the results in ('RS_RDF',etc for select queries / 'RDF/XML',etc for construct/describe queries)").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('f').setLongOpt("fileOutput").withParameter(true, "OUTPUT_FILE").setDescription("the file to output the results in, if not specified writes to stdout").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('d').setLongOpt("dataset").setDescription("execute query against dataset rather than model").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('t').setLongOpt("truncate").setDescription("empty the jena model").setRequired(false));
		return parser;
	}
	
	/**
	 * Config parser for Jena Models
	 * @author Christopher Haines (hainesc@ctrip.ufl.edu)
	 */
	private static class JenaConnectConfigParser extends DefaultHandler {
		/**
		 * Param list from the config file
		 */
		private Map<String, String> params;
		/**
		 * temporary storage for cdata
		 */
		private String tempVal;
		/**
		 * temporary storage for param name
		 */
		private String tempParamName;
		
		/**
		 * Default Constructor
		 */
		protected JenaConnectConfigParser() {
			this.params = new HashMap<String, String>();
			this.tempVal = "";
			this.tempParamName = "";
		}
		
		/**
		 * Build a JenaConnect using the input stream data
		 * @param inputStream stream to read config from
		 * @return the JenaConnect described by the stream
		 * @throws IOException error reading stream
		 */
		protected Map<String, String> parseConfig(InputStream inputStream) throws IOException {
			if(inputStream != null) {
				// get a factory
				SAXParserFactory spf = SAXParserFactory.newInstance();
				try {
					// get a new instance of parser
					SAXParser sp = spf.newSAXParser();
					// parse the file and also register this class for call backs
					sp.parse(inputStream, this);
				} catch(SAXException e) {
					throw new IOException(e);
				} catch(ParserConfigurationException e) {
					throw new IOException(e);
				}
			}
			return this.params;
		}
		
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			this.tempVal = "";
			this.tempParamName = "";
			if(qName.equalsIgnoreCase("Param")) {
				this.tempParamName = attributes.getValue("name");
			} else if(!qName.equalsIgnoreCase("Model") && !qName.equalsIgnoreCase("Config")) {
				throw new SAXException("Unknown Tag: " + qName);
			}
		}
		
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			this.tempVal = new String(ch, start, length);
		}
		
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if(qName.equalsIgnoreCase("Param")) {
				this.params.put(this.tempParamName, this.tempVal);
			} else if(!qName.equalsIgnoreCase("Model") && !qName.equalsIgnoreCase("Config")) {
				throw new SAXException("Unknown Tag: " + qName);
			}
		}
	}
	
	/**
	 * Remove all statements from model
	 */
	public void truncate() {
		Model sourceModel = getJenaModel();
		sourceModel.enterCriticalSection(Lock.WRITE);
		try{
			// this method is used so that any listeners can see each statement removed
			sourceModel.removeAll((Resource)null,(Property)null,(RDFNode)null);
		} finally {
			sourceModel.leaveCriticalSection();
		}
	}
	
	/**
	 * Set the modelName
	 * @param modelName the model name
	 */
	protected void setModelName(String modelName) {
		this.modelName = modelName;
	}
	
	/**
	 * Get the modelName
	 * @return the modelName
	 */
	public String getModelName() {
		return this.modelName;
	}
	
	/**
	 * Is this model empty
	 * @return true if empty, false otherwise
	 * @throws IOException error connecting
	 */
	public boolean isEmpty() throws IOException {
		return !executeAskQuery("ASK { ?s ?p ?o }");
	}
	
	/**
	 * Output the jena model information
	 */
	public void printParameters() {
		log.trace("modelName: '" + getModelName() + "'");
	}
	
	/**
	 * Run from commandline
	 * @param args the commandline args
	 * @throws IOException error parsing args
	 * @throws UsageException user requested usage message
	 */
	public static void run(String... args) throws IOException, UsageException {
		ArgList argList = getParser().parse(args);
		JenaConnect jc = JenaConnect.parseConfig(argList.get("j"), argList.getValueMap("J"));
		if(jc == null) {
			throw new IllegalArgumentException("Must specify a jena model");
		}
		if(argList.has("t")) {
			if(argList.has("q") || argList.has("Q")) {
				throw new IllegalArgumentException("Cannot Execute Query and Truncate");
			}
			log.info("Removing all triples");
			jc.truncate();
			jc.sync();
		} else if(argList.has("q")) {
			log.info("Executing query");
			jc.executeQuery(argList.get("q"), argList.get("Q"), FileAide.getOutputStream(argList.get("f")), argList.has("d"));
		    jc.sync();
		} else {
			throw new IllegalArgumentException("No Operation Specified");
		} 
	}
	
	/**
	 * Main method
	 * @param args commandline arguments
	 */
	public static void main(String... args) {
		Exception error = null;
		try {
			InitLog.initLogger(args, getParser(), "ft");
			log.info(getParser().getAppName() + ": Start");
			run(args);
		} catch(IllegalArgumentException e) {
			log.error(e.getMessage());
			log.debug("Stacktrace:",e);
			System.err.println(getParser().getUsage());
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
