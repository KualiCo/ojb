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
import org.apache.ojb.broker.util.GUID;

/**
 * this implementation of the FieldConversion interface converts
 * between GUIDs and their String representation.
 * @author Thomas Mahler
 * @version $Id: GUID2StringFieldConversion.java,v 1.1 2007-08-24 22:17:31 ewestfal Exp $
 */
public class GUID2StringFieldConversion implements FieldConversion
{

    /**
     * @see FieldConversion#javaToSql(Object)
     */
    public Object javaToSql(Object source)
    {
        if (source instanceof GUID)
        {
        	GUID guid = (GUID) source;
        	return guid.toString();
        }
        return source;
    }

    /**
     * @see FieldConversion#sqlToJava(Object)
     */
    public Object sqlToJava(Object source)
    {   	
    	if (source instanceof String)
    	{
        	return new GUID((String) source);
    	}
    	return source;
    }

}
