/*******************************************************************************
 * Copyright (c) 2010-2011 VIVO Harvester Team. For full list of contributors, please see the AUTHORS file provided.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the new BSD license which accompanies this distribution, and is available at http://www.opensource.org/licenses/bsd-license.html
 ******************************************************************************/
package org.vivoweb.harvester.fetch;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vivoweb.harvester.util.InitLog;
import org.vivoweb.harvester.util.SpecialEntities;
import org.vivoweb.harvester.util.args.ArgDef;
import org.vivoweb.harvester.util.args.ArgList;
import org.vivoweb.harvester.util.args.ArgParser;
import org.vivoweb.harvester.util.args.UsageException;
import org.vivoweb.harvester.util.repo.RecordHandler;

/**
 * Fetches rdf data from a JDBC database placing the data in the supplied record handler.
 * @author Christopher Haines (hainesc@ctrip.ufl.edu)
 */
public class JDBCFetch {
	/**
	 * SLF4J Logger
	 */
	private static Logger log = LoggerFactory.getLogger(JDBCFetch.class);
	/**
	 * Record Handler to write records to
	 */
	private RecordHandler rh;
	/**
	 * Statement processor for the database
	 */
	private Statement cursor;
	/**
	 * Mapping of tablename to idField name
	 */
	private Map<String, List<String>> idFields = null;
	/**
	 * Mapping of tablename to mapping of fieldname to tablename
	 */
	private Map<String, Map<String, String>> fkRelations = null;
	/**
	 * Mapping of tablename to list of datafields
	 */
	private Map<String, List<String>> dataFields = null;
	/**
	 * Set of table names
	 */
	private Set<String> tableNames = null;
	/**
	 * List of conditions
	 */
	private Map<String, List<String>> whereClauses;
	/**
	 * Mapping of extra tables for the from section
	 */
	private Map<String, String> fromClauses;
	/**
	 * Namespace for RDF made from this database
	 */
	private String uriNS;
	/**
	 * Prefix each field in query with this
	 */
	private String queryPre;
	/**
	 * Suffix each field in query with this
	 */
	private String querySuf;
	/**
	 * The user defined SQL Query string
	 */
	private Map<String, String> queryStrings;
	/**
	 * accepted table types
	 */
	private String[] tableTypes;
	
	/**
	 * Constructor
	 * @param dbConn connection to the database
	 * @param output RecordHandler to write data to
	 * @param uriNameSpace namespace base for rdf records
	 * @throws IOException error talking with database
	 */
	public JDBCFetch(Connection dbConn, RecordHandler output, String uriNameSpace) throws IOException {
		this(dbConn, output, uriNameSpace, null, null, null, null, null, null, null, null, null);
	}
	
	/**
	 * Constructor
	 * @param driverClass the jdbc driver
	 * @param connLine the jdbc connection line
	 * @param username the username
	 * @param password the password
	 * @param output RecordHandler to write data to
	 * @param uriNameSpace namespace base for rdf records
	 * @throws IOException error talking with database
	 */
	public JDBCFetch(String driverClass, String connLine, String username, String password, RecordHandler output, String uriNameSpace) throws IOException {
		this(driverClass, connLine, username, password, output, uriNameSpace, null, null, null, null, null, null, null, null, null);
	}
	
	/**
	 * Command line Constructor
	 * @param args commandline arguments
	 * @throws IOException error creating task
	 * @throws UsageException user requested usage message
	 */
	private JDBCFetch(String[] args) throws IOException, UsageException {
		this(getParser().parse(args));
	}
	
	/**
	 * Arglist Constructor
	 * @param args option set of parsed args
	 * @throws IOException error creating task
	 */
	private JDBCFetch(ArgList args) throws IOException {
		this(
			args.get("d"),
			args.get("c"),
			args.get("u"),
			args.get("p"), 
			RecordHandler.parseConfig(args.get("o"), args.getValueMap("O")), 
			(args.has("n")?args.get("n"):(args.get("c")+"/")), 
			args.get("delimiterPrefix"), 
			args.get("delimiterSuffix"), 
			new TreeSet<String>(args.getAll("t")), 
			(args.has("T")?args.getValueMap("T"):null), 
			(args.has("F")?splitCommaList(args.getValueMap("F")):null), 
			(args.has("I")?splitCommaList(args.getValueMap("I")):null), 
			(args.has("W")?splitCommaList(args.getValueMap("W")):null), 
			(args.has("R")?splitTildeMap(args.getValueMap("R")):null), 
			(args.has("Q")?args.getValueMap("Q"):null),
			args.getAll("v").toArray(new String[]{})
		);
	}
	
