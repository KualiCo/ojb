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

import java.io.*;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;

import xdoclet.DocletTask;
import xdoclet.XmlSubTask;
import xdoclet.template.TemplateEngine;

/**
 * Runs xdoclet in a separate thread (with a special class loader).
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class XDocletRunner extends Thread
{
    private FileHandling _classWriter       = new FileHandling();
    private ArrayList    _srcFiles          = new ArrayList();
    private ArrayList    _srcDirectories    = new ArrayList();
    private File         _destFile          = null;
    private String       _taskName          = null;
    private String       _subTaskName       = null;
    private HashMap      _taskProperties    = new HashMap();
    private HashMap      _subTaskProperties = new HashMap();
    private String       _result            = null;

    public XDocletRunner()
    {
        super("XDocletRunner");
    }

    public void addClass(String name, String content)
    {
        try
        {
            _srcFiles.add(_classWriter.write(name, content));
        }
        catch (IOException ex)
        {
            cleanFiles();
            throw new RuntimeException(ex);
        }
    }

    public void setDestFile(String path)
    {
        _destFile = _classWriter.ensurePath(path);
        _destFile.delete();
    }

    public void setTaskName(String className)
    {
        _taskName = className;
    }

    public void setSubTaskName(String className)
    {
        _subTaskName = className;
    }

    public void setTaskProperty(String name, Object value)
    {
        _taskProperties.put(name, value);
    }

    public void setSubTaskProperty(String name, Object value)
    {
        _subTaskProperties.put(name, value);
    }

    /* (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run()
    {
        _result = null;

        DocletTask task = null;

        try
        {
            task = initTask();
            if (_subTaskName != null)
            {
                task.addSubTask(initSubTask());
            }
        }
        catch (Exception ex)
        {
            cleanFiles();
            throw new RuntimeException(ex);
        }

        try
        {
            task.setVerbose(false);
            task.init();
            task.execute();
        }
        catch (Exception ex)
        {
            // what to do with any exception ? rethrow it ?
            cleanFiles();
            return;
        }

        if (_destFile != null)
        {
            if (_destFile.exists())
            {
                try
                {
                    _result = getDestFileContents(_destFile.getAbsolutePath()); 
                }
                catch (IOException ex)
                {
                }
            }
        }
        cleanFiles();
    }

    public String getResult()
    {
        return _result;
    }

    public void destroy()
    {
        cleanFiles();
    }

    private void cleanFiles()
    {
        _destFile = null;
        _srcFiles.clear();
        _classWriter.removeTmpDir();
    }

    private DocletTask initTask() throws Exception
    {
        DocletTask task  = (DocletTask)this.getContextClassLoader().loadClass(_taskName).newInstance();
        FileSet    files = new FileSet();

        task.setDestDir(_classWriter.getTmpDir());
        files.setDir(_classWriter.getTmpDir());

        StringBuffer includes = new StringBuffer();
        
        for (Iterator it = _srcFiles.iterator() ; it.hasNext();)
        {
            if (includes.length() > 0)
            {
                includes.append(" ");
            }
            includes.append(((File)it.next()).getName());
        }

        task.addFileset(files);
        task.setProject(new Project());

        String name;

        for (Iterator it = _taskProperties.keySet().iterator(); it.hasNext();)
        {
            name = (String)it.next();
            setProperty(task, name, _taskProperties.get(name));
        }

        return task;
    }

    private XmlSubTask initSubTask() throws Exception
    {
        XmlSubTask subTask = (XmlSubTask)this.getContextClassLoader().loadClass(_subTaskName).newInstance();

        subTask.setEngine(TemplateEngine.getEngineInstance());
        if (_destFile != null)
        {
            subTask.setDestinationFile(_destFile.getName());
        }

        String name;

        for (Iterator it = _subTaskProperties.keySet().iterator(); it.hasNext();)
        {
            name = (String)it.next();
            setProperty(subTask, name, _subTaskProperties.get(name));
        }

        return subTask;
    }

    private void setProperty(Object obj, String propertyName, Object propertyValue) throws Exception
    {
        String methodName = "set"+Character.toUpperCase(propertyName.charAt(0));

        if (propertyName.length() > 1)
        {
            methodName += propertyName.substring(1);
        }

        Method method = null;

        try
        {
            method = obj.getClass().getMethod(methodName, new Class[]{propertyValue.getClass()});
        }
        catch (NoSuchMethodException ex)
        {
            // we trying any method with that name then
            Method[] methods = obj.getClass().getMethods();

            for (int idx = 0; idx < methods.length; idx++)
            {
                if (methodName.equals(methods[idx].getName()))
                {
                    method = methods[idx];
                    break;
                }
            }
            if (method == null)
            {
                throw ex;
            }
        }
        method.invoke(obj, new Object[]{propertyValue});
    }

    private String getDestFileContents(String filename) throws IOException
    {
        BufferedReader input  = new BufferedReader(new FileReader(filename));
        StringBuffer   result = new StringBuffer(); 
        String         line;

        while ((line = input.readLine()) != null)
        {
            result.append(line);
            result.append("\n");
        }
        input.close();
        return result.toString().trim();
    }
}
