/*******************************************************************************
 * Copyright (c) 2010-2011 VIVO Harvester Team. For full list of contributors, please see the AUTHORS file provided.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the new BSD license which accompanies this distribution, and is available at http://www.opensource.org/licenses/bsd-license.html
 ******************************************************************************/
package org.vivoweb.harvester.score;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vivoweb.harvester.score.algorithm.Algorithm;
import org.vivoweb.harvester.score.algorithm.EqualityIgnoreCaseTest;
import org.vivoweb.harvester.score.algorithm.EqualityTest;
import org.vivoweb.harvester.util.InitLog;
import org.vivoweb.harvester.util.IterableAdaptor;
import org.vivoweb.harvester.util.args.ArgDef;
import org.vivoweb.harvester.util.args.ArgList;
import org.vivoweb.harvester.util.args.ArgParser;
import org.vivoweb.harvester.util.args.UsageException;
import org.vivoweb.harvester.util.repo.JenaConnect;
import org.vivoweb.harvester.util.repo.MemJenaConnect;
import org.vivoweb.harvester.util.repo.TDBJenaConnect;
import org.apache.commons.lang.time.StopWatch;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;

/**
 * VIVO Score
 * @author Nicholas Skaggs nskaggs@ctrip.ufl.edu
 * @author Stephen Williams svwilliams@ctrip.ufl.edu
 * @author Christopher Haines hainesc@ctrip.ufl.edu
 * @thanks Chris Westling cmw48@cornell.edu
 */
public class Score2 {
	/** 
	 * SLF4J Logger
	 */
	private static Logger log = LoggerFactory.getLogger(Score2.class);

	/**
	 * model containing statements to be scored
	 */
	private JenaConnect inputJena;
	/**
	 * model containing vivoJena statements
	 */
	private JenaConnect vivoJena;
	/**
	 * model containing scoring data statements
	 */
	private JenaConnect scoreJena;
	/**
	 * model in which to store temp copy of input and vivo data statements
	 */
	private JenaConnect tempJena;
	/**
	 * the class of the Algorithm to execute
	 */
	private Map<String, Class<? extends Algorithm>> algorithms;
	/**
	 * the predicates to look for in inputJena model
	 */
	private Map<String, String> inputPredicates;
	/**
	 * the predicates to look for in vivoJena model
	 */
	private Map<String, String> vivoPredicates;
	/**
	 * String containing a list of common names separated by ","
	 */
	private String commonNames;
	/**
	 * limit match Algorithm to only match rdf nodes in inputJena whose URI begin with this namespace
	 */
	private String namespace;
	/**
	 * the weighting (0.0 , 1.0) for this score
	 */
	private Map<String, Float> weights;
	/**
	 * are all algorithms org.vivoweb.harvester.score.algorithm.EqualityTest
	 */
	private boolean equalityOnlyMode;
	/**
	 * Match threshold
	 */
	private final Float matchThreshold;
	/**
	 * number of records to use in batch
	 */
	private int batchSize;
	/**
	 * reload the temp copy of input, only needed if input has changed since last score
	 */
	private boolean reloadInput;
	/**
	 * reload the temp copy of Vivo, only needed if Vivo has changed since last score
	 */
	private boolean reloadVivo;
	
