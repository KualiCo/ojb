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
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Writer;
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
 * Command to write the SQL used to insert data defined in terms of the repository model into database, into an XML file.
 * 
 * @author Thomas Dudziak
 * @version $Revision: 1.1 $
 */
public class WriteDataSqlToFileCommand extends Command
{
    /** A single data file to read. */
    private File _singleDataFile = null;
    /** The input data files. */
    private ArrayList _fileSets = new ArrayList();
    /** The file to output the data sql to. */
    private File _outputFile;

    /**
     * Adds a fileset specifying data files.
     * 
     * @param fileset The additional input files
     */
    public void addConfiguredFileset(FileSet fileset)
    {
        _fileSets.add(fileset);
    }

    /**
     * Set the xml file containing the data.
     *
     * @param schemaFile The data xml file
     */
    public void setDataFile(File dataFile)
    {
        _singleDataFile = dataFile;
    }

    /**
     * Sets the file to output the data to.
     * 
     * @param outputFile The output file
     */
    public void setOutputFile(File outputFile)
    {
        _outputFile = outputFile;
    }

    /**
     * {@inheritDoc}
     */
    public void execute(Task task, Database dbModel, DescriptorRepository objModel) throws BuildException
    {
        try
        {
            DdlUtilsDataHandling handling = new DdlUtilsDataHandling();
            PrintWriter          writer   = new PrintWriter(new FileWriter(_outputFile), true);

            handling.setModel(dbModel, objModel);
            handling.setPlatform(getPlatform());

            if (_singleDataFile != null)
            {
                readSingleDataFile(task, handling, _singleDataFile, writer);
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
                        readSingleDataFile(task, handling, new File(fileSetDir, files[idx]), writer);
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
    private void readSingleDataFile(Task task, DdlUtilsDataHandling handling, File dataFile, Writer output)
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
            try
            {
                FileReader reader = new FileReader(dataFile.getAbsolutePath());

                handling.getInsertDataSql(reader, output);
                output.flush();
                output.close();
                task.log("Read data file "+dataFile.getAbsolutePath(), Project.MSG_INFO);
            }
            catch (Exception ex)
            {
                if (isFailOnError())
                {
                    throw new BuildException("Could not read data file "+dataFile.getAbsolutePath(), ex);
                }
            }
        }
    }
}
