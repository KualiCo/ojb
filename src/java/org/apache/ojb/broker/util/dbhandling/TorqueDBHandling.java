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

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.ojb.broker.metadata.JdbcConnectionDescriptor;
import org.apache.ojb.broker.platforms.PlatformException;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.SQLExec;
import org.apache.tools.ant.types.FileSet;
import org.apache.torque.task.TorqueDataModelTask;
import org.apache.torque.task.TorqueSQLExec;
import org.apache.torque.task.TorqueSQLTask;

/**
 * Provides basic database handling (drop, create, init) via torque.
 * 
 * @author Thomas Dudziak
 */
public class TorqueDBHandling implements DBHandling
{
    /** Torque db platforms */
    protected static final String TORQUE_PLATFORM_DB2        = "db2";
    protected static final String TORQUE_PLATFORM_HYPERSONIC = "hypersonic";
    protected static final String TORQUE_PLATFORM_INTERBASE  = "interbase";
    protected static final String TORQUE_PLATFORM_MSSQL      = "mssql";
    protected static final String TORQUE_PLATFORM_MYSQL      = "mysql";
    protected static final String TORQUE_PLATFORM_ORACLE     = "oracle";
    protected static final String TORQUE_PLATFORM_POSTGRESQL = "postgresql";
    protected static final String TORQUE_PLATFORM_SAPDB      = "sapdb";
    protected static final String TORQUE_PLATFORM_SYBASE     = "sybase";

    /** The name of the db-creation script */
    private static final String CREATION_SCRIPT_NAME = "create-db.sql";
    /** The name of the torque database mapping file */
    private static final String SQL_DB_MAP_NAME      = "sqldb.map";

    /** Mapping from ojb dbms to torque database setting */
    private static HashMap _dbmsToTorqueDb = new HashMap();
    
    static
    {
        _dbmsToTorqueDb.put("db2",         TORQUE_PLATFORM_DB2);
        _dbmsToTorqueDb.put("hsqldb",      TORQUE_PLATFORM_HYPERSONIC);
        _dbmsToTorqueDb.put("firebird",    TORQUE_PLATFORM_INTERBASE);
        _dbmsToTorqueDb.put("mssqlserver", TORQUE_PLATFORM_MSSQL);
        _dbmsToTorqueDb.put("mysql",       TORQUE_PLATFORM_MYSQL);
        _dbmsToTorqueDb.put("oracle",      TORQUE_PLATFORM_ORACLE);
        _dbmsToTorqueDb.put("oracle9i",    TORQUE_PLATFORM_ORACLE);
        _dbmsToTorqueDb.put("postgresql",  TORQUE_PLATFORM_POSTGRESQL);
        _dbmsToTorqueDb.put("sapdb",       TORQUE_PLATFORM_SAPDB);
        _dbmsToTorqueDb.put("sybaseasa",   TORQUE_PLATFORM_SYBASE);
        _dbmsToTorqueDb.put("sybasease",   TORQUE_PLATFORM_SYBASE);
        _dbmsToTorqueDb.put("sybase",      TORQUE_PLATFORM_SYBASE);
    }
    
    /** The jdbc connection for communicating with the db */
    private JdbcConnectionDescriptor _jcd;
    /** The target database */
    private String _targetDatabase;
    /** The directory where we work in */
    private File _workDir;
    /** The compressed contents of the torque schemata */
    private HashMap _torqueSchemata = new HashMap();
    /** The compressed content of the creation script */
    private byte[] _creationScript;
    /** The compressed contents of the db initialization scripts */
    private HashMap _initScripts = new HashMap();

    /**
     * Creates a new handling object.
     */
    public TorqueDBHandling()
    {}

    /**
     * Sets the jdbc connection to use.
     * 
     * @param jcd The connection to use
     * @throws PlatformException If the target database cannot be handled with torque
     */
    public void setConnection(JdbcConnectionDescriptor jcd) throws PlatformException
    {
        _jcd = jcd;

        String targetDatabase = (String)_dbmsToTorqueDb.get(_jcd.getDbms().toLowerCase());

        if (targetDatabase == null)
        {
            throw new PlatformException("Database "+_jcd.getDbms()+" is not supported by torque");
        }
        if (!targetDatabase.equals(_targetDatabase))
        {
            _targetDatabase = targetDatabase;
            _creationScript = null;
            _initScripts.clear();
        }
    }

