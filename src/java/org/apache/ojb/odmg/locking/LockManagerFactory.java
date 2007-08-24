package org.apache.ojb.odmg.locking;

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

import org.apache.commons.lang.SystemUtils;
import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.locking.LockManager;
import org.apache.ojb.broker.util.ClassHelper;
import org.apache.ojb.broker.util.configuration.Configuration;
import org.apache.ojb.broker.util.configuration.impl.OjbConfigurator;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * This factory class creates LockManager instances according
 * to the setting in the OJB properties file.
 */
public class LockManagerFactory
{
    private static LockManagerFactory lockManagerFactory = null;
    private Logger log = LoggerFactory.getLogger(LockManagerFactory.class);
    private org.apache.ojb.odmg.locking.LockManager lockManager;

    private LockManagerFactory()
    {
        init();
    }

    private void init()
    {
        Configuration conf = OjbConfigurator.getInstance().getConfigurationFor(null);
        Class lockMapClass = conf.getClass("LockMapClass", Object.class);
        Class lockManagerClass = conf.getClass("LockManagerClass", null);
        if(lockManagerClass == null)
        {
            throw new OJBRuntimeException(buildErrorMsg(lockMapClass, lockManagerClass));
        }
        if(!lockMapClass.equals(Object.class))
        {
            // use the deprecated old odmg locking stuff
            log.info("Setup *deprecated* odmg-locking api.");
            log.info("Used LockManagerClass=" + lockManagerClass);
            log.info("Used LockMapClass=" + lockMapClass);
            if(!org.apache.ojb.odmg.locking.LockManager.class.isAssignableFrom(lockManagerClass))
            {
                throw new OJBRuntimeException(buildErrorMsg(lockMapClass, lockManagerClass));
            }
            setupLockManager(lockManagerClass);
        }
        else
        {
            // use the kernel locking api
            log.info("Setup odmg-locking api.");
            log.info("Used LockManagerClass=" + lockManagerClass);
            if(org.apache.ojb.odmg.locking.LockManager.class.isAssignableFrom(lockManagerClass))
            {
                throw new OJBRuntimeException(buildErrorMsg(lockMapClass, lockManagerClass));
            }
            setupLockManager(conf, lockManagerClass);
        }
    }

    private void setupLockManager(Configuration conf, Class lockManagerClass)
    {
        long timeout = conf.getInteger("LockTimeout", 60000);
        log.info("LockTimeout=" + timeout);
        try
        {
            LockManager lm = (LockManager) ClassHelper.newInstance(lockManagerClass);
            lm.setLockTimeout(timeout);
            lockManager = new LockManagerOdmgImpl(lm);
        }
        catch(Exception e)
        {
            throw new OJBRuntimeException("Can't setup odmg lock manager instance", e);
        }
    }

    private void setupLockManager(Class lockManagerClass)
    {
        try
        {
            lockManager = (org.apache.ojb.odmg.locking.LockManager) ClassHelper.newInstance(lockManagerClass);

        }
        catch(Exception e)
        {
            throw new OJBRuntimeException("Can't setup odmg lock manager instance", e);
        }
    }


    private String buildErrorMsg(Class lockMap, Class lockManager)
    {
        String eol = SystemUtils.LINE_SEPARATOR;
        StringBuffer msg = new StringBuffer("Can't setup LockManager. Current used properties are:" + eol);
        msg.append("LockMapClass=").append(lockMap != null ? lockMap.getName() : null)
                .append(eol)
                .append("LockManagerClass=").append(lockManager != null ? lockManager.getName() : null).append(eol)
                .append("For correct setup of the lock manager, please enable the 'LockManagerClass' property")
                .append(" in OJB configuration, OJB expects an 'org.apache.ojb.broker.locking.LockManager' implementation class.")
                .append(eol)
                .append("Or to enable the *deprecated* odmg-locking api enable the 'LockMapClass' AND the 'LockManager' properties")
                .append(", in this case OJB expects an 'org.apache.ojb.odmg.locking.LockManager' implementation class.");
        return msg.toString();
    }

    private org.apache.ojb.odmg.locking.LockManager getManager()
    {
        return lockManager;
    }

    /**
     * Get a {@link org.apache.ojb.odmg.locking.LockManager} instance. The implementation class is
     * configured in the OJB properties file.
     */
    public static synchronized org.apache.ojb.odmg.locking.LockManager getLockManager()
    {
        if(lockManagerFactory == null)
        {
            lockManagerFactory = new LockManagerFactory();
        }
        return lockManagerFactory.getManager();
    }
}
