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

import java.util.Collections;
import java.util.List;

import org.apache.ojb.broker.PBLifeCycleEvent;
import org.apache.ojb.broker.PBLifeCycleListener;
import org.apache.ojb.broker.PBListener;
import org.apache.ojb.broker.PBStateEvent;
import org.apache.ojb.broker.PBStateListener;
import org.apache.ojb.broker.PersistenceBrokerEvent;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.PersistenceBrokerInternal;
import org.apache.ojb.broker.util.IdentityArrayList;
import org.apache.ojb.broker.util.configuration.Configuration;
import org.apache.ojb.broker.util.configuration.ConfigurationException;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * Abstract Implementation of the {@link org.apache.ojb.broker.PersistenceBroker}
 * encapsulating the used PB-event/listener concept.
 *
 * @see org.apache.ojb.broker.PersistenceBroker
 * @see org.apache.ojb.broker.PBLifeCycleListener
 * @see org.apache.ojb.broker.PersistenceBrokerAware
 * @see org.apache.ojb.broker.PBStateListener
 *
 * @version $Id: PersistenceBrokerAbstractImpl.java,v 1.1 2007-08-24 22:17:35 ewestfal Exp $
 */
public abstract class PersistenceBrokerAbstractImpl implements PersistenceBrokerInternal
{
    /**
     * Returns <em>true</em> if PB-transaction check is enabled.
     */
    private boolean txCheck;

    /**
     * List containing all permanent {@link org.apache.ojb.broker.PBStateListener}
     * instances.
     */
    private List permanentStateListeners = Collections.synchronizedList(new IdentityArrayList());

    /**
     * List containing all temporary {@link org.apache.ojb.broker.PBStateListener}
     * instances.
     */
    private List temporaryStateListeners = Collections.synchronizedList(new IdentityArrayList());

    /**
     * List containing all permanent {@link org.apache.ojb.broker.PBLifeCycleListener}
     * instances.
     */
    private List permanentLifeCycleListeners = Collections.synchronizedList(new IdentityArrayList(100));

    /**
     * List containing all temporary {@link org.apache.ojb.broker.PBLifeCycleListener}
     * instances.
     */
    private List temporaryLifeCycleListeners = Collections.synchronizedList(new IdentityArrayList(100));

    /**
     * Override if needed.
     *
     * @see org.apache.ojb.broker.util.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration pConfig) throws ConfigurationException
    {
        txCheck = pConfig.getBoolean("TxCheck", false);
    }

    /**
     * Returns <em>true</em> if the development checks are enabled.
     *
     * @see #setTxCheck(boolean)
     */
    public boolean isTxCheck()
    {
        return txCheck;
    }

    /**
     * This setting can be helpful during development if the PersistenceBroker transaction
     * demarcation was used (this is true in most cases). If set 'true' on PB#store(...)
     * and PB#delete(...) methods calls OJB check for active PB-tx and if no active tx is
     * found a error is logged. This can help to avoid store/delete calls without a running
     * PB-tx while development. Default setting is 'false'.
     * <p/>
     * <strong>Note:</strong> When using OJB in a managed
     * environment <em>without</em> OJB-caching, it's valid to use store/delete
     * calls without a running PB-tx.
     *
     * @param txCheck Set <em>true</em> to enable the checks
     */
    public void setTxCheck(boolean txCheck)
    {
        this.txCheck = txCheck;
    }

    /**
     * @see org.apache.ojb.broker.PersistenceBroker#addListener(PBListener listener)
     */
    public void addListener(PBListener listener) throws PersistenceBrokerException
    {
        addListener(listener, false);
    }

    /**
     * @see org.apache.ojb.broker.PersistenceBroker#addListener(PBListener listener, boolean permanent)
     */
    public void addListener(PBListener listener, boolean permanent) throws PersistenceBrokerException
    {
        if (listener instanceof PBStateListener)
        {
            if (permanent)
            {
                if (!permanentStateListeners.contains(listener))
                {
                    permanentStateListeners.add(listener);
                }
            }
            else
            {
                if (!temporaryStateListeners.contains(listener))
                {
                    temporaryStateListeners.add(listener);
                }
            }
        }

        if (listener instanceof PBLifeCycleListener)
        {
            if (permanent)
            {
                if (!permanentLifeCycleListeners.contains(listener))
                {
                    permanentLifeCycleListeners.add(listener);
                }
            }
            else
            {
                if (!temporaryLifeCycleListeners.contains(listener))
                {
                    temporaryLifeCycleListeners.add(listener);
                }
            }
        }
    }

    /**
     * @see org.apache.ojb.broker.PersistenceBroker#removeListener(PBListener listener)
     */
    public void removeListener(PBListener listener) throws PersistenceBrokerException
    {
        if (listener instanceof PBStateListener)
        {
            permanentStateListeners.remove(listener);
            temporaryStateListeners.remove(listener);
        }

        if (listener instanceof PBLifeCycleListener)
        {
            permanentLifeCycleListeners.remove(listener);
            temporaryLifeCycleListeners.remove(listener);
        }
    }

