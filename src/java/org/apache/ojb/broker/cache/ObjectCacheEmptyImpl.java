package org.apache.ojb.broker.cache;

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
import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PersistenceBroker;

import java.util.Properties;

/**
 * This is an 'empty' ObjectCache implementation.
 * Useful when caching was not desired.
 * <br/>
 * NOTE: This implementation does not prevent infinite loops caused by
 * 'circular references' of loaded object graphs.
 * (this will change in versions &gt; 1.0).
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
 *     <td> - </td>
 *     <td>
 *          -
 *    </td>
 * </tr>
 * </table>
 *
 * @author Thomas Mahler
 * @version $Id: ObjectCacheEmptyImpl.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 *
 */
public class ObjectCacheEmptyImpl implements ObjectCache
{

    public ObjectCacheEmptyImpl(PersistenceBroker broker, Properties prop)
    {

    }

    /**
     * @see org.apache.ojb.broker.cache.ObjectCache#cache(Identity, Object)
     */
    public void cache(Identity oid, Object obj)
    {
        //do nothing
    }

    /**
     * @see org.apache.ojb.broker.cache.ObjectCache#lookup(Identity)
     */
    public Object lookup(Identity oid)
    {
        return null;
    }

    /**
     * @see org.apache.ojb.broker.cache.ObjectCache#remove(Identity)
     */
    public void remove(Identity oid)
    {
        //do nothing
    }

    /**
     * @see org.apache.ojb.broker.cache.ObjectCache#clear()
     */
    public void clear()
    {
        //do nothing
    }

    public String toString()
    {
        ToStringBuilder buf = new ToStringBuilder(this, ToStringStyle.DEFAULT_STYLE);
        return buf.toString();
    }
}

