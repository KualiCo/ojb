package org.apache.ojb.broker.ant;

/* Copyright 2005 The Apache Software Foundation
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
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import org.apache.ddlutils.io.DatabaseIO;
import org.apache.ddlutils.model.Database;
import org.apache.ddlutils.task.DatabaseTaskBase;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.metadata.JdbcConnectionDescriptor;
import org.apache.ojb.broker.metadata.MetadataManager;
import org.apache.ojb.broker.metadata.RepositoryPersistor;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.FileSet;

/**
 * Task for inserting data XML that is defined in terms of the repository elements.
 * 
 * @author Thomas Dudziak
 */
public class RepositoryDataTask extends DatabaseTaskBase
{
    /** The sub tasks to execute. */
    private ArrayList _commands = new ArrayList();
    /** The alias of the jdbc connection to use (empty = default connection) */
    private String _jcdAlias;
    /** The properties file */
    private File _ojbPropertiesFile;
    /** The repository file */
    private File _repositoryFile;
    /** A single schema file to read. */
    private File _singleSchemaFile = null;
    /** The input files. */
    private ArrayList _fileSets = new ArrayList();
    /** Whether XML input files are validated against the internal or an external DTD. */
    private boolean _useInternalDtd = true;


    /**
     * Sets the alias of the jdbc connection to use.
     * 
     * @param alias The alias of the connection
     */
    public void setJcdAlias(String alias)
    {
        _jcdAlias = alias;
    }

    /**
     * Returns the alias of the jdbc connection.
     * 
     * @return The alias
     */
    public String getJcdAlias()
    {
        return _jcdAlias;
    }

    /**
     * Returns the ojb properties file, per default 'OJB.properties' in the current directory.
     * 
     * @return The ojb properties file
     */
    public File getOjbPropertiesFile()
    {
        return _ojbPropertiesFile;
    }
    /**
     * Sets the ojb properties file.
     *
     * @param ojbPropertiesFile The ojb properties file
     */
    public void setOjbPropertiesFile(File ojbPropertiesFile)
    {
        _ojbPropertiesFile = ojbPropertiesFile;
    }

    /**
     * Returns the repository file.
     * 
     * @return The repository file
     */
    public File getRepositoryFile()
    {
        return _repositoryFile;
    }

    /**
     * Sets the repository file (per default the one configured in the ojb properties file is used).
     * 
     * @param file The repository file
     */
    public void setRepositoryFile(File file)
    {
        _repositoryFile = file;
    }

    /**
     * Specifies whether XML schema files are validated against the internal or an external DTD.
     *
     * @param useInternalDtd <code>true</code> if input files are to be validated against the internal DTD
     */
    public void setUseInternalDtd(boolean useInternalDtd)
    {
        _useInternalDtd = useInternalDtd;
    }

    /**
     * Adds a fileset specifying the schema files.
     * 
     * @param fileset The additional input files
     */
    public void addConfiguredFileset(FileSet fileset)
    {
        _fileSets.add(fileset);
    }

    /**
     * Set the xml schema describing the application model.
     *
     * @param schemaFile The schema
     */
    public void setSchemaFile(File schemaFile)
    {
        _singleSchemaFile = schemaFile;
    }

    /**
     * Adds the "write dtd to file"-command.
     * 
     * @param command The command
     */
    public void addWriteDtdToFile(WriteDtdToFileCommand command)
    {
        _commands.add(command);
    }

    /**
     * Adds the "write data to database"-command.
     * 
     * @param command The command
     */
    public void addWriteDataToDatabase(WriteDataToDatabaseCommand command)
    {
        _commands.add(command);
    }

    /**
     * Adds the "write data sql to file"-command.
     * 
     * @param command The command
     */
    public void addWriteDataSqlToFile(WriteDataSqlToFileCommand command)
    {
        _commands.add(command);
    }

    /**
     * {@inheritDoc}
     */
    protected Database readModel()
    {
        DatabaseIO reader = new DatabaseIO();
        Database   model  = null;

        reader.setUseInternalDtd(_useInternalDtd);
        if ((_singleSchemaFile != null) && !_fileSets.isEmpty())
        {
            throw new BuildException("Please use either the schemafile attribute or the sub fileset element, but not both");
        }
        if (_singleSchemaFile != null)
        {
            model = readSingleSchemaFile(reader, _singleSchemaFile);
        }
        else
        {
            for (Iterator it = _fileSets.iterator(); it.hasNext();)
            {
                FileSet          fileSet    = (FileSet)it.next();
                File             fileSetDir = fileSet.getDir(getProject());
                DirectoryScanner scanner    = fileSet.getDirectoryScanner(getProject());
                String[]         files      = scanner.getIncludedFiles();
    
                for (int idx = 0; (files != null) && (idx < files.length); idx++)
                {
                    Database curModel = readSingleSchemaFile(reader, new File(fileSetDir, files[idx]));
    
                    if (model == null)
                    {
                        model = curModel;
                    }
                    else if (curModel != null)
                    {
                        try
                        {
                            model.mergeWith(curModel);
                        }
                        catch (IllegalArgumentException ex)
                        {
                            throw new BuildException("Could not merge with schema from file "+files[idx]+": "+ex.getLocalizedMessage(), ex);
                        }
                    }
                }
            }
        }
        return model;
    }

