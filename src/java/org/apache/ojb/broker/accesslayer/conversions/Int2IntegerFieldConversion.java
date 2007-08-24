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

/**
 * this implementation of the FieldConversion interface converts 
 * int to Integer, Integer to int. If the int is zero, then
 * we don't have a value for the key and it must be set to null, not
 * zero.
 * @author Aaron Oathout
 * @version $Id: Int2IntegerFieldConversion.java,v 1.1 2007-08-24 22:17:31 ewestfal Exp $
 */
public class Int2IntegerFieldConversion implements FieldConversion
{

    private static final Integer NULL_INTEGER = null;
    private static final Integer ZERO = new Integer(0);

    public Object javaToSql(Object obj) throws ConversionException
    {
        if (obj instanceof Integer)
        {
            Integer instance = (Integer) obj;
            if (instance.equals(ZERO))
            {
                return NULL_INTEGER;
            }
            else
            {
                return obj;
            }
        }
        else
        {
            return obj;
        }
    }

    public Object sqlToJava(Object obj) throws ConversionException
    {
        return obj;
    }
}