	/**
	 * Library style Constructor
	 * @param dbConn The database Connection
	 * @param rh Record Handler to write records to
	 * @param uriNS the uri namespace to use
	 * @param queryPre Query prefix often "["
	 * @param querySuf Query suffix often "]"
	 * @param tableNames set of the table names
	 * @param fromClauses Mapping of extra tables for the from section
	 * @param dataFields Mapping of tablename to list of datafields
	 * @param idFields Mapping of tablename to idField name
	 * @param whereClauses List of conditions
	 * @param relations Mapping of tablename to mapping of fieldname to tablename
	 * @param queryStrings Mapping of tablename to query
	 * @param tableTypes accepted table types
	 * @throws IOException error accessing database
	 */
	public JDBCFetch(Connection dbConn, RecordHandler rh, String uriNS, String queryPre, String querySuf, Set<String> tableNames, Map<String, String> fromClauses, Map<String, List<String>> dataFields, Map<String, List<String>> idFields, Map<String, List<String>> whereClauses, Map<String, Map<String, String>> relations, Map<String, String> queryStrings, String... tableTypes) throws IOException {
		try {
			this.cursor = dbConn.createStatement();
		} catch(SQLException e) {
			throw new IOException(e);
		}
		this.rh = rh;
		Set<String> argTables = tableNames;
		this.fromClauses = fromClauses;
		this.dataFields = dataFields;
		this.idFields = idFields;
		this.whereClauses = whereClauses;
		this.fkRelations = relations;
		this.uriNS = uriNS;
		this.queryPre = queryPre;
		this.querySuf = querySuf;
		this.queryStrings = queryStrings;
		if(tableTypes.length > 0 && tableTypes[0] != null) {
			this.tableTypes = tableTypes;
			for(int x=0; x<this.tableTypes.length; x++) {
				this.tableTypes[x] = this.tableTypes[x].toUpperCase();
			}
		} else {
			this.tableTypes = new String[]{"TABLE"};
		}
		
		if(this.rh == null) {
			throw new IllegalArgumentException("Must provide output recordhandler!");
		}
		
		if(argTables == null) {
			argTables = new TreeSet<String>();
		}
		
		if(this.fromClauses != null) {
			argTables.addAll(this.fromClauses.keySet());
		}
		
		if(this.dataFields != null) {
			argTables.addAll(this.dataFields.keySet());
		}
		
		if(this.idFields != null) {
			argTables.addAll(this.idFields.keySet());
		}
		
		if(this.whereClauses != null) {
			argTables.addAll(this.whereClauses.keySet());
		}
		
		if(this.fkRelations != null) {
			argTables.addAll(this.fkRelations.keySet());
		}
		
		if(this.queryStrings != null) {
			// tablenames for queries are arbitrary
			argTables.removeAll(this.queryStrings.keySet());
		}
		

		this.tableNames = new TreeSet<String>();
		try {
			this.tableNames = getTableNames();
		} catch(SQLException e) {
			throw new IOException(e);
		}
		Set<String> realDBTables = this.tableNames;
		
		this.tableNames = new TreeSet<String>();
		//TODO: this is required as getTableNames loads data in and we want this to be a fresh start
		// there should be a nicer way to do all of this, as this is very hacky at the moment
		for(String argTable : argTables) {
			boolean found = false;
			for(String realTableName : realDBTables) {
				realTableName = realTableName.trim();
				if(argTable.trim().equalsIgnoreCase(realTableName)) {
					this.tableNames.add(realTableName);
					// fix the tablename in all the other structures >.>
					if(this.fromClauses != null) {
						Map<String, String> tempMap = new HashMap<String, String>();
						for(String fromClausesTable : this.fromClauses.keySet()) {
							if(fromClausesTable.trim().equalsIgnoreCase(realTableName)) {
								tempMap.put(realTableName, this.fromClauses.get(fromClausesTable));
							} else {
								tempMap.put(fromClausesTable, this.fromClauses.get(fromClausesTable));
							}
						}
						this.fromClauses = tempMap;
					}
					if(this.dataFields != null) {
						Map<String, List<String>> tempMap = this.dataFields;
						for(String dataFieldsTable : this.dataFields.keySet()) {
							if(dataFieldsTable.trim().equalsIgnoreCase(realTableName)) {
								tempMap.put(realTableName, this.dataFields.get(dataFieldsTable));
							} else {
								tempMap.put(dataFieldsTable, this.dataFields.get(dataFieldsTable));
							}
						}
						this.dataFields = tempMap;
					}
					if(this.idFields != null) {
						Map<String, List<String>> tempMap = this.idFields;
						for(String idFieldsTable : this.idFields.keySet()) {
							if(idFieldsTable.trim().equalsIgnoreCase(realTableName)) {
								tempMap.put(realTableName, this.idFields.get(idFieldsTable));
							} else {
								tempMap.put(idFieldsTable, this.idFields.get(idFieldsTable));
							}
						}
						this.idFields = tempMap;
					}
					if(this.whereClauses != null) {
						Map<String, List<String>> tempMap = this.whereClauses;
						for(String whereClausesTable : this.whereClauses.keySet()) {
							if(whereClausesTable.trim().equalsIgnoreCase(realTableName)) {
								tempMap.put(realTableName, this.whereClauses.get(whereClausesTable));
							} else {
								tempMap.put(whereClausesTable, this.whereClauses.get(whereClausesTable));
							}
						}
						this.whereClauses = tempMap;
					}
					if(this.fkRelations != null) {
						Map<String, Map<String, String>> tempMap = this.fkRelations;
						for(String fkRelationsTable : this.fkRelations.keySet()) {
							if(fkRelationsTable.trim().equalsIgnoreCase(realTableName)) {
								tempMap.put(realTableName, this.fkRelations.get(fkRelationsTable));
							} else {
								tempMap.put(fkRelationsTable, this.fkRelations.get(fkRelationsTable));
							}
						}
						this.fkRelations = tempMap;
					}
					found = true;
					break;
				}
			}
			if(!found) {
				throw new IllegalArgumentException("Database Does Not Contain A Table Named '"+argTable+"'");
			}
		}
		
		if(this.queryStrings != null) {
			this.tableNames.addAll(this.queryStrings.keySet());
		}
	}
	
