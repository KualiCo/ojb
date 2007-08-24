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

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.SerializationUtils;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.core.PersistenceBrokerConfiguration;
import org.apache.ojb.broker.util.configuration.impl.OjbConfigurator;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * Central class for metadata operations/manipulations - manages OJB's
 * metadata objects, in particular:
 * <ul>
 * <li>{@link org.apache.ojb.broker.metadata.DescriptorRepository} contains
 * metadata of persistent objects</li>
 * <li>{@link org.apache.ojb.broker.metadata.ConnectionRepository} contains
 * all connection metadata information</li>
 * </ul>
 *
 * This class allows transparent flexible metadata loading/manipulation at runtime.
 *
 * <p>
 * <b>How to read/merge metadata</b><br/>
 * Per default OJB loads default {@link org.apache.ojb.broker.metadata.DescriptorRepository}
 * and {@link org.apache.ojb.broker.metadata.ConnectionRepository} instances, by reading the
 * specified repository file. This is done first time the <code>MetadataManager</code> instance
 * was used.
 * <br/>
 * To read metadata information at runtime use
 * {@link #readDescriptorRepository readDescriptorRepository} and
 * {@link #readConnectionRepository readConnectionRepository}
 * methods.
 * <br/>
 * It is also possible to merge different repositories using
 * {@link #mergeDescriptorRepository mergeDescriptorRepository}
 * and {@link #mergeConnectionRepository mergeConnectionRepository}
 *
 * </p>
 *
 * <a name="perThread"/>
 * <h3>Per thread handling of metadata</h3>
 * <p>
 * Per default the manager handle one global {@link org.apache.ojb.broker.metadata.DescriptorRepository}
 * for all calling threads, but it is ditto possible to use different metadata <i>profiles</i> in a per thread
 * manner - <i>profiles</i> means different copies of {@link org.apache.ojb.broker.metadata.DescriptorRepository}
 * objects.
 * <p/>
 *
 * <p>
 * <a name="enablePerThreadMode"/>
 * <b>Enable the per thread mode</b><br/>
 * To enable the 'per thread' mode for {@link org.apache.ojb.broker.metadata.DescriptorRepository}
 * instances:
 * <pre>
 *   MetadataManager mm = MetadataManager.getInstance();
 *   // tell the manager to use per thread mode
 *   mm.setEnablePerThreadChanges(true);
 *   ...
 * </pre>
 * This could be done e.g. at start up.<br/>
 * Now it's possible to use dedicated <code>DescriptorRepository</code> instances
 * per thread:
 *  <pre>
 *   // e.g we get a coppy of the global repository
 *   DescriptorRepository dr = mm.copyOfGlobalRepository();
 *   // now we can manipulate the persistent object metadata of the copy
 *   ......
 *
 *   // set the changed repository for this thread
 *   mm.setDescriptor(dr);
 *
 *   // now let this thread lookup a PersistenceBroker instance
 *   // with the modified metadata
 *   // all other threads use the global metadata
 *   PersistenceBroker broker = Persis......
 * </pre>
 * Note: Change metadata <i>before</i> lookup the {@link org.apache.ojb.broker.PersistenceBroker}
 * instance for current thread, because the metadata was bound to the PB at lookup.
 * </p>
 *
 * <p>
 * <b>How to use different metadata profiles</b><br/>
 * MetadataManager was shipped with a simple mechanism to
 * add, remove and load different persistent objects metadata
 * profiles (different {@link org.apache.ojb.broker.metadata.DescriptorRepository}
 * instances) in a per thread manner. Use
 * <ul>
 * <li>{@link #addProfile addProfile} add different persistent object metadata profiles</li>
 * <li>{@link #removeProfile removeProfile} remove a persistent object metadata profiles</li>
 * <li>{@link #loadProfile loadProfile} load a profile for the current thread</li>
 * </ul>
 * Note: method {@link #loadProfile loadProfile} only works if
 * the <a href="#enablePerThreadMode">per thread mode</a> is enabled.
 * </p>
 *
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: MetadataManager.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class MetadataManager
{
    private static Logger log = LoggerFactory.getLogger(MetadataManager.class);

    private static final String MSG_STR = "* Can't find DescriptorRepository for current thread, use default one *";
    private static ThreadLocal threadedRepository = new ThreadLocal();
    private static ThreadLocal currentProfileKey = new ThreadLocal();
    private static MetadataManager singleton;

    private Hashtable metadataProfiles;
    private DescriptorRepository globalRepository;
    private ConnectionRepository connectionRepository;
    private boolean enablePerThreadChanges;
    private PBKey defaultPBKey;

    // singleton
    private MetadataManager()
    {
        init();
    }

    private void init()
    {
        metadataProfiles = new Hashtable();
        final String repository = ((PersistenceBrokerConfiguration) OjbConfigurator.getInstance()
                .getConfigurationFor(null)).getRepositoryFilename();
        try
        {
            globalRepository     = new RepositoryPersistor().readDescriptorRepository(repository);
            connectionRepository = new RepositoryPersistor().readConnectionRepository(repository);
        }
        catch (FileNotFoundException ex)
        {
            log.warn("Could not access '" + repository + "' or a DOCTYPE/DTD-dependency. "
                     + "(Check letter case for file names and HTTP-access if using DOCTYPE PUBLIC)"
                     + " Starting with empty metadata and connection configurations.", ex);
            globalRepository     = new DescriptorRepository();
            connectionRepository = new ConnectionRepository();
        }
        catch (Exception ex)
        {
            throw new MetadataException("Can't read repository file '" + repository + "'", ex);
        }
    }

    public void shutdown()
    {
        threadedRepository = null;
        currentProfileKey = null;
        globalRepository = null;
        metadataProfiles = null;
        singleton = null;
    }

    /**
     * Returns an instance of this class.
     */
    public static synchronized MetadataManager getInstance()
    {
        // lazy initialization
        if (singleton == null)
        {
            singleton = new MetadataManager();
        }
        return singleton;
    }

    /**
     * Returns the current valid {@link org.apache.ojb.broker.metadata.DescriptorRepository} for
     * the caller. This is the provided way to obtain the
     * {@link org.apache.ojb.broker.metadata.DescriptorRepository}.
     * <br>
     * When {@link #isEnablePerThreadChanges per thread descriptor handling}  is enabled
     * it search for a specific {@link org.apache.ojb.broker.metadata.DescriptorRepository}
     * for the calling thread, if none can be found the global descriptor was returned.
     *
     * @see MetadataManager#getGlobalRepository
     * @see MetadataManager#copyOfGlobalRepository
     */
    public DescriptorRepository getRepository()
    {
        DescriptorRepository repository;
        if (enablePerThreadChanges)
        {
            repository = (DescriptorRepository) threadedRepository.get();
            if (repository == null)
            {
                repository = getGlobalRepository();
                log.info(MSG_STR);
            }
// arminw:
// TODO: Be more strict in per thread mode and throw a exception when not find descriptor for calling thread?
//            if (repository == null)
//            {
//                throw new MetadataException("Can't find a DescriptorRepository for current thread, don't forget" +
//                        " to set a DescriptorRepository if enable per thread changes before perform other action");
//            }
            return repository;
        }
        else
        {
            return globalRepository;
        }
    }

    /**
     * Returns explicit the global {@link org.apache.ojb.broker.metadata.DescriptorRepository} - use with
     * care, because it ignores the {@link #isEnablePerThreadChanges per thread mode}.
     *
     * @see MetadataManager#getRepository
     * @see MetadataManager#copyOfGlobalRepository
     */
    public DescriptorRepository getGlobalRepository()
    {
        return globalRepository;
    }

    /**
     * Returns the {@link ConnectionRepository}.
     */
    public ConnectionRepository connectionRepository()
    {
        return connectionRepository;
    }

    /**
     * Merge the given {@link ConnectionRepository} with the existing one (without making
     * a deep copy of the containing connection descriptors).
     * @see #mergeConnectionRepository(ConnectionRepository targetRepository, ConnectionRepository sourceRepository, boolean deep)
     */
    public void mergeConnectionRepository(ConnectionRepository repository)
    {
        mergeConnectionRepository(connectionRepository(), repository, false);
    }

    /**
     * Merge the given source {@link ConnectionRepository} with the
     * existing target. If parameter
     * <tt>deep</tt> is set <code>true</code> deep copies of source objects were made.
     * <br/>
     * Note: Using <tt>deep copy mode</tt> all descriptors will be serialized
     * by using the default class loader to resolve classes. This can be problematic
     * when classes are loaded by a context class loader.
     * <p>
     * Note: All classes within the repository structure have to implement
     * <code>java.io.Serializable</code> to be able to create a cloned copy.
     */
    public void mergeConnectionRepository(
            ConnectionRepository targetRepository, ConnectionRepository sourceRepository, boolean deep)
    {
        List list = sourceRepository.getAllDescriptor();
        for (Iterator iterator = list.iterator(); iterator.hasNext();)
        {
            JdbcConnectionDescriptor jcd = (JdbcConnectionDescriptor) iterator.next();
            if (deep)
            {
                //TODO: adopt copy/clone methods for metadata classes?
                jcd = (JdbcConnectionDescriptor) SerializationUtils.clone(jcd);
            }
            targetRepository.addDescriptor(jcd);
        }
    }

    /**
     * Merge the given {@link org.apache.ojb.broker.metadata.DescriptorRepository}
     * (without making a deep copy of containing class-descriptor objects) with the
     * global one, returned by method {@link #getRepository()} - keep
     * in mind if running in <a href="#perThread">per thread mode</a>
     * merge maybe only takes effect on current thread.
     *
     * @see #mergeDescriptorRepository(DescriptorRepository targetRepository, DescriptorRepository sourceRepository, boolean deep)
     */
    public void mergeDescriptorRepository(DescriptorRepository repository)
    {
        mergeDescriptorRepository(getRepository(), repository, false);
    }

    /**
     * Merge the given {@link org.apache.ojb.broker.metadata.DescriptorRepository}
     * files, the source objects will be pushed to the target repository. If parameter
     * <tt>deep</tt> is set <code>true</code> deep copies of source objects were made.
     * <br/>
     * Note: Using <tt>deep copy mode</tt> all descriptors will be serialized
     * by using the default class loader to resolve classes. This can be problematic
     * when classes are loaded by a context class loader.
     * <p>
     * Note: All classes within the repository structure have to implement
     * <code>java.io.Serializable</code> to be able to create a cloned copy.
     *
     * @see #isEnablePerThreadChanges
     * @see #setEnablePerThreadChanges
     */
    public void mergeDescriptorRepository(
            DescriptorRepository targetRepository, DescriptorRepository sourceRepository, boolean deep)
    {
        Iterator it = sourceRepository.iterator();
        while (it.hasNext())
        {
            ClassDescriptor cld = (ClassDescriptor) it.next();
            if (deep)
            {
                //TODO: adopt copy/clone methods for metadata classes?
                cld = (ClassDescriptor) SerializationUtils.clone(cld);
            }
            targetRepository.put(cld.getClassOfObject(), cld);
            cld.setRepository(targetRepository);
        }
    }

    /**
     * Read ClassDescriptors from the given repository file.
     * @see #mergeDescriptorRepository
     */
    public DescriptorRepository readDescriptorRepository(String fileName)
    {
        try
        {
            RepositoryPersistor persistor = new RepositoryPersistor();
            return persistor.readDescriptorRepository(fileName);
        }
        catch (Exception e)
        {
            throw new MetadataException("Can not read repository " + fileName, e);
        }
    }

    /**
     * Read ClassDescriptors from the given InputStream.
     * @see #mergeDescriptorRepository
     */
    public DescriptorRepository readDescriptorRepository(InputStream inst)
    {
        try
        {
            RepositoryPersistor persistor = new RepositoryPersistor();
            return persistor.readDescriptorRepository(inst);
        }
        catch (Exception e)
        {
            throw new MetadataException("Can not read repository " + inst, e);
        }
    }

    /**
     * Read JdbcConnectionDescriptors from the given repository file.
     *
     * @see #mergeConnectionRepository
     */
    public ConnectionRepository readConnectionRepository(String fileName)
    {
        try
        {
            RepositoryPersistor persistor = new RepositoryPersistor();
            return persistor.readConnectionRepository(fileName);
        }
        catch (Exception e)
        {
            throw new MetadataException("Can not read repository " + fileName, e);
        }
    }

    /**
     * Read JdbcConnectionDescriptors from this InputStream.
     *
     * @see #mergeConnectionRepository
     */
    public ConnectionRepository readConnectionRepository(InputStream inst)
    {
        try
        {
            RepositoryPersistor persistor = new RepositoryPersistor();
            return persistor.readConnectionRepository(inst);
        }
        catch (Exception e)
        {
            throw new MetadataException("Can not read repository from " + inst, e);
        }
    }

    /**
     * Set the {@link org.apache.ojb.broker.metadata.DescriptorRepository} - if <i>global</i> was true, the
     * given descriptor aquire global availability (<i>use with care!</i>),
     * else the given descriptor was associated with the calling thread.
     *
     * @see #isEnablePerThreadChanges
     * @see #setEnablePerThreadChanges
     */
    public void setDescriptor(DescriptorRepository repository, boolean global)
    {
        if (global)
        {
            if (log.isDebugEnabled()) log.debug("Set new global repository: " + repository);
            globalRepository = repository;
        }
        else
        {
            if (log.isDebugEnabled()) log.debug("Set new threaded repository: " + repository);
            threadedRepository.set(repository);
        }
    }

    /**
     * Set {@link DescriptorRepository} for the current thread.
     * Convenience method for
     * {@link #setDescriptor(DescriptorRepository repository, boolean global) setDescriptor(repository, false)}.
     */
    public void setDescriptor(DescriptorRepository repository)
    {
        setDescriptor(repository, false);
    }

    /**
     * Convenience method for
     * {@link #setDescriptor setDescriptor(repository, false)}.
     * @deprecated use {@link #setDescriptor}
     */
    public void setPerThreadDescriptor(DescriptorRepository repository)
    {
        setDescriptor(repository, false);
    }

    /**
     * Returns a copy of the current global
     * {@link org.apache.ojb.broker.metadata.DescriptorRepository}
     * <p>
     * Note: All classes within the repository structure have to implement
     * <code>java.io.Serializable</code> to be able to create a cloned copy.
     *
     * @see MetadataManager#getGlobalRepository
     * @see MetadataManager#getRepository
     */
    public DescriptorRepository copyOfGlobalRepository()
    {
        return (DescriptorRepository) SerializationUtils.clone(globalRepository);
    }

    /**
     * If returns <i>true</i> if <a href="#perThread">per thread</a> runtime
     * changes of the {@link org.apache.ojb.broker.metadata.DescriptorRepository}
     * is enabled and the {@link #getRepository} method returns a threaded
     * repository file if set, or the global if no threaded was found.
     * <br>
     * If returns <i>false</i> the {@link #getRepository} method return
     * always the {@link #getGlobalRepository() global} repository.
     *
     * @see #setEnablePerThreadChanges
     */
    public boolean isEnablePerThreadChanges()
    {
        return enablePerThreadChanges;
    }

    /**
     * Enable the possibility of making <a href="#perThread">per thread</a> runtime changes
     * of the {@link org.apache.ojb.broker.metadata.DescriptorRepository}.
     *
     * @see #isEnablePerThreadChanges
     */
    public void setEnablePerThreadChanges(boolean enablePerThreadChanges)
    {
        this.enablePerThreadChanges = enablePerThreadChanges;
    }

    /**
     * Add a metadata profile.
     * @see #loadProfile
     */
    public void addProfile(Object key, DescriptorRepository repository)
    {
        if (metadataProfiles.contains(key))
        {
            throw new MetadataException("Duplicate profile key. Key '" + key + "' already exists.");
        }
        metadataProfiles.put(key, repository);
    }

    /**
     * Load the given metadata profile for the current thread.
     *
     */
    public void loadProfile(Object key)
    {
        if (!isEnablePerThreadChanges())
        {
            throw new MetadataException("Can not load profile with disabled per thread mode");
        }
        DescriptorRepository rep = (DescriptorRepository) metadataProfiles.get(key);
        if (rep == null)
        {
            throw new MetadataException("Can not find profile for key '" + key + "'");
        }
        currentProfileKey.set(key);
        setDescriptor(rep);
    }

    /**
     * Returns the last activated profile key.
     * @return the last activated profile key or null if no profile has been loaded
     * @throws MetadataException if per-thread changes has not been activated
     * @see #loadProfile(Object)
     */
    public Object getCurrentProfileKey() throws MetadataException
    {
        if (!isEnablePerThreadChanges())
        {
            throw new MetadataException("Call to this method is undefined, since per-thread mode is disabled.");
        }
        return currentProfileKey.get();
    }

    /**
     * Remove the given metadata profile.
     */
    public DescriptorRepository removeProfile(Object key)
    {
        return (DescriptorRepository) metadataProfiles.remove(key);
    }

    /**
     * Remove all metadata profiles.
     */
    public void clearProfiles()
    {
        metadataProfiles.clear();
        currentProfileKey.set(null);
    }

    /**
     * Remove all profiles
     *
     * @see #removeProfile
     * @see #addProfile
     */
    public void removeAllProfiles()
    {
        metadataProfiles.clear();
        currentProfileKey.set(null);
    }

    /**
     * Return the default {@link PBKey} used in convinience method
     * {@link org.apache.ojb.broker.PersistenceBrokerFactory#defaultPersistenceBroker}.
     * <br/>
     * If in {@link JdbcConnectionDescriptor} the
     * {@link JdbcConnectionDescriptor#isDefaultConnection() default connection}
     * is enabled, OJB will detect the default {@link org.apache.ojb.broker.PBKey} by itself.
     *
     * @see #setDefaultPBKey
     */
    public PBKey getDefaultPBKey()
    {
        if(defaultPBKey == null)
        {
            defaultPBKey = buildDefaultKey();
        }
        return defaultPBKey;
    }

    /**
     * Set the {@link PBKey} used in convinience method
     * {@link org.apache.ojb.broker.PersistenceBrokerFactory#defaultPersistenceBroker}.
     * <br/>
     * It's only allowed to use one {@link JdbcConnectionDescriptor} with enabled
     * {@link JdbcConnectionDescriptor#isDefaultConnection() default connection}. In this case
     * OJB will automatically set the default key.
     * <br/>
     * Note: It's recommended to set this key only once and not to change at runtime
     * of OJB to avoid side-effects.
     * If set more then one time a warning will be logged.
     * @throws MetadataException if key was set more than one time
     */
    public void setDefaultPBKey(PBKey defaultPBKey)
    {
        if(this.defaultPBKey != null)
        {
            log.warn("The used default PBKey change. Current key is " + this.defaultPBKey + ", new key will be " + defaultPBKey);
        }
        this.defaultPBKey = defaultPBKey;
        log.info("Set default PBKey for convenience broker creation: " + defaultPBKey);
    }

    /**
     * Try to build an default PBKey for convenience PB create method.
     *
     * @return PBKey or <code>null</code> if default key was not declared in
     * metadata
     */
    private PBKey buildDefaultKey()
    {
        List descriptors = connectionRepository().getAllDescriptor();
        JdbcConnectionDescriptor descriptor;
        PBKey result = null;
        for (Iterator iterator = descriptors.iterator(); iterator.hasNext();)
        {
            descriptor = (JdbcConnectionDescriptor) iterator.next();
            if (descriptor.isDefaultConnection())
            {
                if(result != null)
                {
                    log.error("Found additional connection descriptor with enabled 'default-connection' "
                            + descriptor.getPBKey() + ". This is NOT allowed. Will use the first found descriptor " + result
                            + " as default connection");
                }
                else
                {
                    result = descriptor.getPBKey();
                }
            }
        }

        if(result == null)
        {
            log.info("No 'default-connection' attribute set in jdbc-connection-descriptors," +
                    " thus it's currently not possible to use 'defaultPersistenceBroker()' " +
                    " convenience method to lookup PersistenceBroker instances. But it's possible"+
                    " to enable this at runtime using 'setDefaultKey' method.");
        }
        return result;
    }
}
