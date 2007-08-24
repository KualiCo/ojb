package org.apache.ojb.broker.ant;

/* Copyright 2002-2005 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.sql.*;
import java.util.Hashtable;

/**
 * DBUtility is a utility class for verifying that various Database objects
 * exist in a specified database.  This utility does not use the jdbc 
 * DatabaseMetaData object because not all jdbc drivers fully implement
 * it (like org.hsqldb.jdbcDriver - suckers).
 * 
 * @author <a href="mailto:daren@softwarearena.com">Daren Drummond</a>
 * @version $Id: DBUtility.java,v 1.1 2007-08-24 22:17:41 ewestfal Exp $
 */
public class DBUtility 
{
	private Connection m_connection = null;
	
	private String m_url = null;
	private String m_user = null;
	private String m_pwd = null;
	private static String m_ORA_EXCEPTION_1000 = "ORA-01000";
	private static String m_ORA_EXCEPTION_604 = "ORA-00604";

	/**
	 * DBUtility connects to the database in this constructor.
	 * 
	 * @param url			String representing the jdbc connection url.  For example, "jdbc:hsqldb:target/test/OJB".
	 * @param user			The database user account to use for logging on.
	 * @param pwd			The password for the user
	 * 
	 * @throws SQLException			Throws SQLException if there are problems connecting to the database.
	 * @throws ClassNotFoundException	Throws ClassNotFoundException if the jdbc driver class can not be found.
	 */
	public DBUtility(String url, String user, String pwd) 
	throws SQLException
	{
		m_url = url;
		m_user = user;
		m_pwd = pwd;
		m_connection = connect(url, user, pwd);		
	}
	
	public void release() throws SQLException
	{
		if(m_connection != null)
		{
			m_connection.close();	
		}
	}
	
	private void resetConnection()
	{
		try
		{
			release();
			connect(m_url, m_user, m_pwd);
		}
		catch(Exception e)
		{
			System.out.println("Could not reconnect to database!!!! " + e.getMessage());
		}
	}

	private Connection connect(String url, String user, String pwd) throws SQLException
	{
		m_connection = DriverManager.getConnection(url, user, pwd);			
		return m_connection;
	}
	
	
	/**
	 * 	Checks the database for the existence of this table.  Returns true if it
	 *  exists, false if it doesn't exist, and throws a SQLException if the 
	 *  connection is not established.  NOTE: If a schema is required for your
	 *  database, then it should have been provided in the connection url.
	 * 
	 * @param 	tableName	String name of the table that you want check for existence.
	 * @return boolean		true if the table exists, false if it doesn't exist.
	 */
	public boolean exists(String tableName)
	{
		boolean bReturn = false;

		if (tableName == null) return bReturn;
		PreparedStatement checkTable = null;
		try
		{
			//System.out.println("DBUtility: looking up table: " + tableName);
			//System.out.println("Select * from " + tableName + " where 1=0");
			checkTable = m_connection.prepareStatement("Select * from " + tableName + " where 1=0");
			checkTable.executeQuery();
			bReturn = true;
		}
		catch(Exception e)
		{
			if (e.getMessage().startsWith(m_ORA_EXCEPTION_1000) || e.getMessage().startsWith(m_ORA_EXCEPTION_604))
			{
				System.out.println("Exceeded available Oracle cursors.  Resetting connection and trying the SQL statement again...");
				resetConnection();
				return exists(tableName);
			}
			else
			{
				//System.out.println("DD - " + e.getMessage());
				bReturn = false;	
			}
		}
	
		return bReturn;
	}
	
	private Hashtable m_columnCache = new Hashtable(79);
	
	private ResultSet getColumns(String tableName)
	{
		return (ResultSet)m_columnCache.get(tableName);
	}
	private void putColumns(String tableName, ResultSet columns)
	{
		m_columnCache.put(tableName, columns);
	}
	
