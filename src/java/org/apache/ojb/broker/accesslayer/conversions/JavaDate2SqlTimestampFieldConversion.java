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
 * between java.util.Date values and java.sql.Timestamp values in the rdbms.
 * @author		Matthew Baird
 * @version $Id: JavaDate2SqlTimestampFieldConversion.java,v 1.1 2007-08-24 22:17:31 ewestfal Exp $
 */
public class JavaDate2SqlTimestampFieldConversion implements FieldConversion
{

    /*
     * @see FieldConversion#javaToSql(Object)
     */
    public Object javaToSql(Object source)
    {
        if (source instanceof java.util.Date)
        {
         	return new java.sql.Timestamp( ((java.util.Date) source).getTime());
        }
        else 
        {
         	return source;   
        }
    }

    /*
     * @see FieldConversion#sqlToJava(Object)
     */
    public Object sqlToJava(Object source)
    {
        if (source instanceof java.sql.Timestamp)
        {
         	return new java.util.Date( ((java.sql.Timestamp) source).getTime());  
        }
        else
        {
         	return source;   
        }
    }

}
