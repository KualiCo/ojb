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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.apache.ojb.broker.PBFactoryException;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PBState;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerInternal;
import org.apache.ojb.broker.util.BrokerHelper;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * This is the default implementation of the {@link PersistenceBrokerFactoryIF}
 * interface.
 * <p>
 * This implementation use a pool of {@link org.apache.ojb.broker.PersistenceBroker}
 * instances [abbr. PB]. Each pooled PB instance (the implementation class was specified
 * in OJB configuration file) is wrapped by {@link PoolablePersistenceBroker} class
 * before add to pool.
 * </p>
 * <p>
 * When calling {@link #createPersistenceBroker} or {@link #defaultPersistenceBroker} the pooled-PB
 * instance (<tt>PoolablePersistenceBroker</tt>) on its part was wrapped with {@link PersistenceBrokerHandle}
 * handle.
 * </p>
 * <p>
 * When a client do a PB.close() call on the handle the wrapped <tt>PoolablePersistenceBroker</tt> will
 * be closed and returned to pool. All further method calls on the handle
 * (except <tt>PB.isClosed()</tt> and <tt>PB.isInTransaction()</tt>) result in an exception.
 * </p>
 * Each different {@link org.apache.ojb.broker.PBKey} (based on <code>PBKey.equals(...)</code> method)
 * get its own PB-pool.
 *
 * @see PersistenceBrokerFactoryBaseImpl
 *
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: PersistenceBrokerFactoryDefaultImpl.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public class PersistenceBrokerFactoryDefaultImpl extends PersistenceBrokerFactoryBaseImpl
{
    private static Logger log = LoggerFactory.getLogger(PersistenceBrokerFactoryDefaultImpl.class);
    private GenericKeyedObjectPool brokerPool;
    private PBPoolInfo poolConfig;

    public PersistenceBrokerFactoryDefaultImpl()
    {
        super();
        // get PB-pool configuration properties from OJB.properties
        poolConfig = new PBPoolInfo();
        // setup pool for PB instances
        brokerPool = this.createPool();
        log.info("Create PersistenceBroker instance pool, pool configuration was " + getPoolConfiguration());
    }

    /**
     * Return broker instance from pool. If given {@link PBKey} was not found in pool
     * a new pool for given
     * @param pbKey
     * @return
     * @throws PBFactoryException
     */
    public PersistenceBrokerInternal createPersistenceBroker(PBKey pbKey) throws PBFactoryException
    {
        if (log.isDebugEnabled()) log.debug("Obtain broker from pool, used PBKey is " + pbKey);
        PersistenceBrokerInternal broker = null;

        /*
        try to find a valid PBKey, if given key does not full match
        */
        pbKey = BrokerHelper.crossCheckPBKey(pbKey);

        try
        {
            /*
            get a pooled PB instance, the pool is reponsible to create new
            PB instances if not found in pool
            */
            broker = ((PersistenceBrokerInternal) brokerPool.borrowObject(pbKey));
            /*
            now warp pooled PB instance with a handle to avoid PB corruption
            of closed PB instances.
            */
            broker = wrapRequestedBrokerInstance(broker);

        }
        catch (Exception e)
        {
            try
            {
                // if something going wrong, tryto close broker
                if(broker != null) broker.close();
            }
            catch (Exception ignore)
            {
                //ignore it
            }
            throw new PBFactoryException("Borrow broker from pool failed, using PBKey " + pbKey, e);
        }
        return broker;
    }

    /**
     * Each real pooled {@link PersistenceBroker} instance was wrapped by a
     * pooling handle when a new instance was created.
     *
     * @see PoolablePersistenceBroker
     * @param broker real {@link PersistenceBroker} instance
     * @param pool use {@link KeyedObjectPool}
     * @return wrapped broker instance
     */
    protected PersistenceBrokerInternal wrapBrokerWithPoolingHandle(PersistenceBrokerInternal broker, KeyedObjectPool pool)
    {
        return new PoolablePersistenceBroker(broker, pool);
    }

    /**
     * Wraps the requested pooled broker instance. The returned handle
     * warps a pooled broker instance to avoid corruption
     * of already closed broker instances.
     *
     * @see PersistenceBrokerHandle
     * @param broker
     * @return The broker handle.
     */
    protected PersistenceBrokerInternal wrapRequestedBrokerInstance(PersistenceBrokerInternal broker)
    {
        return new PersistenceBrokerHandle(broker);
    }

    /**
     * @see PersistenceBrokerFactoryIF#releaseAllInstances()
     */
    public synchronized void releaseAllInstances()
    {
        log.warn("Release all instances referenced by this object");
        super.releaseAllInstances();
        try
        {
            brokerPool.close();
            brokerPool = this.createPool();
        }
        catch (Exception e)
        {
            log.error("Error while release all pooled broker instances and refresh pool", e);
        }
    }

    public void shutdown()
    {
        try
        {
            brokerPool.close();
            brokerPool = null;
        }
        catch(Exception e)
        {
            log.error("Error while shutdown of broker pool", e);
        }
        super.shutdown();
    }

    public int activePersistenceBroker()
    {
        return brokerPool.getNumActive();
    }

    /**
     * could be used for monitoring
     * TODO: is this useful?
     */
    public Properties getPoolConfiguration()
    {
        return poolConfig;
    }

    /**
     * could be used for runtime configuration
     * TODO: is this useful?
     */
    public void setPoolConfiguration(Properties prop)
    {
        poolConfig = new PBPoolInfo(prop);
        log.info("Change pooling configuration properties: " + poolConfig.getKeyedObjectPoolConfig());
        brokerPool.setConfig(poolConfig.getKeyedObjectPoolConfig());
    }


    /**
     * Create the {@link org.apache.commons.pool.KeyedObjectPool}, pooling
     * the {@link PersistenceBroker} instances - override this method to
     * implement your own pool and {@link org.apache.commons.pool.KeyedPoolableObjectFactory}.
     */
    private GenericKeyedObjectPool createPool()
    {
        GenericKeyedObjectPool.Config conf = poolConfig.getKeyedObjectPoolConfig();
        if (log.isDebugEnabled())
            log.debug("PersistenceBroker pool will be setup with the following configuration " +
                    ToStringBuilder.reflectionToString(conf, ToStringStyle.MULTI_LINE_STYLE));
        GenericKeyedObjectPool pool = new GenericKeyedObjectPool(null, conf);
        pool.setFactory(new PersistenceBrokerFactoryDefaultImpl.PBKeyedPoolableObjectFactory(this, pool));
        return pool;
    }

//**************************************************************************************
// Inner classes
//**************************************************************************************
//

    /**
     * This is a {@link org.apache.commons.pool.KeyedPoolableObjectFactory} implementation,
     * manage the life-cycle of {@link PersistenceBroker} instances
     * hold in an {@link org.apache.commons.pool.KeyedObjectPool}.
     *
     * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
     */
    class PBKeyedPoolableObjectFactory implements KeyedPoolableObjectFactory
    {
        private PersistenceBrokerFactoryDefaultImpl pbf;
        private KeyedObjectPool pool;

        public PBKeyedPoolableObjectFactory(PersistenceBrokerFactoryDefaultImpl pbf, KeyedObjectPool pool)
        {
            this.pbf = pbf;
            this.pool = pool;
        }

        public Object makeObject(Object key) throws Exception
        {
            return wrapBrokerWithPoolingHandle(pbf.createNewBrokerInstance((PBKey) key), pool);
        }

        /**
         * Do all cleanup stuff here.
         */
        public void destroyObject(Object key, Object obj) throws Exception
        {
            PoolablePersistenceBroker pb = (PoolablePersistenceBroker) obj;
            PersistenceBroker broker = pb.getInnermostDelegate();
            if (broker instanceof PersistenceBrokerImpl)
            {
                log.info("Destroy PersistenceBroker instance " + obj);
                ((PersistenceBrokerImpl) broker).destroy();
            }
            pb.destroy();
        }

        /**
         * Check if the given PersistenceBroker instance
         * was already in transaction.
         * Was called when
         * {@link PBPoolInfo#init}
         * method does set <code>testOnBorrow(true)</code>.
         * (Default was false, thus this method wasn't called)
         * See documentation jakarta-connons-pool api.
         */
        public boolean validateObject(Object key, Object obj)
        {
            // here we could validate the PB instance
            // if corresponding configuration properties are set
            if (((PersistenceBroker) obj).isInTransaction())
            {
                log.error("Illegal broker state! This broker instance was already in transaction.");
                return false;
            }
            return true;
        }

        /**
         * Called before borrow object from pool.
         */
        public void activateObject(Object key, Object obj) throws Exception
        {
            ((PBState) obj).setClosed(false);
        }

        /**
         * Called before return object to pool.
         */
        public void passivateObject(Object key, Object obj) throws Exception
        {
            ((PBState) obj).setClosed(true);
        }
    }
}
