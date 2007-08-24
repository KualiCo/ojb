package org.apache.ojb.broker.util.pooling;

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

import org.apache.commons.dbcp.AbandonedConfig;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.ojb.broker.metadata.AttributeContainer;

/**
 * Encapsulates configuration properties for
 * implementations using {@link org.apache.commons.pool.ObjectPool}.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: PoolConfiguration.java,v 1.1 2007-08-24 22:17:42 ewestfal Exp $
 */
public class PoolConfiguration extends Properties implements Serializable, AttributeContainer
{

	private static final long serialVersionUID = -3850488378321541047L;

    //*****************************************************
    // constants
    //*****************************************************
    public static final String MAX_ACTIVE = "maxActive";
    public static final String MAX_IDLE = "maxIdle";
    public static final String MIN_IDLE = "minIdle";
    public static final String MAX_WAIT = "maxWait";
    public static final String WHEN_EXHAUSTED_ACTION = "whenExhaustedAction";
    public static final String TEST_ON_BORROW = "testOnBorrow";
    public static final String TEST_ON_RETURN = "testOnReturn";
    public static final String TEST_WHILE_IDLE = "testWhileIdle";
    public static final String TIME_BETWEEN_EVICTION_RUNS_MILLIS = "timeBetweenEvictionRunsMillis";
    public static final String NUM_TESTS_PER_EVICTION_RUN = "numTestsPerEvictionRun";
    public static final String MIN_EVICTABLE_IDLE_TIME_MILLIS = "minEvictableIdleTimeMillis";
    public static final String LOG_ABANDONED = "logAbandoned";
    public static final String REMOVE_ABANDONED = "removeAbandoned";
    public static final String REMOVE_ABANDONED_TIMEOUT = "removeAbandonedTimeout";
    public static final String VALIDATION_QUERY = "validationQuery";

    //*****************************************************
    // used default values
    //*****************************************************
    public static final int DEFAULT_MAX_ACTIVE = 21;
    public static final int DEFAULT_MAX_IDLE = -1;
    public static final int DEFAULT_MIN_IDLE = 0;
    public static final long DEFAULT_MAX_WAIT = 5000;
    public static final byte DEFAULT_WHEN_EXHAUSTED_ACTION = GenericObjectPool.WHEN_EXHAUSTED_FAIL;
    public static final boolean DEFAULT_TEST_ON_BORROW = true;
    public static final boolean DEFAULT_TEST_ON_RETURN = false;
    public static final boolean DEFAULT_TEST_WHILE_IDLE = false;
    public static final long DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS = -1L;
    public static final int DEFAULT_NUM_TESTS_PER_EVICTION_RUN = 10;
    public static final long DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS = 1000 * 60 * 10;
    public static final boolean DEFAULT_LOG_ABANDONED = false;
    public static final boolean DEFAULT_REMOVE_ABANDONED = false;
    public static final int DEFAULT_REMOVE_ABANDONED_TIMEOUT = 300;

    public PoolConfiguration()
    {
        this.setMaxActive(DEFAULT_MAX_ACTIVE);
        this.setMaxIdle(DEFAULT_MAX_IDLE);
        this.setMinIdle(DEFAULT_MIN_IDLE);
        this.setMaxWait(DEFAULT_MAX_WAIT);
        this.setWhenExhaustedAction(DEFAULT_WHEN_EXHAUSTED_ACTION);
        this.setTestOnBorrow(DEFAULT_TEST_ON_BORROW);
        this.setTestOnReturn(DEFAULT_TEST_ON_RETURN);
        this.setTestWhileIdle(DEFAULT_TEST_WHILE_IDLE);
        this.setMinEvictableIdleTimeMillis(DEFAULT_MIN_EVICTABLE_IDLE_TIME_MILLIS);
        this.setTimeBetweenEvictionRunsMillis(DEFAULT_TIME_BETWEEN_EVICTION_RUNS_MILLIS);
        this.setNumTestsPerEvictionRun(DEFAULT_NUM_TESTS_PER_EVICTION_RUN);
        this.setLogAbandoned(DEFAULT_LOG_ABANDONED);
        this.setRemoveAbandoned(DEFAULT_REMOVE_ABANDONED);
        this.setRemoveAbandonedTimeout(DEFAULT_REMOVE_ABANDONED_TIMEOUT);
    }