    /**
     * Returns the connection descriptor used by this handling object.
     * 
     * @return The connection descriptor
     */
    public JdbcConnectionDescriptor getConnection()
    {
        return _jcd;
    }

    /**
     * Returns the torque database platform used.
     * 
     * @return The target db platform
     */
    public String getTargetTorquePlatform()
    {
        return _targetDatabase;
    }

    /**
     * Adds the input files (in our case torque schema files) to use.
     * 
     * @param srcDir          The directory containing the files
     * @param listOfFilenames The filenames in a comma-separated list
     */
    public void addDBDefinitionFiles(String srcDir, String listOfFilenames) throws IOException
    {
        StringTokenizer tokenizer = new StringTokenizer(listOfFilenames, ",");
        File            dir       = new File(srcDir);
        String          filename;
        
        while (tokenizer.hasMoreTokens())
        {
            filename = tokenizer.nextToken().trim();
            if (filename.length() > 0)
            {    
                 _torqueSchemata.put("schema"+_torqueSchemata.size()+".xml",
                                     readTextCompressed(new File(dir, filename)));
            }
        }
    }

    /**
     * Adds an input stream of a db definition (in our case of a torque schema file).
     * 
     * @param schemaStream The input stream
     */
    public void addDBDefinitionFile(InputStream schemaStream) throws IOException
    {
        _torqueSchemata.put("schema"+_torqueSchemata.size()+".xml",
                            readStreamCompressed(schemaStream));
    }

    /**
     * Writes the torque schemata to files in the given directory and returns
     * a comma-separated list of the filenames.
     * 
     * @param dir The directory to write the files to
     * @return The list of filenames
     * @throws IOException If an error occurred
     */
    private String writeSchemata(File dir) throws IOException
    {
        writeCompressedTexts(dir, _torqueSchemata);

        StringBuffer includes = new StringBuffer();

        for (Iterator it = _torqueSchemata.keySet().iterator(); it.hasNext();)
        {
            includes.append((String)it.next());
            if (it.hasNext())
            {
                includes.append(",");
            }
        }
        return includes.toString();
    }
    
    /**
     * Creates the db-creation sql script (but does not perform it).
     * 
     * @throws PlatformException If some error occurred
     */
    public void createCreationScript() throws PlatformException
    {
        Project             project    = new Project();
        TorqueDataModelTask modelTask  = new TorqueDataModelTask();
        File                tmpDir     = null;
        File                scriptFile = null;
        
        _creationScript = null;
        try
        {
            tmpDir = new File(getWorkDir(), "schemas");
            tmpDir.mkdir();

            String includes = writeSchemata(tmpDir);
            
            scriptFile = new File(tmpDir, CREATION_SCRIPT_NAME);

            project.setBasedir(tmpDir.getAbsolutePath());

            // populating with defaults
            modelTask.setProject(project);
            modelTask.setUseClasspath(true);
            modelTask.setControlTemplate("sql/db-init/Control.vm");
            modelTask.setOutputDirectory(tmpDir);
            modelTask.setOutputFile(CREATION_SCRIPT_NAME);
            modelTask.setTargetDatabase(_targetDatabase);

            FileSet files = new FileSet();

            files.setDir(tmpDir);
            files.setIncludes(includes);
            modelTask.addFileset(files);
            modelTask.execute();

            _creationScript = readTextCompressed(scriptFile);

            deleteDir(tmpDir);
        }
        catch (Exception ex)
        {
            // clean-up
            if ((tmpDir != null) && tmpDir.exists())
            {
                deleteDir(tmpDir);
            }
            throw new PlatformException(ex);
        }
    }
    
