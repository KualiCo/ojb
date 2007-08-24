package org.apache.ojb.odmg.locking;

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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Collection;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.util.configuration.Configurable;
import org.apache.ojb.broker.util.configuration.Configuration;
import org.apache.ojb.broker.util.configuration.ConfigurationException;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.odmg.TransactionImpl;
import org.apache.ojb.odmg.TxManagerFactory;

/**
 * Servlet based lock mechanism for usage in distributed environment.
 * @author Thomas Mahler
 * @version $Id: RemoteLockMapImpl.java,v 1.1 2007-08-24 22:17:28 ewestfal Exp $
 */
public class RemoteLockMapImpl implements LockMap, Configurable
{
	private Logger log = LoggerFactory.getLogger(RemoteLockMapImpl.class);
	
	private static URL lockservlet = null;

	/**
	 * obtain a PersistenceBroker instance.
	 */
	private PersistenceBroker getBroker()
	{
		return TxManagerFactory.instance().getCurrentTransaction().getBroker();
	}

	/**
	 * returns the LockEntry for the Writer of object obj.
	 * If now writer exists, null is returned.
	 */
	public LockEntry getWriter(Object obj)
	{
		PersistenceBroker broker = getBroker();
		Identity oid = new Identity(obj, broker);
		
        LockEntry result = null;
        try
        {
        	result = getWriterRemote(oid);
        }
        catch (Throwable e)
        {
        	log.error(e);
        }
		return result;
	}

    private LockEntry getWriterRemote(Identity oid)
        throws
            MalformedURLException,
            IOException,
            ProtocolException,
            ClassNotFoundException
    {
    	byte selector = (byte) 'w';
        byte[] requestBarr = buildRequestArray(oid, selector);
        
        HttpURLConnection conn = getHttpUrlConnection();
        
        //post request
        BufferedOutputStream out = new BufferedOutputStream(conn.getOutputStream());
        out.write(requestBarr,0,requestBarr.length);
        out.flush();
        out.close();		
        
        // read result from 
        InputStream in = conn.getInputStream();
        ObjectInputStream ois = new ObjectInputStream(in);
        LockEntry result = (LockEntry) ois.readObject();
        
        // cleanup
        ois.close();
        conn.disconnect();
        return result;
    }

    private HttpURLConnection getHttpUrlConnection()
        throws MalformedURLException, IOException, ProtocolException
    {
        URL lockserver = getLockserverUrl();
        HttpURLConnection conn= (HttpURLConnection) lockserver.openConnection();
        
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setAllowUserInteraction(false);
        conn.setUseCaches(false);
        return conn;
    }

    private byte[] buildRequestArray(Object object, byte selector) throws IOException
    {
        byte[] serialObj = serialize(object);
        int len = serialObj.length; 
        byte[] requestBarr = new byte[len + 1];
        requestBarr[0] = selector;
        System.arraycopy(serialObj,0,requestBarr,1,len);
        return requestBarr;
    }


    private URL getLockserverUrl() throws MalformedURLException
    {
        return lockservlet;
    }



	/**
	 * returns a collection of Reader LockEntries for object obj.
	 * If now LockEntries could be found an empty Vector is returned.
	 */
	public Collection getReaders(Object obj)
	{
		Collection result = null;
        try
        {
            Identity oid = new Identity(obj, getBroker());
            byte selector = (byte) 'r';
            byte[] requestBarr = buildRequestArray(oid, selector);
            
            HttpURLConnection conn = getHttpUrlConnection();
            
            //post request
            BufferedOutputStream out = new BufferedOutputStream(conn.getOutputStream());
            out.write(requestBarr,0,requestBarr.length);
            out.flush();		
            
            // read result from 
            InputStream in = conn.getInputStream();
            ObjectInputStream ois = new ObjectInputStream(in);
            result = (Collection) ois.readObject();
            
            // cleanup
            ois.close();
            out.close();
            conn.disconnect();		
        }
        catch (Throwable t)
        {
            throw new PersistenceBrokerException(t);
        }
        return result;
	}