	/**
	 * Constructor
	 * @param inputJena model containing statements to be scored
	 * @param vivoJena model containing vivoJena statements
	 * @param scoreJena model containing scoring data statements
	 * @param tempJenaDir model in which to store temp copy of input and vivo data statements
	 * @param algorithms the classes of the algorithms to execute
	 * @param inputPredicates the predicates to look for in inputJena model
	 * @param vivoPredicates the predicates to look for in vivoJena model
	 * @param namespace limit match Algorithm to only match rdf nodes in inputJena whose URI begin with this namespace
	 * @param weights the weightings (0.0 , 1.0) for this score
	 * @param matchThreshold score things with a total current score greater than or equal to this threshold
	 * @param batchSize number of records to use in batch
	 * @param reloadInput reload the temp copy of input, only needed if input has changed since last score
	 * @param reloadVivo reload the temp copy of Vivo, only needed if Vivo has changed since last score
	 */
	public Score2(JenaConnect inputJena, JenaConnect vivoJena, JenaConnect scoreJena, String tempJenaDir, Map<String, Class<? extends Algorithm>> algorithms, Map<String, String> inputPredicates, Map<String, String> vivoPredicates, String namespace, Map<String, Float> weights, Float matchThreshold, int batchSize, boolean reloadInput, boolean reloadVivo) {
		if(inputJena == null) {
			throw new IllegalArgumentException("Input model cannot be null");
		}
		this.inputJena = inputJena;
		
		if(vivoJena == null) {
			throw new IllegalArgumentException("Vivo model cannot be null");
		}
		this.vivoJena = vivoJena;
		
		if(scoreJena == null) {
			throw new IllegalArgumentException("Score2 Data model cannot be null");
		}
		this.scoreJena = scoreJena;
		
		String tempDir = tempJenaDir;
		if(tempDir == null) {
			log.trace("temp model directory is not specified, using system temp directory");
			//			tempDir = File.createTempFile("tempVivoInputCopyJena", "db").getAbsolutePath();
			//			log.debug("temp model is not specifiedhi , using memory jena model");
			this.tempJena = new MemJenaConnect("urn:x-arq:UnionGraph");
		} else {
			this.tempJena = new TDBJenaConnect(tempDir, "urn:x-arq:UnionGraph");
		}
		
		if(algorithms == null) {
			throw new IllegalArgumentException("Algorithm cannot be null");
		}
		this.algorithms = algorithms;
		
		if(inputPredicates == null) {
			throw new IllegalArgumentException("Input Predicate cannot be null");
		}
		this.inputPredicates = inputPredicates;
		
		if(vivoPredicates == null) {
			throw new IllegalArgumentException("Vivo Predicate cannot be null");
		}
		this.vivoPredicates = vivoPredicates;
		
		if(this.algorithms.size() < 1) {
			throw new IllegalArgumentException("No runs specified!");
		}
		
		this.namespace = namespace;
		
		for(Float weight : weights.values()) {
			float d = weight.floatValue();
			if(d > 1f) {
				throw new IllegalArgumentException("Weights cannot be greater than 1.0");
			}
			if(d < 0f) {
				throw new IllegalArgumentException("Weights cannot be less than 0.0");
			}
		}
		this.weights = weights;
		
		Map<String, Map<String, ? extends Object>> maps = new HashMap<String, Map<String, ? extends Object>>();
		maps.put("vivoJena predicates", this.vivoPredicates);
		maps.put("inputJena predicates", this.inputPredicates);
		maps.put("algorithms", this.algorithms);
		maps.put("weights", this.weights);
		verifyRunNames(maps);
		boolean test = true;
		for (Class<?> algClass : this.algorithms.values()) {
			log.trace("algClass: "+ algClass.getSimpleName()); 
			if (EqualityTest.class.isAssignableFrom(algClass)  || EqualityIgnoreCaseTest.class.isAssignableFrom(algClass)   ) {
				test = true;
			} else {
			   test = false;
			   break;
			}
			 
		}
		this.equalityOnlyMode = test;
		this.matchThreshold = matchThreshold;
		setBatchSize(batchSize);
		log.trace("equalityOnlyMode: " + this.equalityOnlyMode);
		this.reloadInput = reloadInput;
		this.reloadVivo = reloadVivo;
		
	}

	/**
	 * Constructor
	 * @param inputJena
	 * @param vivoJena
	 * @param scoreJena
	 * @param tempJenaDir
	 * @param algorithms
	 * @param inputPredicates
	 * @param vivoPredicates
	 * @param namespace
	 * @param weights
	 * @param matchThreshold
	 * @param batchSize
	 * @param reloadInput
	 * @param reloadVivo
	 * @param commonNames
	 */
	public Score2(JenaConnect inputJena, JenaConnect vivoJena, JenaConnect scoreJena, String tempJenaDir, Map<String, Class<? extends Algorithm>> algorithms, Map<String, String> inputPredicates, Map<String, String> vivoPredicates, String namespace, Map<String, Float> weights, Float matchThreshold, int batchSize, boolean reloadInput, boolean reloadVivo, String commonNames) {
		this(inputJena, vivoJena, scoreJena, tempJenaDir, algorithms, inputPredicates, vivoPredicates, namespace, weights, matchThreshold, batchSize, reloadInput, reloadVivo);
		this.commonNames = commonNames;
	}
	
	/**
	 * Constructor
	 * @param args argument list
	 * @throws IOException error parsing options
	 * @throws UsageException user requested usage message
	 */
	private Score2(String... args) throws IOException, UsageException {
		this(getParser().parse(args));
	}
	
	/**
	 * Constructor Scoring.close();
	 * @param opts parsed argument list
	 * @throws IOException error parsing options
	 */
	private Score2(ArgList opts) throws IOException {
		this(
			JenaConnect.parseConfig(opts.get("i"), opts.getValueMap("I")), 
			JenaConnect.parseConfig(opts.get("v"), opts.getValueMap("V")), 
			JenaConnect.parseConfig(opts.get("s"), opts.getValueMap("S")), 
			opts.get("t"), 
			initAlgs(opts.getValueMap("A")), 
			opts.getValueMap("F"), 
			opts.getValueMap("P"), 
			opts.get("n"), 
			initWeights(opts.getValueMap("W")), 
			(opts.has("m")?Float.valueOf(opts.get("m")):null), 
			Integer.parseInt(opts.get("b")), 
			opts.has("reloadInput"), 
			opts.has("reloadVivo"),
			(opts.has("c")?opts.get("c"):null)
		);
	}
	
	/**
	 * Initialize the algoritm map from the commandline mapping
	 * @param algs the commandline mapping
	 * @return the algorithm map
	 */
	private static Map<String, Class<? extends Algorithm>> initAlgs(Map<String,String> algs) {
		Map<String, Class<? extends Algorithm>> retVal = new HashMap<String, Class<? extends Algorithm>>();
		for(String runName : algs.keySet()) {
			try {
				retVal.put(runName, Class.forName(algs.get(runName)).asSubclass(Algorithm.class));
			} catch(ClassNotFoundException e) {
				throw new IllegalArgumentException(e);
			} catch(ClassCastException e) {
				throw new IllegalArgumentException(e);
			}
		}
		return retVal;
	}
	
