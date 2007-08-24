package org.apache.ojb.broker.locking;

/* Copyright 2004-2005 The Apache Software Foundation
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

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import org.apache.ojb.broker.util.configuration.Configurable;
import org.apache.ojb.broker.util.configuration.Configuration;
import org.apache.ojb.broker.util.configuration.ConfigurationException;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * This implementation of the {@link LockManager} interface supports locking
 * in distributed environments in combination with a specific lock servlet.
 *
 * @see LockManagerServlet
 * @version $Id: LockManagerRemoteImpl.java,v 1.1 2007-08-24 22:17:41 ewestfal Exp $
 */
public class LockManagerRemoteImpl implements LockManager, Configurable
{
    private Logger log = LoggerFactory.getLogger(LockManagerRemoteImpl.class);

    public static final byte METHOD_READ_LOCK = 'a';
    public static final byte METHOD_WRITE_LOCK = 's';
    public static final byte METHOD_UPGRADE_LOCK = 'u';
    public static final byte METHOD_CHECK_READ = 'r';
    public static final byte METHOD_CHECK_WRITE = 'w';
    public static final byte METHOD_CHECK_UPGRADE = 'v';
    public static final byte METHOD_RELEASE_SINGLE_LOCK = 'e';
    public static final byte METHOD_RELEASE_LOCKS = 'x';
    public static final byte METHOD_LOCK_INFO = 'i';
    public static final byte METHOD_LOCK_TIMEOUT = 't';
    public static final byte METHOD_LOCK_TIMEOUT_SET = 'y';
    public static final byte METHOD_BLOCK_TIMEOUT = 'c';
    public static final byte METHOD_BLOCK_TIMEOUT_SET = 'd';

    private static URL lockservlet = null;

    public LockManagerRemoteImpl()
    {
    }

    /**
     * @see org.apache.ojb.broker.util.configuration.Configurable#configure(org.apache.ojb.broker.util.configuration.Configuration)
     */
    public void configure(Configuration pConfig) throws ConfigurationException
    {
        String url = pConfig.getString("LockServletUrl", "http://127.0.0.1:8080/ojb-lockserver");
        log.info("Lock server servlet URL: " + url);
        try
        {
            lockservlet = new URL(url);
        }
        catch(MalformedURLException e)
        {
            throw new ConfigurationException("Invalid LockServlet Url was specified: " + url, e);
        }

    }

    /**
     * noop
     * @param timeout
     */
    public void setLockTimeout(long timeout)
    {
//        LockInfo info = new LockInfo(timeout, METHOD_LOCK_TIMEOUT_SET);
//        try
//        {
//            byte[] requestBarr = serialize(info);
//            performRequestObject(requestBarr);
//        }
//        catch(Throwable t)
//        {
//            throw new LockRuntimeException("Can't set locking timeout", t);
//        }
    }

    public long getLockTimeout()
    {
        LockInfo info = new LockInfo(METHOD_LOCK_TIMEOUT);
        try
        {
            byte[] requestBarr = serialize(info);
            return performRequestLong(requestBarr);
        }
        catch(Throwable t)
        {
            throw new LockRuntimeException("Can't get locking info", t);
        }
    }

    public long getBlockTimeout()
    {
        LockInfo info = new LockInfo(METHOD_BLOCK_TIMEOUT);
        try
        {
            byte[] requestBarr = serialize(info);
            return performRequestLong(requestBarr);
        }
        catch(Throwable t)
        {
            throw new LockRuntimeException("Can't get block timeout value", t);
        }
    }

    /**
     * noop
     */ 
    public void setBlockTimeout(long timeout)
    {
//        LockInfo info = new LockInfo(timeout, METHOD_BLOCK_TIMEOUT_SET);
//        try
//        {
//            byte[] requestBarr = serialize(info);
//            performRequestObject(requestBarr);
//        }
//        catch(Throwable t)
//        {
//            throw new LockRuntimeException("Can't set block timeout value", t);
//        }
    }

