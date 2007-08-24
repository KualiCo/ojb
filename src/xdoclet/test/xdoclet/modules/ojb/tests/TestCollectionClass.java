package xdoclet.modules.ojb.tests;

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
import org.apache.ojb.broker.ManageableCollection;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerException;

/**
 * Collection class used in some of the unit tests.
 */
public class TestCollectionClass extends java.util.ArrayList implements ManageableCollection
{
    public void ojbAdd(Object anObject)
    {
    }

    public void ojbAddAll(ManageableCollection otherCollection)
    {
    }

    public Iterator ojbIterator()
    {
        return null;
    }

    public void afterStore(PersistenceBroker broker) throws PersistenceBrokerException
    {
    }
}
