package org.apache.ojb.otm.transaction;

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

import org.apache.ojb.broker.PBKey;
import org.apache.ojb.otm.OTMConnection;
import org.apache.ojb.otm.core.Transaction;


/**
 *
 * Factory to fetch current transaction. The various implementations will handle
 * the different implementation sceanrios, like managed and unmanaged platforms. 
 * 
 * @author <a href="mailto:rraghuram@hotmail.com">Raghu Rajah</a>
 * 
 */
public interface TransactionFactory
{
    
    /**
     * 
     *  Get the current Transaction.
     * 
     *  @return     the current Transaction
     * 
     */
    public Transaction getTransactionForConnection (OTMConnection connection);
    
    /**
     * 
     *  Acquire new connection. Creates a new connection. Depending on the implementation of the
     *  factory the connection could be associated to an existing transaction, or not.
     * 
     *  @return     new connection
     * 
     */
    public OTMConnection acquireConnection (PBKey pbKey);
}
