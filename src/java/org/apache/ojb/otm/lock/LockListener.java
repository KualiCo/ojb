package org.apache.ojb.otm.lock;

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

import org.apache.ojb.broker.Identity;
import org.apache.ojb.otm.core.Transaction;

/**
 *
 * Protocol to notify when a lock is released by a transaction. This is used by wait strategies to
 * implementation deadlock prevention/detection/recovery algorithms.
 * 
 * @author <a href="mailto:rraghuram@hotmail.com">Raghu Rajah</a>
 * 
 */
public interface LockListener
{
    public void lockReleased (Transaction tx, Identity oid);
}
