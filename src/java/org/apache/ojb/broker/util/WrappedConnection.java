package org.apache.ojb.broker.util;

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

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Wrapper class for connections.
 * Simplified version of {@link org.apache.commons.dbcp.DelegatingConnection}
 */
public class WrappedConnection implements Connection
{
    private Connection _conn = null;
    private boolean _isClosed = false;

    public WrappedConnection(Connection c)
    {
        _conn = c;
    }

    /**
     * Returns my underlying {@link Connection}.
     * @return my underlying {@link Connection}.
     */
    public Connection getDelegate()
    {
        return _conn;
    }

    /**
     * If my underlying <tt>Connection</tt> is not a
     * <tt>WrappedConnection</tt>, returns it,
     * otherwise recursively invokes this method on
     * my delegate.
     * <p>
     * Hence this method will return the first
     * delegate that is not a <tt>WrappedConnection</tt>,
     * or <tt>null</tt> when no non-<tt>WrappedConnection</tt>
     * delegate can be found by transversing this chain.
     * <p>
     * This method is useful when you may have nested
     * <tt>WrappedConnection</tt>s, and you want to make
     * sure to obtain a "genuine" {@link java.sql.Connection}.
     */
    public Connection getInnermostDelegate()
    {
        Connection c = _conn;
        while (c != null && c instanceof WrappedConnection)
        {
            c = ((WrappedConnection) c).getDelegate();
            if (this == c)
            {
                return null;
            }
        }
        return c;
    }

    /** Sets my delegate. */
    public void setDelegate(Connection c)
    {
        _conn = c;
    }

    protected void checkOpen() throws SQLException
    {
        if (_isClosed)
        {
            throw new SQLException("Connection is closed. " + this);
        }
    }

    /**
     * Activate the connection
     */
    public void activateConnection()
    {
        _isClosed = false;
        if (_conn instanceof WrappedConnection)
        {
            ((WrappedConnection) _conn).activateConnection();
        }
    }

