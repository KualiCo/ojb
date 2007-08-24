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

import org.apache.ojb.broker.util.configuration.Configurable;
import org.apache.ojb.broker.util.configuration.Configuration;
import org.apache.ojb.broker.util.configuration.ConfigurationException;
import org.apache.ojb.broker.util.configuration.impl.OjbConfigurator;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.broker.util.pooling.PoolConfiguration;

import java.util.Properties;

public class PBPoolInfo extends PoolConfiguration implements Configurable
{
    private static final long serialVersionUID = 3331426619896735439L;

    public PBPoolInfo()
    {
        super();
        init();
        OjbConfigurator.getInstance().configure(this);
    }

    public PBPoolInfo(Properties properties)
    {
        super();
        init();
        OjbConfigurator.getInstance().configure(this);
        this.putAll(properties);
    }

    /**
     * Read in the configuration properties.
     */
    public void configure(Configuration pConfig) throws ConfigurationException
    {
        if (pConfig instanceof PBPoolConfiguration)
        {
            PBPoolConfiguration conf = (PBPoolConfiguration) pConfig;
            this.setMaxActive(conf.getMaxActive());
            this.setMaxIdle(conf.getMaxIdle());
            this.setMaxWait(conf.getMaxWaitMillis());
            this.setMinEvictableIdleTimeMillis(conf.getMinEvictableIdleTimeMillis());
            this.setTimeBetweenEvictionRunsMillis(conf.getTimeBetweenEvictionRunsMilli());
            this.setWhenExhaustedAction(conf.getWhenExhaustedAction());
        }
        else
        {
            LoggerFactory.getDefaultLogger().error(this.getClass().getName() +
                    " cannot read configuration properties, use default.");
        }
    }

    /**
     * Init default properties.
     * We set {@link #setTestOnBorrow}, {@link #setTestOnReturn}, {@link #setTestWhileIdle}
     * to <ii>false<ii> (See documentation of jakarta-commons-pool).
     * Override this to change behavior.
     */
    public void init()
    {
        this.setTestOnBorrow(false);
        this.setTestOnReturn(false);
        this.setTestWhileIdle(false);
    }
}
