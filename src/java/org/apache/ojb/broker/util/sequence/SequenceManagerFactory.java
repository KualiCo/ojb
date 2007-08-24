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

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.metadata.SequenceDescriptor;
import org.apache.ojb.broker.util.ClassHelper;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * Threadsafe factory class, creates <code>SequenceManager</code> instances.
 * The implementation class is configured by the OJB.properties file.
 */
public class SequenceManagerFactory
{
    private static Logger log = LoggerFactory.getLogger(SequenceManagerFactory.class);
    private static SequenceManagerFactory singleton;

    private Class defaultSeqManagerClass;

    public SequenceManagerFactory()
    {
        defaultSeqManagerClass = SequenceManagerHighLowImpl.class;
        if(log.isDebugEnabled()) log.debug("Default sequence manager class was " + defaultSeqManagerClass.getName());
    }

    public synchronized static SequenceManager getSequenceManager(PersistenceBroker broker)
    {
        if (singleton == null)
        {
            singleton = new SequenceManagerFactory();
        }
        return singleton.createNewSequenceManager(broker);
    }

    private SequenceManager createNewSequenceManager(PersistenceBroker broker)
    {
        synchronized (singleton)
        {
            if (log.isDebugEnabled()) log.debug("create new sequence manager for broker " + broker);
            try
            {
                // first we use seqMan defined in the OJB.properties
                Class seqManClass = defaultSeqManagerClass;
                SequenceDescriptor sd = broker.serviceConnectionManager().getConnectionDescriptor().getSequenceDescriptor();
                if (sd != null && sd.getSequenceManagerClass() != null)
                {
                    // if a seqMan was defined in repository, use that
                    seqManClass = sd.getSequenceManagerClass();
                    if (log.isDebugEnabled())
                    {
                        log.debug("Jdbc-Connection-Descriptor '" +
                                broker.serviceConnectionManager().getConnectionDescriptor().getJcdAlias() +
                                "' use sequence manager: " + seqManClass);
                    }
                }
                return (SequenceManager) ClassHelper.newInstance(seqManClass, PersistenceBroker.class, broker);
            }
            catch (Exception ex)
            {
                log.error("Could not create sequence manager for broker " + broker, ex);
                throw new PersistenceBrokerException(ex);
            }
        }
    }
}
