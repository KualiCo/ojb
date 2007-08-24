package xdoclet.junit;

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

import java.util.HashMap;
import java.util.Iterator;

import junit.framework.TestCase;

/**
 * Base class for xdoclet test cases.
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public abstract class XDocletTestBase extends TestCase
{
    private HashMap _srcClasses        = new HashMap();
    private String  _destFile          = null;
    private String  _taskName          = null;
    private String  _subTaskName       = null;
    private HashMap _taskProperties    = new HashMap();
    private HashMap _subTaskProperties = new HashMap();

    public XDocletTestBase(String name)
    {
        super(name);
    }

    protected void addClass(String name, String content)
    {
        _srcClasses.put(name, content);
    }

    protected void setDestFile(String path)
    {
        _destFile = path;
    }

    protected void setTaskName(String className)
    {
        _taskName = className;
    }

    protected void setSubTaskName(String className)
    {
        _subTaskName = className;
    }

    protected void setTaskProperty(String name, Object value)
    {
        _taskProperties.put(name, value);
    }

    protected Object getTaskProperty(String propertyName)
    {
        return _taskProperties.get(propertyName);
    }

    protected void clearTaskProperties()
    {
        _taskProperties.clear();
    }

    protected void setSubTaskProperty(String name, Object value)
    {
        _subTaskProperties.put(name, value);
    }

    protected void clearSubTaskProperties()
    {
        _subTaskProperties.clear();
    }

    /**
     * Runs the XDoclet task/subtask with the current settings. Note that XDoclet does not return an exception
     * upon failure as one would expect (e.g. XDocletException), but simply returns <code>null</code>, so use
     * <code>assertNull</code> if expecting an error.
     *  
     * @return The content of the destination file trimmed at both ends (i.e. no whitespaces at the beginning or end)
     */
    protected String runXDoclet()
    {
        String      classPath   = System.getProperty("java.class.path");
        //ClassLoader loader      = new TestCaseClassLoader(classPath);
        ClassLoader loader      = new XDocletClassLoader(classPath);
        Class       runnerClass = null;
        Object      runner      = null;

        try
        {
            runnerClass = loader.loadClass(XDocletRunner.class.getName());
            runner      = runnerClass.newInstance();
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }

        invoke(runnerClass,
               runner,
               "setContextClassLoader",
               new Class[]{ ClassLoader.class },
               new Object[]{ loader });
        copySettings(runnerClass, runner);
        invoke(runnerClass,
               runner,
               "start",
               null,
               null);
        while (((Boolean)invoke(runnerClass,
                                runner,
                                "isAlive",
                                null,
                                null)).booleanValue())
        {
            try
            {
                Thread.sleep(10);
            }
            catch (InterruptedException ex)
            {}
        }

        String result = (String)invoke(runnerClass,
                                       runner,
                                       "getResult",
                                       null,
                                       null);

        invoke(runnerClass,
               runner,
               "destroy",
               null,
               null);
        runnerClass = null;
        runner      = null;
        loader      = null;
        System.gc();

        return result;
    }

    private void copySettings(Class runnerClass, Object runner)
    {
        String name;

        for (Iterator it = _srcClasses.keySet().iterator(); it.hasNext();)
        {
            name = (String)it.next();
            invoke(runnerClass,
                   runner,
                   "addClass",
                   new Class[]{ String.class, String.class },
                   new Object[]{ name, (String)_srcClasses.get(name)});
        }
        invoke(runnerClass,
               runner,
               "setDestFile",
               new Class[]{ String.class },
               new Object[]{ _destFile });
        invoke(runnerClass,
               runner,
               "setTaskName",
               new Class[]{ String.class },
               new Object[]{ _taskName });
        for (Iterator it = _taskProperties.keySet().iterator(); it.hasNext();)
        {
            name = (String)it.next();
            invoke(runnerClass,
                   runner,
                   "setTaskProperty",
                   new Class[]{ String.class, Object.class },
                   new Object[]{ name, _taskProperties.get(name)});
        }
        if (_subTaskName != null)
        {
            invoke(runnerClass,
                   runner,
                   "setSubTaskName",
                   new Class[]{ String.class },
                   new Object[]{ _subTaskName });
            for (Iterator it = _subTaskProperties.keySet().iterator(); it.hasNext();)
            {
                name = (String)it.next();
                invoke(runnerClass,
                       runner,
                       "setSubTaskProperty",
                       new Class[]{ String.class, Object.class },
                       new Object[]{ name, _subTaskProperties.get(name)});
            }
        }
    }

    private Object invoke(Class runnerClass, Object runner, String methodName, Class[] argTypes, Object[] args)
    {
        try
        {
            return runnerClass.getMethod(methodName, argTypes).invoke(runner, args);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }
}
