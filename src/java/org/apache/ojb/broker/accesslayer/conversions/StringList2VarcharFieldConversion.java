package org.apache.ojb.broker.accesslayer.conversions;

/* Copyright 2004-2005 The Apache Software Foundation
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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * This implementation of the {@link FieldConversion} interface converts
 * between a {@link java.util.List} of {@link java.lang.String} objects and a database
 * <em>varchar</em> field.
 * <br/>
 * Strings may not contain "#" as this is used as separator.
 *
 * @author Guillaume Nodet
 * @version $Id: StringList2VarcharFieldConversion.java,v 1.1 2007-08-24 22:17:31 ewestfal Exp $
 */
public class StringList2VarcharFieldConversion implements FieldConversion
{

    private static final String NULLVALUE = "#NULL#";
    private static final String EMPTYCOLLEC = "#EMTPY#";

    public StringList2VarcharFieldConversion()
    {
    }

    /* (non-Javadoc)
     * @see org.apache.ojb.broker.accesslayer.conversions.FieldConversion#javaToSql(java.lang.Object)
     */
    public Object javaToSql(Object source) throws ConversionException
    {

        if (source == null)
        {
            return NULLVALUE;
        }

        try
        {
            List stringList = (List) source;
            if (stringList.isEmpty())
            {
                return NULLVALUE;
            }

            StringBuffer result = new StringBuffer();
            for (int i = 0; i < stringList.size(); i++)
            {
                String newSt = (String) stringList.get(i);
                // introduced in JDK 1.4, replace with commons-lang
                // newSt = newSt.replaceAll("#", "##");
                newSt = StringUtils.replace(newSt, "#", "##");
                if (i > 0)
                {
                    result.append("#");
                }
                result.append(newSt);
            }
            return result.toString();
        }
        catch (ClassCastException e)
        {
            throw new ConversionException("Object is not a List of String it is a"
                    + source.getClass().getName());
        }
    }

    /* (non-Javadoc)
     * @see org.apache.ojb.broker.accesslayer.conversions.FieldConversion#sqlToJava(java.lang.Object)
     */
    public Object sqlToJava(Object source) throws ConversionException
    {
        if (source == null)
        {
            return null;
        }
        if (source.toString().equals(NULLVALUE))
        {
            return null;
        }
        if (source.toString().equals(EMPTYCOLLEC))
        {
            return new ArrayList();
        }
        List v = new ArrayList();
        StringBuffer input = new StringBuffer();
        StringBuffer newString = new StringBuffer();
        int pos = 0;
        int length;

        input.append(source.toString());
        length = input.length();
        while (pos < length)
        {
            if (input.charAt(pos) != '#')
            {
                newString.append(input.charAt(pos));
            }
            else
            {
                if (input.charAt(pos + 1) != '#')
                {
                    v.add(newString.toString());
                    newString = new StringBuffer();
                }
                else
                {
                    newString.append('#');
                    ++pos;
                }
            }
            ++pos;
        }
        v.add(newString.toString());
        return v;
    }
}
