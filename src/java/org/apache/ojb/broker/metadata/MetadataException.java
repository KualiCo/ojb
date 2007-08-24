package org.apache.ojb.broker.metadata;

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
 * This exception is thrown if a MetaData related problem
 * occurs.
 * @author Thomas Mahler
 */
public class MetadataException 
    extends PersistenceBrokerException
{

    public MetadataException()
    {
        super();
    }

    public MetadataException(Throwable t)
    {
        super(t);
    }

    public MetadataException(String message)
    {
        super(message);
    }
    
    public MetadataException(String message,Throwable t)
    {
        super(message,t);
    }

}