    public String getLockInfo()
    {
        LockInfo info = new LockInfo(METHOD_LOCK_INFO);
        try
        {
            byte[] requestBarr = serialize(info);
            return performRequestString(requestBarr);
        }
        catch(Throwable t)
        {
            throw new LockRuntimeException("Can't get locking info", t);
        }
    }

    public boolean readLock(Object key, Object resourceId, int isolationLevel)
    {
        LockInfo info = new LockInfo(key, resourceId, isolationLevel, METHOD_READ_LOCK);
        try
        {
            byte[] requestBarr = serialize(info);
            return performRequest(requestBarr);
        }
        catch(Throwable t)
        {
            throw new LockRuntimeException("Cannot check read lock for '"
                    + resourceId + "' using key '" + key + "'", t);
        }
    }

    public boolean releaseLock(Object key, Object resourceId)
    {
        LockInfo info = new LockInfo(key, resourceId, METHOD_RELEASE_SINGLE_LOCK);
        try
        {
            byte[] requestBarr = serialize(info);
            return performRequest(requestBarr);
        }
        catch(Throwable t)
        {
            throw new LockRuntimeException("Cannot remove write lock for '"
                    + resourceId + "' using key '" + key + "'", t);
        }
    }

//    public boolean removeReader(Object key, Object resourceId)
//    {
//        LockInfo info = new LockInfo(key, resourceId, METHOD_RELEASE_SINGLE_LOCK);
//        try
//        {
//            byte[] requestBarr = serialize(info);
//            return performRequest(requestBarr);
//        }
//        catch(Throwable t)
//        {
//            throw new LockRuntimeException("Cannot remove read lock for '"
//                    + resourceId + "' using key '" + key + "'", t);
//        }
//    }

    public void releaseLocks(Object key)
    {
        LockInfo info = new LockInfo(key, null, METHOD_RELEASE_LOCKS);
        try
        {
            byte[] requestBarr = serialize(info);
            performRequest(requestBarr);
        }
        catch(Throwable t)
        {
            throw new LockRuntimeException("Cannot release locks using owner key '" + key + "'", t);
        }
    }

    public boolean writeLock(Object key, Object resourceId, int isolationLevel)
    {
        LockInfo info = new LockInfo(key, resourceId, isolationLevel, METHOD_WRITE_LOCK);
        try
        {
            byte[] requestBarr = serialize(info);
            return performRequest(requestBarr);
        }
        catch(Throwable t)
        {
            throw new LockRuntimeException("Cannot set write lock for '"
                    + resourceId + "' using key '" + key + "'", t);
        }
    }

    public boolean upgradeLock(Object key, Object resourceId, int isolationLevel)
    {
        LockInfo info = new LockInfo(key, resourceId, isolationLevel, METHOD_UPGRADE_LOCK);
        try
        {
            byte[] requestBarr = serialize(info);
            return performRequest(requestBarr);
        }
        catch(Throwable t)
        {
            throw new LockRuntimeException("Cannot set write lock for '"
                    + resourceId + "' using key '" + key + "'", t);
        }
    }

    public boolean hasRead(Object key, Object resourceId)
    {
        try
        {
            byte[] requestBarr = serialize(new LockInfo(key, resourceId, METHOD_CHECK_READ));
            return performRequest(requestBarr);
        }
        catch(Throwable t)
        {
            throw new LockRuntimeException("Cannot check read lock for '"
                    + resourceId + "' using key '" + key + "'", t);
        }
    }

    public boolean hasWrite(Object key, Object resourceId)
    {
        try
        {
            byte[] requestBarr = serialize(new LockInfo(key, resourceId, METHOD_CHECK_WRITE));
            return performRequest(requestBarr);
        }
        catch(Throwable t)
        {
            throw new LockRuntimeException("Cannot check write lock for '"
                    + resourceId + "' using key '" + key + "'", t);
        }
    }

