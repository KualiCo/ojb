package org.apache.ojb.broker.ant;

/* Copyright 2002-2005 The Apache Software Foundation
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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentFieldPrivilegedImpl;
import org.apache.tools.ant.AntClassLoader;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * An Ant task that will read the OJB repository xml file and confirm the
 * following:
 * 
 * a)Mapped classes exist. b)Mapped class fields exist c)Mapped database tables
 * exist. d)Mapped database columns exist. e)Mapped database columns jdbc type
 * matches the <code><field-descriptor/></code> "jdbc-type" attribute.
 * 
 * Obviously you should have built your classes, written your OJB
 * repository.xml file and built your database schema before running this task.
 * 
 * @author <a href="mailto:daren@softwarearena.com">Daren Drummond</a>
 * @version $Id: VerifyMappingsTask.java,v 1.1 2007-08-24 22:17:41 ewestfal Exp $
 */
public class VerifyMappingsTask extends Task
{
    private int m_errorCount = 0;
    private int m_warningCount = 0;
    private String m_repositoryPath = null;
    private String m_PropertiesFile = null;
    private String m_jdbcDriver = null;
    private String m_url = null;
    private String m_logon = null;
    private String m_password = null;
    private boolean m_ignoreCase = false;
    private boolean m_UseStrictTypeChecking = true;
    private Class m_persistenceClass = null;
    private boolean m_useXMLValidation = true;
    private boolean m_failOnError = true;

    private Path _classpath;
    
    public void setRepositoryFile(String path)
    {
        m_repositoryPath = path;
    }

    /**
     * Gets the path to the OJB repository file. (Includes the file name)
     * 
     * @return The path and file name of the OJB repository file
     */
    public String getRepositoryFile()
    {
        return m_repositoryPath;
    }

    /**
     * Sets the fully qualified path and file name of the OJB properties file.
     * 
     * @param path The path and file name of the OJB properties file
     */
    public void setPropertiesFile(String path)
    {
        m_PropertiesFile = path;
    }

    /**
     * Gets the value set by setPropertiesFile(String path)
     * 
     * @return The path and file name of the OJB properties file
     */
    public String getPropertiesFile()
    {
        return m_PropertiesFile;
    }

    /**
     * Sets the fully qualified class name of the jdbc driver to use.
     * 
     * @param jdbcClass Fully qualified class name of the jdbc driver
     */
    public void setJdbcDriver(String jdbcClass)
    {
        m_jdbcDriver = jdbcClass;
    }

    /**
     * Gets the value set by setJdbcDriver(String jdbcClass)
     * 
     * @return Fully qualified class name of the jdbc driver
     */
    public String getJdbcDriver()
    {
        return m_jdbcDriver;
    }

    /**
     * Sets the url connection string for the jdbc driver.
     * 
     * @param url The connection string for the jdbc driver
     */
    public void setUrl(String url)
    {
        m_url = url;
    }

    /**
     * Gets the value set by setUrl(String url)
     * 
     * @return The connection string for the jdbc driver
     */
    public String getUrl()
    {
        return m_url;
    }

    /**
     * Sets the database logon account that the utility should use.
     * 
     * @param logon The database logon account
     */
    public void setLogon(String logon)
    {
        m_logon = logon;
    }

    /**
     * Gets the value set by setLogon(String logon)
     * 
     * @return The database logon account
     */
    public String getLogon()
    {
        return m_logon;
    }

    /**
     * Sets the password for the database logon account.
     * 
     * @param password The password for the database logon account
     */
    public void setPassword(String password)
    {
        m_password = password;
    }

    /**
     * Gets the value set by setPassword(String password)
     * 
     * @return The password for the database logon account
     */
    public String getPassword()
    {
        return m_password;
    }

    /**
     * Turns on W3C xml validation of the OJB repository.xml file (on by default).
     * 
     * @param sValidationFlag Whether to validate the xml
     */
    public void setUseXMLValidation(String sValidationFlag)
    {
        m_useXMLValidation = Boolean.valueOf(sValidationFlag).booleanValue();
    }

    /**
     * Determines whether the xml syntax is verified.
     * 
     * @return A flag indicating if W3c xml validation will be used to
     *         verify the OJB repository.xml file
     */
    public boolean getUseXMLValidation()
    {
        return m_useXMLValidation;
    }

