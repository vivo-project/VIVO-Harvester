/*******************************************************************************
 * Copyright (c) 2010-2011 VIVO Harvester Team. For full list of contributors, please see the AUTHORS file provided.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the new BSD license which accompanies this distribution, and is available at http://www.opensource.org/licenses/bsd-license.html
 ******************************************************************************/
package org.vivoweb.harvester.translate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vivoweb.harvester.util.FileAide;
import org.vivoweb.harvester.util.InitLog;
import org.vivoweb.harvester.util.args.ArgDef;
import org.vivoweb.harvester.util.args.ArgList;
import org.vivoweb.harvester.util.args.ArgParser;
import org.vivoweb.harvester.util.args.UsageException;
import org.vivoweb.harvester.util.repo.Record;
import org.vivoweb.harvester.util.repo.RecordHandler;

/**
 * Takes XML Files and uses an XSL file to translate the data into the desired ontology
 * @author Stephen V. Williams swilliams@ctrip.ufl.edu
 */
public class XSLTranslator {
	/**
	 * the log property for logging errors, information, debugging
	 */
	private static Logger log = LoggerFactory.getLogger(XSLTranslator.class);
	/**
	 * The translation xsl is the map that will reconstruct our input stream's document into the appropriate format
	 */
	private String translationString;
	/**
	 * the translation xsl, compiled once and reused for every record
	 */
	private Templates translationTemplates;
	/**
	 * record handler for incoming records
	 */
	protected RecordHandler inStore;
	/**
	 * record handler for storing records
	 */
	protected RecordHandler outStore;
	/**
	 * force process records
	 */
	private boolean force;
	
	/**
	 * force decode input as UTF-8 to clean XML
	 */
	private boolean cleanXML;
	
	/**
	 * Constructor
	 * @param args commandline arguments
	 * @throws IOException error creating task
	 * @throws UsageException user requested usage message
	 */
	private XSLTranslator(String[] args) throws IOException, UsageException {
		this(getParser().parse(args));
	}
	
	/**
	 * Constructor
	 * @param argList <ul>
	 *        <li>translationStream the file that details the translation from the original xml to the target format</li>
	 *        <li>inRecordHandler the files/records that require translation</li>
	 *        <li>outRecordHandler the output record for the translated files</li>
	 *        <li>force translate all input records, even if previously processed</li>
	 *        </ul>
	 * @throws IOException error reading files
	 */
	private XSLTranslator(ArgList argList) throws IOException {
		this(
			RecordHandler.parseConfig(argList.get("i"), argList.getValueMap("I")), 
			RecordHandler.parseConfig(argList.get("o"), argList.getValueMap("O")),  
			FileAide.getInputStream(argList.get("x")), 
			argList.has("f"),
			argList.has("c")
		);
	}
	
	/**
	 * Constructor
	 * @param translationStream the file that details the translation from the original xml to the target format</li>
	 * @param inRecordHandler the files/records that require translation
	 * @param outRecordHandler the output record for the translated files
	 * @param force translate all input records, even if previously processed
	 * @param clXML if we should decode XML to clean it
	 * @throws IOException error reading files
	 */
	public XSLTranslator(RecordHandler inRecordHandler, RecordHandler outRecordHandler, InputStream translationStream, 
							boolean force, boolean clXML) throws IOException {
		// set Translation file
		setTranslation(translationStream);
		
		// create record handlers
		this.inStore = inRecordHandler;
		this.outStore = outRecordHandler;
		this.force = force;
		this.cleanXML = clXML;
		if(this.inStore == null) {
			throw new IllegalArgumentException("Must provide an input record handler");
		}
		if(this.outStore == null) {
			throw new IllegalArgumentException("Must provide an output record handler");
		}
	}
	
	/**
	 * Set translation file from an inputSteam
	 * @param transFileStream valid type of translation file is xslt
	 * @throws IOException error reading from stream
	 */
	public void setTranslation(InputStream transFileStream) throws IOException {
		// copy xsl into memory for faster translations
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		IOUtils.copy(transFileStream, baos);
		this.translationString = baos.toString();
		// the stylesheet changed, so any previously compiled form is stale
		this.translationTemplates = null;
	}
	
	/**
	 * Set translation file from a file
	 * @param transFile valid type of translation file is xslt
	 * @throws IOException error reading from file
	 */
	public void setTranslation(File transFile) throws IOException {
		setTranslation(new FileInputStream(transFile));
	}
	
