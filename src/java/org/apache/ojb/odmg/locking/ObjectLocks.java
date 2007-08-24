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

import java.util.Hashtable;

class ObjectLocks
{
    private LockEntry writer;

    private Hashtable readers;
    
    private long m_youngestReader = 0;

    public LockEntry getWriter()
    {
        return writer;
    }

    public void setWriter(LockEntry writer)
    {
        this.writer = writer;
    }

    public Hashtable getReaders()
    {
        return readers;
    }

    public void addReader(LockEntry reader)
    {
    	/**
    	 * MBAIRD:
    	 * we want to track the youngest reader so we can remove all readers at timeout
    	 * if the youngestreader is older than the timeoutperiod.
    	 */
    	if ((reader.getTimestamp() < m_youngestReader) || (m_youngestReader==0))
    	{
    		m_youngestReader = reader.getTimestamp();
    	}
        this.readers.put(reader.getTransactionId(), reader);
    }

	public long getYoungestReader()
	{
		return m_youngestReader;
	}
	
    public LockEntry getReader(String transactionId)
    {
        return (LockEntry) this.readers.get(transactionId);
    }

    ObjectLocks()
    {
        writer = null;
        readers = new Hashtable();
    }

}
