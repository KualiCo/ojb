package org.apache.ojb.broker.platforms;

/* Copyright 2003-2005 The Apache Software Foundation
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

import org.apache.commons.lang.BooleanUtils;
import org.apache.ojb.broker.util.ClassHelper;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Wraps the Oracle CLOB type and makes it accessible via reflection
 * without having to import the Oracle Classes.
 * @author <a href="mailto:mattbaird@yahoo.com">Matthew Baird</a>
 * @author <a href="martin.kalen@curalia.se">Martin Kal&eacute;n</a>
 * @version CVS $Id: ClobWrapper.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public class ClobWrapper
{
	protected Object m_clob;

    // Fields - values must be looked up via reflection not be compile-time Oracle-version dependent
    protected static Field durationSession;
    protected static Field durationCall;
    protected static Field modeReadOnly;
    protected static Field modeReadWrite;

	// Methods
	protected static Method createTemporary;
    protected static Method freeTemporary;
	protected static Method open;
	protected static Method isOpen;
	protected static Method getCharacterStream;
	protected static Method getCharacterOutputStream;
	protected static Method getBufferSize;
	protected static Method close;
	protected static Method trim;

	/**
	 * Initialize all methods and fields via reflection.
	 */
	static
	{
		try
		{
			Class clobClass = ClassHelper.getClass("oracle.sql.CLOB", false);
			createTemporary = clobClass.getMethod("createTemporary", new Class[]{Connection.class, Boolean.TYPE, Integer.TYPE});
            freeTemporary = clobClass.getMethod("freeTemporary", null);
			open = clobClass.getMethod("open", new Class[]{Integer.TYPE});
			isOpen = clobClass.getMethod("isOpen", null);
			getCharacterStream = clobClass.getMethod("getCharacterStream", null);
			getCharacterOutputStream = clobClass.getMethod("getCharacterOutputStream", null);
			getBufferSize = clobClass.getMethod("getBufferSize", null);
			close = clobClass.getMethod("close", null);
			trim = clobClass.getMethod("trim", new Class[]{Long.TYPE});

            durationSession = ClassHelper.getField(clobClass, "DURATION_SESSION");
            durationCall = ClassHelper.getField(clobClass, "DURATION_CALL");
            modeReadOnly = ClassHelper.getField(clobClass, "MODE_READONLY");
            modeReadWrite = ClassHelper.getField(clobClass, "MODE_READWRITE");
		}
		catch (Exception ignore)
		{
            // ignore it
		}
	}

	public Object getClob()
	{
		return m_clob;
	}

	public void setClob(Object clob)
	{
		m_clob = clob;
	}

    protected static int staticIntFieldValue(Field field) {
        int value = 0;
        try {
            value = field.getInt(null);
        } catch (Exception ignore) {
            value = -1;
        }
        return value;
    }

    public static int getDurationSessionValue() {
        return staticIntFieldValue(durationSession);
    }

    public static int getDurationCallValue() {
        return staticIntFieldValue(durationCall);
    }

    public static int getModeReadOnlyValue() {
        return staticIntFieldValue(modeReadOnly);
    }

    public static int getModeReadWriteValue() {
        return staticIntFieldValue(modeReadWrite);
    }

	public static ClobWrapper createTemporary(Connection conn, boolean b, int i) throws Exception
	{
		ClobWrapper retval = new ClobWrapper();
        // Passing null to invoke static method
        retval.setClob(createTemporary.invoke(null, new Object[]{conn, BooleanUtils.toBooleanObject(b), new Integer(i)}));
		return retval;
	}

	public void open(int i) throws SQLException
	{
        if (m_clob == null) {
            return;
        }
		try
		{
			open.invoke(m_clob, new Object[]{new Integer(i)});
		}
		catch (Throwable e)
		{
			throw new SQLException(e.getMessage());
		}
	}

	public boolean isOpen() throws SQLException
	{
        if (m_clob == null) {
            return false;
        }
		boolean clobOpen = false;
		try
		{
			Boolean retval = (Boolean) isOpen.invoke(m_clob, null);
            if (retval != null) {
                clobOpen = retval.booleanValue();
            }
		}
		catch (Throwable e)
		{
			throw new SQLException(e.getMessage());
		}
		return clobOpen;
	}

	public Reader getCharacterStream() throws SQLException
	{
        if (m_clob == null) {
            return null;
        }
		Reader retval = null;
		try
		{
			retval = (Reader) getCharacterStream.invoke(m_clob, null);
		}
		catch (Throwable e)
		{
			throw new SQLException(e.getMessage());
		}
		return retval;
	}

	public Writer getCharacterOutputStream() throws SQLException
	{
        if (m_clob == null) {
            return null;
        }
		Writer retval = null;
		try
		{
			retval = (Writer) getCharacterOutputStream.invoke(m_clob, null);
		}
		catch (Throwable e)
		{
			throw new SQLException(e.getMessage());
		}
		return retval;
	}

	public int getBufferSize() throws SQLException
	{
        if (m_clob == null) {
            return 0;
        }
		Integer retval = null;
		try
		{
			retval = (Integer) getBufferSize.invoke(m_clob, null);
		}
		catch (Throwable e)
		{
			throw new SQLException(e.getMessage());
		}
		return retval.intValue();
	}

	public void close() throws SQLException
	{
        if (m_clob == null) {
            return;
        }
		try
		{
			close.invoke(m_clob, null);
		}
		catch (Throwable e)
		{
			throw new SQLException(e.getMessage());
		}

	}

	public void trim(long l) throws SQLException
	{
        if (m_clob == null) {
            return;
        }
		try
		{
			trim.invoke(m_clob, new Object[]{new Long(l)});
		}
		catch (Throwable e)
		{
			throw new SQLException(e.getMessage());
		}

	}

	public void freeTemporary() throws SQLException
	{
        if (m_clob == null) {
            return;
        }
		try
		{
			freeTemporary.invoke(m_clob, null);
		}
		catch (Throwable e)
		{
			throw new SQLException(e.getMessage());
		}
	}
}
