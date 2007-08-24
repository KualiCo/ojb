package org.apache.ojb.broker.util;

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

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.server.UID;

/**
 * simple GUID (Globally Unique ID) implementation.
 * A GUID is composed of two parts:
 * 1. The IP-Address of the local machine.
 * 2. A java.rmi.server.UID
 *
 * @author Thomas Mahler
 * @version $Id: GUID.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public class GUID implements Serializable
{
	static final long serialVersionUID = -6163239155380515945L;    /**
     * holds the hostname of the local machine.
     */
    private static String localIPAddress;

    /**
     * String representation of the GUID
     */
    private String guid;

    /**
     * compute the local IP-Address
     */
    static
    {
        try
        {
            localIPAddress = InetAddress.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException e)
        {
            localIPAddress = "localhost";
        }
    }

    /**
     * public no args constructor.
     */
    public GUID()
    {
        UID uid = new UID();
        StringBuffer buf = new StringBuffer();
        buf.append(localIPAddress);
        buf.append(":");
        buf.append(uid.toString());
        guid = buf.toString();
    }
 
    /**
     * public constructor.
     * The caller is responsible to feed a globally unique 
     * String into the theGuidString parameter
     * @param theGuidString a globally unique String
     */    
    public GUID(String theGuidString)
    {
    	guid = theGuidString;	
    }

    /**
     *  returns the String representation of the GUID
     */
    public String toString()
    {
        return guid;
    }
    /**
     * @see java.lang.Object#equals(Object)
     */
    public boolean equals(Object obj)
    {
    	if (obj instanceof GUID)
    	{
    		if (guid.equals(((GUID) obj).guid))
    		{
    			return true;	
    		}	
    	}
        return false;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        return guid.hashCode();
    }

}
