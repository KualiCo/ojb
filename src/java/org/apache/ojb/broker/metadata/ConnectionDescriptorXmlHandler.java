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

import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.locking.IsolationLevels;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.broker.util.ClassHelper;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The handler catches Parsing events raised by the xml-parser
 * and builds up the {@link ConnectionRepository} that is used
 * within the OJB.
 * <p>
 * TODO: Reading of metadata are split in two classes {@link RepositoryXmlHandler} and
 * {@link ConnectionDescriptorXmlHandler}. Thus we should only read relevant tags in this
 * classes. In further versions we should split repository.dtd in two parts, one for connetion
 * metadata, one for pc object metadata.
 * </p>
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: ConnectionDescriptorXmlHandler.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class ConnectionDescriptorXmlHandler
        extends DefaultHandler
        implements RepositoryElements, IsolationLevels
{
    private Logger logger = LoggerFactory.getLogger(ConnectionDescriptorXmlHandler.class);

    private ConnectionRepository con_repository;
    private JdbcConnectionDescriptor m_CurrentJCD;
    private SequenceDescriptor currentSequenceDescriptor;
    private List conDesList;
    private AttributeContainer currentAttributeContainer;
    private boolean defaultConnectionFound = false;

    /**
     * All known xml tags are kept in this table.
     * The tags table allows lookup from literal to id
     * and from id to literal.
     */
    private RepositoryTags tags = RepositoryTags.getInstance();

    /**
     * build a handler that fills the given repository
     * from an XML file.
     */
    public ConnectionDescriptorXmlHandler(ConnectionRepository cr)
    {
        if (cr != null)
        {
            con_repository = cr;
            conDesList = new ArrayList();
        }
        else
        {
            throw new MetadataException("Given ConnectionRepository argument was null");
        }
    }

    /**
     * startDocument callback, nothing to do here.
     */
    public void startDocument()
    {
        logger.debug("****   startDoc   ****");
    }

    /**
     * Here we overgive the found descriptors to {@link ConnectionRepository}.
     */
    public void endDocument()
    {
        logger.debug("****   endDoc   ****");
        for (Iterator iterator = conDesList.iterator(); iterator.hasNext();)
        {
            JdbcConnectionDescriptor jcd = (JdbcConnectionDescriptor) iterator.next();
            con_repository.addDescriptor(jcd);
        }
    }

    /**
     * startElement callback.
     * Only some Elements need special start operations.
     * @throws MetadataException indicating mapping errors
     */
    public void startElement(String uri, String name, String qName, Attributes atts)
    {
        boolean isDebug = logger.isDebugEnabled();
        try
        {
            switch (getLiteralId(qName))
            {
                case JDBC_CONNECTION_DESCRIPTOR:
                    {
                        if (isDebug) logger.debug("   > " + tags.getTagById(JDBC_CONNECTION_DESCRIPTOR));
                        JdbcConnectionDescriptor newJcd = new JdbcConnectionDescriptor();
                        currentAttributeContainer = newJcd;

                        conDesList.add(newJcd);
                        m_CurrentJCD = newJcd;

                        // set the jcdAlias attribute
                        String jcdAlias = atts.getValue(tags.getTagById(JCD_ALIAS));
                        if (isDebug) logger.debug("     " + tags.getTagById(JCD_ALIAS) + ": " + jcdAlias);
                        m_CurrentJCD.setJcdAlias(jcdAlias);

                        // set the jcdAlias attribute
                        String defaultConnection = atts.getValue(tags.getTagById(DEFAULT_CONNECTION));
                        if (isDebug) logger.debug("     " + tags.getTagById(DEFAULT_CONNECTION) + ": " + defaultConnection);
                        m_CurrentJCD.setDefaultConnection(Boolean.valueOf(defaultConnection).booleanValue());
                        if (m_CurrentJCD.isDefaultConnection())
                        {
                            if (defaultConnectionFound)
                            {
                                throw new MetadataException("Found two jdbc-connection-descriptor elements with default-connection=\"true\"");
                            }
                            else
                            {
                                defaultConnectionFound = true;
                            }
                        }

                        // set platform attribute
                        String platform = atts.getValue(tags.getTagById(DBMS_NAME));
                        if (isDebug) logger.debug("     " + tags.getTagById(DBMS_NAME) + ": " + platform);
                        m_CurrentJCD.setDbms(platform);

                        // set jdbc-level attribute
                        String level = atts.getValue(tags.getTagById(JDBC_LEVEL));
                        if (isDebug) logger.debug("     " + tags.getTagById(JDBC_LEVEL) + ": " + level);
                        m_CurrentJCD.setJdbcLevel(level);

                        // set driver attribute
                        String driver = atts.getValue(tags.getTagById(DRIVER_NAME));
                        if (isDebug) logger.debug("     " + tags.getTagById(DRIVER_NAME) + ": " + driver);
                        m_CurrentJCD.setDriver(driver);

                        // set protocol attribute
                        String protocol = atts.getValue(tags.getTagById(URL_PROTOCOL));
                        if (isDebug) logger.debug("     " + tags.getTagById(URL_PROTOCOL) + ": " + protocol);
                        m_CurrentJCD.setProtocol(protocol);

                        // set subprotocol attribute
                        String subprotocol = atts.getValue(tags.getTagById(URL_SUBPROTOCOL));
                        if (isDebug) logger.debug("     " + tags.getTagById(URL_SUBPROTOCOL) + ": " + subprotocol);
                        m_CurrentJCD.setSubProtocol(subprotocol);

                        // set the dbalias attribute
                        String dbalias = atts.getValue(tags.getTagById(URL_DBALIAS));
                        if (isDebug) logger.debug("     " + tags.getTagById(URL_DBALIAS) + ": " + dbalias);
                        m_CurrentJCD.setDbAlias(dbalias);

                        // set the datasource attribute
                        String datasource = atts.getValue(tags.getTagById(DATASOURCE_NAME));
                        // check for empty String
                        if(datasource != null && datasource.trim().equals("")) datasource = null;
                        if (isDebug) logger.debug("     " + tags.getTagById(DATASOURCE_NAME) + ": " + datasource);
                        m_CurrentJCD.setDatasourceName(datasource);

                        // set the user attribute
                        String user = atts.getValue(tags.getTagById(USER_NAME));
                        if (isDebug) logger.debug("     " + tags.getTagById(USER_NAME) + ": " + user);
                        m_CurrentJCD.setUserName(user);

                        // set the password attribute
                        String password = atts.getValue(tags.getTagById(USER_PASSWD));
                        if (isDebug) logger.debug("     " + tags.getTagById(USER_PASSWD) + ": " + password);
                        m_CurrentJCD.setPassWord(password);

                        // set eager-release attribute
                        String eagerRelease = atts.getValue(tags.getTagById(EAGER_RELEASE));
                        if (isDebug) logger.debug("     " + tags.getTagById(EAGER_RELEASE) + ": " + eagerRelease);
                        m_CurrentJCD.setEagerRelease(Boolean.valueOf(eagerRelease).booleanValue());

                        // set batch-mode attribute
                        String batchMode = atts.getValue(tags.getTagById(BATCH_MODE));
                        if (isDebug) logger.debug("     " + tags.getTagById(BATCH_MODE) + ": " + batchMode);
                        m_CurrentJCD.setBatchMode(Boolean.valueOf(batchMode).booleanValue());

                        // set useAutoCommit attribute
                        String useAutoCommit = atts.getValue(tags.getTagById(USE_AUTOCOMMIT));
                        if (isDebug) logger.debug("     " + tags.getTagById(USE_AUTOCOMMIT) + ": " + useAutoCommit);
                        m_CurrentJCD.setUseAutoCommit(Integer.valueOf(useAutoCommit).intValue());

                        // set ignoreAutoCommitExceptions attribute
                        String ignoreAutoCommitExceptions = atts.getValue(tags.getTagById(IGNORE_AUTOCOMMIT_EXCEPTION));
                        if (isDebug) logger.debug("     " + tags.getTagById(IGNORE_AUTOCOMMIT_EXCEPTION) + ": " + ignoreAutoCommitExceptions);
                        m_CurrentJCD.setIgnoreAutoCommitExceptions(Boolean.valueOf(ignoreAutoCommitExceptions).booleanValue());

                        break;
                    }
                case CONNECTION_POOL:
                    {
                        if (m_CurrentJCD != null)
                        {
                            if (isDebug) logger.debug("    > " + tags.getTagById(CONNECTION_POOL));
                            final ConnectionPoolDescriptor m_CurrentCPD = m_CurrentJCD.getConnectionPoolDescriptor();
                            this.currentAttributeContainer = m_CurrentCPD;

                            String maxActive = atts.getValue(tags.getTagById(CON_MAX_ACTIVE));
                            if (isDebug) logger.debug("     " + tags.getTagById(CON_MAX_ACTIVE) + ": " + maxActive);
                            if (checkString(maxActive)) m_CurrentCPD.setMaxActive(Integer.parseInt(maxActive));

                            String maxIdle = atts.getValue(tags.getTagById(CON_MAX_IDLE));
                            if (isDebug) logger.debug("     " + tags.getTagById(CON_MAX_IDLE) + ": " + maxIdle);
                            if (checkString(maxIdle)) m_CurrentCPD.setMaxIdle(Integer.parseInt(maxIdle));

                            String maxWait = atts.getValue(tags.getTagById(CON_MAX_WAIT));
                            if (isDebug) logger.debug("     " + tags.getTagById(CON_MAX_WAIT) + ": " + maxWait);
                            if (checkString(maxWait)) m_CurrentCPD.setMaxWait(Integer.parseInt(maxWait));

                            String minEvictableIdleTimeMillis = atts.getValue(tags.getTagById(CON_MIN_EVICTABLE_IDLE_TIME_MILLIS));
                            if (isDebug) logger.debug("     " + tags.getTagById(CON_MIN_EVICTABLE_IDLE_TIME_MILLIS) + ": " + minEvictableIdleTimeMillis);
                            if (checkString(minEvictableIdleTimeMillis)) m_CurrentCPD.setMinEvictableIdleTimeMillis(Long.parseLong(minEvictableIdleTimeMillis));

                            String numTestsPerEvictionRun = atts.getValue(tags.getTagById(CON_NUM_TESTS_PER_EVICTION_RUN));
                            if (isDebug) logger.debug("     " + tags.getTagById(CON_NUM_TESTS_PER_EVICTION_RUN) + ": " + numTestsPerEvictionRun);
                            if (checkString(numTestsPerEvictionRun)) m_CurrentCPD.setNumTestsPerEvictionRun(Integer.parseInt(numTestsPerEvictionRun));

                            String testOnBorrow = atts.getValue(tags.getTagById(CON_TEST_ON_BORROW));
                            if (isDebug) logger.debug("     " + tags.getTagById(CON_TEST_ON_BORROW) + ": " + testOnBorrow);
                            if (checkString(testOnBorrow)) m_CurrentCPD.setTestOnBorrow(Boolean.valueOf(testOnBorrow).booleanValue());

                            String testOnReturn = atts.getValue(tags.getTagById(CON_TEST_ON_RETURN));
                            if (isDebug) logger.debug("     " + tags.getTagById(CON_TEST_ON_RETURN) + ": " + testOnReturn);
                            if (checkString(testOnReturn)) m_CurrentCPD.setTestOnReturn(Boolean.valueOf(testOnReturn).booleanValue());

                            String testWhileIdle = atts.getValue(tags.getTagById(CON_TEST_WHILE_IDLE));
                            if (isDebug) logger.debug("     " + tags.getTagById(CON_TEST_WHILE_IDLE) + ": " + testWhileIdle);
                            if (checkString(testWhileIdle)) m_CurrentCPD.setTestWhileIdle(Boolean.valueOf(testWhileIdle).booleanValue());

                            String timeBetweenEvictionRunsMillis = atts.getValue(tags.getTagById(CON_TIME_BETWEEN_EVICTION_RUNS_MILLIS));
                            if (isDebug) logger.debug("     " + tags.getTagById(CON_TIME_BETWEEN_EVICTION_RUNS_MILLIS) + ": " + timeBetweenEvictionRunsMillis);
                            if (checkString(timeBetweenEvictionRunsMillis)) m_CurrentCPD.setTimeBetweenEvictionRunsMillis(Long.parseLong(timeBetweenEvictionRunsMillis));

                            String whenExhaustedAction = atts.getValue(tags.getTagById(CON_WHEN_EXHAUSTED_ACTION));
                            if (isDebug) logger.debug("     " + tags.getTagById(CON_WHEN_EXHAUSTED_ACTION) + ": " + whenExhaustedAction);
                            if (checkString(whenExhaustedAction)) m_CurrentCPD.setWhenExhaustedAction(Byte.parseByte(whenExhaustedAction));

                            String connectionFactoryStr = atts.getValue(tags.getTagById(CONNECTION_FACTORY));
                            if (isDebug) logger.debug("     " + tags.getTagById(CONNECTION_FACTORY) + ": " + connectionFactoryStr);
                            if (checkString(connectionFactoryStr)) m_CurrentCPD.setConnectionFactory(ClassHelper.getClass(connectionFactoryStr));

                            String validationQuery = atts.getValue(tags.getTagById(VALIDATION_QUERY));
                            if (isDebug) logger.debug("     " + tags.getTagById(VALIDATION_QUERY) + ": " + validationQuery);
                            if (checkString(validationQuery)) m_CurrentCPD.setValidationQuery(validationQuery);

                            // abandoned connection properties
                            String logAbandoned = atts.getValue(tags.getTagById(CON_LOG_ABANDONED));
                            if (isDebug) logger.debug("     " + tags.getTagById(CON_LOG_ABANDONED) + ": " + logAbandoned);
                            if (checkString(logAbandoned)) m_CurrentCPD.setLogAbandoned(Boolean.valueOf(logAbandoned).booleanValue());

                            String removeAbandoned = atts.getValue(tags.getTagById(CON_REMOVE_ABANDONED));
                            if (isDebug) logger.debug("     " + tags.getTagById(CON_REMOVE_ABANDONED) + ": " + removeAbandoned);
                            if (checkString(removeAbandoned)) m_CurrentCPD.setRemoveAbandoned(Boolean.valueOf(removeAbandoned).booleanValue());

                            String removeAbandonedTimeout = atts.getValue(tags.getTagById(CON_REMOVE_ABANDONED_TIMEOUT));
                            if (isDebug) logger.debug("     " + tags.getTagById(CON_REMOVE_ABANDONED_TIMEOUT) + ": " + removeAbandonedTimeout);
                            if (checkString(removeAbandonedTimeout)) m_CurrentCPD.setRemoveAbandonedTimeout(Integer.parseInt(removeAbandonedTimeout));
                        }
                        break;
                    }

                case OBJECT_CACHE:
                    {
                        String className = atts.getValue(tags.getTagById(CLASS_NAME));
                        if(checkString(className) && m_CurrentJCD != null)
                        {
                            ObjectCacheDescriptor ocd = m_CurrentJCD.getObjectCacheDescriptor();
                            this.currentAttributeContainer = ocd;
                            ocd.setObjectCache(ClassHelper.getClass(className));
                            if (isDebug) logger.debug("    > " + tags.getTagById(OBJECT_CACHE));
                            if (isDebug) logger.debug("     " + tags.getTagById(CLASS_NAME) + ": " + className);
                        }
                        break;
                    }
                case SEQUENCE_MANAGER:
                    {
                        String className = atts.getValue(tags.getTagById(SEQUENCE_MANAGER_CLASS));
                        if(checkString(className))
                        {
                            this.currentSequenceDescriptor = new SequenceDescriptor(this.m_CurrentJCD);
                            this.currentAttributeContainer = currentSequenceDescriptor;
                            this.m_CurrentJCD.setSequenceDescriptor(this.currentSequenceDescriptor);
                            if (isDebug) logger.debug("    > " + tags.getTagById(SEQUENCE_MANAGER));
                            if (isDebug) logger.debug("     " + tags.getTagById(SEQUENCE_MANAGER_CLASS) + ": " + className);
                            if (checkString(className)) currentSequenceDescriptor.setSequenceManagerClass(ClassHelper.getClass(className));
                        }
                        break;
                    }
                case ATTRIBUTE:
                    {
                        //handle custom attributes
                        String attributeName = atts.getValue(tags.getTagById(ATTRIBUTE_NAME));
                        String attributeValue = atts.getValue(tags.getTagById(ATTRIBUTE_VALUE));

                        // If we have a container to store this attribute in, then do so.
                        if (this.currentAttributeContainer != null)
                        {

                            if (checkString(attributeName))
                            {
                                if (isDebug) logger.debug("      > " + tags.getTagById(ATTRIBUTE));
                                if (isDebug) logger.debug("       " + tags.getTagById(ATTRIBUTE_NAME) + ": " + attributeName
                                        + "  "+tags.getTagById(ATTRIBUTE_VALUE) + ": " + attributeValue);
                                this.currentAttributeContainer.addAttribute(attributeName, attributeValue);
//                                logger.info("attribute ["+attributeName+"="+attributeValue+"] add to "+currentAttributeContainer.getClass());
                            }
                            else
                            {
                                logger.info("Found 'null' or 'empty' attribute object for element "+currentAttributeContainer.getClass() +
                                        " attribute-name=" + attributeName + ", attribute-value=" + attributeValue+
                                        " See jdbc-connection-descriptor with jcdAlias '"+m_CurrentJCD.getJcdAlias()+"'");
                            }
                        }
//                        else
//                        {
//                            logger.info("Found attribute (name="+attributeName+", value="+attributeValue+
//                                    ") but I could not assign them to a descriptor");
//                        }

                        break;
                    }
                default :
                    {
                        // noop
                    }
            }
        }
        catch (Exception ex)
        {
            logger.error(ex);
            throw new PersistenceBrokerException(ex);
        }
    }

    private boolean checkString(String str)
    {
        return (str != null && !str.trim().equals(""));
    }

    /**
     * returns the XmlCapable id associated with the literal.
     * OJB maintains a RepositoryTags table that provides
     * a mapping from xml-tags to XmlCapable ids.
     * @param literal the literal to lookup
     * @return the int value representing the XmlCapable
     *
     * @throws MetadataException if no literal was found in tags mapping
     */
    private int getLiteralId(String literal) throws PersistenceBrokerException
    {
        try
        {
            return tags.getIdByTag(literal);
        }
        catch (NullPointerException e)
        {
            throw new MetadataException("unknown literal: '" + literal + "'", e);
        }
    }

    /**
     * endElement callback. most elements are build up from here.
     */
    public void endElement(String uri, String name, String qName)
    {
        boolean isDebug = logger.isDebugEnabled();
        try
        {
            switch (getLiteralId(qName))
            {
                case MAPPING_REPOSITORY:
                    {
                        currentAttributeContainer = null;
                        break;
                    }
                case CLASS_DESCRIPTOR:
                    {
                        currentAttributeContainer = null;
                        break;
                    }
                case JDBC_CONNECTION_DESCRIPTOR:
                    {
                        logger.debug("   < " + tags.getTagById(JDBC_CONNECTION_DESCRIPTOR));
                        m_CurrentJCD = null;
                        currentAttributeContainer = null;
                        break;
                    }
                case CONNECTION_POOL:
                    {
                        logger.debug("   < " + tags.getTagById(CONNECTION_POOL));
                        currentAttributeContainer = m_CurrentJCD;
                        break;
                    }
                case SEQUENCE_MANAGER:
                    {
                        if (isDebug) logger.debug("    < " + tags.getTagById(SEQUENCE_MANAGER));
                        // set to null at the end of the tag!!
                        this.currentSequenceDescriptor = null;
                        currentAttributeContainer = m_CurrentJCD;
                        break;
                    }
                case OBJECT_CACHE:
                    {
                        if(currentAttributeContainer != null)
                        {
                            if (isDebug) logger.debug("    < " + tags.getTagById(OBJECT_CACHE));
                            // set to null or previous element level at the end of the tag!!
                            currentAttributeContainer = m_CurrentJCD;
                        }
                        break;
                    }
                case ATTRIBUTE:
                    {
                        if(currentAttributeContainer != null)
                        {
                            if (isDebug) logger.debug("      < " + tags.getTagById(ATTRIBUTE));
                        }
                        break;
                    }
                default :
                    {
                        // noop
                    }
            }
        }
        catch (Exception ex)
        {
            logger.error(ex);
            throw new PersistenceBrokerException(ex);
        }
    }

    /**
     * Error callback.
     */
    public void error(SAXParseException e) throws SAXException
    {
        logger.error(e);
        throw e;
    }

    /**
     * fatal error callback.
     */
    public void fatalError(SAXParseException e) throws SAXException
    {
        logger.fatal(e);
        throw e;
    }

    /**
     * warning callback.
     */
    public void warning(SAXParseException e) throws SAXException
    {
        logger.warn(e);
        throw e;
    }
}
