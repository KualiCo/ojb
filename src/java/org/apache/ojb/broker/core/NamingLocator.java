package org.apache.ojb.broker.core;

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

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * Encapsulates a reference to the JNDI Naming context.
 *
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @version $Id: NamingLocator.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */

public class NamingLocator
{
    private static Logger log = LoggerFactory.getLogger(NamingLocator.class);
    private static Context ctx = null;
    private static Properties prop;

    /**
     * Returns the naming context.
     */
    public static Context getContext()
    {
        if (ctx == null)
        {
            try
            {
                setContext(null);
            }
            catch (Exception e)
            {
                log.error("Cannot instantiate the InitialContext", e);
                throw new OJBRuntimeException(e);
            }
        }
        return ctx;
    }

    /**
     * Lookup an object instance from JNDI context.
     *
     * @param jndiName JNDI lookup name
     * @return Matching object or <em>null</em> if none found.
     */
    public static Object lookup(String jndiName)
    {
        if(log.isDebugEnabled()) log.debug("lookup("+jndiName+") was called");
        try
        {
            return getContext().lookup(jndiName);
        }
        catch (NamingException e)
        {
            throw new OJBRuntimeException("Lookup failed for: " + jndiName, e);
        }
        catch(OJBRuntimeException e)
        {
            throw e;
        }
    }

    /**
     * Refresh the used {@link InitialContext} instance.
     */
    public static void refresh()
    {
        try
        {
            setContext(prop);
        }
        catch (NamingException e)
        {
            log.error("Unable to refresh the naming context");
            throw new OJBRuntimeException("Refresh of context failed, used properties: "
                    + (prop != null ? prop.toString() : "none"), e);
        }
    }

    /**
     * Set the used {@link InitialContext}. If properties argument is <em>null</em>, the default
     * initial context was used.
     *
     * @param properties The properties used for context instantiation - the properties are:
     *  {@link Context#INITIAL_CONTEXT_FACTORY}, {@link Context#PROVIDER_URL}, {@link Context#URL_PKG_PREFIXES}
     * @throws NamingException
     */
    public static synchronized void setContext(Properties properties) throws NamingException
    {
        log.info("Instantiate naming context, properties: " + properties);
        if(properties != null)
        {
            ctx = new InitialContext(properties);
        }
        else
        {
            ctx = new InitialContext();
        }
        prop = properties;
    }
}
