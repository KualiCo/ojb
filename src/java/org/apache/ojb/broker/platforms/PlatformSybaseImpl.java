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
 * This class is a concrete implementation of {@link Platform}.
 * Provides an base implementation for all Sybase DB server. It's recommended
 * to use one of the specific Sybase platform classes like {@link PlatformSybaseASEImpl}
 * or {@link PlatformSybaseASAImpl}.
 *
 *@author     Oleg Nitz
 *@version $Id: PlatformSybaseImpl.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public class PlatformSybaseImpl extends PlatformDefaultImpl
{
    /**
     * Get join syntax type for this RDBMS - one on of the constants from JoinSyntaxType interface
     */
    public byte getJoinSyntaxType()
    {
        return SYBASE_JOIN_SYNTAX;
    }
}
