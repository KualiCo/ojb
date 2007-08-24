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

import java.util.Vector;

/**
 * Converts a Vector of string elements back and forth from a database varchar field
 * Strings may not contain "#" as this is used as separator.
 * This class maybe useful if it's important to have the string vector stored in a 
 * human readable form that allows editing.
 * @see Object2ByteArrFieldConversion uses Java serialization and is not suited for
 * this purpose. 
 * 
 * @author  sschloesser  mailto: stefan.schl@gmx.de 
 * @version $Id: StringVector2VarcharFieldConversion.java,v 1.1 2007-08-24 22:17:31 ewestfal Exp $
 */
public class StringVector2VarcharFieldConversion implements FieldConversion
{

    private static final String NULLVALUE = "#NULL#";
    private static final String EMPTYCOLLEC = "#EMTPY#";
    private static final String SEPARATOR = "#";

    /** Creates a new instance of StringVector2VarcharFieldConversion */
    public StringVector2VarcharFieldConversion()
    {
    }

    public Object javaToSql(Object obj)
        throws org.apache.ojb.broker.accesslayer.conversions.ConversionException
    {

        if (obj == null)
        {
            return NULLVALUE;
        }

        if (!(obj instanceof Vector))
        {
            throw new ConversionException(
                "Object is not a vector it is a" + obj.getClass().getName());
        }

        Vector v = (Vector) obj;
        if (v.size() == 0)
        {
            return EMPTYCOLLEC;
        }

        StringBuffer result = new StringBuffer();
        for (int i = 0; i < v.size(); i++)
        {
            String newSt = v.get(i).toString();
            if (newSt.indexOf(SEPARATOR) >= 0)
            {
                throw new ConversionException(
                    "An entry in the Vector contains the forbidden "
                        + SEPARATOR
                        + " character used to separate the strings on the DB");
            }
            result.append(newSt);
            result.append(SEPARATOR);
        }
        return result.toString();
    }

    public Object sqlToJava(Object obj)
        throws org.apache.ojb.broker.accesslayer.conversions.ConversionException
    {

        if (obj == null)
        {
            return null;
        }
        if (obj.toString().equals(NULLVALUE))
        {
            return null;
        }
        if (obj.toString().equals(EMPTYCOLLEC))
        {
            return new Vector();
        }

        Vector v = new Vector();
        String input = obj.toString();
        int pos = input.indexOf(SEPARATOR);

        while (pos >= 0)
        {
            if (pos == 0)
            {
                v.add("");
            }
            else
            {
                v.add(input.substring(0, pos));
            }

            if (pos + 1 > input.length()) //# at end causes outof bounds
            {
                break;
            }

            input = input.substring(pos + 1, input.length());
            pos = input.indexOf(SEPARATOR);
        }
        return v;
    }
}
