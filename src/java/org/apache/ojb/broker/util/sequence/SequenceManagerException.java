package org.apache.ojb.broker.util.sequence;

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

import org.apache.ojb.broker.OJBException;

/**
 * An exception thrown by {@link org.apache.ojb.broker.util.sequence.SequenceManager}
 * implementations.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: SequenceManagerException.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class SequenceManagerException extends OJBException
{
    public SequenceManagerException(String msg)
    {
        super(msg);
    }

    public SequenceManagerException(Throwable cause)
    {
        super(cause);
    }

    public SequenceManagerException(String msg, Throwable cause)
    {
        super(msg, cause);
    }
}