	/**
	 * Library style Constructor
	 * @param driverClass the jdbc driver
	 * @param connLine the jdbc connection line
	 * @param username the username
	 * @param password the password
	 * @param rh Record Handler to write records to
	 * @param uriNS the uri namespace to use
	 * @param queryPre Query prefix often "["
	 * @param querySuf Query suffix often "]"
	 * @param tableNames set of the table names
	 * @param fromClauses Mapping of extra tables for the from section
	 * @param dataFields Mapping of tablename to list of datafields
	 * @param idFields Mapping of tablename to idField name
	 * @param whereClauses List of conditions
	 * @param relations Mapping of tablename to mapping of fieldname to tablename
	 * @param queryStrings Mapping of tablename to query
	 * @param tableTypes accepted table types
	 * @throws IOException error accessing database
	 */
	public JDBCFetch(String driverClass, String connLine, String username, String password, RecordHandler rh, String uriNS, String queryPre, String querySuf, Set<String> tableNames, Map<String, String> fromClauses, Map<String, List<String>> dataFields, Map<String, List<String>> idFields, Map<String, List<String>> whereClauses, Map<String, Map<String, String>> relations, Map<String, String> queryStrings, String... tableTypes) throws IOException {
		this(createConnection(driverClass, connLine, username, password), rh, uriNS, queryPre, querySuf, tableNames, fromClauses, dataFields, idFields, whereClauses, relations, queryStrings, tableTypes);
	}
	
	/**
	 * Split the values of a comma separated list mapping
	 * @param list the original mapping
	 * @return the split mapping
	 */
	private static Map<String, List<String>> splitCommaList(Map<String, String> list) {
		Map<String, List<String>> splitList = new HashMap<String, List<String>>();
		for(String tableName : list.keySet()) {
			splitList.put(tableName, Arrays.asList(list.get(tableName).split("\\s?,\\s?")));
		}
		return splitList;
	}
	