	/**
	 * Add a reader lock entry for transaction tx on object obj
	 * to the persistent storage.
	 */
	public boolean addReader(TransactionImpl tx, Object obj)
	{
		try
		{
			LockEntry lock = new LockEntry(new Identity(obj,getBroker()).toString(),
					tx.getGUID(),
					System.currentTimeMillis(),
					LockStrategyFactory.getIsolationLevel(obj),
					LockEntry.LOCK_READ);
            addReaderRemote(lock);
			return true;
		}
		catch (Throwable t)
		{
			log.error("Cannot store LockEntry for object " + obj + " in transaction " + tx, t);
			return false;
		}
	}

    private void addReaderRemote(LockEntry lock) throws IOException, ClassNotFoundException
    {
		byte selector = (byte) 'a';
		byte[] requestBarr = buildRequestArray(lock,selector);
        
		HttpURLConnection conn = getHttpUrlConnection();
        
		//post request
		BufferedOutputStream out = new BufferedOutputStream(conn.getOutputStream());
		out.write(requestBarr,0,requestBarr.length);
		out.flush();		
        
		// read result from 
		InputStream in = conn.getInputStream();
		ObjectInputStream ois = new ObjectInputStream(in);
		Boolean result = (Boolean) ois.readObject();
        
		// cleanup
		ois.close();
		out.close();
		conn.disconnect();
		if (! result.booleanValue())
		{
			throw new PersistenceBrokerException("could not add reader!");		
		}
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

	/**
	 * remove a reader lock entry for transaction tx on object obj
	 * from the persistent storage.
	 */
	public void removeReader(TransactionImpl tx, Object obj)
	{
		try
		{
			LockEntry lock = new LockEntry(new Identity(obj,getBroker()).toString(), tx.getGUID());
            removeReaderRemote(lock);
		}
		catch (Throwable t)
		{
			log.error("Cannot remove LockEntry for object " + obj + " in transaction " + tx);
		}
	}

    private void removeReaderRemote(LockEntry lock) throws IOException, ClassNotFoundException
    {
		byte selector = (byte) 'e';
		byte[] requestBarr = buildRequestArray(lock,selector);
        
		HttpURLConnection conn = getHttpUrlConnection();
        
		//post request
		BufferedOutputStream out = new BufferedOutputStream(conn.getOutputStream());
		out.write(requestBarr,0,requestBarr.length);
		out.flush();		
        
		// read result from 
		InputStream in = conn.getInputStream();
		ObjectInputStream ois = new ObjectInputStream(in);
		Boolean result = (Boolean) ois.readObject();
        
		// cleanup
		ois.close();
		out.close();
		conn.disconnect();
		if (! result.booleanValue())
		{
			throw new PersistenceBrokerException("could not remove reader!");		
		}

    }

	/**
	 * remove a writer lock entry for transaction tx on object obj
	 * from the persistent storage.
	 */
	public void removeWriter(LockEntry writer)
	{
		try
		{
			removeWriterRemote(writer);
		}
		catch (Throwable t)
		{
			log.error("Cannot remove LockEntry", t);
		}
	}

	private void removeWriterRemote(LockEntry lock) throws IOException, ClassNotFoundException
	{
		byte selector = (byte) 'm';
		byte[] requestBarr = buildRequestArray(lock,selector);
        
		HttpURLConnection conn = getHttpUrlConnection();
        
		//post request
		BufferedOutputStream out = new BufferedOutputStream(conn.getOutputStream());
		out.write(requestBarr,0,requestBarr.length);
		out.flush();		
        
		// read result from 
		InputStream in = conn.getInputStream();
		ObjectInputStream ois = new ObjectInputStream(in);
		Boolean result = (Boolean) ois.readObject();
        
		// cleanup
		ois.close();
		out.close();
		conn.disconnect();
		if (! result.booleanValue())
		{
			throw new PersistenceBrokerException("could not remove writer!");		
		}

	}

	/**
	 * upgrade a reader lock entry for transaction tx on object obj
	 * and write it to the persistent storage.
	 */
	public boolean upgradeLock(LockEntry reader)
	{
		try
		{
			upgradeLockRemote(reader);
			reader.setLockType(LockEntry.LOCK_WRITE);
			return true;
		}
		catch (Throwable t)
		{
			log.error("Cannot upgrade LockEntry " + reader, t);
			return false;
		}
	}

	private void upgradeLockRemote(LockEntry lock) throws IOException, ClassNotFoundException
	{
		byte selector = (byte) 'u';
		byte[] requestBarr = buildRequestArray(lock,selector);
        
		HttpURLConnection conn = getHttpUrlConnection();
        
		//post request
		BufferedOutputStream out = new BufferedOutputStream(conn.getOutputStream());
		out.write(requestBarr,0,requestBarr.length);
		out.flush();		
        
		// read result from 
		InputStream in = conn.getInputStream();
		ObjectInputStream ois = new ObjectInputStream(in);
		Boolean result = (Boolean) ois.readObject();
        
		// cleanup
		ois.close();
		out.close();
		conn.disconnect();
		if (! result.booleanValue())
		{
			throw new PersistenceBrokerException("could not remove writer!");		
		}

	}

	/**
	 * generate a writer lock entry for transaction tx on object obj
	 * and write it to the persistent storage.
	 */
	public boolean setWriter(TransactionImpl tx, Object obj)
	{
		try
		{
			LockEntry lock = new LockEntry(new Identity(obj,getBroker()).toString(),
					tx.getGUID(),
					System.currentTimeMillis(),
					LockStrategyFactory.getIsolationLevel(obj),
					LockEntry.LOCK_WRITE);

			setWriterRemote(lock);
			return true;
		}
		catch (Throwable t)
		{
			log.error("Cannot set LockEntry for object " + obj + " in transaction " + tx);
			return false;
		}
	}

	private void setWriterRemote(LockEntry lock) throws IOException, ClassNotFoundException
	{
		byte selector = (byte) 's';
		byte[] requestBarr = buildRequestArray(lock,selector);
        
		HttpURLConnection conn = getHttpUrlConnection();
        
		//post request
		BufferedOutputStream out = new BufferedOutputStream(conn.getOutputStream());
		out.write(requestBarr,0,requestBarr.length);
		out.flush();		
        
		// read result from 
		InputStream in = conn.getInputStream();
		ObjectInputStream ois = new ObjectInputStream(in);
		Boolean result = (Boolean) ois.readObject();
        
		// cleanup
		ois.close();
		out.close();
		conn.disconnect();
		if (! result.booleanValue())
		{
			throw new PersistenceBrokerException("could not set writer!");		
		}

	}

	/**
	 * check if there is a reader lock entry for transaction tx on object obj
	 * in the persistent storage.
	 */
	public boolean hasReadLock(TransactionImpl tx, Object obj)
	{
		try
		{
			LockEntry lock = new LockEntry(new Identity(obj,getBroker()).toString(), tx.getGUID());
			boolean result = hasReadLockRemote(lock);			
			return result;
		}
		catch (Throwable t)
		{
			log.error("Cannot check read lock for object " + obj + " in transaction " + tx, t);
			return false;
		}
	}

	private boolean hasReadLockRemote(LockEntry lock) throws IOException, ClassNotFoundException
	{
		byte selector = (byte) 'h';
		byte[] requestBarr = buildRequestArray(lock,selector);
        
		HttpURLConnection conn = getHttpUrlConnection();
        
		//post request
		BufferedOutputStream out = new BufferedOutputStream(conn.getOutputStream());
		out.write(requestBarr,0,requestBarr.length);
		out.flush();		
        
		// read result from 
		InputStream in = conn.getInputStream();
		ObjectInputStream ois = new ObjectInputStream(in);
		Boolean result = (Boolean) ois.readObject();
        
		// cleanup
		ois.close();
		out.close();
		conn.disconnect();
		return result.booleanValue();
	}



	
    /**
     * @see org.apache.ojb.broker.util.configuration.Configurable#configure(org.apache.ojb.broker.util.configuration.Configuration)
     */
    public void configure(Configuration pConfig) throws ConfigurationException
    {
        String url = pConfig.getString("LockServletUrl","http://127.0.0.1:8080/ojb-lockserver");
        try
        {
            lockservlet = new URL(url);
        }
        catch (MalformedURLException e)
        {
            throw new ConfigurationException("Invalid LockServlet Url was specified: " + url, e);
        }

    }

}