    /**
     * @see org.apache.ojb.broker.PersistenceBroker#removeAllListeners(boolean)
     */
    public void removeAllListeners(boolean permanent) throws PersistenceBrokerException
    {
        if (permanent)
        {
            // remove permanent listeners as well
            permanentStateListeners.clear();
            // this could be huge, thus simply replace instance
            if(permanentLifeCycleListeners.size() > 10000)
            {
                permanentLifeCycleListeners = Collections.synchronizedList(new IdentityArrayList(100));
            }
            else
            {
                permanentLifeCycleListeners.clear();
            }
        }

        temporaryStateListeners.clear();
        // this could be huge, thus simply replace instance
        if(temporaryLifeCycleListeners.size() > 10000)
        {
            temporaryLifeCycleListeners = Collections.synchronizedList(new IdentityArrayList(100));
        }
        else
        {
            temporaryLifeCycleListeners.clear();
        }
    }

    /**
     * @see org.apache.ojb.broker.PersistenceBroker#removeAllListeners()
     */
    public void removeAllListeners() throws PersistenceBrokerException
    {
        removeAllListeners(false);
    }

    public void fireBrokerEvent(PersistenceBrokerEvent event)
    {
        if (event instanceof PBLifeCycleEvent)
        {
            fireBrokerEvent((PBLifeCycleEvent) event);
        }
        else if (event instanceof PBStateEvent)
        {
            fireBrokerEvent((PBStateEvent) event);
        }
        else
        {
            LoggerFactory.getDefaultLogger().error(
                    PersistenceBrokerAbstractImpl.class.getName() + ": Unkown PersistenceBrokerEvent was fired " + event);
        }
    }

    public void fireBrokerEvent(PBLifeCycleEvent event)
    {
        if (event.getPersitenceBrokerAware() != null)
        {
            // first we do the persistent object callback
            performCallBack(event);
        }

        // now we notify the listeners
        synchronized (permanentLifeCycleListeners) {
	        for (int i = permanentLifeCycleListeners.size() - 1; i >= 0; i--)
	        {
	            notifiyObjectLifeCycleListener((PBLifeCycleListener) permanentLifeCycleListeners.get(i), event);
	        }
		}

		synchronized (temporaryLifeCycleListeners) {
	        for(int i = temporaryLifeCycleListeners.size() - 1; i >= 0; i--)
	        {
	            notifiyObjectLifeCycleListener((PBLifeCycleListener) temporaryLifeCycleListeners.get(i), event);
			}
        }
    }

    public void fireBrokerEvent(PBStateEvent event)
    {
		synchronized (permanentStateListeners) {
        	for(int i = permanentStateListeners.size() - 1; i >= 0; i--)
        	{
        	    notifiyStateListener((PBStateListener) permanentStateListeners.get(i), event);
        	}
		}

		synchronized (temporaryStateListeners) {
        	for(int i = temporaryStateListeners.size() - 1; i >= 0; i--)
        	{
        	    notifiyStateListener((PBStateListener) temporaryStateListeners.get(i), event);
        	}
		}
    }

    private void performCallBack(PBLifeCycleEvent event)
    {
        // Check for null
        if (event.getPersitenceBrokerAware() == null) return;
        switch (event.getEventType().typeId())
        {
            case PBLifeCycleEvent.TYPE_AFTER_LOOKUP:
                event.getPersitenceBrokerAware().afterLookup(event.getTriggeringBroker());
                break;
            case PBLifeCycleEvent.TYPE_BEFORE_UPDATE:
                event.getPersitenceBrokerAware().beforeUpdate(event.getTriggeringBroker());
                break;
            case PBLifeCycleEvent.TYPE_AFTER_UPDATE:
                event.getPersitenceBrokerAware().afterUpdate(event.getTriggeringBroker());
                break;
            case PBLifeCycleEvent.TYPE_BEFORE_INSERT:
                event.getPersitenceBrokerAware().beforeInsert(event.getTriggeringBroker());
                break;
            case PBLifeCycleEvent.TYPE_AFTER_INSERT:
                event.getPersitenceBrokerAware().afterInsert(event.getTriggeringBroker());
                break;
            case PBLifeCycleEvent.TYPE_BEFORE_DELETE:
                event.getPersitenceBrokerAware().beforeDelete(event.getTriggeringBroker());
                break;
            case PBLifeCycleEvent.TYPE_AFTER_DELETE:
                event.getPersitenceBrokerAware().afterDelete(event.getTriggeringBroker());
                break;
        }
    }

