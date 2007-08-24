package org.apache.ojb.broker.locking;

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

import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.commons.lang.StringUtils;

/**
 * @author <a href="mailto:arminw@apache.org">Armin Waibel</a>
 * @version $Id: LockHelper.java,v 1.1 2007-08-24 22:17:41 ewestfal Exp $
 */
public class LockHelper
{
    LockHelper()
    {
    }

    /**
     * maps IsolationLevel literals to the corresponding id
     * @param isoLevel
     * @return the id
     */
    public static int getIsolationLevelFor(String isoLevel)
    {
        if(isoLevel == null || StringUtils.isEmpty(isoLevel))
        {
            LoggerFactory.getDefaultLogger().debug(
                    "[LockHelper] Specified isolation level string is 'null', using the default isolation level");
            return IsolationLevels.IL_DEFAULT;
        }
        if (isoLevel.equalsIgnoreCase(IsolationLevels.LITERAL_IL_READ_UNCOMMITTED))
        {
            return IsolationLevels.IL_READ_UNCOMMITTED;
        }
        else if (isoLevel.equalsIgnoreCase(IsolationLevels.LITERAL_IL_READ_COMMITTED))
        {
            return IsolationLevels.IL_READ_COMMITTED;
        }
        else if (isoLevel.equalsIgnoreCase(IsolationLevels.LITERAL_IL_REPEATABLE_READ))
        {
            return IsolationLevels.IL_REPEATABLE_READ;
        }
        else if (isoLevel.equalsIgnoreCase(IsolationLevels.LITERAL_IL_SERIALIZABLE))
        {
            return IsolationLevels.IL_SERIALIZABLE;
        }
        else if (isoLevel.equalsIgnoreCase(IsolationLevels.LITERAL_IL_OPTIMISTIC))
        {
            return IsolationLevels.IL_OPTIMISTIC;
        }
        else if (isoLevel.equalsIgnoreCase(IsolationLevels.LITERAL_IL_NONE))
        {
            return IsolationLevels.IL_NONE;
        }
        LoggerFactory.getDefaultLogger().warn("[LockHelper] Unknown isolation-level '" + isoLevel + "', using default isolation level");
        return IsolationLevels.IL_DEFAULT;
    }
}
