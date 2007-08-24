package org.apache.ojb.broker;

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

/**
 * A immutable key to identify PB instances in pools, ...
 * <br>
 * The used <i>jcdAlias</i> name represents an alias for a connection
 * defined in the repository file.
 *
 * @author Armin Waibel
 * @version $Id: PBKey.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public class PBKey implements Cloneable, Serializable
{
	private static final long serialVersionUID = -8858811398162391578L;
    private final String jcdAlias;
    private final String user;
    private final String password;
    private int hashCode;

    /**
     * Creates a new PBKey.
     * 
     * @param jcdAlias The jdbc connection descriptor name as defined in the repository file
     * @param user     The username
     * @param password The password
     */
    public PBKey(final String jcdAlias, final String user, final String password)
    {
        this.jcdAlias = jcdAlias;
        this.user = user;
        this.password = password;
    }

    /**
     * Convenience constructor for <code>PBKey(jcdAlias, null, null)</code>.
     * 
     * @param jcdAlias The jdbc connection descriptor name as defined in the repository file
     */
    public PBKey(final String jcdAlias)
    {
        this(jcdAlias, null, null);
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }
        if (!(obj instanceof PBKey))
        {
            return false;
        }
        PBKey other = (PBKey) obj;
        return this.jcdAlias.equals(other.getAlias())
                && (user != null ? user.equals(other.user) : null == other.user)
                && (password != null ? password.equals(other.password) : null == other.password);
    }

    /**
     * {@inheritDoc}
     */
    protected Object clone() throws CloneNotSupportedException
    {
        return new PBKey(this.jcdAlias, this.user, this.password);
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        if(hashCode == 0)
        {
            hashCode = jcdAlias.hashCode()
                    + (user != null ? user.hashCode() : 0)
                    + (password != null ? password.hashCode() : 0);
        }
        return hashCode;
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return this.getClass().getName() + ": jcdAlias="+jcdAlias+", user="+user+
                (user != null ? ", password=*****" : ", password="+password);
    }

    /**
     * Returns the jdbc connection descriptor name as defined in the repository file.
     * 
     * @return The JCD alias
     */
    public String getAlias()
    {
        return jcdAlias;
    }

    /**
     * Returns the jdbc connection descriptor name as defined in the repository file.
     * 
     * @return The JCD alias
     * @deprecated use {@link #getAlias} instead.
     */
    public String getRepositoryFile()
    {
        return jcdAlias;
    }

    /**
     * Returns the username.
     * 
     * @return The username
     */
    public String getUser()
    {
        return user;
    }

    /**
     * Returns the password.
     * 
     * @return The password
     */
    public String getPassword()
    {
        return password;
    }
}