    private void notifiyStateListener(PBStateListener listener, PBStateEvent stateEvent)
    {
        switch (stateEvent.getEventType().typeId())
        {
            case PBStateEvent.KEY_BEFORE_COMMIT:
                listener.beforeCommit(stateEvent);
                break;
            case PBStateEvent.KEY_AFTER_COMMIT:
                listener.afterCommit(stateEvent);
                break;
            case PBStateEvent.KEY_BEFORE_BEGIN:
                listener.beforeBegin(stateEvent);
                break;
            case PBStateEvent.KEY_AFTER_BEGIN:
                listener.afterBegin(stateEvent);
                break;
            case PBStateEvent.KEY_BEFORE_CLOSE:
                listener.beforeClose(stateEvent);
                break;
            case PBStateEvent.KEY_AFTER_OPEN:
                listener.afterOpen(stateEvent);
                break;
            case PBStateEvent.KEY_AFTER_ROLLBACK:
                listener.afterRollback(stateEvent);
                break;
            case PBStateEvent.KEY_BEFORE_ROLLBACK:
                listener.beforeRollback(stateEvent);
                break;
        }
    }

    private void notifiyObjectLifeCycleListener(PBLifeCycleListener listener, PBLifeCycleEvent lifeEvent)
    {
        switch (lifeEvent.getEventType().typeId())
        {
            case PBLifeCycleEvent.TYPE_AFTER_LOOKUP:
                listener.afterLookup(lifeEvent);
                break;
            case PBLifeCycleEvent.TYPE_BEFORE_UPDATE:
                listener.beforeUpdate(lifeEvent);
                break;
            case PBLifeCycleEvent.TYPE_AFTER_UPDATE:
                listener.afterUpdate(lifeEvent);
                break;
            case PBLifeCycleEvent.TYPE_BEFORE_INSERT:
                listener.beforeInsert(lifeEvent);
                break;
            case PBLifeCycleEvent.TYPE_AFTER_INSERT:
                listener.afterInsert(lifeEvent);
                break;
            case PBLifeCycleEvent.TYPE_BEFORE_DELETE:
                listener.beforeDelete(lifeEvent);
                break;
            case PBLifeCycleEvent.TYPE_AFTER_DELETE:
                listener.afterDelete(lifeEvent);
                break;
        }
    }

    /*
    arminw:
    get the PB state event here, this helps to
    avoid object instantiation for any event call
    */
    protected final PBStateEvent AFTER_OPEN_EVENT = new PBStateEvent(this, PBStateEvent.Type.AFTER_OPEN);
    protected final PBStateEvent AFTER_BEGIN_EVENT = new PBStateEvent(this, PBStateEvent.Type.AFTER_BEGIN);
    protected final PBStateEvent AFTER_COMMIT_EVENT = new PBStateEvent(this, PBStateEvent.Type.AFTER_COMMIT);
    protected final PBStateEvent AFTER_ROLLBACK_EVENT = new PBStateEvent(this, PBStateEvent.Type.AFTER_ROLLBACK);
    protected final PBStateEvent BEFORE_BEGIN_EVENT = new PBStateEvent(this, PBStateEvent.Type.BEFORE_BEGIN);
    protected final PBStateEvent BEFORE_COMMIT_EVENT = new PBStateEvent(this, PBStateEvent.Type.BEFORE_COMMIT);
    protected final PBStateEvent BEFORE_ROLLBACK_EVENT = new PBStateEvent(this, PBStateEvent.Type.BEFORE_ROLLBACK);
    protected final PBStateEvent BEFORE_CLOSE_EVENT = new PBStateEvent(this, PBStateEvent.Type.BEFORE_CLOSE);

    /*
    arminw:
    here we could get the PB state event, this helps to
    avoid object instantiation on every event
    NOTE: It's a little critical, because caller shouldn't forget
    to set the target object using #setTargetObject(...) method.
    This means that we use the same event object with changed
    fields.
    TODO: find a better solution (and performant)
    */
    protected PBLifeCycleEvent BEFORE_STORE_EVENT =
            new PBLifeCycleEvent(this, PBLifeCycleEvent.Type.BEFORE_INSERT);
    protected PBLifeCycleEvent AFTER_STORE_EVENT =
            new PBLifeCycleEvent(this, PBLifeCycleEvent.Type.AFTER_INSERT);
    protected PBLifeCycleEvent BEFORE_DELETE_EVENT =
            new PBLifeCycleEvent(this, PBLifeCycleEvent.Type.BEFORE_DELETE);
    protected PBLifeCycleEvent AFTER_DELETE_EVENT =
            new PBLifeCycleEvent(this, PBLifeCycleEvent.Type.AFTER_DELETE);
    protected PBLifeCycleEvent AFTER_LOOKUP_EVENT =
            new PBLifeCycleEvent(this, PBLifeCycleEvent.Type.AFTER_LOOKUP);
    protected PBLifeCycleEvent BEFORE_UPDATE_EVENT =
            new PBLifeCycleEvent(this, PBLifeCycleEvent.Type.BEFORE_UPDATE);
    protected PBLifeCycleEvent AFTER_UPDATE_EVENT =
            new PBLifeCycleEvent(this, PBLifeCycleEvent.Type.AFTER_UPDATE);
}