	/**
	 * Split the values of comma separated ~ maps mapping
	 * @param list the original mapping
	 * @return the split mappings
	 */
	private static Map<String, Map<String, String>> splitTildeMap(Map<String, String> list) {
		Map<String, List<String>> splitList = splitCommaList(list);
		Map<String, Map<String, String>> splitMaps = new HashMap<String, Map<String, String>>();
		for(String tableName : splitList.keySet()) {
			if(!splitMaps.containsKey(tableName)) {
				splitMaps.put(tableName, new HashMap<String, String>());
			}
			for(String relLine : splitList.get(tableName)) {
				String[] relPair = relLine.split("\\s?~\\s?", 2);
				if(relPair.length != 2) {
					throw new IllegalArgumentException("Bad Relation Line: " + relLine);
				}
				splitMaps.get(tableName).put(relPair[0], relPair[1]);
			}
		}
		return splitMaps;
	}
	
	/**
	 * Create a connection
	 * @param driverClass the jdbc driver
	 * @param connLine the jdbc connection line
	 * @param username the username
	 * @param password the password
	 * @return the connection
	 * @throws IOException error connecting or loading driver
	 */
	private static Connection createConnection(String driverClass, String connLine, String username, String password) throws IOException {
		try {
			Class.forName(driverClass);
			return DriverManager.getConnection(connLine, username, password);
		} catch(SQLException e) {
			throw new IOException(e);
		} catch(ClassNotFoundException e) {
			throw new IOException(e);
		}
	}
	
	/**
	 * Get the field prefix
	 * @return the field prefix
	 */
	private String getFieldPrefix() {
		if(this.queryPre == null) {
			this.queryPre = getParser().getOptMap().get("delimiterPrefix").getDefaultValue();
		}
		return this.queryPre;
	}
	
	/**
	 * Set the field prefix
	 * @param fieldPrefix the field prefix to use
	 */
	public void setFieldPrefix(String fieldPrefix) {
		this.queryPre = fieldPrefix;
	}
	
	/**
	 * Get the field suffix
	 * @return the field suffix
	 */
	private String getFieldSuffix() {
		if(this.querySuf == null) {
			this.querySuf = getParser().getOptMap().get("delimiterSuffix").getDefaultValue();
		}
		return this.querySuf;
	}
	
	/**
	 * Set the field suffix
	 * @param fieldSuffix the field suffix to use
	 */
	public void setFieldSuffix(String fieldSuffix) {
		this.querySuf = fieldSuffix;
	}
	
	/**
	 * Get the data field information for a table from the database
	 * @param tableName the table to get the data field information for
	 * @return the data field list
	 * @throws SQLException error connecting to DB
	 */
	private List<String> getDataFields(String tableName) throws SQLException {
		// TODO: the part after the OR looks like it should be on the next if statement, look into this
		if((this.dataFields == null) || ((this.queryStrings != null) && this.queryStrings.containsKey(tableName))) {
			this.dataFields = new HashMap<String, List<String>>();
		}
		if(!this.dataFields.containsKey(tableName)) {
			log.debug("Finding data column names for table: "+tableName);
			this.dataFields.put(tableName, new LinkedList<String>());
			if((this.queryStrings == null) || !this.queryStrings.containsKey(tableName)) {
				ResultSet columnData = this.cursor.getConnection().getMetaData().getColumns(this.cursor.getConnection().getCatalog(), null, tableName, "%");
				while(columnData.next()) {
					String colName = columnData.getString("COLUMN_NAME");
					log.trace("Found data column: "+colName);
					if(!getFkRelationFields(tableName).containsKey(colName)) {
						this.dataFields.get(tableName).add(colName);
					}
				}
			}
		}
		return this.dataFields.get(tableName);
	}
	
	/**
	 * Get the relation field information for a table from the database
	 * @param tableName the table to get the relation field information for
	 * @return the relation field mapping
	 * @throws SQLException error connecting to DB
	 */
	private Map<String, String> getFkRelationFields(String tableName) throws SQLException {
		// TODO: the part after the OR looks like it should be on the next if statement, look into this
		if(this.fkRelations == null) {
			this.fkRelations = new HashMap<String, Map<String, String>>();
		}
		if(!this.fkRelations.containsKey(tableName)) {
			this.fkRelations.put(tableName, new HashMap<String, String>());
		}
		if((this.queryStrings == null || !this.queryStrings.containsKey(tableName)) && this.fkRelations.get(tableName).isEmpty()) {
			log.debug("Finding relation column names for table: "+tableName);
			if((this.queryStrings == null) || !this.queryStrings.containsKey(tableName)) {
				ResultSet foreignKeys = this.cursor.getConnection().getMetaData().getImportedKeys(this.cursor.getConnection().getCatalog(), null, tableName);
				while(foreignKeys.next()) {
					String colName = foreignKeys.getString("FKCOLUMN_NAME");
					String foreignTable = foreignKeys.getString("PKTABLE_NAME");
					log.trace("Found relation column: "+colName);
					log.trace("links to table: "+foreignTable);
					this.fkRelations.get(tableName).put(colName, foreignTable);
				}
			}
		}
		return this.fkRelations.get(tableName);
	}
	