    public boolean hasUpgrade(Object key, Object resourceId)
    {
        try
        {
            byte[] requestBarr = serialize(new LockInfo(key, resourceId, METHOD_CHECK_UPGRADE));
            return performRequest(requestBarr);
        }
        catch(Throwable t)
        {
            throw new LockRuntimeException("Cannot check write lock for '"
                    + resourceId + "' using key '" + key + "'", t);
        }
    }

    private HttpURLConnection getHttpUrlConnection()
            throws MalformedURLException, IOException, ProtocolException
    {
        URL lockserver = getLockserverUrl();
        HttpURLConnection conn = (HttpURLConnection) lockserver.openConnection();

        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setAllowUserInteraction(false);
        conn.setUseCaches(false);
        return conn;
    }

    private URL getLockserverUrl()
    {
        return lockservlet;
    }

    public byte[] serialize(Object obj) throws IOException
    {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bao);
        oos.writeObject(obj);
        oos.close();
        bao.close();
        byte[] result = bao.toByteArray();
        return result;
    }

    private boolean performRequest(byte[] requestBarr) throws IOException, ClassNotFoundException
    {
        Object result = performRequestObject(requestBarr);
        if(result instanceof Boolean)
        {
            return ((Boolean) result).booleanValue();
        }
        else
        {
            throw new LockRuntimeException("Remote lock server error, expect return value of type 'Boolean'");
        }
    }

    private String performRequestString(byte[] requestBarr) throws IOException, ClassNotFoundException
    {
        Object result = performRequestObject(requestBarr);
        if(result instanceof String)
        {
            return (String) result;
        }
        else
        {
            throw new LockRuntimeException("Remote lock server error, expect return value of type 'String'");
        }
    }

    private long performRequestLong(byte[] requestBarr) throws IOException, ClassNotFoundException
    {
        Object result = performRequestObject(requestBarr);
        if(result instanceof Long)
        {
            return ((Long) result).longValue();
        }
        else
        {
            throw new LockRuntimeException("Remote lock server error, expect return value of type 'String'");
        }
    }

    private Object performRequestObject(byte[] requestBarr) throws IOException, ClassNotFoundException
    {
        HttpURLConnection conn = getHttpUrlConnection();

        //post request
        BufferedOutputStream out = new BufferedOutputStream(conn.getOutputStream());
        out.write(requestBarr, 0, requestBarr.length);
        out.flush();

        // read result from
        InputStream in = conn.getInputStream();
        ObjectInputStream ois = new ObjectInputStream(in);
        Object result = ois.readObject();

        // cleanup
        ois.close();
        out.close();
        conn.disconnect();

        if(result instanceof Throwable)
        {
            throw new LockRuntimeException("Remote lock server error", (Throwable) result);
        }
        else
        {
            return result;
        }
    }

    public static final class LockInfo implements Serializable
    {
        public Object key;
        public Object resourceId;
        public int isolationLevel;
        public byte methodName;
        public long lockTimeout;
        public long blockTimeout;

        public LockInfo(byte methodName)
        {
            this.methodName = methodName;
        }

        public LockInfo(Object key, Object resourceId, byte methodName)
        {
            this.key = key;
            this.resourceId = resourceId;
            this.methodName = methodName;
        }

        public LockInfo(Object key, Object resourceId, int isolationLevel, byte methodName)
        {
            this.key = key;
            this.resourceId = resourceId;
            this.isolationLevel = isolationLevel;
            this.methodName = methodName;
        }

//        public LockInfo(long timeout, byte methodName)
//        {
//            if(methodName == METHOD_LOCK_TIMEOUT_SET)
//            {
//                this.lockTimeout = timeout;
//            }
//            else
//            {
//                this.blockTimeout = timeout;
//            }
//            this.methodName = methodName;
//        }
    }
}
