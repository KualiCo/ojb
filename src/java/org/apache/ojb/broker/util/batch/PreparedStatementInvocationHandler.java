package org.apache.ojb.broker.util.batch;

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


import org.apache.ojb.broker.metadata.JdbcConnectionDescriptor;
import org.apache.ojb.broker.platforms.PlatformFactory;
import org.apache.ojb.broker.platforms.PlatformException;
import org.apache.ojb.broker.platforms.Platform;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;

//#ifdef JDK13
import java.lang.reflect.InvocationHandler;
//#else
/*
import com.develop.java.lang.reflect.InvocationHandler;
*/
//#endif

/**
 * The implementation of {@link java.reflect.InvocationHandler} which is used
 * to create dynamic proxy which will implement {@link java.sql.PreparedStatement} and
 * {@link BatchPreparedStatement}.
 *
 * @author Oleg Nitz (<a href="mailto:olegnitz@apache.org">olegnitz@apache.org</a>)
 */
public class PreparedStatementInvocationHandler implements InvocationHandler
{

    private final static Integer ONE = new Integer(1);

    private static Method ADD_BATCH;

    private final static Method SET_BIG_DECIMAL;

    static
    {
        Method setBigDecimal = null;
        try
        {
            setBigDecimal = PreparedStatement.class.getMethod("setBigDecimal",
                                                              new Class[] {Integer.TYPE, BigDecimal.class});
        }
        catch ( Exception ex )
        {
            // ignore it
        }
        SET_BIG_DECIMAL = setBigDecimal;
    }

    private final BatchConnection _batchConn;

    private final String _sql;

    private ArrayList _methods = new ArrayList();

    private ArrayList _params = new ArrayList();

    private Platform m_platform = null;

    public PreparedStatementInvocationHandler(BatchConnection batchConn, String sql, JdbcConnectionDescriptor jcd)
    {
        _batchConn = batchConn;
        _sql = sql;
        m_platform = PlatformFactory.getPlatformFor(jcd);
        try
        {
            ADD_BATCH = m_platform.getClass().getMethod("addBatch",new Class[]{PreparedStatement.class});
        }
        catch ( NoSuchMethodException e )
        {
            /**
             * should never happen
             */
            ADD_BATCH = null;
        }
        catch ( SecurityException e )
        {
            // ignore it
        }
    }

    public Object invoke(Object proxy, Method method, Object[] args)
    throws Throwable
    {
        String name = method.getName();
        if ( name.equals("executeUpdate") )
        {
            _methods.add(ADD_BATCH);
            _params.add(null);
            _batchConn.nextExecuted(_sql);
            return ONE;
        }
        else if ( name.equals("doExecute") )
        {
            doExecute((Connection) args[0]);
        }
        else if ( name.startsWith("set") )
        {
            // workaround for the bug in Sybase jConnect JDBC driver
            if ( name.equals("setLong") )
            {
                method = SET_BIG_DECIMAL;
                args[1] = BigDecimal.valueOf(((Long) args[1]).longValue());
            }
            _methods.add(method);
            _params.add(args);
        }
        return null;
    }

    /**
     * This method performs database modification at the very and of transaction.
     */
    private void doExecute(Connection conn) throws SQLException
    {
        PreparedStatement stmt;
        int size;

        size = _methods.size();
        if ( size == 0 )
        {
            return;
        }
        stmt = conn.prepareStatement(_sql);
        try
        {
            m_platform.afterStatementCreate(stmt);
        }
        catch ( PlatformException e )
        {
            if ( e.getCause() instanceof SQLException )
            {
                throw (SQLException)e.getCause();
            }
            else
            {
                throw new SQLException(e.getMessage());
            }
        }
        try
        {
            m_platform.beforeBatch(stmt);
        }
        catch ( PlatformException e )
        {
            if ( e.getCause() instanceof SQLException )
            {
                throw (SQLException)e.getCause();
            }
            else
            {
                throw new SQLException(e.getMessage());
            }
        }
        try
        {
            for ( int i = 0; i < size; i++ )
            {
                Method method = (Method) _methods.get(i);
                try
                {
                    if ( method.equals(ADD_BATCH) )
                    {
                        /**
                         * we invoke on the platform and pass the stmt as an arg.
                         */
                        m_platform.addBatch(stmt);
                    }
                    else
                    {
                        method.invoke(stmt, (Object[]) _params.get(i));
                    }
                }
                catch (IllegalArgumentException ex)
                {
					StringBuffer buffer = generateExceptionMessage(i, stmt, ex);
					throw new SQLException(buffer.toString());
                }
                catch ( IllegalAccessException ex )
                {
					StringBuffer buffer = generateExceptionMessage(i, stmt, ex);
                    throw new SQLException(buffer.toString());
                }
                catch ( InvocationTargetException ex )
                {
                    Throwable th = ex.getTargetException();

                    if ( th == null )
                    {
                        th = ex;
                    }
                    if ( th instanceof SQLException )
                    {
                        throw ((SQLException) th);
                    }
                    else
                    {
                        throw new SQLException(th.toString());
                    }
                }
				catch (PlatformException e)
				{
					throw new SQLException(e.toString());
				}
            }
            try
            {
                /**
                 * this will call the platform specific call
                 */
                m_platform.executeBatch(stmt);
            }
            catch ( PlatformException e )
            {
                if ( e.getCause() instanceof SQLException )
                {
                    throw (SQLException)e.getCause();
                }
                else
                {
                    throw new SQLException(e.getMessage());
                }
            }

        }
        finally
        {
            stmt.close();
            _methods.clear();
            _params.clear();
        }
    }
    
    private StringBuffer generateExceptionMessage(int i, PreparedStatement stmt, Exception ex)
	{
		StringBuffer buffer = new StringBuffer();
		buffer.append("Method of type: ");
		buffer.append(_methods.get(i));
		buffer.append(" invoking on instance: ");
		if (( _methods.get(i)).equals(ADD_BATCH))
			buffer.append(m_platform);
		else
			buffer.append(stmt);
		buffer.append(" with parameters: ");
		if ((_methods.get(i)).equals(ADD_BATCH))
			buffer.append(stmt);
		else
			buffer.append(_params.get(i));
		buffer.append(" with root: ");
		buffer.append(ex.toString());
		return buffer;
	}
}