    /**
     * Sets a flag indicating that this Ant task should throw a BuildException
     * if it encounters any verification errors. In most cases, this will have
     * the effect of stopping the build process.
     * 
     * @param sFailFlag Whether to stop the task upon the first error
     */
    public void setFailOnError(String sFailFlag)
    {
        m_failOnError = Boolean.valueOf(sFailFlag).booleanValue();
    }

    /**
     * Determines whether this task stops by throwing a BuildException when the first
     * error is encountered.
     * 
     * @return A flag indicating that the Ant task will throw a
     *         BuildException if it encounters any validation errors
     */
    public boolean getFailOnError()
    {
        return m_failOnError;
    }

    /**
     * Sets the flag for ignoring the db column name case when looking for db
     * columns.
     * 
     * @param sIgnoreCaseFlag Whether the case of the db column name is ignored
     */
    public void setIgnoreFieldNameCase(String sIgnoreCaseFlag)
    {
        m_ignoreCase = Boolean.valueOf(sIgnoreCaseFlag).booleanValue();
    }

    /**
     * Determines whether the task ignores the case of the db column name case
     * when looking for db columns. 
     * 
     * @return Flag indicating if the field name case will be
     *         ignored when searching for table column names
     */
    public boolean getIgnoreFieldNameCase()
    {
        return m_ignoreCase;
    }

    /**
     * Sets the flag for stict type checking of database column types. If this
     * value is set to "true" then the task will log a warning if the table
     * column jdbc type doesn't match the type specified in the OJB repository
     * field descriptor.
     * 
     * @param sTypeCheckingFlag Whether to use strict type checking
     */
    public void setUseStrictTypeChecking(String sTypeCheckingFlag)
    {
        m_UseStrictTypeChecking = Boolean.valueOf(sTypeCheckingFlag)
        .booleanValue();
    }

    /**
     * Gets the boolean equivalent of the value set by
     * setUseStrictTypeChecking(String sTypeCheckingFlag)
     * 
     * @return Flag indicating if strict type checking will be
     *         used when searching for database table columns
     */
    public boolean getUseStrictTypeChecking()
    {
        return m_UseStrictTypeChecking;
    }

    /**
     * Set the classpath for loading the driver.
     *
     * @param classpath the classpath
     */
    public void setClasspath(Path classpath)
    {
        if (_classpath == null)
        {
            _classpath = classpath;
        }
        else
        {
            _classpath.append(classpath);
        }
        log("Verification classpath is "+ _classpath,
            Project.MSG_VERBOSE);
    }

    /**
     * Create the classpath for loading the driver.
     *
     * @return the classpath
     */
    public Path createClasspath()
    {
        if (_classpath == null)
        {
            _classpath = new Path(getProject());
        }
        return _classpath.createPath();
    }

    /**
     * Set the classpath for loading the driver using the classpath reference.
     *
     * @param r reference to the classpath
     */
    public void setClasspathRef(Reference r)
    {
        createClasspath().setRefid(r);
        log("Verification classpath is "+ _classpath,
            Project.MSG_VERBOSE);
    }
    
    public void execute() throws BuildException
    {
        if (getRepositoryFile() == null)
        {    
            throw new BuildException("Could not find the repository file.");
        }
        try
        {
            System.setProperty("OJB.properties", getPropertiesFile());
            //Thread.currentThread().setContextClassLoader(getClassLoader());
            logWarning("IgnoreFieldNameCase: " + String.valueOf(getIgnoreFieldNameCase()));
            logWarning("UseStrictTypeChecking: " + String.valueOf(getUseStrictTypeChecking()));
            logWarning("UseXMLValidation: " + String.valueOf(getUseXMLValidation()));
            logWarning("UseStrictTypeChecking: " + String.valueOf(getUseStrictTypeChecking()));
            verifyRepository(getRepositoryFile());
            logWarning(getSummaryString(getErrorCount(), getWarningCount()));
        }
        catch (Exception e)
        {
            logWarning("There was an exception while verifying the repsitory: " + e.getMessage());
            if (getFailOnError())
            {
                throw new BuildException("There was an exception while verifying the repsitory.", e);
            }
        }
        if (getFailOnError() && (getErrorCount() > 0))
        {
            throw new BuildException("Failed because 'failonerror' = true and there are " +
                                     String.valueOf(getErrorCount()) + " mapping error(s).");
        }
    }