    /**
     * Creates the database.
     * 
     * @throws PlatformException If some error occurred
     */
    public void createDB() throws PlatformException
    {
        if (_creationScript == null)
        {
            createCreationScript();
        }

        Project             project    = new Project();
        TorqueDataModelTask modelTask  = new TorqueDataModelTask();
        File                tmpDir     = null;
        File                scriptFile = null;
        
        try
        {
            tmpDir = new File(getWorkDir(), "schemas");
            tmpDir.mkdir();

            scriptFile = new File(tmpDir, CREATION_SCRIPT_NAME);

            writeCompressedText(scriptFile, _creationScript);

            project.setBasedir(tmpDir.getAbsolutePath());

            // we use the ant task 'sql' to perform the creation script
	        SQLExec         sqlTask = new SQLExec();
	        SQLExec.OnError onError = new SQLExec.OnError();
	
	        onError.setValue("continue");
	        sqlTask.setProject(project);
	        sqlTask.setAutocommit(true);
	        sqlTask.setDriver(_jcd.getDriver());
	        sqlTask.setOnerror(onError);
	        sqlTask.setUserid(_jcd.getUserName());
	        sqlTask.setPassword(_jcd.getPassWord() == null ? "" : _jcd.getPassWord());
	        sqlTask.setUrl(getDBCreationUrl());
	        sqlTask.setSrc(scriptFile);
	        sqlTask.execute();

	        deleteDir(tmpDir);
        }
        catch (Exception ex)
        {
            // clean-up
            if ((tmpDir != null) && tmpDir.exists())
            {
                try
                {
                    scriptFile.delete();
                }
                catch (NullPointerException e) 
                {
                    LoggerFactory.getLogger(this.getClass()).error("NPE While deleting scriptFile [" + scriptFile.getName() + "]", e);
                }
            }
            throw new PlatformException(ex);
        }
    }

    /**
     * Creates the initialization scripts (creation of tables etc.) but does
     * not perform them.
     * 
     * @throws PlatformException If some error occurred
     */
    public void createInitScripts() throws PlatformException
    {
        Project       project   = new Project();
        TorqueSQLTask sqlTask   = new TorqueSQLTask(); 
        File          schemaDir = null;
        File          sqlDir    = null;
        
        _initScripts.clear();
        try
        {
            File tmpDir = getWorkDir();

            schemaDir = new File(tmpDir, "schemas");
            sqlDir    = new File(tmpDir, "sql");
            schemaDir.mkdir();
            sqlDir.mkdir();

            String includes     = writeSchemata(schemaDir);
            File   sqlDbMapFile = new File(sqlDir, SQL_DB_MAP_NAME);

            sqlDbMapFile.createNewFile();
            project.setBasedir(sqlDir.getAbsolutePath());
            
            // populating with defaults
            sqlTask.setProject(project);
            sqlTask.setUseClasspath(true);
            sqlTask.setBasePathToDbProps("sql/base/");
            sqlTask.setControlTemplate("sql/base/Control.vm");
            sqlTask.setOutputDirectory(sqlDir);
            // we put the report in the parent directory as we don't want
            // to read it in later on
            sqlTask.setOutputFile("../report.sql.generation");
            sqlTask.setSqlDbMap(SQL_DB_MAP_NAME);
            sqlTask.setTargetDatabase(_targetDatabase);

            FileSet files = new FileSet();
            
            files.setDir(schemaDir);
            files.setIncludes(includes);
            sqlTask.addFileset(files);
            sqlTask.execute();

            readTextsCompressed(sqlDir, _initScripts);
            deleteDir(schemaDir);
            deleteDir(sqlDir);
        }
        catch (Exception ex)
        {
            // clean-up
            if ((schemaDir != null) && schemaDir.exists())
            {
                deleteDir(schemaDir);
            }
            if ((sqlDir != null) && sqlDir.exists())
            {
                deleteDir(sqlDir);
            }
            throw new PlatformException(ex);
        }
    }

    /**
     * Creates the tables according to the schema files.
     * 
     * @throws PlatformException If some error occurred
     */
    public void initDB() throws PlatformException
    {
        if (_initScripts.isEmpty())
        {
            createInitScripts();
        }

        Project       project   = new Project();
        TorqueSQLTask sqlTask   = new TorqueSQLTask(); 
        File          outputDir = null;
        
        try
        {
            outputDir = new File(getWorkDir(), "sql");

            outputDir.mkdir();
            writeCompressedTexts(outputDir, _initScripts);

            project.setBasedir(outputDir.getAbsolutePath());

            // executing the generated sql, but this time with a torque task 
            TorqueSQLExec         sqlExec = new TorqueSQLExec();
            TorqueSQLExec.OnError onError = new TorqueSQLExec.OnError();

            sqlExec.setProject(project);
            onError.setValue("continue");
            sqlExec.setAutocommit(true);
            sqlExec.setDriver(_jcd.getDriver());
            sqlExec.setOnerror(onError);
            sqlExec.setUserid(_jcd.getUserName());
            sqlExec.setPassword(_jcd.getPassWord() == null ? "" : _jcd.getPassWord());
            sqlExec.setUrl(getDBManipulationUrl());
            sqlExec.setSrcDir(outputDir.getAbsolutePath());
            sqlExec.setSqlDbMap(SQL_DB_MAP_NAME);
            sqlExec.execute();
            
            deleteDir(outputDir);
        }
        catch (Exception ex)
        {
            // clean-up
            if (outputDir != null)
            {
                deleteDir(outputDir);
            }
            throw new PlatformException(ex);
        }
    }

