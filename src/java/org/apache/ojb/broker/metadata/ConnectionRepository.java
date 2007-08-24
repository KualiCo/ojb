package org.apache.ojb.broker.metadata;

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

import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.sql.DataSource;

public class ConnectionRepository implements Serializable, XmlCapable
{
	private static final long serialVersionUID = -5581126412817848887L;
    private static Logger log = LoggerFactory.getLogger(ConnectionRepository.class);

    private HashMap jcdMap;
    private Hashtable jcdAliasToPBKeyMap;
    private JdbcMetadataUtils utils;

    public ConnectionRepository()
    {
        jcdMap             = new HashMap();
        jcdAliasToPBKeyMap = new Hashtable();
        utils              = new JdbcMetadataUtils();
    }

    /**
     * Returns the matching {@link JdbcConnectionDescriptor}
     * or <code>null</code> if no descriptor could be found. The user name
     * and pass word will be set to match the supplied </code>PBKey</code>
     * object. If the original user name and pass word are desired, the PBKey
     * should be obtained with {@link #getStandardPBKeyForJcdAlias(String)}.
     */ 
    public JdbcConnectionDescriptor getDescriptor(PBKey pbKey)
    {
        JdbcConnectionDescriptor result = (JdbcConnectionDescriptor) jcdMap.get(pbKey);
        if (result == null)
        {
            result = deepCopyOfFirstFound(pbKey.getAlias());
            if (result != null)
            {
                result.setUserName(pbKey.getUser());
                result.setPassWord(pbKey.getPassword());
                // this build connection descriptor could not be the default connection
                result.setDefaultConnection(false);
                log.info("Automatic create of new jdbc-connection-descriptor for PBKey " + pbKey);
                addDescriptor(result);
            }
            else
            {
                log.info("Could not find " + JdbcConnectionDescriptor.class.getName() + " for PBKey " + pbKey);
            }
        }
        return result;
    }

    /**
     * Returns a deep copy of the first found connection descriptor
     * with the given <code>jcdAlias</code> name or <code>null</code>
     * if none found.
     */
    private JdbcConnectionDescriptor deepCopyOfFirstFound(String jcdAlias)
    {
        Iterator it = jcdMap.values().iterator();
        JdbcConnectionDescriptor jcd;
        while (it.hasNext())
        {
            jcd = (JdbcConnectionDescriptor) it.next();
            if (jcdAlias.equals(jcd.getJcdAlias()))
            {
                return (JdbcConnectionDescriptor) SerializationUtils.clone(jcd);
            }
        }
        return null;
    }

    /**
     * Return the matching {@link org.apache.ojb.broker.PBKey} for
     * the given jcdAlias name, or <code>null</code> if no match
     * was found.
     */
    public PBKey getStandardPBKeyForJcdAlias(String jcdAlias)
    {
        return (PBKey) jcdAliasToPBKeyMap.get(jcdAlias);
    }

    /**
     * Add a new {@link JdbcConnectionDescriptor}.
     */
    public void addDescriptor(JdbcConnectionDescriptor jcd)
    {
        synchronized (jcdMap)
        {
            if (jcdMap.containsKey(jcd.getPBKey()))
            {
                throw new MetadataException("Found duplicate connection descriptor using PBKey " +
                        jcd.getPBKey() + ", remove the old descriptor first, before add the new one. " + jcd);
            }
            jcdMap.put(jcd.getPBKey(), jcd);
            // only if the jcdAlias was not found, put the new PBKey,
            // because we don't want to replace the original PBKey with
            // automatic generated descriptors PBKey's - see method getDescriptor(PBKey key)
            if (!jcdAliasToPBKeyMap.containsKey(jcd.getJcdAlias()))
            {
                jcdAliasToPBKeyMap.put(jcd.getJcdAlias(), jcd.getPBKey());
            }
            if (log.isDebugEnabled()) log.debug("New descriptor was added: " + jcd);
        }
    }