    private String getSummaryString(int iBadCount, int iWarningCount)
    {
        return "\n---------------------------------------------------\n       Found " +
               String.valueOf(iBadCount)+
               " error(s) in the repository. \n       Found "+
               String.valueOf(iWarningCount)+
               " warning(s) in the repository.      \n---------------------------------------------------";
    }

    private void verifyRepository(String repositoryFile) throws ParserConfigurationException,
                                                                SAXException,
                                                                IOException
    {
        log("verifyRepository: Entered.");

        long      start  = System.currentTimeMillis();
        SAXParser p      = SAXParserFactory.newInstance().newSAXParser();
        XMLReader reader = p.getXMLReader();

        reader.setFeature("http://xml.org/sax/features/validation",
                          getUseXMLValidation());

        // create handler for verifying the repository structure
        ContentHandler handler = new RepositoryVerifierHandler(this);

        logInfo("Starting Parser...");
        reader.setContentHandler(handler);
        reader.parse(repositoryFile);
        logInfo("Done Parsing.");

        long stop = System.currentTimeMillis();

        setErrorCount(((RepositoryVerifierHandler)handler).getErrorCount());
        setWarningCount(((RepositoryVerifierHandler)handler).getWarningCount());
        logWarning("loading XML took " + (stop - start) + " msecs");
    }

    /**
     * Log a warning with the Ant out stream.
     * 
     * @param msg The message to log
     */
    public void logWarning(String msg)
    {
        log(msg, Project.MSG_WARN);
    }

    /**
     * Log an Info message with the Ant out stream. Info messages can be
     * suppressed from the command line by starting ant with the -quiet option.
     * 
     * @param msg The message to log
     */
    public void logInfo(String msg)
    {
        log(msg, Project.MSG_INFO);
    }

    protected int getErrorCount()
    {
        return m_errorCount;
    }

    protected void setErrorCount(int count)
    {
        m_errorCount = count;
    }

    protected int getWarningCount()
    {
        return m_warningCount;
    }

    protected void setWarningCount(int count)
    {
        m_warningCount = count;
    }

    /**
     * Tests to see if the jdbc connection information was specified in the tag
     * xml.
     * 
     * @return <code>true</code> if the jdbc connection information was
     *         supplied in the tag xml
     */
    public boolean hasConnectionInfo()
    {
        return (m_jdbcDriver != null && m_url != null && m_logon != null && m_password != null);
    }

    Class loadClass(String className) throws ClassNotFoundException
    {
        if (_classpath != null)
        {
            log("Loading " + className + " using AntClassLoader with classpath " + _classpath,
                Project.MSG_VERBOSE);

            AntClassLoader loader = new AntClassLoader(getProject(), _classpath);

            return loader.loadClass(className);
        }
        else
        {
            log("Loading " + className + " using system loader.",
                Project.MSG_VERBOSE);
            return Class.forName(className);
        }
    }
    
    /**
     * Returns the Class object of the class specified in the OJB.properties
     * file for the "PersistentFieldClass" property.
     * 
     * @return Class The Class object of the "PersistentFieldClass" class
     *         specified in the OJB.properties file.
     */
    public Class getPersistentFieldClass()
    {
        if (m_persistenceClass == null)
        {
            Properties properties = new Properties();
            try
            {
                this.logWarning("Loading properties file: " + getPropertiesFile());
                properties.load(new FileInputStream(getPropertiesFile()));
            }
            catch (IOException e)
            {
                this.logWarning("Could not load properties file '" + getPropertiesFile()
                        + "'. Using PersistentFieldDefaultImpl.");
                e.printStackTrace();
            }
            try
            {
                String className = properties.getProperty("PersistentFieldClass");

                m_persistenceClass = loadClass(className);
            }
            catch (ClassNotFoundException e)
            {
                e.printStackTrace();
                m_persistenceClass = PersistentFieldPrivilegedImpl.class;
            }
            logWarning("PersistentFieldClass: " + m_persistenceClass.toString());
        }
        return m_persistenceClass;
    }
}
