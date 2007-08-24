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

import java.io.Serializable;
import java.util.Properties;

import org.apache.commons.lang.SystemUtils;
import org.apache.ojb.broker.util.pooling.PoolConfiguration;
import org.apache.ojb.broker.util.XmlHelper;

/**
 * Encapsulates connection pooling and JDBC-driver configuration properties managed by
 * {@link org.apache.ojb.broker.metadata.JdbcConnectionDescriptor}.
 * <p>
 * Every new instantiated <code>ConnectionPoolDescriptor</code> is associated with
 * default connection pool attributes.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: ConnectionPoolDescriptor.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class ConnectionPoolDescriptor extends PoolConfiguration implements Serializable, XmlCapable
{
	private static final long serialVersionUID = -3071461685659671879L;

    /** String prefix for JDBC properties passed to DriverManager. */
    public static final String JDBC_PROPERTY_NAME_PREFIX = "jdbc.";
    private static final int JDBC_PROPERTY_NAME_LENGTH = JDBC_PROPERTY_NAME_PREFIX.length();
    /**
     * String prefix for DBCP properties.
     * Currently OJB only uses this for setting DBCP parameters for pooling of Statement,
     * not the max/test-parameters etc for the DBCP Connection pool
     * (since there is only a JDBC2.0+ version of the Basic-classes ie BasicDataSource
     *  and no DriverManager-based one).
     */
    public static final String DBCP_PROPERTY_NAME_PREFIX = "dbcp.";
    private static final int DBCP_PROPERTY_NAME_LENGTH = DBCP_PROPERTY_NAME_PREFIX.length();

    /** JDBC properties configured in OJB (not used for DataSource connections). */
    protected Properties jdbcProperties;
    /** DBCP Statement cache properties configured in OJB (not used for DataSource connections). */
    protected Properties dbcpProperties;

    /** Configuration attribute name for JDBC fetchSize hint. */
    public static final String FETCH_SIZE = "fetchSize";

    private Class connectionFactory;


    public ConnectionPoolDescriptor()
    {
        super();
        init();
    }

    /**
     * Set some initial values.
     */
    private void init()
    {
        jdbcProperties = new Properties();
        dbcpProperties = new Properties();
        setFetchSize(0);
        this.setTestOnBorrow(true);
        this.setTestOnReturn(false);
        this.setTestWhileIdle(false);
        this.setLogAbandoned(false);
        this.setRemoveAbandoned(false);
    }

    public Class getConnectionFactory()
    {
        return this.connectionFactory;
    }

    public void setConnectionFactory(Class connectionFactory)
    {
        if (connectionFactory == null) throw new MetadataException("Given ConnectionFactory was null");
        this.connectionFactory = connectionFactory;
    }

    /**
     * Returns the fetchSize hint set for this connection pool.
     * @return fetchSize hint or 0 if JDBC-driver specific default is used
     */
    public int getFetchSize()
    {
        // We depend on init() to always set fetchSize hint
        return Integer.parseInt(getProperty(FETCH_SIZE));
    }

    /**
     * Sets the fetchSize hint for this connection pool.
     * @param fetchSize fetchSize hint or 0 to use JDBC-driver specific default
     */
    public void setFetchSize(int fetchSize)
    {
        setProperty(FETCH_SIZE, Integer.toString(fetchSize));
    }

    /**
     * Returns the JDBC properties to be used by the ConnectionFactory
     * when creating connections from DriverManager.
     * @return JDBC-driver specific properties (might be empty, never null)
     */
    public Properties getJdbcProperties()
    {
        return jdbcProperties;
    }

    /**
     * Returns the DBCP properties to be used for Statement caching
     * when creating DBCP connection pool in OJB ConnectionFactory.
     * @return DBCP properties (might be empty, never null)
     */
    public Properties getDbcpProperties()
    {
        return dbcpProperties;
    }

    /**
     * Sets a custom configuration attribute.
     * @param attributeName the attribute name. Names starting with
     * {@link #JDBC_PROPERTY_NAME_PREFIX} will be used (without the prefix) by the
     * ConnectionFactory when creating connections from DriverManager
     * (not used for external DataSource connections). Names starting with
     * {@link #DBCP_PROPERTY_NAME_PREFIX} to Commons DBCP (if used, also without prefix).
     * @param attributeValue the attribute value
     */
    public void addAttribute(String attributeName, String attributeValue)
    {
        if (attributeName != null && attributeName.startsWith(JDBC_PROPERTY_NAME_PREFIX))
        {
            final String jdbcPropertyName = attributeName.substring(JDBC_PROPERTY_NAME_LENGTH);
            jdbcProperties.setProperty(jdbcPropertyName, attributeValue);
        }
        else if (attributeName != null && attributeName.startsWith(DBCP_PROPERTY_NAME_PREFIX))
        {
            final String dbcpPropertyName = attributeName.substring(DBCP_PROPERTY_NAME_LENGTH);
            dbcpProperties.setProperty(dbcpPropertyName, attributeValue);
        }
        else
        {
            super.addAttribute(attributeName, attributeValue);
        }
    }

    public String toXML()
    {
        RepositoryTags tags = RepositoryTags.getInstance();
        String eol = SystemUtils.LINE_SEPARATOR;
        StringBuffer buf = new StringBuffer();
        //opening tag + attributes
        buf.append("      ").append(tags.getOpeningTagById(CONNECTION_POOL)).append(eol);
        buf.append("         " + tags.getAttribute(RepositoryElements.CON_MAX_ACTIVE, "" + getMaxActive()) + eol);
        buf.append("         " + tags.getAttribute(RepositoryElements.CON_MAX_IDLE, "" + getMaxIdle()) + eol);
        buf.append("         " + tags.getAttribute(RepositoryElements.CON_MAX_WAIT, "" + getMaxWait()) + eol);
        buf.append("         " + tags.getAttribute(RepositoryElements.CON_MIN_EVICTABLE_IDLE_TIME_MILLIS, "" +
                getMinEvictableIdleTimeMillis()) + eol);
        buf.append("         " + tags.getAttribute(RepositoryElements.CON_NUM_TESTS_PER_EVICTION_RUN, "" +
                getNumTestsPerEvictionRun()) + eol);
        buf.append("         " + tags.getAttribute(RepositoryElements.CON_TEST_ON_BORROW, "" + isTestOnBorrow()) + eol);
        buf.append("         " + tags.getAttribute(RepositoryElements.CON_TEST_ON_RETURN, "" + isTestOnReturn()) + eol);
        buf.append("         " + tags.getAttribute(RepositoryElements.CON_TEST_WHILE_IDLE, "" + isTestWhileIdle()) + eol);
        buf.append("         " + tags.getAttribute(RepositoryElements.CON_TIME_BETWEEN_EVICTION_RUNS_MILLIS, "" +
                getTimeBetweenEvictionRunsMillis()) + eol);
        buf.append("         " + tags.getAttribute(RepositoryElements.CON_WHEN_EXHAUSTED_ACTION, "" +
                getWhenExhaustedAction()) + eol);
        buf.append("         " + tags.getAttribute(RepositoryElements.VALIDATION_QUERY, "" + getValidationQuery()) + eol);

        buf.append("         " + tags.getAttribute(RepositoryElements.CON_LOG_ABANDONED, "" + isLogAbandoned()) + eol);
        buf.append("         " + tags.getAttribute(RepositoryElements.CON_REMOVE_ABANDONED, "" +
                isRemoveAbandoned()) + eol);
        buf.append("         " + tags.getAttribute(RepositoryElements.CON_REMOVE_ABANDONED_TIMEOUT, "" +
                getRemoveAbandonedTimeout()) + eol);

        buf.append("         <!-- ");
        buf.append(eol);
        buf.append("         Add JDBC-level properties here, like fetchSize.");
        buf.append("         Attributes with name prefix \"jdbc.\" are passed directly to the JDBC driver.");
        buf.append(eol);
        buf.append("         e.g. <attribute attribute-name=\"fetchSize\" attribute-value=\"100\"/>");
        buf.append(eol);
        buf.append("         -->");
        XmlHelper.appendSerializedAttributes(buf, "         ", this);

        buf.append("      ").append(tags.getClosingTagById(CONNECTION_POOL)).append(eol);
        return buf.toString();
    }

}
