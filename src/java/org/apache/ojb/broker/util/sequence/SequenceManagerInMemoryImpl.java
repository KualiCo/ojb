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
import org.apache.ojb.broker.metadata.FieldDescriptor;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Very fast in memory sequence manager implementation, only the first
 * time an id was requested for a class, the manager query the database
 * for the max id in requested column - all following request were
 * performed in memory.
 * </p>
 * <b>Limitations:</b>
 * <ul type="disc">
 *	<li>do not use in clustered environments</li>
 *	<li>do not use if other applications generate id's for objects</li>
 * </ul>
 *
 *
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
 *     <td>sequenceStart</td>
 *     <td>
 *          <em>Deprecated, use property 'seq.start'.</em> Set the start index
 *          of used sequences (e.g. set 100000, id generation starts with 100001).
 *          Default start index is <em>1</em>.
 *    </td>
 * </tr>
 * </table>
 *
 * <br/>
 * <br/>
 * <br/>
 *
 * @see org.apache.ojb.broker.util.sequence.SequenceManager
 * @see org.apache.ojb.broker.util.sequence.SequenceManagerFactory
 * @see org.apache.ojb.broker.util.sequence.SequenceManagerHelper
 *
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: SequenceManagerInMemoryImpl.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class SequenceManagerInMemoryImpl extends AbstractSequenceManager
{
    protected static Map sequencesDBMap = new HashMap();
    private long sequenceStart;

    public SequenceManagerInMemoryImpl(PersistenceBroker broker)
    {
        super(broker);
        Long start = SequenceManagerHelper.getSeqStart(getConfigurationProperties());
        sequenceStart = start != null ? start.longValue() : 1;
    }

    protected long getUniqueLong(FieldDescriptor field) throws SequenceManagerException
    {
        String seqName = calculateSequenceName(field);
        // we have to be threadsafe
        synchronized (SequenceManagerInMemoryImpl.class)
        {
            // get id for given seq name
            Long currentId = getSequence(seqName);
            // check - first time we search for sequence name
            if (currentId == null)
            {
                long maxKey = SequenceManagerHelper.getMaxForExtent(getBrokerForClass(), field);
                maxKey = sequenceStart > maxKey ? sequenceStart : maxKey;
                currentId = new Long(maxKey);
            }
            currentId = new Long(currentId.longValue() + 1);
            // put new id back to map
            addSequence(seqName, currentId);
            return currentId.intValue();
        }
    }

    /**
     * Returns last used sequence value or <code>null</code> if no sequence
     * was add for given sequence name.
     *
     * @param sequenceName Name of the sequence.
     * @return Last used sequence value or <code>null</code>
     */
    private Long getSequence(String sequenceName)
    {
        Long result = null;
        // now lookup the sequence map for calling DB
        Map mapForDB = (Map) sequencesDBMap.get(getBrokerForClass()
                .serviceConnectionManager().getConnectionDescriptor().getJcdAlias());
        if(mapForDB != null)
        {
            result = (Long) mapForDB.get(sequenceName);
        }
        return result;
    }

    /**
     * Add new sequence value for sequence name.
     * @param sequenceName Name of the sequence.
     * @param seq The sequence value to add.
     */
    private void addSequence(String sequenceName, Long seq)
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
            synchronized(SequenceManagerInMemoryImpl.class)
            {
                mapForDB.remove(sequenceName);
            }
        }
    }
}
