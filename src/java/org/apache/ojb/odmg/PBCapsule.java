package org.apache.ojb.odmg;

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

import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.PBFactoryException;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.odmg.Transaction;

public final class PBCapsule
{
    private static Logger log = LoggerFactory.getLogger(PBCapsule.class);

    PersistenceBroker broker;
    PBKey pbKey;
    Transaction tx;
    boolean needsTxCommit = false;
    boolean needsPBCommit = false;
    boolean isIlleagal = false;

    public PBCapsule(final PBKey pbKey, final Transaction tx)
    {
        this.tx = tx;
        this.pbKey = pbKey;
        prepare();
    }

    public PersistenceBroker getBroker()
    {
        if(isIlleagal) throw new OJBRuntimeException("You could not reuse PBCapsule after destroy");
        return broker;
    }

    private void prepare()
    {
        if(isIlleagal) throw new OJBRuntimeException("You could not reuse PBCapsule after destroy");
        // we allow queries even if no ODMG transaction is running.
        // thus we use direct access to PBF via the given PBKey to
        // get a PB instance
        if (tx == null)
        {
            if (log.isDebugEnabled())
                log.debug("No running transaction found, try to get " +
                        "PersistenceBroker instance via PBKey " + pbKey);
            broker = obtainBroker();
            // begin tx on the PB instance
            if (!broker.isInTransaction())
            {
                broker.beginTransaction();
                needsPBCommit = true;
            }
        }
        else
        {
            // we allow to work with unopened transactions.
            // we assume that such a tx is to be closed after performing the query
            if (!tx.isOpen())
            {
                tx.begin();
                needsTxCommit = true;
            }
            // obtain a broker instance from the current transaction
            broker = ((HasBroker) tx).getBroker();
        }
    }

    public void destroy()
    {
        if (needsTxCommit)
        {
            if (log.isDebugEnabled()) log.debug("Indicated to commit tx");
            tx.commit();
        }
        else if (needsPBCommit)
        {
            if (log.isDebugEnabled()) log.debug("Indicated to commit PersistenceBroker");
            try
            {
                broker.commitTransaction();
            }
            finally
            {
                if (broker != null) broker.close();
            }
        }
        isIlleagal = true;
        needsTxCommit = false;
        needsPBCommit = false;
    }

    /**
     * Used to get PB, when no tx is running.
     */
    private PersistenceBroker obtainBroker()
    {
        PersistenceBroker _broker;
        try
        {
            if (pbKey == null)
            {
                //throw new OJBRuntimeException("Not possible to do action, cause no tx runnning and no PBKey is set");
                log.warn("No tx runnning and PBKey is null, try to use the default PB");
                _broker = PersistenceBrokerFactory.defaultPersistenceBroker();
            }
            else
            {
                _broker = PersistenceBrokerFactory.createPersistenceBroker(pbKey);
            }
        }
        catch (PBFactoryException e)
        {
            log.error("Could not obtain PB for PBKey " + pbKey, e);
            throw new OJBRuntimeException("Unexpected micro-kernel exception", e);
        }
        return _broker;
    }
}