	/**
	 * 	Checks the database for the existence of this table.column of the specified
	 *  jdbc type.  Returns true if it exists, false if it doesn't exist, and throws 
	 *  a SQLException if the connection is not established.  NOTE: If a schema is 
	 *  required for your database, then it should have been provided in the 
	 * 	connection url.
	 * 
	 * @param	tableName		String name of the table to check.
	 * @param	columnName		String name of the table column to check.
	 * @param	jdbcType		Case insensitive String representation of 
	 * 							the jdbc type of the column.  Valid values 
	 * 							are string representations of the types listed
	 * 							in java.sql.Types.  For example, "bit", "float",
	 * 							"varchar", "clob", etc.
	 * @param	ignoreCase		boolean flag that determines if the utility should
	 * 							consider the column name case when searching for 
	 * 							the database table.column.
	 * 
	 * @throws SQLException if the Table doesn't exist, if the column doesn't exist, if the column type doesn't match the specified jdbcType.
	 */	
	public void exists(String tableName, String columnName, String jdbcType, boolean ignoreCase) throws SQLException
	{
		if (tableName == null) throw new SQLException("TableName was null.  You must specify a valid table name.");
		if (columnName == null) throw new SQLException("Column name was null.  You must specify a valid column name.");

		ResultSet columns = getColumns(tableName);

		if(columns == null)
		{
			//columns not in the cache, look them up and cache
			PreparedStatement checkTable = null;
			try
			{
				//System.out.println("DBUtility: looking up table: " + tableName);
				//System.out.println("Select * from " + tableName + " where 1=0");
				checkTable = m_connection.prepareStatement("Select * from " + tableName + " where 1=0");
				columns = checkTable.executeQuery();
				putColumns(tableName, columns);
			}
			catch(SQLException sqle)
			{
				if (sqle.getMessage().startsWith(m_ORA_EXCEPTION_1000) || sqle.getMessage().startsWith(m_ORA_EXCEPTION_604))
				{
					System.out.println("Exceeded available Oracle cursors.  Resetting connection and trying the SQL statement again...");
					resetConnection();
					exists(tableName, columnName, jdbcType, ignoreCase);
				}
				else
				{
					//System.out.println(sqle.getMessage());
					throw sqle;
				}
			}	
		}
		
		ResultSetMetaData rsMeta = columns.getMetaData();
		int iColumns = rsMeta.getColumnCount();
		int jdbcTypeConst = this.getJdbcType(jdbcType);
		for(int i = 1; i <= iColumns; i++)
		{
			if(ignoreCase)
			{
				//ignore case while testing
				if(columnName.equalsIgnoreCase(rsMeta.getColumnName(i)))
				{
					//The column exists, does the type match?
					if(jdbcTypeConst != rsMeta.getColumnType(i))
					{
						throw new SQLException("The column '" + tableName + "." + columnName + "' is of type '" + rsMeta.getColumnTypeName(i) + "' and cannot be mapped to the jdbc type '" + jdbcType + "'.");
					}
					else
					{
						return;	
					}
				}
			}
			else
			{
				//enforce case-sensitive compare
				if(columnName.equals(rsMeta.getColumnName(i)))
				{
					//The column exists, does the type match?
					if(jdbcTypeConst != rsMeta.getColumnType(i))
					{
						throw new SQLException("The column '" + tableName + "." + columnName + "' is of type '" + rsMeta.getColumnTypeName(i) + "' and cannot be mapped to the jdbc type '" + jdbcType + "'.");
					}
					else
					{
						return;	
					}
				}				
				
			}
			
			//System.out.println("Found column: " + rsMeta.getColumnName(i));
		}

		throw new SQLException("The column '" + columnName + "' was not found in table '" + tableName + "'.");

	}	
	