	/**
	 * Initialize the weight map from the commandline mapping
	 * @param weight the commandline mapping
	 * @return the weight map
	 */
	private static Map<String, Float> initWeights(Map<String, String> weight) {
		Map<String, Float> retVal = new HashMap<String, Float>();
		for(String runName : weight.keySet()) {
			retVal.put(runName, Float.valueOf(weight.get(runName)));
		}
		return retVal;
	}
	
	/**
	 * Set the processing batch size
	 * @param size the size to use
	 */
	public void setBatchSize(int size) {
		this.batchSize = size;
		if(this.batchSize <= 1) {
			log.warn("Batch Size of '"+size+"' invalid, must be greater than or equal to 1.  Using '1' as Batch Size.");
			this.batchSize = 1;
		}
	}
	
	/**
	 * Verify that each map contains the same keys
	 * @param maps mapping of map name to map
	 */
	private void verifyRunNames(Map<String, Map<String, ? extends Object>> maps) {
		for(String x : maps.keySet()) {
			for(String y : maps.keySet()) {
				if((x != y) && !maps.get(y).keySet().containsAll(maps.get(x).keySet())) {
					for(String runName : maps.get(x).keySet()) {
						if((x != y) && !maps.get(y).containsKey(runName)) {
							throw new IllegalArgumentException("run name '" + runName + "' found in " + x + ", but not in " + y);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Get the ArgParser
	 * @return the ArgParser
	 */
	private static ArgParser getParser() {
		ArgParser parser = new ArgParser("Score2");
		// Models
		parser.addArgument(new ArgDef().setShortOption('i').setLongOpt("inputJena-config").withParameter(true, "CONFIG_FILE").setDescription("inputJena JENA configuration filename").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('I').setLongOpt("inputOverride").withParameterValueMap("JENA_PARAM", "VALUE").setDescription("override the JENA_PARAM of inputJena jena model config using VALUE").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('v').setLongOpt("vivoJena-config").withParameter(true, "CONFIG_FILE").setDescription("vivoJena JENA configuration filename").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('V').setLongOpt("vivoOverride").withParameterValueMap("JENA_PARAM", "VALUE").setDescription("override the JENA_PARAM of vivoJena jena model config using VALUE").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('s').setLongOpt("score-config").withParameter(true, "CONFIG_FILE").setDescription("score data JENA configuration filename").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('S').setLongOpt("scoreOverride").withParameterValueMap("JENA_PARAM", "VALUE").setDescription("override the JENA_PARAM of score jena model config using VALUE").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('t').setLongOpt("tempJenaDir").withParameter(true, "DIRECTORY_PATH").setDescription("directory to store temp jena model").setRequired(false));
		
		// Parameters
		parser.addArgument(new ArgDef().setShortOption('A').setLongOpt("algorithms").withParameterValueMap("RUN_NAME", "CLASS_NAME").setDescription("for RUN_NAME, use this CLASS_NAME (must implement Algorithm) to evaluate matches").setRequired(true));
		parser.addArgument(new ArgDef().setShortOption('W').setLongOpt("weights").withParameterValueMap("RUN_NAME", "WEIGHT").setDescription("for RUN_NAME, assign this weight (0,1) to the scores").setRequired(true));
		parser.addArgument(new ArgDef().setShortOption('F').setLongOpt("inputJena-predicates").withParameterValueMap("RUN_NAME", "PREDICATE").setDescription("for RUN_NAME, match ").setRequired(true));
		parser.addArgument(new ArgDef().setShortOption('P').setLongOpt("vivoJena-predicates").withParameterValueMap("RUN_NAME", "PREDICAATE").setDescription("for RUN_NAME, assign this weight (0,1) to the scores").setRequired(true));
		parser.addArgument(new ArgDef().setShortOption('n').setLongOpt("namespace").withParameter(true, "SCORE_NAMESPACE").setDescription("limit match Algorithm to only match rdf nodes in inputJena whose URI begin with SCORE_NAMESPACE").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('b').setLongOpt("batch-size").withParameter(true, "BATCH_SIZE").setDescription("approximate number of triples to process in each batch - default 2000 - lower this if getting StackOverflow or OutOfMemory").setDefaultValue("2000").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('m').setLongOpt("matchThreshold").withParameter(true, "THRESHOLD").setDescription("match records with a score over THRESHOLD").setRequired(false));
		parser.addArgument(new ArgDef().setLongOpt("reloadInput").setDescription("reload the temp copy of input, only needed if input has changed since last score").setRequired(false));
		parser.addArgument(new ArgDef().setLongOpt("reloadVivo").setDescription("reload the temp copy of Vivo, only needed if Vivo has changed since last score").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('c').setLongOpt("common-names").withParameter(true, "COMMON_NAMES").setDescription("use these names to check if the score needs to be modified.").setRequired(false));
		return parser;
	}
	
	/**
	 * Build the vivo and input clones and get the dataset
	 * @return the dataset
	 * @throws IOException error connecting to the models
	 */
	private Dataset prepDataset() throws IOException {
		// Bring all models into a single Dataset
		JenaConnect vivoClone = this.tempJena.neighborConnectClone("http://vivoweb.org/harvester/model/scoring#vivoClone");
		if(vivoClone.isEmpty() || this.reloadVivo) {
			if(this.reloadVivo) {
				log.debug("Clearing old VIVO model data from temp copy model");
				vivoClone.truncate();
			}
			log.debug("Loading VIVO model into temp copy model");
			vivoClone.loadRdfFromJC(this.vivoJena);
//			log.debug("vivo clone contents:\n"+vivoClone.exportRdfToString());
		} else {
			log.debug("VIVO model already in temp copy model");
		}
		JenaConnect inputClone = this.tempJena.neighborConnectClone("http://vivoweb.org/harvester/model/scoring#inputClone");
		if(inputClone.isEmpty() || this.reloadInput) {
			if(this.reloadInput) {
				log.debug("Clearing old Input model data from temp copy model");
				inputClone.truncate();
			}
			log.debug("Loading Input model into temp copy model");
			inputClone.loadRdfFromJC(this.inputJena);
//			log.debug("input clone contents:\n"+inputClone.exportRdfToString());
		} else {
			log.debug("Input model already in temp copy model");
		}
		Dataset ds = this.tempJena.getDataset();
		log.trace("testing Dataset");
		if(!this.tempJena.executeAskQuery("ASK { ?s ?p ?o }")) {
			log.trace("Empty Dataset");
		}
		return ds;
	}
	
	/**
	 * Get the result set
	 * @return the resultset
	 * @throws IOException error connecting to the models
	 */
	private ResultSet getResultSet() throws IOException {
		Dataset ds = prepDataset();
		log.trace("Building Query");
		String sQuery = buildSelectQuery();
		log.trace("Score2 Query:\n" + sQuery);
		// Execute query
		log.trace("Building Query Execution");
		Query query = QueryFactory.create(sQuery, Syntax.syntaxARQ);
		QueryExecution queryExec = QueryExecutionFactory.create(query, ds);
		 
		log.debug("Executing Query");
		ResultSet rs = queryExec.execSelect(); 
		log.debug("Query Finished");
		return rs;
	}
	
	/**
	 * Build the solution set
	 * @return the solution set
	 * @throws IOException error connecting to the models
	 */
	private List<Map<String, String>> buildSolutionSet() throws IOException {
		 
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		if (this.matchThreshold != null) {
			return buildFilterSolutionSet();
		}
		ResultSet rs = getResultSet();
	    Set<String> runNames = this.vivoPredicates.keySet();
		// Set<Map<String, String>> solSet = getNewSolSet();
		List<Map<String, String>> solSet = new ArrayList<Map<String, String>>();
		boolean hasResults = false;
		try {
			hasResults = rs.hasNext();
		} catch (java.lang.UnsupportedOperationException ex) {
			log.warn("UnsupportedOperationException encountered in resultSet", ex);
		}
		if (!hasResults) {
			log.info("No Results Found");
		} else {
			log.info("Building Record Set");
			
			Map<String, String> tempMap; 
			int rowNum = 0;
			//while (rs.hasNext()) {
			try {
				for (QuerySolution solution : IterableAdaptor.adapt(rs)) {
					rowNum = rs.getRowNumber();
					//log.trace("solution "+ rowNum);
					//QuerySolution solution = rs.next();
					
					String sinputuri = solution.getResource("sInput").getURI();
					String svivouri = solution.getResource("sVivo").getURI();
					log.debug("("+rowNum+") Potential Match: <" + sinputuri + "> to <" + svivouri + ">");
					tempMap = new HashMap<String, String>();
					tempMap.put("sInput", sinputuri);
					tempMap.put("sVivo", svivouri);
					
					for (String runName : runNames) { 
						RDFNode os = solution.get("os_" + runName);
						RDFNode op = solution.get("op_" + runName);
						addRunName(tempMap, runName, os, op); 
					}
					//log.trace("Add to solSet");
					solSet.add(tempMap); 
					//log.trace("Added to solSet\n"); 
				}
			} catch (java.lang.UnsupportedOperationException ex) {
				log.warn("UnsupportedOperationException encountered at row: "+rowNum, ex);
			}
			
			
		}
		stopWatch.stop();
		log.trace("BuildSolutionSet took this much time: "+stopWatch.getTime());
		log.info("Finished building Record Set");
		log.info("Added this many records: "+ solSet.size());
		return solSet;
	}
	
	/**
	 * Build the solution set for a filtered score
	 * @return the solution set
	 * @throws IOException error connecting to the models
	 */
	private List<Map<String, String>> buildFilterSolutionSet() throws IOException {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		log.trace("buildFilterSolutionSet");
		Set<Map<String, String>> matchSet = Match.match(this.matchThreshold.floatValue(), this.scoreJena);
		// Set<Map<String, String>> solSet = getNewSolSet();
		List<Map<String, String>> solSet = new ArrayList<Map<String, String>>();
		
		if (matchSet.isEmpty()) {
			log.info("No Results Found");
		} else {
			log.info("Building Record Set");
			Map<String, String> tempMap;
			for (Map<String, String> entry : matchSet) {
				String sinputuri = entry.get("sInputURI");
				String svivouri = entry.get("sVivoURI");
				log.trace("Potential Match: <" + sinputuri + "> to <" + svivouri + ">");
				tempMap = new HashMap<String, String>();
				tempMap.put("sInput", sinputuri);
				Resource sInput = this.inputJena.getJenaModel().getResource(sinputuri);
				tempMap.put("sVivo", svivouri);
				Resource sVivo = this.vivoJena.getJenaModel().getResource(svivouri);
				for (String runName : this.vivoPredicates.keySet()) {
					 
					Property os_runName = this.inputJena.getJenaModel().getProperty(this.inputPredicates.get(runName));
					Statement os_stmnt = sInput.getProperty(os_runName);
					RDFNode os = null;
					if (os_stmnt != null) {
						os = os_stmnt.getObject();
					}
					Property op_runName = this.vivoJena.getJenaModel().getProperty(this.vivoPredicates.get(runName));
					Statement op_stmnt = sVivo.getProperty(op_runName);
					RDFNode op = null;
					if (op_stmnt != null) {
						op = op_stmnt.getObject();
					}
					
					addRunName(tempMap, runName, os, op);
					 
				}
				log.trace("Adding to solSet"); 
				solSet.add(tempMap);
			}
			
		}
		stopWatch.stop();
		log.trace("BuildFilterSolutionSet took this much time: "+stopWatch.getTime());
		log.info("Finished building Record Set");
		log.info("Added this many records: "+ solSet.size());
		return solSet;
	}
	
	/**
	 * Add the os and op nodes to tempMap for runName
	 * @param tempMap the map to add to
	 * @param runName the runName
	 * @param os the os
	 * @param op the op
	 */
	private static void addRunName(Map<String,String> tempMap, String runName, RDFNode os, RDFNode op) {
		if((os != null) && os.isResource()) {
			tempMap.put("URI_os_" + runName, os.asResource().getURI());
		} else if((os != null) && os.isLiteral()) {
			tempMap.put("LIT_os_" + runName, os.asLiteral().getValue().toString());
		}
		if((op != null) && op.isResource()) {
			tempMap.put("URI_op_" + runName, op.asResource().getURI());
		} else if((op != null) && op.isLiteral()) {
			tempMap.put("LIT_op_" + runName, op.asLiteral().getValue().toString());
		}
	}
	
	

	/**
	 * Execute score object algorithms
	 * @throws IOException error connecting
	 */
	public void execute() throws IOException {
		List<Map<String, String>> solSet = buildSolutionSet();
		Collections.sort(solSet, new MapComparator());
		
		if(!solSet.isEmpty()) {
			log.info("Processing Results");
			int total = solSet.size();
			int count = 0;
			int incrementer = 0;
			double recordBatchSize = Math.ceil(this.batchSize / (2.0+(this.vivoPredicates.size()*7)));
			StringBuilder indScore;
			StringBuilder scoreSparql = new StringBuilder();
			for(Map<String, String> eval : solSet) {
				count++;
				incrementer++;
				indScore = new StringBuilder();
				String sInputURI = eval.get("sInput");
				String sVivoURI = eval.get("sVivo");
				float percent = Math.round(10000f * count / total) / 100f;
				log.debug("(" + count + "/" + total + ": " + percent + "%): Evaluating <" + sInputURI + "> from inputJena as match for <" + sVivoURI + "> from vivoJena");
				// Build Score Record
				indScore.append("" +
					"  _:node" + incrementer + " scoreValue:VivoRes <" + sVivoURI + "> .\n" +
					"  _:node" + incrementer + " scoreValue:InputRes <" + sInputURI + "> .\n"
				);
				double sum_total = 0;
				for(String runName : this.vivoPredicates.keySet()) {
					String osUri = eval.get("URI_os_" + runName);
					String osLit = eval.get("LIT_os_" + runName);
					String opUri = eval.get("URI_op_" + runName);
					String opLit = eval.get("LIT_op_" + runName);
					//log.trace("os_" + runName + ": '" + ((osUri != null) ? osUri : osLit) + "'");
					//log.trace("op_" + runName + ": '" + ((opUri != null) ? opUri : opLit) + "'");
					sum_total += appendScoreSparqlFragment(indScore, incrementer, opUri, opLit, osUri, osLit, runName);
				}
				log.debug("sum_total: "+sum_total);
				//log.trace("Scores for inputJena node <" + sInputURI + "> to vivoJena node <" + sVivoURI + ">:\n" + indScore.toString());
				scoreSparql.append(indScore);
				if(incrementer == recordBatchSize) {
					loadRdfToScoreData(scoreSparql.toString());
					incrementer = 0;
					scoreSparql = new StringBuilder();
				}
			}
			if(incrementer > 0) {
				loadRdfToScoreData(scoreSparql.toString());
			}
			log.info("Result Processing Complete");
		}
		this.scoreJena.sync();
	}
	
	/**
	 * Load a batch of scoring data to the score model
	 * @param scores the score rdf/xml fragments
	 * @throws IOException error connecting
	 */
	private void loadRdfToScoreData(String scores) throws IOException {
		String sparql = "" +
			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
			"PREFIX scoreValue: <http://vivoweb.org/harvester/scoreValue/> \n" +
			"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n" +
			"INSERT DATA {\n" +
				scores +
			"}";
		// Push Score Data into score model
		//log.trace("Loading Score Data into Score Model:\n" + sparql);
		this.scoreJena.executeUpdateQuery(sparql);
	}
	
	/**
	 * Builds the select query for equality only mode
	 * @return the equality only mode query
	 */
	private String buildEqualitySelectQuery() {
		//Build query to find all nodes matching on the given predicates
		StringBuilder sQuery = new StringBuilder("PREFIX scoring: <http://vivoweb.org/harvester/model/scoring#>\n" + "SELECT DISTINCT ?sVivo ?sInput");
		
		List<String> filters = new ArrayList<String>();
		List<String> vivoSelects = new ArrayList<String>();
		List<String> inputSelects = new ArrayList<String>();
		
		for(String runName : this.inputPredicates.keySet()) {
			String vivoProperty = this.vivoPredicates.get(runName);
			String inputProperty = this.inputPredicates.get(runName);
			sQuery.append(" ?os_" + runName);
			sQuery.append(" ?op_" + runName);
			vivoSelects.add("?sVivo <" + vivoProperty + "> ?op_" + runName + " .");
			inputSelects.add("?sInput <" + inputProperty + "> ?os_" + runName + " .");
			filters.add("(str(?os_" + runName + ") = str(?op_" + runName + "))");
			//filters.add("(SAMETERM(?os_" + runName + ",?op_" + runName + ")");
			filters.add("(str(?os_" + runName + ") != str(\"\"))");
			filters.add("(str(?op_" + runName + ") != str(\"\"))");
		}
		
		sQuery.append("\n" + "FROM NAMED <http://vivoweb.org/harvester/model/scoring#vivoClone>\n" + "FROM NAMED <http://vivoweb.org/harvester/model/scoring#inputClone>\n" + "WHERE {\n");
		sQuery.append("  GRAPH scoring:vivoClone {\n    ");
		sQuery.append(StringUtils.join(vivoSelects, "\n    "));
		sQuery.append("\n  } . \n  GRAPH scoring:inputClone {\n" + "    ");
		sQuery.append(StringUtils.join(inputSelects, "\n    "));
		sQuery.append("\n  } . \n  FILTER( (");
		sQuery.append(StringUtils.join(filters, " && "));
		sQuery.append(") && (str(?sVivo) != str(?sInput))");
		if(this.namespace != null) {
			sQuery.append(" && regex(str(?sInput), \"^" + this.namespace + "\")");
			//sQuery.append(" && STRSTARTS(str(?sInput), \"^" + this.namespace + "\")");
		}
		sQuery.append(" ) .\n");
		sQuery.append("}");
		return sQuery.toString();
	}
	/*private String buildEqualitySelectQuery() {
		//Build query to find all nodes matching on the given predicates
		StringBuilder sQuery = new StringBuilder("PREFIX scoring: <http://vivoweb.org/harvester/model/scoring#>\n" + "SELECT DISTINCT ?sVivo ?sInput");
		
		List<String> filters = new ArrayList<String>();
		List<String> vivoSelects = new ArrayList<String>();
		List<String> inputSelects = new ArrayList<String>();
		
		for (String runName : this.inputPredicates.keySet()) {
			String vivoProperty = this.vivoPredicates.get(runName);
			String inputProperty = this.inputPredicates.get(runName);
			
			sQuery.append(" ?os_" + runName);
			sQuery.append(" ?op_" + runName);
			vivoSelects.add("?sVivo <" + vivoProperty + "> ?op_" + runName + " .");
			inputSelects.add("?sInput <" + inputProperty + "> ?os_" + runName + " . ");
			
			filters.add("(str(?os_" + runName + ") = str(?op_" + runName + "))");
			//filters.add("(SAMETERM(?os_" + runName + ",?op_" + runName + ")");
			filters.add("(str(?os_" + runName + ") != str(\"\"))");
			filters.add("(str(?op_" + runName + ") != str(\"\"))");
		}
		
		sQuery.append("\n" + "FROM NAMED <http://vivoweb.org/harvester/model/scoring#vivoClone>\n" + "FROM NAMED <http://vivoweb.org/harvester/model/scoring#inputClone>\n" + "WHERE {\n");
		sQuery.append("  GRAPH scoring:vivoClone {\n    ");
		sQuery.append(StringUtils.join(vivoSelects, "\n    "));
		sQuery.append("\n  } . \n  GRAPH scoring:inputClone {\n" + "    ");
		sQuery.append(StringUtils.join(inputSelects, "\n    "));
		sQuery.append("\n  } . ");
		
		if (this.namespace != null) { 
			sQuery.append("\n FILTER(  (regex(str(?sInput), \"^" + this.namespace + "\"))) ."); 
		}
		sQuery.append("\n FILTER( (");
		sQuery.append(StringUtils.join(filters, " && "));
		sQuery.append(") && (str(?sVivo) != str(?sInput))");
		//if(this.namespace != null) {
		//	sQuery.append(" && regex(str(?sInput), \"^" + this.namespace + "\")");
			//sQuery.append(" && STRSTARTS(str(?sInput), \"^" + this.namespace + "\")");
		//}
		sQuery.append(" ) .\n");
		sQuery.append("}");
		return sQuery.toString();
	}*/
	
	/**
	 * Build the select query
	 * @return the query
	 */
	private String buildSelectQuery() {
		if(this.equalityOnlyMode) {
			return buildEqualitySelectQuery();
		}
		//Build query to find all nodes matching on the given predicates
		StringBuilder sQuery = new StringBuilder("PREFIX scoring: <http://vivoweb.org/harvester/model/scoring#>\n" + "SELECT DISTINCT ?sVivo ?sInput");
		
		StringBuilder vivoOptionals = new StringBuilder();
		StringBuilder inputOptionals = new StringBuilder();
		List<String> filters = new ArrayList<String>();
		List<String> vivoUnions = new ArrayList<String>();
		List<String> inputUnions = new ArrayList<String>();
		
		for(String runName : this.inputPredicates.keySet()) {
			String vivoProperty = this.vivoPredicates.get(runName);
			String inputProperty = this.inputPredicates.get(runName);
			sQuery.append(" ?os_" + runName);
//			sQuery.append(" ?ov_" + runName);
			sQuery.append(" ?op_" + runName);
//			sQuery.append(" ?oi_" + runName);
			vivoUnions.add("{ ?sVivo <" + vivoProperty + "> ?ov_" + runName + " }");
			vivoOptionals.append("    OPTIONAL { ?sVivo <").append(vivoProperty).append("> ").append("?op_" + runName).append(" } . \n");
			inputUnions.add("{ ?sInput <" + inputProperty + "> ?oi_" + runName + " }");
			inputOptionals.append("    " + "OPTIONAL { " + "?sInput <").append(inputProperty).append("> ").append("?os_" + runName).append(" }" + " . \n");
			filters.add("(str(?os_" + runName + ") = str(?ov_" + runName + "))");
			//filters.add("(SAMETERM(?os_" + runName + ",?ov_" + runName + "))");
		}
		
		sQuery.append("\n" + "FROM NAMED <http://vivoweb.org/harvester/model/scoring#vivoClone>\n" + "FROM NAMED <http://vivoweb.org/harvester/model/scoring#inputClone>\n" + "WHERE {\n");
		sQuery.append("  GRAPH scoring:vivoClone {\n    ");
		sQuery.append(StringUtils.join(vivoUnions, " UNION \n    "));
		sQuery.append(" . \n");
		sQuery.append(vivoOptionals.toString());
		sQuery.append("  } . \n");
		sQuery.append("  GRAPH scoring:inputClone {\n" + "    ");
		sQuery.append(StringUtils.join(inputUnions, " UNION \n    "));
		sQuery.append(" . \n");
		sQuery.append(inputOptionals.toString());
		sQuery.append("  } . \n");
		sQuery.append("  FILTER( (");
		sQuery.append(StringUtils.join(filters, " || "));
		sQuery.append(") && (str(?sVivo) != str(?sInput))");
		if(this.namespace != null) {
			sQuery.append(" && regex(str(?sInput), \"^" + this.namespace + "\")"); 
		}
		sQuery.append(" ) .\n");
		 
		sQuery.append("}");
		return sQuery.toString();
	}
	/*private String buildSelectQuery() {
		if (this.equalityOnlyMode) {
			return buildEqualitySelectQuery();
		}
		//Build query to find all nodes matching on the given predicates
		StringBuilder sQuery = new StringBuilder("PREFIX scoring: <http://vivoweb.org/harvester/model/scoring#>\n" + "SELECT DISTINCT ?sVivo ?sInput");
		
		StringBuilder vivoOptionals = new StringBuilder();
		StringBuilder inputOptionals = new StringBuilder();
		List<String> filters = new ArrayList<String>();
		List<String> vivoUnions = new ArrayList<String>();
		List<String> inputUnions = new ArrayList<String>();
		
		for(String runName : this.inputPredicates.keySet()) {
			String vivoProperty = this.vivoPredicates.get(runName);
			String inputProperty = this.inputPredicates.get(runName);
			sQuery.append(" ?os_" + runName);
//			sQuery.append(" ?ov_" + runName);
			sQuery.append(" ?op_" + runName);
//			sQuery.append(" ?oi_" + runName);
			vivoUnions.add("{ ?sVivo <" + vivoProperty + "> ?ov_" + runName + " }");
			vivoOptionals.append("    OPTIONAL { ?sVivo <").append(vivoProperty).append("> ").append("?op_" + runName).append(" } . \n");
			inputUnions.add("{ ?sInput <" + inputProperty + "> ?oi_" + runName + " }");
			inputOptionals.append("    " + "OPTIONAL { " + "?sInput <").append(inputProperty).append("> ").append("?os_" + runName).append(" }" + " . \n");
			filters.add("(str(?os_" + runName + ") = str(?ov_" + runName + "))");
			//filters.add("(SAMETERM(?os_" + runName + ",?ov_" + runName + "))");
		}
		
		sQuery.append("\n" + "FROM NAMED <http://vivoweb.org/harvester/model/scoring#vivoClone>\n" + "FROM NAMED <http://vivoweb.org/harvester/model/scoring#inputClone>\n" + "WHERE {\n");
		sQuery.append("  GRAPH scoring:vivoClone {\n    ");
		sQuery.append(StringUtils.join(vivoUnions, " UNION \n    "));
		sQuery.append(" . \n");
		sQuery.append(vivoOptionals.toString());
		sQuery.append("  } . \n");
		sQuery.append("  GRAPH scoring:inputClone {\n" + "    ");
		sQuery.append(StringUtils.join(inputUnions, " UNION \n    "));
		sQuery.append(" . \n");
		sQuery.append(inputOptionals.toString());
		sQuery.append("  } . \n");
		if (this.namespace != null) { 
			sQuery.append("\n FILTER(  (regex(str(?sInput), \"^" + this.namespace + "\"))) ."); 
		}
		sQuery.append("\n  FILTER( (");
		sQuery.append(StringUtils.join(filters, " || "));
		sQuery.append(") && (str(?sVivo) != str(?sInput))");
		 
		sQuery.append(" ) .\n");
		 
		sQuery.append("}");
		return sQuery.toString();
	}*/
	
	/**
	 * Append the sparql fragment for two rdf nodes to the given stringbuilder
	 * @param sb the stringbuilder to append fragment to
	 * @param nodenum the node number
	 * @param opUri vivoJena node as a URI
	 * @param opLit vivoJena node as a Literal string
	 * @param osUri inputJena node as a URI
	 * @param osLit inputJena node as a Literal string
	 * @param runName the run identifier
	 * @return the score
	 */
	private double appendScoreSparqlFragment(StringBuilder sb, int nodenum, String opUri, String opLit, String osUri, String osLit, String runName) {
		float score = 0f;
		// if a resource and same uris
		if(this.equalityOnlyMode || ((osUri != null) && (opUri != null) && osUri.equals(opUri))) {
			score = 1 / 1f;
		} else if((osLit != null) && (opLit != null)) {
			Class<? extends Algorithm> algClass = this.algorithms.get(runName);
			try {
				if (this.commonNames == null) {
					score = algClass.newInstance().calculate(osLit, opLit);
				} else { 
					score = algClass.newInstance().calculate(osLit, opLit, this.commonNames);
				}
			} catch(IllegalAccessException e) {
				throw new IllegalArgumentException("Unable to create new instance of class <"+algClass+">, does it not have a default (no-params) constructor publically available?", e);
			} catch(InstantiationException e) {
				throw new IllegalArgumentException(e);
			}
		}
		double weightedscore = this.weights.get(runName).doubleValue() * score;
		log.debug("score: " + score);
		log.debug("weighted_score: " + weightedscore);
		String fragment = "" +
			"  _:node" + nodenum + " scoreValue:hasScoreValue _:nodeScoreValue" + runName + nodenum + " .\n" +
			"  _:nodeScoreValue" + runName + nodenum + " scoreValue:VivoProp <" + this.vivoPredicates.get(runName) + "> .\n" +
			"  _:nodeScoreValue" + runName + nodenum + " scoreValue:InputProp <" + this.inputPredicates.get(runName) + "> .\n" +
			"  _:nodeScoreValue" + runName + nodenum + " scoreValue:Algorithm \"" + this.algorithms.get(runName).getName() + "\" .\n" +
			"  _:nodeScoreValue" + runName + nodenum + " scoreValue:Score \"" + score + "\"^^xsd:float .\n" +
			"  _:nodeScoreValue" + runName + nodenum + " scoreValue:Weight \"" + this.weights.get(runName) + "\"^^xsd:float .\n" +
			"  _:nodeScoreValue" + runName + nodenum + " scoreValue:WeightedScore \"" + weightedscore + "\"^^xsd:float .\n";
		sb.append(fragment);
		return weightedscore;
	}
	
	/**
	 * Main method
	 * @param args command line arguments
	 */
	public static void main(String... args) {
		Exception error = null;
		try {
			InitLog.initLogger(args, getParser());
			log.info(getParser().getAppName() + ": Start");
			new Score2(args).execute();
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
	
	/**
	 * @author jaf30
	 * Comparator for result set
	 */
	class MapComparator implements Comparator<Map<String, String>>{ 
		 
		@Override
		public int compare(Map<String, String> o1, Map<String, String> o2) {
			StringBuilder o1sb = new StringBuilder();
			o1sb.append(o1.get("sInput")).append(o1.get("sVivo"));
			StringBuilder o2sb = new StringBuilder();
			o2sb.append(o2.get("sInput")).append(o2.get("sVivo"));
			return o1sb.toString().compareTo(o2sb.toString());
		}
	}

	
}