    /**
     * Passivate the connection
     */
    public void passivateConnection() throws SQLException
    {
        _isClosed = true;
        if (_conn instanceof WrappedConnection)
        {
            ((WrappedConnection) _conn).passivateConnection();
        }
    }

    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("wrapped connection", (_conn != null ? _conn.toString() : null))
                .toString();
    }

    /**
     * Closes the underlying connection, and close
     * any Statements that were not explicitly closed.
     */
    public void close() throws SQLException
    {
        passivateConnection();
        _conn.close();
    }

    public boolean isClosed() throws SQLException
    {
        if (_isClosed || _conn.isClosed())
        {
            return true;
        }
        return false;
    }

    public Statement createStatement() throws SQLException
    {
        checkOpen();
        return _conn.createStatement();
    }

    public Statement createStatement(int resultSetType,
                                     int resultSetConcurrency)
            throws SQLException
    {
        checkOpen();
        return _conn.createStatement(resultSetType, resultSetConcurrency);
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException
    {
        checkOpen();
        return _conn.prepareStatement(sql);
    }

    public PreparedStatement prepareStatement(String sql,
                                              int resultSetType,
                                              int resultSetConcurrency)
            throws SQLException
    {
        checkOpen();
        return _conn.prepareStatement(sql, resultSetType, resultSetConcurrency);
    }

    public CallableStatement prepareCall(String sql) throws SQLException
    {
        checkOpen();
        return _conn.prepareCall(sql);
    }

    public CallableStatement prepareCall(String sql,
                                         int resultSetType,
                                         int resultSetConcurrency)
            throws SQLException
    {
        checkOpen();
        return _conn.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    public void clearWarnings() throws SQLException
    {
        checkOpen();
        _conn.clearWarnings();
    }

    public void commit() throws SQLException
    {
        checkOpen();
        _conn.commit();
    }

    public boolean getAutoCommit() throws SQLException
    {
        checkOpen();
        return _conn.getAutoCommit();
    }

    public String getCatalog() throws SQLException
    {
        checkOpen();
        return _conn.getCatalog();
    }

    public DatabaseMetaData getMetaData() throws SQLException
    {
        checkOpen();
        return _conn.getMetaData();
    }

    public int getTransactionIsolation() throws SQLException
    {
        checkOpen();
        return _conn.getTransactionIsolation();
    }

    public Map getTypeMap() throws SQLException
    {
        checkOpen();
        return _conn.getTypeMap();
    }

    public SQLWarning getWarnings() throws SQLException
    {
        checkOpen();
        return _conn.getWarnings();
    }

    public boolean isReadOnly() throws SQLException
    {
        checkOpen();
        return _conn.isReadOnly();
    }

    public String nativeSQL(String sql) throws SQLException
    {
        checkOpen();
        return _conn.nativeSQL(sql);
    }

    public void rollback() throws SQLException
    {
        checkOpen();
        _conn.rollback();
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException
    {
        checkOpen();
        _conn.setAutoCommit(autoCommit);
    }

    public void setCatalog(String catalog) throws SQLException
    {
        checkOpen();
        _conn.setCatalog(catalog);
    }

    public void setReadOnly(boolean readOnly) throws SQLException
    {
        checkOpen();
        _conn.setReadOnly(readOnly);
    }

    public void setTransactionIsolation(int level) throws SQLException
    {
        checkOpen();
        _conn.setTransactionIsolation(level);
    }

    @SuppressWarnings("unchecked")
	public void setTypeMap(@SuppressWarnings("rawtypes") Map map) throws SQLException
    {
        checkOpen();
        _conn.setTypeMap(map);
    }

    @Override
	public boolean isWrapperFor(Class<?> arg0) throws SQLException {
		checkOpen();
		return _conn.isWrapperFor(arg0);
	}

	@Override
	public <T> T unwrap(Class<T> arg0) throws SQLException {
		checkOpen();
		return _conn.unwrap(arg0);
	}

	@Override
	public Array createArrayOf(String typeName, Object[] elements)
			throws SQLException {
		checkOpen();
		return _conn.createArrayOf(typeName, elements);
	}

	@Override
	public Blob createBlob() throws SQLException {
		checkOpen();
		return _conn.createBlob();
	}

	@Override
	public Clob createClob() throws SQLException {
		checkOpen();
		return _conn.createClob();
	}

	@Override
	public NClob createNClob() throws SQLException {
		checkOpen();
		return _conn.createNClob();
	}

	@Override
	public SQLXML createSQLXML() throws SQLException {
		checkOpen();
		return _conn.createSQLXML();
	}

	@Override
	public Struct createStruct(String typeName, Object[] attributes)
			throws SQLException {
		checkOpen();
		return _conn.createStruct(typeName, attributes);
	}

	@Override
	public Properties getClientInfo() throws SQLException {
		checkOpen();
		return _conn.getClientInfo();
	}

	@Override
	public String getClientInfo(String name) throws SQLException {
		checkOpen();
		return _conn.getClientInfo(name);
	}

	@Override
	public boolean isValid(int timeout) throws SQLException {
		checkOpen();
		return _conn.isValid(timeout);
	}

	@Override
	public void setClientInfo(Properties properties)
			throws SQLClientInfoException {
		 _conn.setClientInfo(properties);
		
	}

	@Override
	public void setClientInfo(String name, String value)
			throws SQLClientInfoException {
		_conn.setClientInfo(name, value);
		
	}

    // ------------------- JDBC 3.0 -----------------------------------------
    // Will be uncommented by the build process on a JDBC 3.0 system

//#ifdef JDBC30

    public int getHoldability() throws SQLException {
        checkOpen();
        return _conn.getHoldability();
    }

    public void setHoldability(int holdability) throws SQLException {
        checkOpen();
        _conn.setHoldability(holdability);
    }

    public java.sql.Savepoint setSavepoint() throws SQLException {
        checkOpen();
        return _conn.setSavepoint();
    }

    public java.sql.Savepoint setSavepoint(String name) throws SQLException {
        checkOpen();
        return _conn.setSavepoint(name);
    }

    public void rollback(java.sql.Savepoint savepoint) throws SQLException {
        checkOpen();
        _conn.rollback(savepoint);
    }

    public void releaseSavepoint(java.sql.Savepoint savepoint) throws SQLException {
        checkOpen();
        _conn.releaseSavepoint(savepoint);
    }

    public Statement createStatement(int resultSetType,
                                     int resultSetConcurrency,
                                     int resultSetHoldability)
        throws SQLException {
        checkOpen();
        return _conn.createStatement(resultSetType, resultSetConcurrency,
                                     resultSetHoldability);
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType,
                                              int resultSetConcurrency,
                                              int resultSetHoldability)
        throws SQLException {
        checkOpen();
        return _conn.prepareStatement(sql, resultSetType,
                                      resultSetConcurrency,
                                      resultSetHoldability);
    }

    public CallableStatement prepareCall(String sql, int resultSetType,
                                         int resultSetConcurrency,
                                         int resultSetHoldability)
        throws SQLException {
        checkOpen();
        return _conn.prepareCall(sql, resultSetType,
                                 resultSetConcurrency,
                                 resultSetHoldability);
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
        throws SQLException {
        checkOpen();
        return _conn.prepareStatement(sql, autoGeneratedKeys);
    }

    public PreparedStatement prepareStatement(String sql, int columnIndexes[])
        throws SQLException {
        checkOpen();
        return _conn.prepareStatement(sql, columnIndexes);
    }

    public PreparedStatement prepareStatement(String sql, String columnNames[])
        throws SQLException {
        checkOpen();
        return _conn.prepareStatement(sql, columnNames);
      }
	
//#endif
}