	/**
	 * 	Checks the database for the existence of this table.column of the specified
	 *  jdbc type.  Throws a SQLException if if the Table.Column can not be found, and
	 * 	throws a SQLWarning if the column type does not match the passed JDBC type.
	 *  NOTE: If a schema is required for your database, then it should have been 
	 * 	provided in the connection url.
	 * 
	 * @param	tableName		String name of the table to check.
	 * @param	columnName		String name of the table column to check.
	 * @param	jdbcType		Case insensitive String representation of 
	 * 							the jdbc type of the column.  Valid values 
	 * 							are string representations of the types listed
	 * 							in java.sql.Types.  For example, "bit", "float",
	 * 							"varchar", "clob", etc.
	 * @param	ignoreCase		boolean flag that determines if the utility should
	 * 							consider the column name case when searching for 
	 * 							the database table.column.
	 * 
	 * @throws SQLException if the Table doesn't exist, if the column doesn't exist.
	 * @throws SQLWarning if the column type doesn't match the specified jdbcType.
	 */	
	public void existsUseWarnings(String tableName, String columnName, String jdbcType, boolean ignoreCase) throws SQLException, SQLWarning
	{
		if (tableName == null) throw new SQLException("TableName was null.  You must specify a valid table name.");
		if (columnName == null) throw new SQLException("Column name was null.  You must specify a valid column name.");

		ResultSet columns = getColumns(tableName);
		
		if(columns == null)
		{
			//columns not in the cache, look them up and cache
			try
			{
				//System.out.println("DBUtility: looking up table: " + tableName);
				//System.out.println("Select * from " + tableName + " where 1=0");
				PreparedStatement checkTable = m_connection.prepareStatement("Select * from " + tableName + " where 1=0");
				columns = checkTable.executeQuery();
				putColumns(tableName, columns);
			}
			catch(SQLException sqle)
			{
				if (sqle.getMessage().startsWith(m_ORA_EXCEPTION_1000) || sqle.getMessage().startsWith(m_ORA_EXCEPTION_604))
				{
					System.out.println("Exceeded available Oracle cursors.  Resetting connection and trying the SQL statement again...");
					resetConnection();
					existsUseWarnings(tableName, columnName, jdbcType, ignoreCase); 
				}
				else
				{
					//System.out.println(sqle.getMessage());
					throw sqle;
				}
			}
		}
		
		ResultSetMetaData rsMeta = columns.getMetaData();
		int iColumns = rsMeta.getColumnCount();
		int jdbcTypeConst = this.getJdbcType(jdbcType);
		for(int i = 1; i <= iColumns; i++)
		{
			if(ignoreCase)
			{
				//ignore case while testing
				if(columnName.equalsIgnoreCase(rsMeta.getColumnName(i)))
				{
					//The column exists, does the type match?
					if(jdbcTypeConst != rsMeta.getColumnType(i))
					{
						throw new SQLWarning("The column '" + tableName + "." + columnName + "' is of type '" + rsMeta.getColumnTypeName(i) + "' and cannot be mapped to the jdbc type '" + jdbcType + "'.");
					}
					else
					{
						return;	
					}
				}
			}
			else
			{
				//enforce case-sensitive compare
				if(columnName.equals(rsMeta.getColumnName(i)))
				{
					//The column exists, does the type match?
					if(jdbcTypeConst != rsMeta.getColumnType(i))
					{
						throw new SQLWarning("The column '" + tableName + "." + columnName + "' is of type '" + rsMeta.getColumnTypeName(i) + "' and cannot be mapped to the jdbc type '" + jdbcType + "'.");
					}
					else
					{
						return;	
					}
				}				
				
			}
			
			//System.out.println("Found column: " + rsMeta.getColumnName(i));
		}

		throw new SQLException("The column '" + columnName + "' was not found in table '" + tableName + "'.");

	}		
	
	
	/**
	 * 	Checks the database for the existence of this table.column.  
	 * 	Throws a SQLException if if the Table.Column can not be found.
	 *  NOTE: If a schema is required for your
	 *  database, then it should have been provided in the connection url.
	 * 
	 * @param	tableName		String name of the table to check.
	 * @param	columnName		String name of the table column to check.
	 * @param	ignoreCase		boolean flag that determines if the utility should
	 * 							consider the column name case when searching for 
	 * 							the database table.column.
	 * 
	 * @throws SQLException if the Table doesn't exist, if the column doesn't exist.
	 */	
	
