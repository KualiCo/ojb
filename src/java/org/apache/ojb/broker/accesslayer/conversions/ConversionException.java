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

import org.apache.ojb.broker.PersistenceBrokerException;

/**
 * A ConversionException can be thrown by implementors of FieldConversion
 * to signal failures during the conversion process.
 * @author Thomas Mahler
 * @version $Id: ConversionException.java,v 1.1 2007-08-24 22:17:31 ewestfal Exp $
 */
public class ConversionException extends PersistenceBrokerException
{

    /**
     * Constructor for ConversionException.
     */
    public ConversionException()
    {
        super();
    }

    /**
     * Constructor for ConversionException.
     * @param message
     */
    public ConversionException(String message)
    {
        super(message);
    }

    /**
     * Constructor for ConversionException.
     * @param message
     * @param cause
     */
    public ConversionException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * Constructor for ConversionException.
     * @param cause
     */
    public ConversionException(Throwable cause)
    {
        super(cause);
    }

}