    /**
     * Template-and-Hook method for generating the url required by the jdbc driver
     * to allow for creating a database (as opposed to accessing an already-existing
     * database).
     *
     */
    protected String getDBCreationUrl()
    {
        JdbcConnectionDescriptor jcd = getConnection();

        // currently I only know about specifics for mysql
        if (TORQUE_PLATFORM_MYSQL.equals(getTargetTorquePlatform()))
        {
            // we have to remove the db name as the jdbc driver would try to connect to
            // a non-existing db
            // the db-alias has this form: [host&port]/[dbname]?[options]
            String dbAliasPrefix = jcd.getDbAlias();
            String dbAliasSuffix = "";
            int    questionPos   = dbAliasPrefix.indexOf('?');

            if (questionPos > 0)
            {
                dbAliasSuffix = dbAliasPrefix.substring(questionPos);
                dbAliasPrefix = dbAliasPrefix.substring(0, questionPos);
            }

            int slashPos = dbAliasPrefix.lastIndexOf('/');

            if (slashPos > 0)
            {
                // it is important that the slash at the end is present
                dbAliasPrefix = dbAliasPrefix.substring(0, slashPos + 1);
            }
            return jcd.getProtocol()+":"+jcd.getSubProtocol()+":"+dbAliasPrefix+dbAliasSuffix;
        }
        else if (TORQUE_PLATFORM_POSTGRESQL.equals(getTargetTorquePlatform()))
        {
            // we have to replace the db name with 'template1'
            // the db-alias has this form: [host&port]/[dbname]?[options]
            String dbAliasPrefix = jcd.getDbAlias();
            String dbAliasSuffix = "";
            int    questionPos   = dbAliasPrefix.indexOf('?');

            if (questionPos > 0)
            {
                dbAliasSuffix = dbAliasPrefix.substring(questionPos);
                dbAliasPrefix = dbAliasPrefix.substring(0, questionPos);
            }

            int slashPos = dbAliasPrefix.lastIndexOf('/');

            if (slashPos > 0)
            {
                // it is important that the slash at the end is present
                dbAliasPrefix = dbAliasPrefix.substring(0, slashPos + 1);
            }
            else
            {
                dbAliasPrefix += "/";
            }
            dbAliasPrefix += "template1";
            if (dbAliasSuffix.length() > 0)
            {
                dbAliasPrefix += "/";
            }
            return jcd.getProtocol()+":"+jcd.getSubProtocol()+":"+dbAliasPrefix+dbAliasSuffix;
            
        }
        else
        {
            return jcd.getProtocol()+":"+jcd.getSubProtocol()+":"+jcd.getDbAlias();
        }
    }

    /**
     * Template-and-Hook method for generating the url required by the jdbc driver
     * to allow for modifying an existing database.
     *
     */
    protected String getDBManipulationUrl()
    {
        JdbcConnectionDescriptor jcd = getConnection();

        return jcd.getProtocol()+":"+jcd.getSubProtocol()+":"+jcd.getDbAlias();
    }

    /**
     * Reads the given text file and compressed its content.
     * 
     * @param file The file
     * @return A byte array containing the GZIP-compressed content of the file
     * @throws IOException If an error ocurred
     */
    private byte[] readTextCompressed(File file) throws IOException
    {
        return readStreamCompressed(new FileInputStream(file));
    }

