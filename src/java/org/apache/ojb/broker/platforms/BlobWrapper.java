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

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Wraps the Oracle BLOB type and makes it accessible via reflection
 * without having to import the Oracle Classes.
 * @author <a href="mailto:mattbaird@yahoo.com">Matthew Baird</a>
 * @author <a href="mailto:erik@cj.com">Erik Forkalsrud</a>
 * @author <a href="martin.kalen@curalia.se">Martin Kal&eacute;n</a>
 * @version CVS $Id: BlobWrapper.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public class BlobWrapper
{
	protected Object m_blob;

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
	protected static Method getBinaryStream;
	protected static Method getBinaryOutputStream;
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
            Class blobClass = ClassHelper.getClass("oracle.sql.BLOB", false);
            createTemporary = blobClass.getMethod("createTemporary", new Class[]{Connection.class, Boolean.TYPE, Integer.TYPE});
            freeTemporary = blobClass.getMethod("freeTemporary", null);
            open = blobClass.getMethod("open", new Class[]{Integer.TYPE});
            isOpen = blobClass.getMethod("isOpen", null);
            getBinaryStream = blobClass.getMethod("getBinaryStream", null);
            getBinaryOutputStream = blobClass.getMethod("getBinaryOutputStream", null);
            getBufferSize = blobClass.getMethod("getBufferSize", null);
            close = blobClass.getMethod("close", null);
            trim = blobClass.getMethod("trim", new Class[]{Long.TYPE});

            durationSession = ClassHelper.getField(blobClass, "DURATION_SESSION");
            durationCall = ClassHelper.getField(blobClass, "DURATION_CALL");
            modeReadOnly = ClassHelper.getField(blobClass, "MODE_READONLY");
            modeReadWrite = ClassHelper.getField(blobClass, "MODE_READWRITE");
        }
        catch (Exception ignore)
        {
            // ignore it
        }
    }

    public Object getBlob()
    {
        return m_blob;
    }

    public void setBlob(Object blob)
    {
        m_blob = blob;
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

    public static BlobWrapper createTemporary(Connection conn, boolean b, int i) throws Exception
    {
        BlobWrapper retval = new BlobWrapper();
        // Passing null to invoke static method
        retval.setBlob(createTemporary.invoke(null, new Object[]{conn, BooleanUtils.toBooleanObject(b), new Integer(i)}));
        return retval;
    }

    public void open(int i) throws SQLException
    {
        if (m_blob == null) {
            return;
        }
        try
        {
            open.invoke(m_blob, new Object[]{new Integer(i)});
        }
        catch (Throwable e)
        {
            throw new SQLException(e.getMessage());
        }
    }

    public boolean isOpen() throws SQLException
    {
        if (m_blob == null) {
            return false;
        }
        
		boolean blobOpen = false;
		try
		{
			Boolean retval = (Boolean) isOpen.invoke(m_blob, null);
            if (retval != null) {
                blobOpen = retval.booleanValue();
            }
		}
		catch (Throwable e)
		{
			throw new SQLException(e.getMessage());
		}
		return blobOpen;
    }

    public InputStream getBinaryStream() throws SQLException
    {
        if (m_blob == null) {
            return null;
        }
        InputStream retval = null;
        try
        {
            retval = (InputStream) getBinaryStream.invoke(m_blob, null);
        }
        catch (Throwable e)
        {
            throw new SQLException(e.getMessage());
        }
        return retval;
    }

    public OutputStream getBinaryOutputStream() throws SQLException
    {
        if (m_blob == null) {
            return null;
        }
        OutputStream retval = null;
        try
        {
            retval = (OutputStream) getBinaryOutputStream.invoke(m_blob, null);
        }
        catch (Throwable e)
        {
            throw new SQLException(e.getMessage());
        }
        return retval;
    }

    public int getBufferSize() throws SQLException
    {
        if (m_blob == null) {
            return 0;
        }
        Integer retval = null;
        try
        {
            retval = (Integer) getBufferSize.invoke(m_blob, null);
        }
        catch (Throwable e)
        {
            throw new SQLException(e.getMessage());
        }
        return retval.intValue();
    }

    public void close() throws SQLException
    {
        if (m_blob == null) {
            return;
        }
        try
        {
            close.invoke(m_blob, null);
        }
        catch (Throwable e)
        {
            throw new SQLException(e.getMessage());
        }

    }

    public void trim(long l) throws SQLException
    {
        if (m_blob == null) {
            return;
        }
        try
        {
            trim.invoke(m_blob, new Object[]{new Long(l)});
        }
        catch (Throwable e)
        {
            throw new SQLException(e.getMessage());
        }

    }

    public void freeTemporary() throws SQLException
    {
        if (m_blob == null) {
            return;
        }
        try
        {
            freeTemporary.invoke(m_blob, null);
        }
        catch (Throwable e)
        {
            throw new SQLException(e.getMessage());
        }
    }

}
