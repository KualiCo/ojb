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

import java.io.Serializable;

import org.apache.commons.lang.SerializationUtils;

/**
 * This implementation of the FieldConversion interface converts
 * between java.lang.Objects values and byte[] values in the rdbms.
 * This conversion is useful to store serialized objects in database
 * columns.
 */
public class Object2ByteArrUncompressedFieldConversion implements FieldConversion
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
            return SerializationUtils.serialize((Serializable) source);
        }
        catch(Throwable t)
        {
            throw new ConversionException(t);
        }
    }

    /*
     * @see FieldConversion#sqlToJava(Object)
     */
    public Object sqlToJava(Object source)
    {
        if(source == null)
            return null;
        try
        {
            return SerializationUtils.deserialize((byte[]) source);
        }
        catch(Throwable t)
        {
            throw new ConversionException(t);
        }
    }

}
