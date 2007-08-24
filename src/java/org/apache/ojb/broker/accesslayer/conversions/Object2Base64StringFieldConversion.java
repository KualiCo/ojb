package org.apache.ojb.broker.accesslayer.conversions;

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.ojb.broker.util.Base64;

/**
 * this implementation of the FieldConversion interface converts
 * between java.lang.Objects values and char[] values in the rdbms.
 * This conversion is useful to store serialized objects in database
 * columns. For an example have a look at the mapping of
 * org.apache.ojb.odmg.collections.DlistEntry.
 *
 * @author Thomas Mahler, Scott C. Gray
 * @version $Id: Object2Base64StringFieldConversion.java,v 1.1 2007-08-24 22:17:31 ewestfal Exp $
 */
public class Object2Base64StringFieldConversion implements FieldConversion
{
    private ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
    private Base64.OutputStream uuOut =
        new Base64.OutputStream(byteOut, Base64.ENCODE, false);
    private GZIPOutputStream gzipOut;
    private ObjectOutputStream objOut;

    /*
    ** @see FieldConversion#javaToSql(Object)
    */
    public Object javaToSql(Object source)
    {
        synchronized (byteOut)
        {
            try
            {
                if (gzipOut == null)
                {
                    gzipOut = new GZIPOutputStream(uuOut);
                    objOut = new ObjectOutputStream(gzipOut);
                }

                /*
                ** Clear out the byte array
                */
                byteOut.reset();

                objOut.writeObject(source);
                objOut.flush();
                gzipOut.finish();
                gzipOut.flush();

                return (byteOut.toString());
            }
            catch (Throwable t)
            {
                throw new ConversionException(t);
            }
        }
    }

    /*
    ** @see FieldConversion#sqlToJava(Object)
    */
    public Object sqlToJava(Object source)
    {
        try
        {
            ByteArrayInputStream stringIn =
                new ByteArrayInputStream(((String) source).getBytes());
            Base64.InputStream uuIn =
                new Base64.InputStream(stringIn, Base64.DECODE, false);
            GZIPInputStream gzipIn = new GZIPInputStream(uuIn);
            ObjectInputStream objIn = new ObjectInputStream(gzipIn);
            Object result = objIn.readObject();

            objIn.close();
            return result;
        }
        catch (Throwable t)
        {
            throw new ConversionException(t);
        }
    }

}
