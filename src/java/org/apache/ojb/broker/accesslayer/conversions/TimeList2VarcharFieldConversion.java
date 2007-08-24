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

import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * This implementation of the {@link FieldConversion} interface converts
 * between a {@link java.util.List} of {@link java.sql.Time} objects and a database
 * <em>varchar</em> field.
 *
 * @author Guillaume Nodet
 * @version $Id: TimeList2VarcharFieldConversion.java,v 1.1 2007-08-24 22:17:31 ewestfal Exp $
 */
public class TimeList2VarcharFieldConversion implements FieldConversion
{

    private static final String NULLVALUE = "#NULL#";
    private static final String EMPTYCOLLEC = "#EMTPY#";

    public TimeList2VarcharFieldConversion()
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
            List timeList = (List) source;
            if (timeList.isEmpty())
            {
                return NULLVALUE;
            }

            StringBuffer result = new StringBuffer();
            for (int i = 0; i < timeList.size(); i++)
            {
                Time obj = (Time) timeList.get(i);
                String newSt = obj.toString();
                // introduced in JDK 1.4, replace with commons-lang
                // newSt = newSt.replaceAll("#", "##");
                newSt = StringUtils.replace(newSt, "#", "##");
                result.append(newSt);
                result.append("#");
            }
            return result.toString();
        }
        catch (ClassCastException e)
        {
            throw new ConversionException("Object is not a List of Time it is a" + source.getClass().getName());
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
        String input = source.toString();
        int pos = input.indexOf("#");

        while (pos >= 0)
        {
            if (pos == 0)
            {
                v.add("");
            }
            else
            {
                v.add(Time.valueOf(input.substring(0, pos)));
            }

            if (pos + 1 > input.length())
            {
                //# at end causes outof bounds
                break;
            }

            input = input.substring(pos + 1, input.length());
            pos = input.indexOf("#");
        }
        return v;
    }
}