	public void exists(String tableName, String columnName, boolean ignoreCase) throws SQLException
	{
		if (tableName == null) throw new SQLException("TableName was null.  You must specify a valid table name.");
		if (columnName == null) throw new SQLException("Column name was null.  You must specify a valid column name.");

		ResultSet columns = getColumns(tableName);
		
		if(columns == null)
		{
			//columns not in the cache, look them up and cache
			try
			{
				//System.out.println("DBUtility: looking up table: " + tableName);
				//System.out.println("Select * from " + tableName + " where 1=0");
				PreparedStatement checkTable = m_connection.prepareStatement("Select * from " + tableName + " where 1=0");
				columns = checkTable.executeQuery();
				putColumns(tableName, columns);
			}
			catch(SQLException sqle)
			{
				if (sqle.getMessage().startsWith(m_ORA_EXCEPTION_1000) || sqle.getMessage().startsWith(m_ORA_EXCEPTION_604))
				{
					System.out.println("Exceeded available Oracle cursors.  Resetting connection and trying the SQL statement again...");
					resetConnection();
					exists(tableName, columnName, ignoreCase);
				}
				else
				{
					System.out.println(sqle.getMessage());
					throw sqle;
				}
			}
		}
		
		ResultSetMetaData rsMeta = columns.getMetaData();
		int iColumns = rsMeta.getColumnCount();
		for(int i = 1; i <= iColumns; i++)
		{
			if(ignoreCase)
			{
				//ignore case while testing
				if(columnName.equalsIgnoreCase(rsMeta.getColumnName(i)))
				{
					return;
				}
			}
			else
			{
				//enforce case-sensitive compare
				if(columnName.equals(rsMeta.getColumnName(i)))
				{
					return;
				}				
				
			}
			
			//System.out.println("Found column: " + rsMeta.getColumnName(i));
		}

		throw new SQLException("The column '" + columnName + "' was not found in table '" + tableName + "'.");

	}	
		
	
  	/**
     * Determines the java.sql.Types constant value from an OJB 
     * FIELDDESCRIPTOR value.
     * 
     * @param type The FIELDDESCRIPTOR which JDBC type is to be determined.
     * 
     * @return int the int value representing the Type according to
     * 
     * @throws SQLException if the type is not a valid jdbc type.
     * java.sql.Types
     */
    public int getJdbcType(String ojbType) throws SQLException
    {
        int result;
        if(ojbType == null) ojbType = "";
		ojbType = ojbType.toLowerCase();
        if (ojbType.equals("bit"))
            result = Types.BIT;
        else if (ojbType.equals("tinyint"))
            result = Types.TINYINT;
        else if (ojbType.equals("smallint"))
            result = Types.SMALLINT;
        else if (ojbType.equals("integer"))
            result = Types.INTEGER;
        else if (ojbType.equals("bigint"))
            result = Types.BIGINT;

        else if (ojbType.equals("float"))
            result = Types.FLOAT;
        else if (ojbType.equals("real"))
            result = Types.REAL;
        else if (ojbType.equals("double"))
            result = Types.DOUBLE;

        else if (ojbType.equals("numeric"))
            result = Types.NUMERIC;
        else if (ojbType.equals("decimal"))
            result = Types.DECIMAL;

        else if (ojbType.equals("char"))
            result = Types.CHAR;
        else if (ojbType.equals("varchar"))
            result = Types.VARCHAR;
        else if (ojbType.equals("longvarchar"))
            result = Types.LONGVARCHAR;

        else if (ojbType.equals("date"))
            result = Types.DATE;
        else if (ojbType.equals("time"))
            result = Types.TIME;
        else if (ojbType.equals("timestamp"))
            result = Types.TIMESTAMP;

        else if (ojbType.equals("binary"))
            result = Types.BINARY;
        else if (ojbType.equals("varbinary"))
            result = Types.VARBINARY;
        else if (ojbType.equals("longvarbinary"))
            result = Types.LONGVARBINARY;

		else if (ojbType.equals("clob"))
     		result = Types.CLOB;
		else if (ojbType.equals("blob"))
			result = Types.BLOB;
        else
            throw new SQLException(
                "The type '"+ ojbType + "' is not a valid jdbc type.");
        return result;
    }	

	protected void finalize()
	{
		try
		{
			release();
		}
		catch(Exception e)
		{
			e.printStackTrace();	
		}
	}
}
