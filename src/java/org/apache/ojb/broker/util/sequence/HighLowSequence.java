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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.io.Serializable;

/**
 * The HighLowSequence is the persistent part of the {@link SequenceManagerHighLowImpl}.
 * It makes the maximum reserved key persistently available.
 *
 * @version $Id: HighLowSequence.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class HighLowSequence implements Serializable
{
	static final long serialVersionUID = -2174468157880921393L;
    private String name;
    private long maxKey;
    private int grabSize;
    private Integer version;
     // This is not stored in the DB
    protected long curVal = 0;

    /**
     * Default constructor for the HighLowSequence object
     */
    public HighLowSequence()
    {
        // make sure that version column in DB is never 'null'
        // to avoid problems with
        this(null, 0, 0, new Integer(0));
    }

    public HighLowSequence(String tableName, long maxKey, int grabSize, Integer version)
    {
        this.name = tableName;
        this.maxKey = maxKey;
        this.grabSize = grabSize;
        this.version = version;
    }

    public HighLowSequence getCopy()
    {
        HighLowSequence result = new HighLowSequence(this.name, this.maxKey, this.grabSize, this.version);
        result.curVal = this.curVal;
        return result;
    }

    public String toString()
    {
        ToStringBuilder buf = new ToStringBuilder(this, ToStringStyle.DEFAULT_STYLE);
        buf.append("name", name).
                append("grabSize", grabSize).
                append("version", version).
                append("maxKey", maxKey).
                append("currentKey", curVal);
        return buf.toString();
    }

    public Integer getVersion()
    {
        return version;
    }

    public void setVersion(Integer version)
    {
        this.version = version;
    }

    /**
     * Sets the name attribute of the HighLowSequence object
     *
     * @param name  The new className value
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Sets the grab size attribute of the HighLowSequence object
     *
     * @param grabSize  The new grabSize value
     */
    public void setGrabSize(int grabSize)
    {
        this.grabSize = grabSize;
    }

    /**
     * Sets the maxKey attribute of the HighLowSequence object
     *
     * @param maxKey  The new maxKey value
     */
    public void setMaxKey(long maxKey)
    {
        this.maxKey = maxKey;
    }

    /**
     * Gets the name attribute of the HighLowSequence object
     *
     * @return   The className value
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * Gets the grabSize attribute of the HighLowSequence object
     *
     * @return   The grabSize value
     */
    public int getGrabSize()
    {
        return this.grabSize;
    }

    /**
     * Gets the next key from this sequence
     *
     * @return   The next key or 0 if sequence needs to grab new keyset
     */
    public long getNextId()
    {
        if (curVal == maxKey)
        {
            //no reserved IDs, must be reloaded, reserve new keyset and saved
            return 0;
        }
        else
        {
            curVal = curVal + 1;
            return curVal;
        }
    }

    /**
     * Gets the maxKey attribute of the HighLowSequence object
     *
     * @return   The maxKey value
     */
    public long getMaxKey()
    {
        return this.maxKey;
    }

    /**
     * Grabs the next key set, the sequence must be saved afterwards!!
     */
    public void grabNextKeySet()
    {
        curVal = maxKey;
        maxKey = maxKey + grabSize;
    }
}
