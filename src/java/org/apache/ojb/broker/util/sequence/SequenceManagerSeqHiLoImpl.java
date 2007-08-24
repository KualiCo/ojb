package org.apache.ojb.broker.util.sequence;

/* Copyright 2003-2005 The Apache Software Foundation
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

/**
 * <p>
 * A High/Low database sequence based implementation.
 * See {@link org.apache.ojb.broker.util.sequence.SequenceManagerNextValImpl}
 * for more information.
 * </p>
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
 * </table>
 * <br/>
 * <p>
 * <b>Limitations:</b>
 * <ul>
 *	<li>do not use when other applications use the database based sequence ditto</li>
 * </ul>
 * </p>
 * <br/>
 * <br/>
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: SequenceManagerSeqHiLoImpl.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class SequenceManagerSeqHiLoImpl extends SequenceManagerNextValImpl
{
    public static final String PROPERTY_GRAB_SIZE = SequenceManagerHighLowImpl.PROPERTY_GRAB_SIZE;
    private static HashMap hiLoMap = new HashMap();

    protected int grabSize;

    public SequenceManagerSeqHiLoImpl(PersistenceBroker broker)
    {
        super(broker);
        grabSize = Integer.parseInt(getConfigurationProperty(PROPERTY_GRAB_SIZE, "20"));
    }

    protected long getUniqueLong(FieldDescriptor field) throws SequenceManagerException
    {
        String sequenceName = calculateSequenceName(field);
        // we have to be threadsafe
        synchronized (hiLoMap)
        {
            HiLoEntry entry = (HiLoEntry) hiLoMap.get(sequenceName);
            if (entry == null)
            {
                entry = new HiLoEntry(grabSize, grabSize);
                hiLoMap.put(sequenceName, entry);
            }
            if (entry.needNewSequence())
            {
                entry.maxVal = grabSize * (super.getUniqueLong(field) + 1);
                entry.counter = 0;
            }
            return entry.nextVal();
        }
    }

    class HiLoEntry
    {
        long maxVal;
        long counter;

        public HiLoEntry(long maxVal, long counter)
        {
            this.maxVal = maxVal;
            this.counter = counter;
        }

        boolean needNewSequence()
        {
            return counter == grabSize;
        }

        long nextVal()
        {
            return maxVal + counter++;
        }
    }
}
