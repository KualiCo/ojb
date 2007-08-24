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

/**
 * This implementation of the FieldConversion interface converts
 * between java.lang.Objects values and byte[] values in the rdbms.
 * This conversion is useful to store serialized objects in database
 * columns.
 * <p/>
 * NOTE: This implementation use {@link java.util.zip.GZIPOutputStream} and
 * {@link java.util.zip.GZIPInputStream} to compress data.
 *
 * @see Object2ByteArrUncompressedFieldConversion
 * @author Thomas Mahler
 * @version $Id: Object2ByteArrFieldConversion.java,v 1.1 2007-08-24 22:17:31 ewestfal Exp $
 */
public class Object2ByteArrFieldConversion implements FieldConversion
{

    /*
     * @see FieldConversion#javaToSql(Object)
     */
    public Object javaToSql(Object source)
    {
        if (source == null)
            return null;
        try
        {
            ByteArrayOutputStream bao = new ByteArrayOutputStream();
            GZIPOutputStream gos = new GZIPOutputStream(bao);
            ObjectOutputStream oos = new ObjectOutputStream(gos);
            oos.writeObject(source);
            oos.close();
            gos.close();
            bao.close();
            byte[] result = bao.toByteArray();
            return result;
        }
        catch (Throwable t)
        {
            throw new ConversionException(t);
        }
    }

    /*
     * @see FieldConversion#sqlToJava(Object)
     */
    public Object sqlToJava(Object source)
    {
         if (source == null)
            return null;
        try
        {
            ByteArrayInputStream bais = new ByteArrayInputStream((byte[]) source);
            GZIPInputStream gis = new GZIPInputStream(bais);
            ObjectInputStream ois = new ObjectInputStream(gis);
            Object result = ois.readObject();
            ois.close();
            gis.close();
            bais.close();
            return result;
        }
        catch (Throwable t)
        {
            throw new ConversionException(t);
        }
    }

}
