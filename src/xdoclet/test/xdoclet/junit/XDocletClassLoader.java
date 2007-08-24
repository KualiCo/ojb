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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Special class loader for xdoclet tests.
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class XDocletClassLoader extends ClassLoader
{
    private ArrayList _classPath = new ArrayList();

    public XDocletClassLoader(String classPath)
    {
        super();
        setClassPath(classPath);
    }

    public synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException
    {
        Class result = null;

        if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("org.") || name.startsWith("sun."))
        {
            try
            {
                result = findSystemClass(name);
            }
            catch (ClassNotFoundException ex)
            {}
        }
        if (result == null)
        {
            result = findLoadedClass(name);
        }
        if (result == null)
        {
            byte[] data = loadClassData(name);

            if (data != null)
            {
                result = defineClass(name, data, 0, data.length);
            }
        }
        if (result == null)
        {
            throw new ClassNotFoundException(name);
        }
        if (resolve)
        {
            resolveClass(result);
        }
        return result;
    }

    private void setClassPath(String classPath)
    {
        StringTokenizer tokenizer = new StringTokenizer(classPath, System.getProperty("path.separator"));

        _classPath.clear();
        while (tokenizer.hasMoreTokens())
        {
            _classPath.add(tokenizer.nextToken());
        }
    }

    private byte[] loadClassData(String className) throws ClassNotFoundException
    {
        byte[] data     = null;
        String path     = null;
        String fileName = className.replace('.', '/') + ".class";

        for (Iterator it = _classPath.iterator(); it.hasNext();)
        {
            path = (String)it.next();
            if (path.toLowerCase().endsWith(".jar") || path.toLowerCase().endsWith(".zip"))
            {
                data = loadJar(path, fileName);
            }
            else
            {
                data = loadFile(path, fileName);
            }
            if (data != null)
            {
                return data;
            }
        }
        throw new ClassNotFoundException(className);
    }

    private byte[] loadFile(String path, String fileName)
    {
        File file = new File(path, fileName);

        if (file.exists())
        { 
            try
            {
                FileInputStream       input  = new FileInputStream(file);
                ByteArrayOutputStream output = new ByteArrayOutputStream(1000);
                byte[]                data   = new byte[1000];
                int                   numRead;

                while ((numRead = input.read(data)) != -1)
                {
                    output.write(data, 0, numRead); 
                }
                input.close();
                output.close();
                return output.toByteArray();
            }
            catch (IOException ex)
            {}
        }
        return null;
    }

    private byte[] loadJar(String path, String fileName)
    {
        File file = new File(path);

        if (!file.exists())
        {
            return null;
        }

        ZipFile zipFile = null;

        try
        {
            zipFile = new ZipFile(file);
        }
        catch(IOException ex)
        {
            return null;
        }

        ZipEntry entry = zipFile.getEntry(fileName);

        if (entry == null)
        {
            return null;
        }

        InputStream input = null;
        int         size  = (int)entry.getSize();
        byte[]      data  = new byte[size];
        int         numRead;

        try
        {
            input = zipFile.getInputStream(entry);

            for (int pos = 0; pos < size; pos += numRead)
            {
                numRead = input.read(data, pos, data.length - pos);
            }
            zipFile.close();
            return data;
        }
        catch (IOException ex)
        {}
        finally
        {
            try
            {
                if (input != null)
                {
                    input.close();
                }
            }
            catch (IOException ex)
            {}
        }
        return null;
    }
}
