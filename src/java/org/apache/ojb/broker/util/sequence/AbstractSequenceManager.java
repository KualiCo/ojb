package org.apache.ojb.broker.util.sequence;

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

import java.util.Properties;

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.accesslayer.JdbcAccess;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.metadata.SequenceDescriptor;
import org.apache.ojb.broker.platforms.Platform;

/**
 * A base class for sequence manager implementations.
 * <br/>
 * All sequence manager implementations need a constructor
 * with a PersistenceBroker argument used by the
 * {@link org.apache.ojb.broker.util.sequence.SequenceManagerFactory}.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: AbstractSequenceManager.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public abstract class AbstractSequenceManager implements SequenceManager
{
    // private Logger log = LoggerFactory.getLogger(AbstractSequenceManager.class);
    public static final String PROPERTY_AUTO_NAMING = "autoNaming";
    protected static final String GLOBAL_SEQUENCE_NAME = "ojb.global.sequence";

    private PersistenceBroker brokerForClass;
    private Platform platform;
    private Properties configurationProperties;

    /**
     * Constructor used by
     * {@link org.apache.ojb.broker.util.sequence.SequenceManagerFactory}
     *
     * @param broker  PB instance to perform the
     * id generation.
     */
    public AbstractSequenceManager(PersistenceBroker broker)
    {
        this.brokerForClass = broker;
        this.configurationProperties = new Properties();
        this.platform = brokerForClass.serviceConnectionManager().getSupportedPlatform();
        SequenceDescriptor sd = brokerForClass.serviceConnectionManager().
                getConnectionDescriptor().getSequenceDescriptor();
        if (sd != null)
        {
            this.configurationProperties.putAll(sd.getConfigurationProperties());
        }
    }

    /**
     * returns a unique long value for field.
     * the returned number is unique accross all tables in the extent of clazz.
     */
    abstract protected long getUniqueLong(FieldDescriptor field) throws SequenceManagerException;


    public Platform getPlatform()
    {
        return platform;
    }

    public PersistenceBroker getBrokerForClass()
    {
        return brokerForClass;
    }

    public Properties getConfigurationProperties()
    {
        return this.configurationProperties;
    }

    public void setConfigurationProperties(Properties prop)
    {
        this.configurationProperties.putAll(prop);
    }

    public String getConfigurationProperty(String key, String defaultValue)
    {
        String result = this.configurationProperties.getProperty(key);
        return result != null ? result : defaultValue;
    }

    public void setConfigurationProperty(String key, String value)
    {
        this.configurationProperties.setProperty(key, value);
    }

    public boolean useAutoNaming()
    {
        return (Boolean.valueOf(getConfigurationProperty(PROPERTY_AUTO_NAMING, "true"))).booleanValue();
    }

    public String calculateSequenceName(FieldDescriptor field) throws SequenceManagerException
    {
        String seqName;
        seqName = field.getSequenceName();
        /*
        if we found no sequence name for the given field, we try to
        assign a automatic generated sequence name.
        */
        if(seqName == null)
        {
            seqName = SequenceManagerHelper.buildSequenceName(getBrokerForClass(), field, useAutoNaming());
            // already done in method above
            // if(useAutoNaming()) field.setSequenceName(seqName);
        }
        return seqName;
    }


    //****************************************************************
    // method implementations of SequenceManager interface
    //****************************************************************
    /**
     * Returns a unique object for the given field attribute.
     * The returned value takes in account the jdbc-type
     * and the FieldConversion.sql2java() conversion defined for <code>field</code>.
     * The returned object is unique accross all tables in the extent
     * of class the field belongs to.
     */
    public Object getUniqueValue(FieldDescriptor field) throws SequenceManagerException
    {
        Object result = field.getJdbcType().sequenceKeyConversion(new Long(getUniqueLong(field)));
        // perform a sql to java conversion here, so that clients do
        // not see any db specific values
        result = field.getFieldConversion().sqlToJava(result);
        return result;
    }

    /**
     * noop
     */
    public void afterStore(JdbcAccess dbAccess, ClassDescriptor cld, Object obj)
            throws SequenceManagerException
    {
        // do nothing
    }

    /**
     * noop
     */
    public void setReferenceFKs(Object obj, ClassDescriptor cld)
            throws SequenceManagerException
    {
       // do nothing
    }
}