	/**
	 * Get the where clauses for a table from the database
	 * @param tableName the table to get the where clauses for
	 * @return the where clauses
	 */
	private List<String> getWhereClauses(String tableName) {
		if(this.whereClauses == null) {
			this.whereClauses = new HashMap<String, List<String>>();
		}
		if(!this.whereClauses.containsKey(tableName)) {
			this.whereClauses.put(tableName, new LinkedList<String>());
		}
		return this.whereClauses.get(tableName);
	}
	
	/**
	 * Get the id field list for a table from the database
	 * @param tableName the table to get the id field list for
	 * @return the id field list
	 * @throws SQLException error connecting to DB
	 */
	private List<String> getIDFields(String tableName) throws SQLException {
		if(this.idFields == null) {
			this.idFields = new HashMap<String, List<String>>();
		}
		if(!this.idFields.containsKey(tableName)) {
			log.debug("Finding id column names for table: "+tableName);
			this.idFields.put(tableName, new LinkedList<String>());
			ResultSet primaryKeys = this.cursor.getConnection().getMetaData().getPrimaryKeys(this.cursor.getConnection().getCatalog(), null, tableName);
			while(primaryKeys.next()) {
				String colName = primaryKeys.getString("COLUMN_NAME");
				log.trace("Found id column: "+colName);
				this.idFields.get(tableName).add(colName);
			}
		}
		if(this.idFields.get(tableName).isEmpty()) {
			throw new IllegalArgumentException("ID fields for table '" + tableName + "' were not provided and no primary keys are present... please provide an ID field set for this table");
		}
		return this.idFields.get(tableName);
	}
	
	/**
	 * Gets the tablenames in database
	 * @return set of tablenames
	 * @throws SQLException error connecting to DB
	 */
	private Set<String> getTableNames() throws SQLException {
		if(this.tableNames.isEmpty()) {
			ResultSet tableData = this.cursor.getConnection().getMetaData().getTables(this.cursor.getConnection().getCatalog(), null, "%", this.tableTypes);
			while(tableData.next()) {
				String schemaName = tableData.getString("TABLE_SCHEM");
				if (schemaName.equals("INFORMATION_SCHEMA")) {
					continue;
				}
				this.tableNames.add(tableData.getString("TABLE_NAME"));
			}
		}
		return this.tableNames;
	}
	
	/**
	 * Builds a select statement against the table using configured fields
	 * @param tableName the table to build the select statement for
	 * @return the select statement
	 * @throws SQLException error connecting to db
	 */
	private String buildSelect(String tableName) throws SQLException {
		if((this.queryStrings != null) && this.queryStrings.containsKey(tableName)) {
			String query = this.queryStrings.get(tableName);
			log.trace("User defined SQL Query:\n" + query);
			return query;
		}
		boolean multiTable = (this.fromClauses != null) && this.fromClauses.containsKey(tableName);
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT ");
		for(String dataField : getDataFields(tableName)) {
			sb.append(getFieldPrefix());
			if(multiTable && (dataField.split("\\.").length <= 1)) {
				sb.append(tableName);
				sb.append(".");
			}
			sb.append(dataField);
			sb.append(getFieldSuffix());
			sb.append(", ");
		}
		for(String relField : getFkRelationFields(tableName).keySet()) {
			sb.append(getFieldPrefix());
			if(multiTable && (relField.split("\\.").length <= 1)) {
				sb.append(tableName);
				sb.append(".");
			}
			sb.append(relField);
			sb.append(getFieldSuffix());
			sb.append(", ");
		}
		for(String idField : getIDFields(tableName)) {
			sb.append(getFieldPrefix());
			if(multiTable && (idField.split("\\.").length <= 1)) {
				sb.append(tableName);
				sb.append(".");
			}
			sb.append(idField);
			sb.append(getFieldSuffix());
			sb.append(", ");
		}
		sb.delete(sb.lastIndexOf(", "), sb.length());
		sb.append(" FROM ");
		sb.append(tableName);
		if(multiTable) {
			sb.append(", ");
			sb.append(this.fromClauses.get(tableName));
		}
		
		if(getWhereClauses(tableName).size() > 0) {
			sb.append(" WHERE ");
			sb.append(StringUtils.join(getWhereClauses(tableName), " AND "));
		}
		log.trace("Generated SQL Query:\n" + sb.toString());
		return sb.toString();
	}
	