    public PoolConfiguration(Properties properties)
    {
        this();
        this.putAll(properties);
    }

    /**
     * Returns an {@link org.apache.commons.pool.impl.GenericObjectPool.Config} object
     * configurated with the properties extracted from the this instance.
     * Use this to configurate a pool implementation using
     * {@link org.apache.commons.pool.impl.GenericObjectPool}.
     */
    public GenericObjectPool.Config getObjectPoolConfig()
    {
        GenericObjectPool.Config conf = new GenericObjectPool.Config();
        conf.maxActive = getMaxActive();
        conf.maxIdle = getMaxIdle();
        conf.minIdle = getMinIdle();
        conf.maxWait = getMaxWait();
        conf.minEvictableIdleTimeMillis = getMinEvictableIdleTimeMillis();
        conf.numTestsPerEvictionRun = getNumTestsPerEvictionRun();
        conf.testOnBorrow = isTestOnBorrow();
        conf.testOnReturn = isTestOnReturn();
        conf.testWhileIdle = isTestWhileIdle();
        conf.timeBetweenEvictionRunsMillis = getTimeBetweenEvictionRunsMillis();
        conf.whenExhaustedAction = getWhenExhaustedAction();
        return conf;
    }

    /**
     * Returns an {@link org.apache.commons.pool.impl.GenericKeyedObjectPool.Config} object
     * configurated with the properties extracted from the this instance.
     * Use this to configurate a pool implementation using
     * {@link org.apache.commons.pool.impl.GenericKeyedObjectPool}.
     */
    public GenericKeyedObjectPool.Config getKeyedObjectPoolConfig()
    {
        GenericKeyedObjectPool.Config conf = new GenericKeyedObjectPool.Config();
        conf.maxActive = getMaxActive();
        conf.maxIdle = getMaxIdle();
        conf.maxWait = getMaxWait();
        conf.minEvictableIdleTimeMillis = getMinEvictableIdleTimeMillis();
        conf.numTestsPerEvictionRun = getNumTestsPerEvictionRun();
        conf.testOnBorrow = isTestOnBorrow();
        conf.testOnReturn = isTestOnReturn();
        conf.testWhileIdle = isTestWhileIdle();
        conf.timeBetweenEvictionRunsMillis = getTimeBetweenEvictionRunsMillis();
        conf.whenExhaustedAction = getWhenExhaustedAction();
        return conf;
    }

    public AbandonedConfig getAbandonedConfig()
    {
        AbandonedConfig conf = new AbandonedConfig();
        conf.setRemoveAbandoned(isRemoveAbandoned());
        conf.setRemoveAbandonedTimeout(getRemoveAbandonedTimeout());
        conf.setLogAbandoned(isLogAbandoned());
        return conf;
    }

    public void addAttribute(String attributeName, String attributeValue)
    {
        setProperty(attributeName, attributeValue);
    }

    public String getAttribute(String key)
    {
        return getAttribute(key, null);
    }

    public String getAttribute(String attributeName, String defaultValue)
    {
        final String result = getProperty(attributeName);
        return result == null ? defaultValue : result;
    }

    public boolean isLogAbandoned()
    {
    	return Boolean.valueOf(getProperty(LOG_ABANDONED)).booleanValue();
    }

    public void setLogAbandoned(boolean logAbandoned)
    {
        this.setProperty(LOG_ABANDONED, BooleanUtils.toStringTrueFalse(logAbandoned));
    }

    public boolean isRemoveAbandoned()
    {
        return Boolean.valueOf(getProperty(REMOVE_ABANDONED)).booleanValue();
    }

    public void setRemoveAbandoned(boolean removeAbandoned)
    {
        this.setProperty(REMOVE_ABANDONED, BooleanUtils.toStringTrueFalse(removeAbandoned));
    }