    /**
     * Reads the given text stream and compressed its content.
     * 
     * @param stream The input stream
     * @return A byte array containing the GZIP-compressed content of the stream
     * @throws IOException If an error ocurred
     */
    private byte[] readStreamCompressed(InputStream stream) throws IOException
    {
        ByteArrayOutputStream bao    = new ByteArrayOutputStream();
        GZIPOutputStream      gos    = new GZIPOutputStream(bao);
        OutputStreamWriter    output = new OutputStreamWriter(gos);
        BufferedReader        input  = new BufferedReader(new InputStreamReader(stream));
        String                line;

        while ((line = input.readLine()) != null)
        {
            output.write(line);
            output.write('\n');
        }
        input.close();
        stream.close();
        output.close();
        gos.close();
        bao.close();
        return bao.toByteArray();
    }

    /**
     * Reads the text files in the given directory and puts their content
     * in the given map after compressing it. Note that this method does not
     * traverse recursivly into sub-directories.
     * 
     * @param dir     The directory to process
     * @param results Map that will receive the contents (indexed by the relative filenames)
     * @throws IOException If an error ocurred
     */
    private void readTextsCompressed(File dir, HashMap results) throws IOException
    {
        if (dir.exists() && dir.isDirectory())
        {
            File[] files = dir.listFiles();

            for (int idx = 0; idx < files.length; idx++)
            {
                if (files[idx].isDirectory())
                {
                    continue;
                }
                results.put(files[idx].getName(), readTextCompressed(files[idx]));
            }
        }
    }

    /**
     * Uncompresses the given textual content and writes it to the given file.
     * 
     * @param file              The file to write to
     * @param compressedContent The content
     * @throws IOException If an error occurred
     */
    private void writeCompressedText(File file, byte[] compressedContent) throws IOException
    {
        ByteArrayInputStream bais   = new ByteArrayInputStream(compressedContent);
        GZIPInputStream      gis    = new GZIPInputStream(bais);
        BufferedReader       input  = new BufferedReader(new InputStreamReader(gis));
        BufferedWriter       output = new BufferedWriter(new FileWriter(file));
        String               line;

        while ((line = input.readLine()) != null)
        {
            output.write(line);
            output.write('\n');
        }
        input.close();
        gis.close();
        bais.close();
        output.close();
    }
    
    /**
     * Uncompresses the textual contents in the given map and and writes them to the files
     * denoted by the keys of the map.
     * 
     * @param dir      The base directory into which the files will be written 
     * @param contents The map containing the contents indexed by the filename
     * @throws IOException If an error occurred
     */
    private void writeCompressedTexts(File dir, HashMap contents) throws IOException
    {
        String filename;

        for (Iterator nameIt = contents.keySet().iterator(); nameIt.hasNext();)
        {
            filename = (String)nameIt.next();
            writeCompressedText(new File(dir, filename), (byte[])contents.get(filename));
        }
    }
    
    /**
     * Sets the working directory.
     * 
     * @param dir The directory
     * @throws IOException If the directory does not exist or cannot be written/read
     */
    public void setWorkDir(String dir) throws IOException
    {
        File workDir = new File(dir);

        if (!workDir.exists() || !workDir.canWrite() || !workDir.canRead())
        {
            throw new IOException("Cannot access directory "+dir);
        }
        _workDir = workDir;
    }

    /**
     * Returns the temporary directory used by java.
     * 
     * @return The temporary directory
     * @throws IOException If an io error occurred
     */
    private File getWorkDir() throws IOException
    {
        if (_workDir == null)
        {    
            File   dummy   = File.createTempFile("dummy", ".log");
            String workDir = dummy.getPath().substring(0, dummy.getPath().lastIndexOf(File.separatorChar));
    
            if ((workDir == null) || (workDir.length() == 0))
            {
                workDir = ".";
            }
            dummy.delete();
            _workDir = new File(workDir);
        }
        return _workDir;
    }

    /**
     * Little helper function that recursivly deletes a directory.
     * 
     * @param dir The directory
     */
    private void deleteDir(File dir)
    {
        if (dir.exists() && dir.isDirectory())
        {
            File[] files = dir.listFiles();

            for (int idx = 0; idx < files.length; idx++)
            {
                if (!files[idx].exists())
                {
                    continue;
                }
                if (files[idx].isDirectory())
                {
                    deleteDir(files[idx]);
                }
                else
                {
                    files[idx].delete();
                }
            }
            dir.delete();
        }
    }
}