	/**
	 * Builds a table's record namespace
	 * @param tableName the table to build the namespace for
	 * @return the namespace
	 */
	private String buildTableRecordNS(String tableName) {
		return this.uriNS + tableName;
	}
	
	/**
	 * Builds a table's field description namespace
	 * @param tableName the table to build the namespace for
	 * @return the namespace
	 */
	private String buildTableFieldNS(String tableName) {
		return this.uriNS + "fields/" + tableName + "/";
	}
	
	/**
	 * Builds a table's type description namespace
	 * @param tableName the table to build the namespace for
	 * @return the namespace
	 */
	private String buildTableType(String tableName) {
		return this.uriNS + "types#" + tableName;
	}
	
	/**
	 * Get the fields from a result set
	 * @param rs the resultset
	 * @return the list of fields
	 * @throws SQLException error reading resultset
	 */
	private List<String> getResultSetFields(ResultSet rs) throws SQLException {
		ResultSetMetaData rsmd = rs.getMetaData();
		int count = rsmd.getColumnCount();
		List<String> fields = new ArrayList<String>(count);
		for(int x = 1; x <= count; x++) {
			fields.add(rsmd.getColumnLabel(x));
		}
		return fields;
	}
	
	/**
	 * Executes the task
	 * @throws IOException error processing record handler or jdbc connection
	 */
	public void execute() throws IOException {
		int count = 0;
		// For each Table
		try {
			for(String tableName : getTableNames()) {
				StringBuilder sb = new StringBuilder();
				// For each Record
				ResultSet rs = this.cursor.executeQuery(buildSelect(tableName));
				//				ResultSetMetaData rsmd = rs.getMetaData();
				//				int ColumnCount = rsmd.getColumnCount();
				//				Map<String,String> columnData = new HashMap<String,String>();
				while(rs.next()) {
					StringBuilder recID = new StringBuilder();
					recID.append("id");
					for(String idField : getIDFields(tableName)) {
						recID.append("_-_");
						String id = rs.getString(idField);
						if(id != null) {
							id = id.trim();
						}
						id = SpecialEntities.xmlEncode(id);
						recID.append(id);
					}
					// log.trace("Creating RDF for "+tableName+": "+recID);
					// Build RDF BEGIN
					// Header info
					String tableNS = "db-" + tableName;
					sb = new StringBuilder();
					sb.append("<?xml version=\"1.0\"?>\n");
					sb.append("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n");
					sb.append("         xmlns:");
					sb.append(tableNS);
					sb.append("=\"");
					sb.append(buildTableFieldNS(tableName));
					sb.append("\"\n");
					sb.append("         xml:base=\"");
					sb.append(buildTableRecordNS(tableName));
					sb.append("\">\n");
					
					// Record info BEGIN
					sb.append("  <rdf:Description rdf:ID=\"");
					sb.append(recID);
					sb.append("\">\n");
					
					// insert type value
					sb.append("    <rdf:type rdf:resource=\"");
					sb.append(buildTableType(tableName));
					sb.append("\"/>\n");
					
					// DataFields
					List<String> dataFieldList;
					if((this.queryStrings != null) && this.queryStrings.containsKey(tableName)) {
						dataFieldList = getResultSetFields(rs);
					} else {
						dataFieldList = getDataFields(tableName);
					}
					for(String dataField : dataFieldList) {
						// Field BEGIN
						String field = tableNS + ":" + dataField.replaceAll(" ", "_");
						sb.append("    <");
						sb.append(SpecialEntities.xmlEncode(field));
						sb.append(">");
						
						// insert field value
						if(rs.getString(dataField) != null) {
							sb.append(SpecialEntities.xmlEncode(rs.getString(dataField).trim()));
						}
						
						// Field END
						sb.append("</");
						sb.append(SpecialEntities.xmlEncode(field));
						sb.append(">\n");
					}
					
					// Relation Fields
					for(String relationField : getFkRelationFields(tableName).keySet()) {
						// Field BEGIN
						String field = tableNS + ":" + relationField.replaceAll(" ", "_");
						sb.append("    <");
						sb.append(SpecialEntities.xmlEncode(field));
						sb.append(" rdf:resource=\"");
						sb.append(buildTableRecordNS(getFkRelationFields(tableName).get(relationField)));
						
						// insert field value
						sb.append("#id_-_" + rs.getString(relationField).trim());
						
						// Field END
						sb.append("\"/>\n");
					}
					
					// Record info END
					sb.append("  </rdf:Description>\n");
					
					// Footer info
					sb.append("</rdf:RDF>");
					// Build RDF END
					
					// Write RDF to RecordHandler
					log.trace("Adding record: " + tableName + "_" + recID);
					this.rh.addRecord(tableName + "_" + recID, sb.toString(), this.getClass());
					count++;
				}
			}
		} catch(SQLException e) {
			throw new IOException(e);
		}
		log.info("Added " + count + " Records");
	}
	
