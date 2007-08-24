package org.apache.ojb.broker.util.dbhandling;

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.util.ClassHelper;
import org.apache.ojb.broker.metadata.ConnectionRepository;
import org.apache.ojb.broker.metadata.MetadataManager;
import org.apache.tools.ant.*;
import org.apache.tools.ant.types.FileSet;

/**
 * Ant task for performing basic db setup functions.
 * 
 * @author Thomas Dudziak
 */
public class DBHandlingTask extends Task
{
    /** The name of the known db handlings */
    private static final String HANDLING_TORQUE = "torque";
    /** The commands */
    private static final String COMMAND_CREATE  = "create";
    private static final String COMMAND_INIT    = "init";
    
    /** The name of the db handling to use */
    private String _handling = HANDLING_TORQUE;
    /** The path to the properties file */
    private String _propertiesFile = null;
    /** The alias of the jdbc connection to use (empty = default connection) */
    private String _jcdAlias = null;
    /** The working directory */
    private String _workDir = null;
    /** The input files */
    private ArrayList _fileSets = new ArrayList();
    /** The commands to perform */
    private String _commands = "";
    
    /**
     * Sets the name of the handling to use.
     * 
     * @param name The short name of the handling
     */
    public void setHandling(String name)
    {
        _handling = (name == null ? HANDLING_TORQUE : name.toLowerCase());
    }

    /**
     * Returns the name of the handling that is used.
     * 
     * @return The short name of the handling
     */
    public String getHandling()
    {
        return _handling;
    }

    /**
     * Sets the properties file (OJB.properties).
     * 
     * @param path The path to the properties file
     */
    public void setPropertiesFile(String path)
    {
        _propertiesFile = path;
    }

    /**
     * Returns the properties file.
     * 
     * @return The path to the properties file
     */
    public String getPropertiesFile()
    {
        return _propertiesFile;
    }

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
     * Sets the working directory. If none is given, then the system's temporary directory is used.
     * 
     * @param dir The working directory
     */
    public void setWorkDir(String dir)
    {
        _workDir = dir;
    }

    /**
     * Returns the working directory.
     * 
     * @return The working directory
     */
    public String getWorkDir()
    {
        return _workDir;
    }

    /**
     * Adds a fileset.
     * 
     * @param fileset The additional input files
     */
    public void addFileset(FileSet fileset)
    {
        _fileSets.add(fileset);
    }

    /**
     * Sets the list of commands to perform.
     * 
     * @param listOfCommands The comma-separated list of commands
     */
    public void setCommands(String listOfCommands)
    {
        _commands = listOfCommands;
    }

    /**
     * Returns the list of commands.
     * 
     * @return The comma-separated list of commands
     */
    public String getCommands()
    {
        return _commands;
    }

    /* (non-Javadoc)
     * @see org.apache.tools.ant.Task#execute()
     */
    public void execute() throws BuildException
    {
        if ((_commands == null) || (_commands.length() == 0))
        {
            return;
        }

        DBHandling handling = createDBHandling();

        try
        {
            if ((_workDir != null) && (_workDir.length() > 0))
            {
                handling.setWorkDir(_workDir);
                System.setProperty("user.dir", _workDir);
            }
            for (Iterator it = _fileSets.iterator(); it.hasNext();)
            {
                addIncludes(handling, (FileSet)it.next());
            }

            if ((_propertiesFile != null) && (_propertiesFile.length() > 0))
            {
                System.setProperty("OJB.properties", _propertiesFile);
            }

            ConnectionRepository connRep = MetadataManager.getInstance().connectionRepository();
            PBKey                pbKey   = null;

            if ((_jcdAlias == null) || (_jcdAlias.length() == 0))
            {
                pbKey = PersistenceBrokerFactory.getDefaultKey();
            }
            else
            {
                pbKey = connRep.getStandardPBKeyForJcdAlias(_jcdAlias);
                if (pbKey == null)
                {
                    throw new BuildException("Undefined jcdAlias "+_jcdAlias);
                }
            }
            handling.setConnection(connRep.getDescriptor(pbKey));

            String command;

            for (StringTokenizer tokenizer = new StringTokenizer(_commands, ","); tokenizer.hasMoreTokens();)
            {
                command = tokenizer.nextToken().toLowerCase().trim();
                if (COMMAND_CREATE.equals(command))
                {
                    handling.createDB();
                }
                else if (COMMAND_INIT.equals(command))
                {
                    handling.initDB();
                }
                else
                {
                    throw new BuildException("Unknown command "+command);
                }
            }
        }
        catch (Exception ex)
        {
            throw new BuildException(ex);
        }
    }

    /**
     * Creates a db handling object.
     * 
     * @return The db handling object
     * @throws BuildException If the handling is invalid
     */
    private DBHandling createDBHandling() throws BuildException
    {
        if ((_handling == null) || (_handling.length() == 0))
        {
            throw new BuildException("No handling specified");
        }
        try
        {
            String className     = "org.apache.ojb.broker.platforms."+
            					   Character.toTitleCase(_handling.charAt(0))+_handling.substring(1)+
            					   "DBHandling";
            Class  handlingClass = ClassHelper.getClass(className);

            return (DBHandling)handlingClass.newInstance();
        }
        catch (Exception ex)
        {
            throw new BuildException("Invalid handling '"+_handling+"' specified");
        }
    }

    /**
     * Adds the includes of the fileset to the handling.
     * 
     * @param handling The handling
     * @param fileSet  The fileset
     */
    private void addIncludes(DBHandling handling, FileSet fileSet) throws BuildException
    {
        DirectoryScanner scanner  = fileSet.getDirectoryScanner(getProject());
        String[]         files    = scanner.getIncludedFiles();
        StringBuffer     includes = new StringBuffer();

        for (int idx = 0; idx < files.length; idx++)
        {
            if (idx > 0)
            {
                includes.append(",");
            }
            includes.append(files[idx]);
        }
        try
        {
            handling.addDBDefinitionFiles(fileSet.getDir(getProject()).getAbsolutePath(), includes.toString());
        }
        catch (IOException ex)
        {
            throw new BuildException(ex);
        }
    }
}
