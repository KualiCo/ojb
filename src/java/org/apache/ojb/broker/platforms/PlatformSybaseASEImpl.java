package org.apache.ojb.broker.platforms;

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


/**
 * This class extends {@link PlatformSybaseImpl} and defines specific
 * behavior for the Sybase ASE platform.
 *
 * NOTE: Different than the Sybase ASA platform
 *
 * @author <a href="mailto:mattbaird@yahoo.com">Matthew Baird<a>
 * @version $Id: PlatformSybaseASEImpl.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public class PlatformSybaseASEImpl extends PlatformSybaseImpl
{
    public String getLastInsertIdentityQuery(String tableName)
    {
        // the function is used by the
        // org.apache.ojb.broker.util.sequence.SequenceManagerNativeImpl
        // this call must be made before commit the insert cammand, so you
        // must turn off autocommit (e.g. by setting the useAutoCommit="1"
        // or useAutoCommit="2" or by external configuration)
        return "SELECT @@IDENTITY AS id FROM " + tableName;
    }
}