	/**
	 * Get the ArgParser for this task
	 * @return the ArgParser
	 */
	private static ArgParser getParser() {
		ArgParser parser = new ArgParser("JDBCFetch");
		parser.addArgument(new ArgDef().setShortOption('d').setLongOpt("driver").withParameter(true, "JDBC_DRIVER").setDescription("jdbc driver class").setRequired(true));
		parser.addArgument(new ArgDef().setShortOption('c').setLongOpt("connection").withParameter(true, "JDBC_CONN").setDescription("jdbc connection string").setRequired(true));
		parser.addArgument(new ArgDef().setShortOption('u').setLongOpt("username").withParameter(true, "USERNAME").setDescription("database username").setRequired(true));
		parser.addArgument(new ArgDef().setShortOption('p').setLongOpt("password").withParameter(true, "PASSWORD").setDescription("database password").setRequired(true));
		parser.addArgument(new ArgDef().setShortOption('o').setLongOpt("output").withParameter(true, "CONFIG_FILE").setDescription("RecordHandler config file path").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('t').setLongOpt("tableName").withParameters(true, "TABLE_NAME").setDescription("a single database table name [have multiple -t for more table names]").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('v').setLongOpt("validTableType").withParameters(true, "TABLE_TYPE").setDescription("a single table type (TABLE, VIEW, etc) [have multiple -v for more table types]").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('Q').setLongOpt("query").withParameterValueMap("TABLE_NAME", "SQL_QUERY").setDescription("use SQL_QUERY to select from TABLE_NAME").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('I').setLongOpt("id").withParameterValueMap("TABLE_NAME", "ID_FIELD_LIST").setDescription("use columns in ID_FIELD_LIST[comma separated] as identifier for TABLE_NAME").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('F').setLongOpt("fields").withParameterValueMap("TABLE_NAME", "FIELD_LIST").setDescription("fetch columns in FIELD_LIST[comma separated] for TABLE_NAME").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('R').setLongOpt("relations").withParameterValueMap("TABLE_NAME", "RELATION_PAIR_LIST").setDescription("fetch columns in RELATION_PAIR_LIST[comma separated] for TABLE_NAME").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('W').setLongOpt("whereClause").withParameterValueMap("TABLE_NAME", "CLAUSE_LIST").setDescription("filter TABLE_NAME records based on conditions in CLAUSE_LIST[comma separated]").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('T').setLongOpt("tableFromClause").withParameterValueMap("TABLE_NAME", "TABLE_LIST").setDescription("add tables to use in from clauses for TABLE_NAME").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('O').setLongOpt("outputOverride").withParameterValueMap("RH_PARAM", "VALUE").setDescription("override the RH_PARAM of output recordhandler using VALUE").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('n').setLongOpt("namespaceBase").withParameter(true, "NAMESPACE_BASE").setDescription("the base namespace to use for each node created").setRequired(false));
		parser.addArgument(new ArgDef().setLongOpt("delimiterPrefix").withParameter(true, "DELIMITER").setDescription("Prefix each field in the query with this character").setDefaultValue("").setRequired(false));
		parser.addArgument(new ArgDef().setLongOpt("delimiterSuffix").withParameter(true, "DELIMITER").setDescription("Suffix each field in the query with this character").setDefaultValue("").setRequired(false));
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
			new JDBCFetch(args).execute();
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
