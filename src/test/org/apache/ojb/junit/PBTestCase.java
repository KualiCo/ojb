package org.apache.ojb.junit;

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
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.platforms.Platform;

/**
 * A base class for PB-api based test cases.
 * NOTE: The PB instance is declared <tt>public</tt> (no getter/setter) for easy use.
 *
 * @author <a href="mailto:arminw@apache.org">Armin Waibel</a>
 * @version $Id: PBTestCase.java,v 1.1 2007-08-24 22:17:28 ewestfal Exp $
 */
public class PBTestCase extends OJBTestCase
{
    public PersistenceBroker broker;
    private String platformClass;

    public PBTestCase()
    {
    }

    public PBTestCase(String name)
    {
        super(name);
    }

    public void setUp() throws Exception
    {
        Platform platform;

        super.setUp();
        assertNotNull(broker = PersistenceBrokerFactory.defaultPersistenceBroker());
        assertNotNull(platform = broker.serviceConnectionManager().getSupportedPlatform());
        platformClass = platform.getClass().getName();
    }

    public void tearDown() throws Exception
    {
        if(broker != null)
        {
            try
            {
                broker.close();
            }
            catch(Exception ignore)
            {
            }
        }
        super.tearDown();
    }

    /**
     * Returns the platform implementation class name of the currently
     * used broker.
     * @return platform implementation class name
     */
    public String getPlatformClass()
    {
        return platformClass;
    }

    /**
     * Persists an object with PB-API in a method-local transaction.
     * @param obj the object to persist
     * @throws org.apache.ojb.broker.TransactionInProgressException
     *  if external transaction in progress
     * @throws org.apache.ojb.broker.PersistenceBrokerException
     *  on persistence error
     */
    public void pbPersist(Object obj)
    {
        try
        {
            broker.beginTransaction();
            broker.store(obj);
            broker.commitTransaction();
        }
        catch (PersistenceBrokerException pbe)
        {
            throw pbe;
        }
        catch (ClassCastException cce)
        {
            System.err.println("Error in JDBC-driver while storing: " + obj);
            throw cce;
        }
        finally
        {
            if (broker.isInTransaction())
            {
                try
                {
                    broker.abortTransaction();
                }
                catch (Throwable ignore)
                {
                    //ignore
                }
            }
        }
    }

}
