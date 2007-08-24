package org.apache.ojb.broker.transaction.tm;

/* Copyright 2004-2005 The Apache Software Foundation
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

import javax.transaction.TransactionManager;

import org.apache.commons.lang.SystemUtils;
import org.apache.ojb.broker.core.NamingLocator;
import org.apache.ojb.broker.util.ClassHelper;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * Abstract base class implementation of the {@link TransactionManagerFactory} interface, all
 * derived classes have to implement method {@link #getLookupInfo()}.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: AbstractTransactionManagerFactory.java,v 1.1 2007-08-24 22:17:41 ewestfal Exp $
 */

public abstract class AbstractTransactionManagerFactory implements TransactionManagerFactory
{
    private static Logger log = LoggerFactory.getLogger(AbstractTransactionManagerFactory.class);

    /**
     * Returns "getTransactionManager";
     */
    public static String TM_DEFAULT_METHOD_NAME = "getTransactionManager";


    private static TransactionManager tm = null;

    /**
     * Returns an array of possible JNDI lookup / class names for
     * the {@link javax.transaction.TransactionManager} instance. An array was used
     * because for different application server versions the
     * JNDI/class name may change.
     * <p/>
     * Expect an [n][3] string array. Following arguments are available:
     * <ul>
     *    <li>info[i][0] = short description of used TM, e.g. appServer name</li>
     *    <li>info[i][2] = JNDI name to lookup TM or the method name to retrieve TM instance</li>
     *    <li>info[i][3] = if 'null' an JNDI lookup was made with JNDI name set above, if not null
     * the class name of the TM factory was assumed and the method name set above will be invoked</li>
     * </ul>
     * Example:
     * <p>
     * {{"JBoss", "java:/TransactionManager", null}};<br/>
     * In JBoss we lookup the TM via JNDI, so we don't need a TM factory class.
     * </p>
     *
     * <p>
     * {{"Websphere 4", TM_DEFAULT_METHOD_NAME, "com.ibm.ejs.jts.jta.JTSXA"},<br/>
     *    {"Websphere 5", TM_DEFAULT_METHOD_NAME, "com.ibm.ejs.jts.jta.TransactionManagerFactory"},<br/>
     *    {"Websphere >5", TM_DEFAULT_METHOD_NAME, "com.ibm.ws.Transaction.TransactionManagerFactory"}};<br/>
     * In Websphere we have to use a TM factory class and obtain the TM via a <em>getTransactionManager()</em>
     * method call. The TM factory class is varied in different versions.
     * </p>
     */
    public abstract String[][] getLookupInfo();

    /**
     * @see org.apache.ojb.broker.transaction.tm.TransactionManagerFactory
     */
    public synchronized TransactionManager getTransactionManager() throws TransactionManagerFactoryException
    {
        if (tm == null)
        {
            StringBuffer msg = new StringBuffer();
            String[][] lookupInfo = getLookupInfo();
            String EOL = SystemUtils.LINE_SEPARATOR;

            for (int i = 0; i < lookupInfo.length; i++)
            {
                String description = lookupInfo[i][0];
                String methodName = lookupInfo[i][1];
                String className = lookupInfo[i][2];
                try
                {
                    if (className == null)
                    {
                        tm = jndiLookup(description, methodName);
                    }
                    else
                    {
                        tm = instantiateClass(description, className, methodName);
                    }
                    msg.append("Successfully requested TM for " + description + EOL);
                }
                catch (Exception e)
                {
                    if (className == null)
                        msg.append("Error on TM request for " + description +
                                ", using jndi-lookup '" + methodName + "'" + EOL + e.getMessage() + EOL);
                    else
                        msg.append("Error on TM request for " + description + ", using method '" +
                                methodName + "' for class '" + className + "'" + EOL + e.getMessage() + EOL);
                }
                if (tm != null) break;
            }
            // if we don't get an TM instance throw exception
            if (tm == null)
            {
                throw new TransactionManagerFactoryException("Can't lookup transaction manager:" + EOL + msg);
            }
        }
        return tm;
    }

    protected TransactionManager jndiLookup(String description, String methodName)
    {
        log.info(description + ", lookup TransactionManager: '" + methodName + "'");
        return (TransactionManager) NamingLocator.lookup(methodName);
    }

    protected TransactionManager instantiateClass(String description, String className, String methodName) throws Exception
    {
        log.info(description + ", invoke method '"
                + methodName + "()' on class " + className);
        Class tmClass = ClassHelper.getClass(className);
        return (TransactionManager) tmClass.getMethod(methodName, null).invoke(null, null);
    }
}
