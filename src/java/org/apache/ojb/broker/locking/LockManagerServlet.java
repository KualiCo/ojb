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


import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.ojb.broker.util.ClassHelper;


/**
 * @author Thomas Mahler
 */
public class LockManagerServlet extends HttpServlet
{
    protected static LockManager lockmanager;
    static final String STR_LOCK_TIMEOUT = "lockTimeout";
    static final String STR_BLOCK_TIMEOUT = "blockTimeout";
    static final String STR_LOCK_MANAGER = "lockManager";

    private static long numRequests;
    private static Throwable lastError = null;

    public void init(ServletConfig servletConfig) throws ServletException
    {
        super.init(servletConfig);
        // if lock manager was instantiated not yet
        if(lockmanager == null)
        {
            lastError = null;
            numRequests = 0;
            String strLockManager = servletConfig.getInitParameter(STR_LOCK_MANAGER);
            try
            {
                lockmanager = (LockManager) (strLockManager != null ?
                        ClassHelper.newInstance(strLockManager) : ClassHelper.newInstance(LockManagerInMemoryImpl.class));
            }
            catch(Exception e)
            {
                lastError = new LockRuntimeException("Can't instance lock manager, init parameter 'lockManager': " + strLockManager);
                e.printStackTrace();
            }
            String strTimeout = servletConfig.getInitParameter(STR_LOCK_TIMEOUT);
            if(NumberUtils.isNumber(strTimeout))
            {
                try
                {
                    Long lockTimeout = NumberUtils.createLong(strTimeout);
                    lockmanager.setLockTimeout(lockTimeout.longValue());
                }
                catch(Exception e)
                {
                    if(lastError == null)
                    {
                        lastError = new LockRuntimeException("Can't convert 'lockTimeout' init parameter: " + strTimeout);
                    }
                    e.printStackTrace();
                }
            }
            String strBlock = servletConfig.getInitParameter(STR_BLOCK_TIMEOUT);
            if(NumberUtils.isNumber(strBlock))
            {
                try
                {
                    Long blockTimeout = NumberUtils.createLong(strBlock);
                    lockmanager.setLockTimeout(blockTimeout.longValue());
                }
                catch(Exception e)
                {
                    if(lastError == null)
                    {
                        lastError = new LockRuntimeException("Can't convert 'blockTimeout' init parameter: " + strBlock);
                    }
                    e.printStackTrace();
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        response.setContentType("text/html");
        response.setHeader("Pragma", "no-cache");

        PrintWriter out = response.getWriter();

        out.println("<html><head><title>OJB Distributed Locking Servlet Status Page</title>");
        out.println("</head><body><h1>OJB Distributed Locking Servlet</h1>");
        out.println("The servlet is running.<p>");

        if(lastError == null)
        {
            out.println("The LockServer is running.<p>");
            out.println("LockManager info: " + lockmanager.getLockInfo() + "<p>");
            out.println("Processed Lock Request: " + numRequests + "<p>");
        }
        else
        {
            out.println("<h2>The LockServer has a problem!</h2>");
            out.println("The error message is:<p>");
            out.println(lastError.getMessage() + "<p>");
            lastError.printStackTrace(out);
            lastError = null;
        }

        out.println("</body></html>");
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        // update counter
        numRequests++;

        try
        {
            // read request:
            LockManagerRemoteImpl.LockInfo info = (LockManagerRemoteImpl.LockInfo) buildObjectFromRequest(request);
            Object result = null;
            // now execute the command specified by the selector
            try
            {
                switch(info.methodName)
                {
                    case LockManagerRemoteImpl.METHOD_READ_LOCK:
                        {
                            result = new Boolean(lockmanager.readLock(info.key, info.resourceId, info.isolationLevel));
                            break;
                        }
                    case LockManagerRemoteImpl.METHOD_RELEASE_SINGLE_LOCK:
                        {
                            result = new Boolean(lockmanager.releaseLock(info.key, info.resourceId));
                            break;
                        }
                    case LockManagerRemoteImpl.METHOD_RELEASE_LOCKS:
                        {
                            lockmanager.releaseLocks(info.key);
                            result = Boolean.TRUE;
                            break;
                        }
                    case LockManagerRemoteImpl.METHOD_WRITE_LOCK:
                        {
                            result = new Boolean(lockmanager.writeLock(info.key, info.resourceId,
                                    info.isolationLevel));
                            break;
                        }
                    case LockManagerRemoteImpl.METHOD_UPGRADE_LOCK:
                        {
                            result = new Boolean(lockmanager.upgradeLock(info.key, info.resourceId, info.isolationLevel));
                            break;
                        }
                    case LockManagerRemoteImpl.METHOD_CHECK_READ:
                        {
                            result = new Boolean(lockmanager.hasRead(info.key, info.resourceId));
                            break;
                        }
                    case LockManagerRemoteImpl.METHOD_CHECK_WRITE:
                        {
                            result = new Boolean(lockmanager.hasWrite(info.key, info.resourceId));
                            break;
                        }
                    case LockManagerRemoteImpl.METHOD_CHECK_UPGRADE:
                        {
                            result = new Boolean(lockmanager.hasUpgrade(info.key, info.resourceId));
                            break;
                        }
                    case LockManagerRemoteImpl.METHOD_LOCK_INFO:
                        {
                            result = lockmanager.getLockInfo();
                            break;
                        }
                    case LockManagerRemoteImpl.METHOD_LOCK_TIMEOUT:
                        {
                            result = new Long(lockmanager.getLockTimeout());
                            break;
                        }
                    case LockManagerRemoteImpl.METHOD_BLOCK_TIMEOUT:
                        {
                            result = new Long(lockmanager.getBlockTimeout());
                            break;
                        }
//                    case LockManagerRemoteImpl.METHOD_LOCK_TIMEOUT_SET:
//                        {
//                            lockmanager.setLockTimeout(info.lockTimeout);
//                            break;
//                        }
//
//                    case LockManagerRemoteImpl.METHOD_BLOCK_TIMEOUT_SET:
//                        {
//                            lockmanager.setBlockTimeout(info.blockTimeout);
//                            break;
//                        }
                    default :
                        {
                            throw new LockRuntimeException("Unknown command:" + info.methodName);
                        }
                }
            }
            catch(RuntimeException e)
            {
                result = new LockRuntimeException("Error while invoke specified method in servlet.", e);
            }

            ObjectOutputStream oos = new ObjectOutputStream(response.getOutputStream());
            oos.writeObject(result);
            oos.flush();
            oos.close();
        }
        catch(Throwable t)
        {
            lastError = t;
            t.printStackTrace();
        }
    }

    private Object buildObjectFromRequest(HttpServletRequest request) throws IOException, ClassNotFoundException
    {
        Object obj = null;
        // get the body of the request as binary data
        InputStream is = request.getInputStream();
        ObjectInputStream objInputStream = new ObjectInputStream(is);
        obj = objInputStream.readObject();
        objInputStream.close();
        is.close();
        return obj;
    }
}