	/**
	 * checks again for the necessary file and makes sure that they exist
	 * @throws IOException error processing
	 */
	public void execute() throws IOException {
		// get from the in record and translate
		int translated = 0;
		int passed = 0;
		String recordData;
		
		for(Record r : this.inStore) {
			if(this.force || r.needsProcessed(this.getClass())) {
				log.trace("Translating Record " + r.getID());
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				recordData = (this.cleanXML) ? URLDecoder.decode(r.getData(), "UTF-8") : r.getData();
				xmlTranslate(new ByteArrayInputStream(recordData.getBytes(StandardCharsets.UTF_8)), baos);
				this.outStore.addRecord(r.getID(), baos.toString("UTF-8"), this.getClass());
				r.setProcessed(this.getClass());
				baos.close();
				translated++;
			} else {
				log.trace("No Translation Needed: " + r.getID());
				passed++;
			}
		}
		log.info(Integer.toString(translated) + " records translated.");
		log.info(Integer.toString(passed) + " records did not need translation");
	}
	
	/**
	 * Get the compiled form of the translation xsl, compiling it on first use
	 * @return the compiled stylesheet
	 * @throws IOException error compiling the stylesheet
	 */
	private Templates getTranslationTemplates() throws IOException {
		if(this.translationTemplates == null) {
			Source xslSource = new StreamSource(new ByteArrayInputStream(this.translationString.getBytes(StandardCharsets.UTF_8)));
			try {
				// compile once: recompiling per record dominates both runtime and heap on large harvests
				this.translationTemplates = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null).newTemplates(xslSource);
			} catch(TransformerConfigurationException e) {
				throw new IOException(e);
			}
		}
		return this.translationTemplates;
	}
	
	/**
	 * translate a single record using this translator's compiled stylesheet
	 * @param inStream the input stream
	 * @param outStream the output stream
	 * @throws IOException error translating
	 */
	private void xmlTranslate(InputStream inStream, OutputStream outStream) throws IOException {
		try {
			Transformer transformer = getTranslationTemplates().newTransformer();
			transformer.transform(new StreamSource(inStream), new StreamResult(outStream));
		} catch(TransformerConfigurationException e) {
			throw new IOException(e);
		} catch(TransformerException e) {
			throw new IOException(e);
		}
		outStream.flush();
	}
	
	/**
	 * using the javax xml transform factory this method uses the xsl to translate XML into the desired format
	 * designated in the xsl.
	 * @param inStream the input stream
	 * @param outStream the output stream
	 * @param translationStream the stream for the xsl
	 * @throws IOException error translating
	 */
	public static void xmlTranslate(InputStream inStream, OutputStream outStream, InputStream translationStream) throws IOException {
		StreamResult outputResult = new StreamResult(outStream);
		// JAXP reads data using the Source interface
		Source xmlSource = new StreamSource(inStream);
		Source xslSource = new StreamSource(translationStream);
		try {
			// the factory pattern supports different XSLT processors
			// this outputs to outStream (through outputResult)
			TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null).newTransformer(xslSource).transform(xmlSource, outputResult);
		} catch(TransformerConfigurationException e) {
			throw new IOException(e);
		} catch(TransformerException e) {
			throw new IOException(e);
		}
		outStream.flush();
	}
	
	/**
	 * Get the ArgParser for this task
	 * @return the ArgParser
	 */
	private static ArgParser getParser() {
		ArgParser parser = new ArgParser("XSLTranslator");
		parser.addArgument(new ArgDef().setShortOption('i').setLongOpt("input").withParameter(true, "CONFIG_FILE").setDescription("config file for input record handler").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('I').setLongOpt("inputOverride").withParameterValueMap("RH_PARAM", "VALUE").setDescription("override the RH_PARAM of input recordhandler using VALUE").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('o').setLongOpt("output").withParameter(true, "CONFIG_FILE").setDescription("config file for output record handler").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('O').setLongOpt("outputOverride").withParameterValueMap("RH_PARAM", "VALUE").setDescription("override the RH_PARAM of output recordhandler using VALUE").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('x').setLongOpt("xslFile").withParameter(true, "XSL_FILE").setDescription("xsl file").setRequired(true));
		parser.addArgument(new ArgDef().setShortOption('f').setLongOpt("force").setDescription("force translation of all input records, even if previously processed").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('c').setLongOpt("cleanXML").setDescription("Decode and sanitize XML").setRequired(false));
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
			new XSLTranslator(args).execute();
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
