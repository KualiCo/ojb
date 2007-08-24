package org.apache.ojb.broker.ant;

/* Copyright 2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.ddlutils.model.Database;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

/**
 * Command for inserting data XML defined in terms of the repository, into a database.
 * 
 * @author Thomas Dudziak
 * @version $Revision: 1.1 $
 */
public class WriteDataToDatabaseCommand extends Command
{
    /** A single data file to insert. */
    private File      _singleDataFile = null;
    /** The input files. */
    private ArrayList _fileSets = new ArrayList();
    /** Whether we should use batch mode. */
    private Boolean _useBatchMode;
    /** The maximum number of objects to insert in one batch. */
    private Integer _batchSize;
    
    /**
     * Adds a fileset.
     * 
     * @param fileset The additional input files
     */
    public void addConfiguredFileset(FileSet fileset)
    {
        _fileSets.add(fileset);
    }

    /**
     * Set the xml data file.
     *
     * @param dataFile The data file
     */
    public void setDataFile(File dataFile)
    {
        _singleDataFile = dataFile;
    }

    /**
     * Sets the maximum number of objects to insert in one batch.
     *
     * @param batchSize The number of objects
     */
    public void setBatchSize(int batchSize)
    {
        _batchSize = new Integer(batchSize);
    }

    /**
     * Specifies whether we shall be using batch mode.
     *
     * @param useBatchMode <code>true</code> if we shall use batch mode
     */
    public void setUseBatchMode(boolean useBatchMode)
    {
        _useBatchMode = Boolean.valueOf(useBatchMode);
    }

    /**
     * {@inheritDoc}
     */
    public void execute(Task task, Database dbModel, DescriptorRepository objModel) throws BuildException
    {
        try
        {
            DdlUtilsDataHandling handling = new DdlUtilsDataHandling();

            handling.setModel(dbModel, objModel);
            handling.setPlatform(getPlatform());

            if (_singleDataFile != null)
            {
                readSingleDataFile(task, handling, _singleDataFile);
            }
            else
            {
                for (Iterator it = _fileSets.iterator(); it.hasNext();)
                {
                    FileSet          fileSet    = (FileSet)it.next();
                    File             fileSetDir = fileSet.getDir(task.getProject());
                    DirectoryScanner scanner    = fileSet.getDirectoryScanner(task.getProject());
                    String[]         files      = scanner.getIncludedFiles();
    
                    for (int idx = 0; (files != null) && (idx < files.length); idx++)
                    {
                        readSingleDataFile(task, handling, new File(fileSetDir, files[idx]));
                    }
                }
            }
        }
        catch (Exception ex)
        {
            if (ex instanceof BuildException)
            {
                throw (BuildException)ex;
            }
            else
            {
                throw new BuildException(ex);
            }
        }
    }

    /**
     * Reads a single data file.
     * 
     * @param task       The parent task
     * @param reader     The data reader
     * @param schemaFile The schema file
     */
    private void readSingleDataFile(Task task, DdlUtilsDataHandling handling, File dataFile)
    {
        if (!dataFile.exists())
        {
            task.log("Could not find data file "+dataFile.getAbsolutePath(), Project.MSG_ERR);
        }
        else if (!dataFile.isFile())
        {
            task.log("Path "+dataFile.getAbsolutePath()+" does not denote a data file", Project.MSG_ERR);
        }
        else if (!dataFile.canRead())
        {
            task.log("Could not read data file "+dataFile.getAbsolutePath(), Project.MSG_ERR);
        }
        else
        {
            int batchSize = 1;

            if ((_useBatchMode != null) && _useBatchMode.booleanValue())
            {
                if (_batchSize != null)
                {
                    batchSize = _batchSize.intValue();
                }
            }
            try
            {
                handling.insertData(new FileReader(dataFile), batchSize);
                task.log("Read data file "+dataFile.getAbsolutePath(), Project.MSG_INFO);
            }
            catch (Exception ex)
            {
                if (isFailOnError())
                {
                    throw new BuildException("Could not read data file "+dataFile.getAbsolutePath(), ex);
                }
                else
                {
                    task.log("Could not read data file "+dataFile.getAbsolutePath(), Project.MSG_ERR);
                }
            }
        }
    }
}
