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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Performs file i/o, generation and removal of temporary files etc.
 *
 * @author    <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class FileHandling
{
    private File _tmpDir = null;

    public File getTmpDir()
    {
        return _tmpDir;
    }

    public File write(String name, String content) throws IOException
    {
        // first we determine the temp directory
        if (_tmpDir == null)
        {
            File   dummy  = File.createTempFile("dummy", ".java");
            String tmpDir = dummy.getPath().substring(0, dummy.getPath().lastIndexOf(File.separatorChar));

            if ((tmpDir == null) || (tmpDir.length() == 0))
            {
                tmpDir = ".";
            }
            dummy.delete();

            int nr = 0;

            while (_tmpDir == null)
            {
                _tmpDir = new File(tmpDir, "test"+nr);
                if (_tmpDir.exists())
                {
                    nr++;
                    _tmpDir = null;
                }
            }
            _tmpDir.mkdir();
        }

        // next we generate a file for the srcClass
        String     fileName = name.replace('.', File.separatorChar) + ".java";
        File       srcFile  = ensurePath(fileName);
        FileWriter output   = new FileWriter(srcFile.getAbsolutePath());

        output.write(content);
        output.close();
        return srcFile;
    }

    public File ensurePath(String fileName)
    {
        String shortFileName = fileName;
        File   dir           = _tmpDir;
        int    pos           = shortFileName.indexOf(File.separatorChar);

        while (pos >= 0)
        {
            dir = new File(dir, shortFileName.substring(0, pos));
            dir.mkdir();
            shortFileName = shortFileName.substring(pos + 1);
            pos           = shortFileName.indexOf(File.separatorChar);
        }

        return new File(dir, shortFileName);
    }

    public void removeTmpDir()
    {
        if (_tmpDir != null)
        {
            removeDir(_tmpDir);
        }
        _tmpDir = null;
    }

    private void removeDir(File dir)
    {
        if (dir.exists())
        {
            String[] files = dir.list();
            File     sub;

            if (files != null)
            {
                for (int idx = 0; idx < files.length; idx++)
                {
                    sub = new File(dir, files[idx]);
                    if (sub.isDirectory())
                    {
                        removeDir(sub);
                    }
                    else
                    {
                        sub.delete();
                    }
                }
            }
            dir.delete();
        }
    }
}