    /**
     * Creates and adds a new connection descriptor for the given JDBC connection url.
     * This method tries to guess the platform to be used, but it should be checked
     * afterwards nonetheless using the {@link JdbcConnectionDescriptor#getDbms()} method.
     * For properties that are not part of the url, the following standard values are
     * explicitly set:
     * <ul>
     * <li>jdbc level = 2.0</li>
     * </ul>
     * 
     * @param jcdAlias          The connection alias for the created connection; if 'default' is used,
     *                          then the new descriptor will become the default connection descriptor
     * @param jdbcDriver        The fully qualified jdbc driver name 
     * @param jdbcConnectionUrl The connection url of the form '[protocol]:[sub protocol]:{database-specific path]'
     *                          where protocol is usually 'jdbc'
     * @param username          The user name (can be <code>null</code>) 
     * @param password          The password (can be <code>null</code>) 
     * @return The created connection descriptor
     * @see JdbcConnectionDescriptor#getDbms()
     */
    public JdbcConnectionDescriptor addDescriptor(String jcdAlias, String jdbcDriver, String jdbcConnectionUrl, String username, String password)
    {
        JdbcConnectionDescriptor jcd   = new JdbcConnectionDescriptor();
        HashMap                  props = utils.parseConnectionUrl(jdbcConnectionUrl);

        jcd.setJcdAlias(jcdAlias);
        jcd.setProtocol((String)props.get(JdbcMetadataUtils.PROPERTY_PROTOCOL));
        jcd.setSubProtocol((String)props.get(JdbcMetadataUtils.PROPERTY_SUBPROTOCOL));
        jcd.setDbAlias((String)props.get(JdbcMetadataUtils.PROPERTY_DBALIAS));

        String platform = utils.findPlatformFor(jcd.getSubProtocol(), jdbcDriver);

        jcd.setDbms(platform);
        jcd.setJdbcLevel(2.0);
        jcd.setDriver(jdbcDriver);
        if (username != null)
        {
           jcd.setUserName(username);
           jcd.setPassWord(password);
        }
        if ("default".equals(jcdAlias))
        {
            jcd.setDefaultConnection(true);
            // arminw: MM will search for the default key
            // MetadataManager.getInstance().setDefaultPBKey(jcd.getPBKey());
        }

        addDescriptor(jcd);
        return jcd;
    }
    
    /**
     * Creates and adds a new connection descriptor for the given JDBC data source.
     * This method tries to guess the platform to be used, but it should be checked
     * afterwards nonetheless using the {@link JdbcConnectionDescriptor#getDbms()} method.
     * Note that the descriptor won't have a value for the driver because it is not possible
     * to retrieve the driver classname from the data source. 
     * 
     * @param jcdAlias   The connection alias for the created connection; if 'default' is used,
     *                   then the new descriptor will become the default connection descriptor
     * @param dataSource The data source
     * @param username   The user name (can be <code>null</code>) 
     * @param password   The password (can be <code>null</code>) 
     * @return The created connection descriptor
     * @see JdbcConnectionDescriptor#getDbms()
     */
    public JdbcConnectionDescriptor addDescriptor(String jcdAlias, DataSource dataSource, String username, String password)
    {
        JdbcConnectionDescriptor jcd = new JdbcConnectionDescriptor();

        jcd.setJcdAlias(jcdAlias);
        jcd.setDataSource(dataSource);
        if (username != null)
        {
           jcd.setUserName(username);
           jcd.setPassWord(password);
        }
        utils.fillJCDFromDataSource(jcd, dataSource, username, password);
        if ("default".equals(jcdAlias))
        {
            jcd.setDefaultConnection(true);
            // arminw: MM will search for the default key
            // MetadataManager.getInstance().setDefaultPBKey(jcd.getPBKey());
        }
        addDescriptor(jcd);
        return jcd;
    }

    /**
     * Remove a descriptor.
     * @param validKey  This could be the {@link JdbcConnectionDescriptor}
     * itself, or the associated {@link JdbcConnectionDescriptor#getPBKey PBKey}.
     */
    public void removeDescriptor(Object validKey)
    {
        PBKey pbKey;
        if (validKey instanceof PBKey)
        {
            pbKey = (PBKey) validKey;
        }
        else if (validKey instanceof JdbcConnectionDescriptor)
        {
            pbKey = ((JdbcConnectionDescriptor) validKey).getPBKey();
        }
        else
        {
            throw new MetadataException("Could not remove descriptor, given object was no vaild key: " +
                    validKey);
        }
        Object removed = null;
        synchronized (jcdMap)
        {
            removed = jcdMap.remove(pbKey);
            jcdAliasToPBKeyMap.remove(pbKey.getAlias());
        }
        log.info("Remove descriptor: " + removed);
    }

    /**
     * Return a deep copy of all managed {@link JdbcConnectionDescriptor}.
     */
    public List getAllDescriptor()
    {
        return (List) SerializationUtils.clone(new ArrayList(jcdMap.values()));
    }

    public String toXML()
    {
        String eol = SystemUtils.LINE_SEPARATOR;
        // use copy to avoid sync problems
        HashMap map = (HashMap) jcdMap.clone();
        StringBuffer buf = new StringBuffer();
        Iterator it = map.values().iterator();
        while (it.hasNext())
        {
            JdbcConnectionDescriptor jcd = (JdbcConnectionDescriptor) it.next();
            buf.append(jcd.toXML());
            buf.append(eol);
        }
        return buf.toString();
    }
}
