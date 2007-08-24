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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.SystemUtils;
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.OptimisticLockException;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.util.ObjectModification;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * High/Low sequence manager implementation generates unique and continuous
 * id's (during runtime) by using sequences to avoid database access.
 * <br/>
 *
 * <p>
 * Implementation configuration properties:
 * </p>
 *
 * <table cellspacing="2" cellpadding="2" border="3" frame="box">
 * <tr>
 *     <td><strong>Property Key</strong></td>
 *     <td><strong>Property Values</strong></td>
 * </tr>
 * <tr>
 *     <td>seq.start</td>
 *     <td>
 *         Set the start index of used sequences (e.g. set 100000, id generation starts with 100001).
 *         Default start index is <em>1</em>.
 *    </td>
 * </tr>
 * <tr>
 *     <td>grabSize</td>
 *     <td>
 *         Integer entry determines the
 *         number of IDs allocated within the
 *         H/L sequence manager implementation.
 *         Default was '20'.
 *    </td>
 * </tr>
 * <tr>
 *     <td>autoNaming</td>
 *     <td>
 *          Default was 'true'. If set 'true' OJB try to build a
 *          sequence name automatic if none found in field-descriptor
 *          and set this generated name as <code>sequence-name</code>
 *          in field-descriptor. If set 'false' OJB throws an exception
 *          if none sequence name was found in field-descriptor.
 *    </td>
 * </tr>
 * <tr>
 *     <td>globalSequenceId</td>
 *     <td>
 *         Deprecated! If set 'true' implementation use global unique
 *         id's for all fields. Default was 'false'.
 *    </td>
 * </tr>
 * <tr>
 *     <td>globalSequenceStart</td>
 *     <td>
 *         <em>Deprecated, use property 'seq.start'.</em> Set the start index of used global id
 *         generation (e.g. set 100000, id generation starts with 100001)
 *    </td>
 * </tr>
 *  <tr>
 *     <td>sequenceStart</td>
 *     <td>
 *         <em>Deprecated, use property 'seq.start'.</em> Set the start index of used
 *          sequences (e.g. set 100000, id generation starts with 100001). Default start index is <em>1</em>.
 *    </td>
 * </tr>
 * </table>
 *
 * <br/>
 * <p>
 * <b>Limitations:</b>
 * <ul>
 *	<li>Do NOT use this implementation in managed environment or
 * any comparable system where any connection was associated
 * with the running transaction.</li>
 * </ul>
 * </p>
 *
 *
 * <br/>
 * <br/>
 *
 *
 * @see org.apache.ojb.broker.util.sequence.SequenceManager
 * @see org.apache.ojb.broker.util.sequence.SequenceManagerFactory
 * @see org.apache.ojb.broker.util.sequence.SequenceManagerHelper
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: SequenceManagerHighLowImpl.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class SequenceManagerHighLowImpl extends AbstractSequenceManager
{
    private static Logger log = LoggerFactory.getLogger(SequenceManagerHighLowImpl.class);
    /**
     * sequence name used for global id generation.
     */
    private static final String GLOBAL_SEQUENCE_NAME = "global - default sequence name";
    public static final String PROPERTY_GRAB_SIZE = "grabSize";
    public static final String PROPERTY_GLOBAL_SEQUENCE_ID = "globalSequenceId";
    public static final String PROPERTY_GLOBAL_SEQUENCE_START = "globalSequenceStart";

    protected static Map sequencesDBMap = new HashMap();

    protected boolean useGlobalSequenceIdentities;
    protected int grabSize;
    protected long sequenceStart;
    protected int attempts;

    public SequenceManagerHighLowImpl(PersistenceBroker broker)
    {
        super(broker);
        Long start = SequenceManagerHelper.getSeqStart(getConfigurationProperties());
        sequenceStart = start != null ? start.longValue() : 1;
        grabSize = Integer.parseInt(getConfigurationProperty(PROPERTY_GRAB_SIZE, "20"));
        useGlobalSequenceIdentities = Boolean.getBoolean(getConfigurationProperty(PROPERTY_GLOBAL_SEQUENCE_ID, "false"));
        // support for deprecated properties
        long globalSequenceStart = Long.parseLong(getConfigurationProperty(PROPERTY_GLOBAL_SEQUENCE_START, "1"));
        if(useGlobalSequenceIdentities && globalSequenceStart > sequenceStart)
        {
            sequenceStart = globalSequenceStart;
        }
    }

    protected long getUniqueLong(FieldDescriptor field) throws SequenceManagerException
    {
        HighLowSequence seq;
        String sequenceName = buildSequenceName(field);
        synchronized (SequenceManagerHighLowImpl.class)
        {
            // try to find sequence
            seq = getSequence(sequenceName);

            if (seq == null)
            {
                // not found, get sequence from database or create new
                seq = getSequence(getBrokerForClass(), field, sequenceName);
                addSequence(sequenceName, seq);
            }

            // now we have a sequence
            long id = seq.getNextId();
            // seq does not have reserved IDs => catch new block of keys
            if (id == 0)
            {
                seq = getSequence(getBrokerForClass(), field, sequenceName);
                // replace old sequence!!
                addSequence(sequenceName, seq);
                id = seq.getNextId();
                if (id == 0)
                {
                    // something going wrong
                    removeSequence(sequenceName);
                    throw new SequenceManagerException("Sequence generation failed: " +
                            SystemUtils.LINE_SEPARATOR + "Sequence: " + seq +
                            ". Unable to build new ID, id was always 0." +
                            SystemUtils.LINE_SEPARATOR + "Thread: " + Thread.currentThread() +
                            SystemUtils.LINE_SEPARATOR + "PB: " + getBrokerForClass());
                }
            }
            return id;
        }
    }

    /**
     * Returns last used sequence object or <code>null</code> if no sequence
     * was add for given sequence name.
     *
     * @param sequenceName Name of the sequence.
     * @return Sequence object or <code>null</code>
     */
    private HighLowSequence getSequence(String sequenceName)
    {
        HighLowSequence result = null;
        // now lookup the sequence map for calling DB
        Map mapForDB = (Map) sequencesDBMap.get(getBrokerForClass()
                .serviceConnectionManager().getConnectionDescriptor().getJcdAlias());
        if(mapForDB != null)
        {
            result = (HighLowSequence) mapForDB.get(sequenceName);
        }
        return result;
    }

    /**
     * Put new sequence object for given sequence name.
     * @param sequenceName Name of the sequence.
     * @param seq The sequence object to add.
     */
    private void addSequence(String sequenceName, HighLowSequence seq)
    {
        // lookup the sequence map for calling DB
        String jcdAlias = getBrokerForClass()
                .serviceConnectionManager().getConnectionDescriptor().getJcdAlias();
        Map mapForDB = (Map) sequencesDBMap.get(jcdAlias);
        if(mapForDB == null)
        {
            mapForDB = new HashMap();
        }
        mapForDB.put(sequenceName, seq);
        sequencesDBMap.put(jcdAlias, mapForDB);
    }

    /**
     * Remove the sequence for given sequence name.
     *
     * @param sequenceName Name of the sequence to remove.
     */
    protected void removeSequence(String sequenceName)
    {
        // lookup the sequence map for calling DB
        Map mapForDB = (Map) sequencesDBMap.get(getBrokerForClass()
                .serviceConnectionManager().getConnectionDescriptor().getJcdAlias());
        if(mapForDB != null)
        {
            synchronized(SequenceManagerHighLowImpl.class)
            {
                mapForDB.remove(sequenceName);
            }
        }
    }

    protected HighLowSequence getSequence(PersistenceBroker brokerForSequence,
                                        FieldDescriptor field,
                                        String sequenceName)  throws SequenceManagerException
    {
        HighLowSequence newSequence = null;
        PersistenceBroker internBroker = null;
        try
        {
            /*
            arminw:
            we use a new internBroker instance, because we run into problems
            when current internBroker was rollback, then we have new sequence
            in memory, but not in database and a concurrent thread will
            get the same sequence.
            Thus we use a new internBroker instance (with new connection) to
            avoid this problem.
            */
            internBroker = PersistenceBrokerFactory.createPersistenceBroker(brokerForSequence.getPBKey());
            internBroker.beginTransaction();

            newSequence = lookupStoreSequence(internBroker, field, sequenceName);

            internBroker.commitTransaction();

            if (log.isDebugEnabled()) log.debug("new sequence was " + newSequence);
        }
        catch(Exception e)
        {
            log.error("Can't lookup new HighLowSequence for field "
                    + (field != null ? field.getAttributeName() : null)
                    + " using sequence name " + sequenceName, e);
            if(internBroker != null && internBroker.isInTransaction()) internBroker.abortTransaction();
            throw new SequenceManagerException("Can't build new sequence", e);
        }
        finally
        {
            attempts = 0;
            if (internBroker != null) internBroker.close();
        }
        return newSequence;
    }

    protected HighLowSequence lookupStoreSequence(PersistenceBroker broker, FieldDescriptor field, String seqName)
    {
        HighLowSequence newSequence;
        boolean needsInsert = false;

        Identity oid = broker.serviceIdentity().buildIdentity(HighLowSequence.class, seqName);
        // first we lookup sequence object in database
        newSequence = (HighLowSequence) broker.getObjectByIdentity(oid);

        //not in db --> we have to store a new sequence
        if (newSequence == null)
        {
            if (log.isDebugEnabled())
            {
                log.debug("sequence for field " + field + " not found in db, store new HighLowSequence");
            }
            /*
            here we lookup the max key for the given field in system
            */
            // !!! here we use current broker instance to avoid deadlock !!!
            long maxKey = getMaxKeyForSequence(getBrokerForClass(), field);

            newSequence = newSequenceObject(seqName, field);
            newSequence.setMaxKey(maxKey);
            needsInsert = true;
        }
        // maybe property 'sequenceStart' was changed, so we check maxKey against
        // current set sequence start index
        if(newSequence.getMaxKey() < sequenceStart)
        {
            newSequence.setMaxKey(sequenceStart);
        }

        // set current grab size
        newSequence.setGrabSize(grabSize);

        //grab the next key scope
        newSequence.grabNextKeySet();

        //store the sequence to db
        try
        {
            if(needsInsert) broker.store(newSequence, ObjectModification.INSERT);
            else broker.store(newSequence, ObjectModification.UPDATE);
        }
        catch (OptimisticLockException e)
        {
            // we try five times to get a new sequence
            if(attempts < 5)
            {
                log.info("OptimisticLockException was thrown, will try again to store sequence. Sequence was "+newSequence);
                attempts++;
                newSequence = lookupStoreSequence(broker, field, seqName);
            }
            else throw e;
        }
        return newSequence;
    }

    protected HighLowSequence newSequenceObject(String sequenceName,
                                              FieldDescriptor field)
    {
        HighLowSequence seq = new HighLowSequence();
        seq.setName(sequenceName);
        seq.setGrabSize(grabSize);
        return seq;
    }

    protected long getMaxKeyForSequence(PersistenceBroker broker,
                                        FieldDescriptor field)
    {
        long maxKey;
        if (useGlobalSequenceIdentities)
        {
            maxKey = sequenceStart;
        }
        else
        {
            /*
            here we lookup the max key for the given field in system
            */
            maxKey = SequenceManagerHelper.getMaxForExtent(broker, field);
            // check against start index
            maxKey = sequenceStart > maxKey ? sequenceStart : maxKey;
        }
        return maxKey;
    }

    private String buildSequenceName(FieldDescriptor field) throws SequenceManagerException
    {
        String seqName;
        if (useGlobalSequenceIdentities)
        {
            seqName = GLOBAL_SEQUENCE_NAME;
        }
        else
        {
            seqName = calculateSequenceName(field);
        }
        return seqName;
    }
}