    public int getRemoveAbandonedTimeout()
    {
        return Integer.parseInt(getProperty(REMOVE_ABANDONED_TIMEOUT));
    }

    public void setRemoveAbandonedTimeout(int removeAbandonedTimeout)
    {
        this.setProperty(REMOVE_ABANDONED_TIMEOUT, Integer.toString(removeAbandonedTimeout));
    }

    public String getValidationQuery()
    {
        String result = getProperty(VALIDATION_QUERY);
        return StringUtils.isEmpty(result) ? null : result;
    }

    public void setValidationQuery(String validationQuery)
    {
        setProperty(VALIDATION_QUERY, validationQuery);
    }

    public int getMaxActive()
    {
        return Integer.parseInt(getProperty(MAX_ACTIVE));
    }

    public void setMaxActive(int maxActive)
    {
        this.setProperty(MAX_ACTIVE, Integer.toString(maxActive));
    }

    public int getMaxIdle()
    {
        return Integer.parseInt(getProperty(MAX_IDLE));
    }

    public void setMaxIdle(int maxIdle)
    {
        this.setProperty(MAX_IDLE, Integer.toString(maxIdle));
    }

    public int getMinIdle()
    {
        return Integer.parseInt(getProperty(MIN_IDLE));
    }

    public void setMinIdle(int minIdle)
    {
        this.setProperty(MIN_IDLE, Integer.toString(minIdle));
    }

    public long getMaxWait()
    {
        return Long.parseLong(getProperty(MAX_WAIT));
    }

    public void setMaxWait(long maxWait)
    {
        this.setProperty(MAX_WAIT, Long.toString(maxWait));
    }


    public byte getWhenExhaustedAction()
    {
        return new Byte(getProperty(WHEN_EXHAUSTED_ACTION)).byteValue();
    }

    public void setWhenExhaustedAction(byte whenExhaustedAction)
    {
        this.setProperty(WHEN_EXHAUSTED_ACTION, Byte.toString(whenExhaustedAction));
    }


    public boolean isTestOnBorrow()
    {
        return Boolean.valueOf(getProperty(TEST_ON_BORROW)).booleanValue();
    }

    public void setTestOnBorrow(boolean testOnBorrow)
    {
        this.setProperty(TEST_ON_BORROW, BooleanUtils.toStringTrueFalse(testOnBorrow));
    }


    public boolean isTestOnReturn()
    {
        return Boolean.valueOf(getProperty(TEST_ON_RETURN)).booleanValue();
    }

    public void setTestOnReturn(boolean testOnReturn)
    {
        this.setProperty(TEST_ON_RETURN, BooleanUtils.toStringTrueFalse(testOnReturn));
    }


    public boolean isTestWhileIdle()
    {
        return Boolean.valueOf(getProperty(TEST_WHILE_IDLE)).booleanValue();
    }

    public void setTestWhileIdle(boolean testWhileIdle)
    {
        this.setProperty(TEST_WHILE_IDLE, BooleanUtils.toStringTrueFalse(testWhileIdle));
    }


    public long getMinEvictableIdleTimeMillis()
    {
        return Long.parseLong(getProperty(MIN_EVICTABLE_IDLE_TIME_MILLIS));
    }

    public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis)
    {
        this.setProperty(MIN_EVICTABLE_IDLE_TIME_MILLIS, Long.toString(minEvictableIdleTimeMillis));
    }


    public long getTimeBetweenEvictionRunsMillis()
    {
        return Long.parseLong(getProperty(TIME_BETWEEN_EVICTION_RUNS_MILLIS));
    }

    public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis)
    {
        this.setProperty(TIME_BETWEEN_EVICTION_RUNS_MILLIS, Long.toString(timeBetweenEvictionRunsMillis));
    }


    public int getNumTestsPerEvictionRun()
    {
        return Integer.parseInt(getProperty(NUM_TESTS_PER_EVICTION_RUN));
    }

    public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun)
    {
        this.setProperty(NUM_TESTS_PER_EVICTION_RUN, Integer.toString(numTestsPerEvictionRun));
    }

}

