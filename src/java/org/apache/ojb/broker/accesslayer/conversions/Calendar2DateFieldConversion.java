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

import java.sql.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * This implementation of the {@link FieldConversion} interface converts
 * between {@link java.util.Calendar} and {@link java.sql.Date}. When
 * convert sql to java always a {@link java.util.GregorianCalendar}
 * object is returned.
 *
 * @author Guillaume Nodet
 * @version $Id: Calendar2DateFieldConversion.java,v 1.1 2007-08-24 22:17:31 ewestfal Exp $
 */
public class Calendar2DateFieldConversion implements FieldConversion
{
    /**
     * @see FieldConversion#javaToSql(Object)
     */
    public Object javaToSql(Object source)
    {
        if (source instanceof Calendar)
        {
            // only valid >= JDK 1.4
            // return new Date(((Calendar) source).getTimeInMillis());
            return new Date(((Calendar) source).getTime().getTime());
        }
        else
        {
            return source;
        }
    }

    /**
     * @see FieldConversion#sqlToJava(Object)
     */
    public Object sqlToJava(Object source)
    {
        if (source instanceof Date)
        {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(((Date) source));
            return cal;
        }
        else
        {
            return source;
        }
    }
}


