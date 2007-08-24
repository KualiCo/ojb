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

import javax.sql.DataSource;

import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.broker.PBKey;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.lang.SystemUtils;

/**
 * JdbcConnectionDescriptor describes all relevant parameters of
 * JDBC Connections used by the PersistenceBroker.
 *
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @version $Id: JdbcConnectionDescriptor.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class JdbcConnectionDescriptor extends DescriptorBase implements Serializable, XmlCapable
{
	private static final long serialVersionUID = -600900924512028960L;
    private Logger logger = LoggerFactory.getLogger(JdbcConnectionDescriptor.class);

    public static final int AUTO_COMMIT_IGNORE_STATE = 0;
    public static final int AUTO_COMMIT_SET_TRUE_AND_TEMPORARY_FALSE = 1;
    public static final int AUTO_COMMIT_SET_FALSE = 2;

    private String m_jcdAlias;
    private String m_Dbms;
    private String m_Driver;
    private String m_Protocol;
    private String m_SubProtocol;
    private String m_DbAlias;
    private String m_DatasourceName;
    private String m_UserName;
    private String m_Password;
    private double m_JdbcLevel = 2.0;
    private boolean m_eagerRelease = false;
    private boolean m_batchMode = false;
    private boolean defaultConnection = false;
    private int useAutoCommit = AUTO_COMMIT_SET_TRUE_AND_TEMPORARY_FALSE;
    private boolean ignoreAutoCommitExceptions = false;
    private PBKey pbKey;
    private ConnectionPoolDescriptor cpd;
    private SequenceDescriptor sequenceDescriptor;
    private ObjectCacheDescriptor objectCacheDescriptor;
    private transient DataSource dataSource;

    /**
     * Constructor declaration
     */
    public JdbcConnectionDescriptor()
    {
        cpd = new ConnectionPoolDescriptor();
        objectCacheDescriptor = new ObjectCacheDescriptor();
    }

    /**
     * Returns the appropriate {@link ObjectCacheDescriptor}
     * or <code>null</code> if not specified.
     */
    public ObjectCacheDescriptor getObjectCacheDescriptor()
    {
        return objectCacheDescriptor;
    }

    /**
     * Sets the {@link ObjectCacheDescriptor} for representing connection/database.
     */
    public void setObjectCacheDescriptor(ObjectCacheDescriptor objectCacheDescriptor)
    {
        this.objectCacheDescriptor = objectCacheDescriptor;
    }

    /**
     * Returns the data source that this connection descriptor represents if any.
     * 
     * @return The data source or <code>null</code>
     */
    public DataSource getDataSource()
    {
        return dataSource;        
    }

    /**
     * Sets the data source that this connection descriptor represents.
     * 
     * @param dataSource The data source
     */
    public void setDataSource(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    /**
     * Get the alias name for this descriptor.
     */
    public String getJcdAlias()
    {
        return m_jcdAlias;
    }

    /**
     * Set an alias name for this descriptor.
     */
    public void setJcdAlias(String jcdAlias)
    {
        this.clearPBKey();
        this.m_jcdAlias = jcdAlias;
    }

    /**
     *
     */
    public boolean isDefaultConnection()
    {
        return defaultConnection;
    }

    public boolean isDataSource()
    {
        return (getDataSource() != null) || (getDatasourceName() != null);
    }

    /**
     *
     */
    public void setDefaultConnection(boolean defaultConnection)
    {
        this.defaultConnection = defaultConnection;
    }

    /**
     * Return the associated <code>SequenceDescriptor</code>
     * or <code>null</code> if not set.
     */
    public SequenceDescriptor getSequenceDescriptor()
    {
        return sequenceDescriptor;
    }

    /**
     * Set the <code>SequenceDescriptor</code> for this
     * connection descriptor.
     */
    public void setSequenceDescriptor(SequenceDescriptor sequenceDescriptor)
    {
        this.sequenceDescriptor = sequenceDescriptor;
    }

    /**
     * Returns the connection pool descriptor.
     */
    public ConnectionPoolDescriptor getConnectionPoolDescriptor()
    {
        return cpd;
    }

    /**
     * Sets the connection pool descriptor.
     */
    public void setConnectionPoolDescriptor(ConnectionPoolDescriptor cpd)
    {
        this.cpd = cpd;
    }

    /**
     * Return a key to identify the connection descriptor.
     */
    public PBKey getPBKey()
    {
        if (pbKey == null)
        {
            this.pbKey = new PBKey(this.getJcdAlias(), this.getUserName(), this.getPassWord());
        }
        return pbKey;
    }

    private void clearPBKey()
    {
        this.pbKey = null;
    }

    public int getUseAutoCommit()
    {
        return useAutoCommit;
    }

    public void setUseAutoCommit(int useAutoCommit)
    {
        this.useAutoCommit = useAutoCommit;
    }

    public boolean isIgnoreAutoCommitExceptions()
    {
        return ignoreAutoCommitExceptions;
    }

    public void setIgnoreAutoCommitExceptions(boolean ignoreAutoCommitExceptions)
    {
        this.ignoreAutoCommitExceptions = ignoreAutoCommitExceptions;
    }

    /**
     * Returns the database platform name.
     */
    public String getDbms()
    {
        return m_Dbms;
    }

    /**
     * Sets the database platform name.
     */
    public void setDbms(String str)
    {
        m_Dbms = str;
    }

    /**
     * Returns the driver name.
     */
    public String getDriver()
    {
        return m_Driver;
    }

    /**
     * Set the database driver.
     */
    public void setDriver(String str)
    {
        m_Driver = str;
    }

    /**
     * Returns the database protocol.
     */
    public String getProtocol()
    {
        return m_Protocol;
    }

    /**
     * Sets the database protocol.
     */
    public void setProtocol(String str)
    {
        m_Protocol = str;
    }

    /**
     * Returns the database sub-protocol.
     */
    public String getSubProtocol()
    {
        return m_SubProtocol;
    }

    /**
     * Sets the database sub-protocol.
     */
    public void setSubProtocol(String str)
    {
        m_SubProtocol = str;
    }

    /**
     * Returns the database alias name
     * used by OJB.
     */
    public String getDbAlias()
    {
        return m_DbAlias;
    }

    /**
     * Sets the database alias name. These
     * names you could find in the repository.dtd.
     */
    public void setDbAlias(String str)
    {
        m_DbAlias = str;
    }

    /**
     * Returns the database user name.
     */
    public String getUserName()
    {
        return m_UserName;
    }

    /**
     * Sets the database user name.
     */
    public void setUserName(String str)
    {
        this.clearPBKey();
        m_UserName = str;
    }

    /**
     * Returns the database password.
     */
    public String getPassWord()
    {
        return m_Password;
    }

    /**
     * Sets the database password.
     */
    public void setPassWord(String str)
    {
        this.clearPBKey();
        m_Password = str;
    }

    /**
     * Gets the datasourceName.
     * @return Returns a String
     */
    public String getDatasourceName()
    {
        return m_DatasourceName;
    }

    /**
     * Sets the datasourceName.
     * @param datasourceName The datasourceName to set
     */
    public void setDatasourceName(String datasourceName)
    {
        m_DatasourceName = datasourceName;
    }

    /**
     * Gets the jdbcLevel.
     * @return Returns a String
     */
    public double getJdbcLevel()
    {
        return m_JdbcLevel;
    }

    /**
     * Sets the jdbcLevel. parse the string setting and check that it is indeed an integer.
     * @param jdbcLevel The jdbcLevel to set
     */
    public void setJdbcLevel(String jdbcLevel)
    {
        if (jdbcLevel != null)
        {
            try
            {
                double intLevel = Double.parseDouble(jdbcLevel);
                setJdbcLevel(intLevel);
            }
            catch(NumberFormatException nfe)
            {
                setJdbcLevel(2.0);
                logger.info("Specified JDBC level was not numeric (Value=" + jdbcLevel + "), used default jdbc level of 2.0 ");
            }
        }
        else
        {
            setJdbcLevel(2.0);
            logger.info("Specified JDBC level was null, used default jdbc level of 2.0 ");
        }
    }

    public void setJdbcLevel(double jdbcLevel)
    {
        m_JdbcLevel = jdbcLevel;
    }

    public boolean getEagerRelease()
    {
    	return m_eagerRelease;
    }

    public void setEagerRelease(boolean flag)
    {
    	m_eagerRelease = flag;
    }

    public boolean getBatchMode()
    {
    	return m_batchMode;
    }

    public void setBatchMode(boolean flag)
    {
    	m_batchMode = flag;
    }

    /**
     * Returns a String representation of this class.
     */
    public String toString()
    {
        ToStringBuilder buf = new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE);
        buf.
        append("jcd-alias", m_jcdAlias).
        append("default-connection", defaultConnection).
        append("dbms", m_Dbms).
        append("jdbc-level", m_JdbcLevel).
        append("driver", m_Driver).
        append("protocol", m_Protocol).
        append("sub-protocol", m_SubProtocol).
        append("db-alias", m_DbAlias).
        append("user", m_UserName).
        append("password", "*****").
        append("eager-release", m_eagerRelease).
        append("ConnectionPoolDescriptor", cpd).
        append("batchMode", m_batchMode).
        append("useAutoCommit", getUseAutoCommitAsString(useAutoCommit)).
        append("ignoreAutoCommitExceptions", ignoreAutoCommitExceptions).
        append("sequenceDescriptor", sequenceDescriptor);
        return buf.toString();
    }

    /*
     * @see XmlCapable#toXML()
     */
    public String toXML()
    {
        RepositoryTags tags = RepositoryTags.getInstance();
        String eol = SystemUtils.LINE_SEPARATOR;

        StringBuffer strReturn = new StringBuffer( 1024 );
        strReturn.append( eol );
        strReturn.append( "  <!-- Descriptor for Connection " );
        strReturn.append( getProtocol() );
        strReturn.append( ":" );
        strReturn.append( getSubProtocol() );
        strReturn.append( ":" );
        strReturn.append( getDbAlias() );
        strReturn.append( " -->" );
        strReturn.append( eol );

        strReturn.append( "  " );
        strReturn.append( tags.getOpeningTagNonClosingById( JDBC_CONNECTION_DESCRIPTOR ) );
        strReturn.append( eol );
        strReturn.append( "    " );
        strReturn.append( tags.getAttribute( JCD_ALIAS, this.getJcdAlias() ) );
        strReturn.append( eol );
        strReturn.append( "    " );
        strReturn.append( tags.getAttribute( DEFAULT_CONNECTION, "" + this.isDefaultConnection() ) );
        strReturn.append( eol );
        strReturn.append( "    " );
        strReturn.append( tags.getAttribute( DBMS_NAME, this.getDbms() ) );
        strReturn.append( eol );
        strReturn.append( "    " );
        strReturn.append( tags.getAttribute( JDBC_LEVEL, "" + this.getJdbcLevel() ) );
        strReturn.append( eol );

        //username is optional
        String user = getUserName();
        if( user != null )
        {
            strReturn.append( "    " );
            strReturn.append( tags.getAttribute( USER_NAME, user ) );
            strReturn.append( eol );
        }
        // password is optional
        String passwd = getPassWord();
        if( passwd != null )
        {
            strReturn.append( "    " );
            strReturn.append( tags.getAttribute( USER_PASSWD, passwd ) );
            strReturn.append( eol );
        }

        // JDBC Datasource or DriverManager information are alternatives:
        String dsn = getDatasourceName();
        if( dsn != null )
        {
            strReturn.append( "    " );
            strReturn.append( tags.getAttribute( DATASOURCE_NAME, this.getDatasourceName() ) );
            strReturn.append( eol );
        }
        else
        {
            strReturn.append( "    " );
            strReturn.append( tags.getAttribute( DRIVER_NAME, this.getDriver() ) );
            strReturn.append( eol );
            strReturn.append( "    " );
            strReturn.append( tags.getAttribute( URL_PROTOCOL, this.getProtocol() ) );
            strReturn.append( eol );
            strReturn.append( "    " );
            strReturn.append( tags.getAttribute( URL_SUBPROTOCOL, this.getSubProtocol() ) );
            strReturn.append( eol );
            strReturn.append( "    " );
            strReturn.append( encode( tags.getAttribute( URL_DBALIAS, this.getDbAlias() ) ) );
            strReturn.append( eol );
        }
        strReturn.append( "    " );
        strReturn.append( tags.getAttribute( EAGER_RELEASE, "" + this.getEagerRelease() ) );
        strReturn.append( eol );
        strReturn.append( "    " );
        strReturn.append( tags.getAttribute( BATCH_MODE, "" + this.getBatchMode() ) );
        strReturn.append( eol );
        strReturn.append( "    " );
        strReturn.append( tags.getAttribute( USE_AUTOCOMMIT, "" + this.getUseAutoCommit() ) );
        strReturn.append( eol );
        strReturn.append( "    " );
        strReturn.append( tags.getAttribute( IGNORE_AUTOCOMMIT_EXCEPTION, "" + this.isIgnoreAutoCommitExceptions() ) );
        strReturn.append( eol );

        strReturn.append( "  >" );
        strReturn.append( eol );
        strReturn.append( eol );

        strReturn.append( this.getConnectionPoolDescriptor().toXML() );
        strReturn.append( eol );
        if( this.getSequenceDescriptor() != null )
        {
            strReturn.append( this.getSequenceDescriptor().toXML() );
        }
        strReturn.append( eol );
        strReturn.append( "  " );
        strReturn.append( tags.getClosingTagById( JDBC_CONNECTION_DESCRIPTOR ) );
        strReturn.append( eol );
        return strReturn.toString();
    }

    private static String encode(String toBeEncoded)
    {
        StringBuffer retval = new StringBuffer();
        char c;
        for (int i=0; i < toBeEncoded.length(); i++)
        {
            c = toBeEncoded.charAt(i);
            if (c == '<')
            {
                retval.append("&lt;");
            }
            else if (c == '>')
            {
                retval.append("&gt;");
            }
            //else if (c == '"')
            //{
            //    retval.append("&quot;");
            //}
            else if (c == '&')
            {
                retval.append("&amp;");
            }
            else if (c == ' ')
            {
                retval.append("&nbsp;");
            }
            else
            {
                retval.append(c);
            }
        }
        return retval.toString();
    }

    private static String getUseAutoCommitAsString(int state)
    {
        switch (state)
        {
            case AUTO_COMMIT_SET_TRUE_AND_TEMPORARY_FALSE:
                return "AUTO_COMMIT_SET_TRUE_AND_TEMPORARY_FALSE";
            case AUTO_COMMIT_SET_FALSE:
                return "AUTO_COMMIT_SET_FALSE";
            case AUTO_COMMIT_IGNORE_STATE:
                return "AUTO_COMMIT_IGNORE_STATE";
            default: return "UNKOWN_STATE";
        }
    }
}
