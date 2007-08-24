package org.apache.ojb.otm.lock.map;

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

import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.otm.lock.ObjectLock;

/**
 *
 * <javadoc>
 *
 * @author <a href="mailto:rraghuram@hotmail.com">Raghu Rajah</a>
 *
 */
public class InMemoryLockMap implements LockMap
{

    private HashMap _locks;

    public InMemoryLockMap ()
    {
        _locks = new HashMap();
    }


    /**
     *
     * @see org.apache.ojb.otm.lock.map.LockMap#getLock(Identity)
     *
     */
    public ObjectLock getLock(Identity oid)
    {
        ObjectLock lock;

        synchronized (_locks)
        {
            lock = (ObjectLock) _locks.get(oid);
            if (lock == null)
            {
                lock = new ObjectLock(oid);
                _locks.put(oid, lock);
            }
        }
        return lock;
    }

    public void gc()
    {
        synchronized (_locks)
        {
            for (Iterator it = _locks.entrySet().iterator(); it.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) it.next();
                ObjectLock lock = (ObjectLock) entry.getValue();
                if (lock.isFree())
                {
                    it.remove();
                }
            }
        }
    }

    public String toString()
    {
        return _locks.toString();
    }
}