    /**
     * Reads a single schema file.
     * 
     * @param reader     The schema reader 
     * @param schemaFile The schema file
     * @return The model
     */
    private Database readSingleSchemaFile(DatabaseIO reader, File schemaFile)
    {
        Database model = null;

        if (!schemaFile.isFile())
        {
            log("Path "+schemaFile.getAbsolutePath()+" does not denote a schema file", Project.MSG_ERR);
        }
        else if (!schemaFile.canRead())
        {
            log("Could not read schema file "+schemaFile.getAbsolutePath(), Project.MSG_ERR);
        }
        else
        {
            try
            {
                model = reader.read(schemaFile);
                log("Read schema file "+schemaFile.getAbsolutePath(), Project.MSG_INFO);
            }
            catch (Exception ex)
            {
                throw new BuildException("Could not read schema file "+schemaFile.getAbsolutePath()+": "+ex.getLocalizedMessage(), ex);
            }
        }
        return model;
    }

    /**
     * Initializes OJB for the purposes of this task.
     * 
     * @return The metadata manager used by OJB
     */
    private MetadataManager initOJB()
    {
        try
        {
            if (_ojbPropertiesFile == null)
            {
                _ojbPropertiesFile = new File("OJB.properties");
                if (!_ojbPropertiesFile.exists())
                {
                    throw new BuildException("Could not find OJB.properties, please specify it via the ojbpropertiesfile attribute");
                }
            }
            else
            {
                if (!_ojbPropertiesFile.exists())
                {
                    throw new BuildException("Could not load the specified OJB properties file "+_ojbPropertiesFile);
                }
                log("Using properties file "+_ojbPropertiesFile.getAbsolutePath(), Project.MSG_INFO);
                System.setProperty("OJB.properties", _ojbPropertiesFile.getAbsolutePath());
            }

            MetadataManager     metadataManager = MetadataManager.getInstance();
            RepositoryPersistor persistor       = new RepositoryPersistor();

            if (_repositoryFile != null)
            {
                if (!_repositoryFile.exists())
                {
                    throw new BuildException("Could not load the specified repository file "+_repositoryFile);
                }
                log("Loading repository file "+_repositoryFile.getAbsolutePath(), Project.MSG_INFO);

                // this will load the info from the specified repository file
                // and merge it with the existing info (if it has been loaded)
                metadataManager.mergeConnectionRepository(persistor.readConnectionRepository(_repositoryFile.getAbsolutePath()));
                metadataManager.mergeDescriptorRepository(persistor.readDescriptorRepository(_repositoryFile.getAbsolutePath()));
            }
            else if (metadataManager.connectionRepository().getAllDescriptor().isEmpty() &&
                     metadataManager.getGlobalRepository().getDescriptorTable().isEmpty())
            {
                // Seems nothing was loaded, probably because we're not starting in the directory
                // that the properties file is in, and the repository file path is relative
                // So lets try to resolve this path and load the repository info manually
                Properties props = new Properties();

                props.load(new FileInputStream(_ojbPropertiesFile));
    
                String repositoryPath = props.getProperty("repositoryFile", "repository.xml");
                File   repositoryFile = new File(repositoryPath);
    
                if (!repositoryFile.exists())
                {
                    repositoryFile = new File(_ojbPropertiesFile.getParentFile(), repositoryPath);
                }
                metadataManager.mergeConnectionRepository(persistor.readConnectionRepository(repositoryFile.getAbsolutePath()));
                metadataManager.mergeDescriptorRepository(persistor.readDescriptorRepository(repositoryFile.getAbsolutePath()));
            }
            // we might have to determine the default pb key ourselves
            if (metadataManager.getDefaultPBKey() == null)
            {
                for (Iterator it = metadataManager.connectionRepository().getAllDescriptor().iterator(); it.hasNext();)
                {
                    JdbcConnectionDescriptor descriptor = (JdbcConnectionDescriptor)it.next();

                    if (descriptor.isDefaultConnection())
                    {
                        metadataManager.setDefaultPBKey(new PBKey(descriptor.getJcdAlias(), descriptor.getUserName(), descriptor.getPassWord()));
                        break;
                    }
                }
            }
            return metadataManager;
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
     * {@inheritDoc}
     */
    public void execute() throws BuildException
    {
        if (_commands.isEmpty())
        {
            log("No sub tasks specified, so there is nothing to do.", Project.MSG_INFO);
            return;
        }

        ClassLoader    sysClassLoader = Thread.currentThread().getContextClassLoader();
        AntClassLoader newClassLoader = new AntClassLoader(getClass().getClassLoader(), true);

        // we're changing the thread classloader so that we can access resources
        // from the classpath used to load this task's class
        Thread.currentThread().setContextClassLoader(newClassLoader);
        
        try
        {
            MetadataManager      manager  = initOJB();
            Database             dbModel  = readModel();
            DescriptorRepository objModel = manager.getGlobalRepository();

            if (dbModel == null)
            {
                throw new BuildException("No database model specified");
            }
            for (Iterator it = _commands.iterator(); it.hasNext();)
            {
                Command cmd = (Command)it.next();

                cmd.setPlatform(getPlatform());
                cmd.execute(this, dbModel, objModel);
            }
        }
        finally
        {
            // rollback of our classloader change
            Thread.currentThread().setContextClassLoader(sysClassLoader);
        }
    }
}
