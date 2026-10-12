/*******************************************************************************
 * Copyright (c) 2010-2011 VIVO Harvester Team. For full list of contributors, please see the AUTHORS file provided.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the new BSD license which accompanies this distribution, and is available at http://www.opensource.org/licenses/bsd-license.html
 ******************************************************************************/
/**
 * 
 */
package org.vivoweb.test.harvester.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vivoweb.harvester.util.DatabaseClone;
import org.vivoweb.harvester.util.FileAide;
import org.vivoweb.harvester.util.InitLog;
import junit.framework.TestCase;

/**
 * @author cah
 *
 */
public class DatabaseCloneTest extends TestCase {
	/**
	 * SLF4J Logger
	 */
	private static Logger log = LoggerFactory.getLogger(DatabaseCloneTest.class);
	/** */
	private static String inputDriverClass = "org.h2.Driver";
	/** */
	private static String inputConnLine = "jdbc:h2:mem:DatabaseCloneTest";
	/** */
	private static String inputUsername = "sa";
	/** */
	private static String inputPassword = "";
//	/** */
//	private static String inputDriverClass = "com.mysql.jdbc.Driver";
//	/** */
//	private static String inputConnLine = "jdbc:mysql://127.0.0.1/junit";
//	/** */
//	private static String inputUsername = "junit";
//	/** */
//	private static String inputPassword = "HDAVvLh5aD2TsumX";
	/** */
	private Connection inputConn;
	/** */
	private static String outputDriverClass = "org.h2.Driver";
	/** */
	private static String outputConnLine = "jdbc:h2:mem:DatabaseCloneTestTmp";
	/** */
	private static String outputUsername = "sa";
	/** */
	private static String outputPassword = "";
	/** */
	private Connection outputConn;
	
	@Override
	protected void setUp() throws Exception {
		try {
			InitLog.initLogger(null, null);
			Class.forName(inputDriverClass);
			this.inputConn = DriverManager.getConnection(inputConnLine, inputUsername, inputPassword);
			Class.forName(outputDriverClass);
			this.outputConn = DriverManager.getConnection(outputConnLine, outputUsername, outputPassword);
			
			Statement cursor = this.inputConn.createStatement();
			cursor.executeUpdate("" +
					"CREATE TABLE `department` (" +
					"  `dept_id` varchar(8) NOT NULL," +
					"  `dept_name` varchar(255) NOT NULL," +
					"  `type_id` INT NOT NULL," +
					"  `super_dept_id` varchar(8) NOT NULL," +
					"  `mod_ts` timestamp," +
					"  `is_current` tinyint," +
					"  PRIMARY KEY (`dept_id`)" +
					");" +
			"");
			cursor.executeUpdate("" +
					"INSERT INTO `department` (`dept_id`, `dept_name`, `type_id`, `super_dept_id`, `mod_ts`, `is_current`) VALUES" +
					"('12345678', 'Division of Medicine', 402, '00000000', '2010-09-15 14:51:35', 1)," +
					"('87654321', 'Department of Research Informatics', 403, '12345678', '1999-11-03 01:24:27', 0)," +
					"('09876543', 'Clinical and Translational Program', 405, '87654321', '2004-05-21 09:32:57', 1)," +
					"('00000000', 'University of Sample Data', 401, '', '2000-06-11 18:14:03', 1);" +
			"");
		} catch(Exception e) {
			if(this.inputConn != null) {
				try {
					this.inputConn.createStatement().executeUpdate("DROP TABLE `department`");
				} catch(SQLException e2) {
					// ignore
				}
				try {
					this.inputConn.close();
				} catch(SQLException e2) {
					// ignore
				}
			}
			if(this.outputConn != null) {
				try {
					this.outputConn.createStatement().executeUpdate("DROP TABLE `department`");
				} catch(SQLException e2) {
					// ignore
				}
				try {
					this.outputConn.close();
				} catch(SQLException e2) {
					// ignore
				}
			}
			FileAide.delete("databaseclonetest-temp/");
			throw e;
		}
	}
	
	/**
	 * Test method for {@link org.vivoweb.harvester.util.DatabaseClone#execute() execute()}.
	 * @throws Exception error
	 */
	public final void testExecute() throws Exception {
		new DatabaseClone(this.inputConn, null, this.outputConn, null, null, null).execute();
		Statement cursor = this.outputConn.createStatement();
		ResultSet rs = cursor.executeQuery("SELECT * FROM department");
		assertTrue(rs.next());
		int count = 0;
		Set<String> ids = new HashSet<String>();
		ids.add("12345678");
		ids.add("87654321");
		ids.add("09876543");
		ids.add("00000000");
		Map<String, String> current = new HashMap<String, String>();
		current.put("12345678", "TRUE");
		current.put("87654321", "FALSE");
		current.put("09876543", "TRUE");
		current.put("00000000", "TRUE");
		
		do{
			String deptid = rs.getString("dept_id");
			log.trace("dept_id: "+deptid);
			assertTrue(ids.remove(deptid));
			String currentVal = current.get(deptid);
			log.trace("currentVal: "+currentVal);
			String is_current = Boolean.valueOf(rs.getBoolean("is_current")).toString().toUpperCase();
			log.trace("is_current: "+is_current);
			assertEquals(currentVal, is_current);
			count++;
		}while(rs.next());
		assertEquals(4, count);
	}
	
	@Override
	protected void tearDown() throws Exception {
		if(this.inputConn != null) {
			try {
				this.inputConn.createStatement().executeUpdate("DROP TABLE `department`");
			} catch(SQLException e) {
				// ignore
			}
			try {
				this.inputConn.close();
			} catch(SQLException e) {
				// ignore
			}
		}
		if(this.outputConn != null) {
			try {
				this.outputConn.createStatement().executeUpdate("DROP TABLE `department`");
			} catch(SQLException e) {
				// ignore
			}
			try {
				this.outputConn.close();
			} catch(SQLException e) {
				// ignore
			}
		}
		FileAide.delete("databaseclonetest-temp/");
	}
}
