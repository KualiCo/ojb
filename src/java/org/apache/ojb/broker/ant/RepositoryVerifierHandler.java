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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.locking.IsolationLevels;
import org.apache.ojb.broker.metadata.*;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A SAX parser handler used by the Ant VerifyMappingsTask to process an
 * OJB DescriptorRepository xml file.
 *
 * @author <a href="mailto:daren@softwarearena.com">Daren Drummond</a>
 * @version $Id: RepositoryVerifierHandler.java,v 1.1 2007-08-24 22:17:41 ewestfal Exp $
 */
public class RepositoryVerifierHandler
    extends DefaultHandler
    implements RepositoryElements, IsolationLevels
{
    private Logger logger;

    private DescriptorRepository m_repository;
    private JdbcConnectionDescriptor m_CurrentJCD;
    private ClassDescriptor m_CurrentCLD;
    private FieldDescriptor m_CurrentFLD;
    private ObjectReferenceDescriptor m_CurrentORD;
    private CollectionDescriptor m_CurrentCOD;
    private String m_CurrentString;
    private VerifyMappingsTask m_callingTask;
    private DBUtility m_dBUtility;

    //ClassDescriptor members;
    //private String m_CurrentClass = "";
    private Class m_currentClass = null;
    private String m_CurrentTable = null;
    private boolean m_currentTableExists = false;

    /** the default isolation level*/
    private int defIsoLevel = IL_DEFAULT;

    /**
     * All known xml tags are kept in this table.
     * The tags table allows lookup from literal to id
     * and from id to literal.
     */
    private RepositoryTags tags = RepositoryTags.getInstance();

    private Collection m_VerifyExceptions = new ArrayList(69);
    private Collection m_VerifyWarnings = new ArrayList(69);
    
    /**
     * Allows not to specify field id.
     */
    private int m_lastId = 0;


    /**
     * The only public constructor for RepositoryVerifierHandler.
     *
     * @param callingTask	A reference to the ant task using this parser.
     */
    public RepositoryVerifierHandler(VerifyMappingsTask callingTask)
    {
        m_callingTask = callingTask;
        m_callingTask.logWarning("Loaded RepositoryVerifierHandler.");
        logger = LoggerFactory.getLogger(this.getClass());
    }

    /**
     * returns the XmlCapable id associated with the literal.
     * OJB maintains a RepositoryTags table that provides
     * a mapping from xml-tags to XmlCapable ids.
     *
     * @param literal the literal to lookup
     * @return the int value representing the XmlCapable
     *
     * @throws MetadataException if no literal was found in tags mapping
     */
    private int getLiteralId(String literal) throws PersistenceBrokerException
    {
        ////logger.debug("lookup: " + literal);
        try
        {
            return tags.getIdByTag(literal);
        }
        catch (NullPointerException t)
        {
            throw new MetadataException("unknown literal: '" + literal + "'",t);
        }

    }

    /**
     * startDocument callback, nothing to do here.
     */
    public void startDocument()
    {
        //logger.debug("startDoc");
    }

    /**
     * endDocument callback, nothing to do here.
     */
    public void endDocument()
    {
        //logger.debug("endDoc");
    }


    public void startElement(String uri, String name, String qName, Attributes atts)
    {
        m_CurrentString = null;
        try
        {

            switch (getLiteralId(qName))
            {
                case MAPPING_REPOSITORY :
                    {
                        String defIso = atts.getValue(tags.getTagById(ISOLATION_LEVEL));
                        if (defIso != null)
                        {
                            defIsoLevel = getIsoLevel(defIso);
                        }

                        break;
                    }
                case JDBC_CONNECTION_DESCRIPTOR :
                    {
                        m_CurrentJCD = new JdbcConnectionDescriptor();

                        // set platform attribute
                        String platform = atts.getValue(tags.getTagById(DBMS_NAME));
                        //logger.debug("     " + tags.getTagById(Dbms_name) + ": " + platform);
                        m_CurrentJCD.setDbms(platform);

                        // set jdbc-level attribute
                        String level = atts.getValue(tags.getTagById(JDBC_LEVEL));
                        //logger.debug("     " + tags.getTagById(Jdbc_level) + ": " + level);
                        m_CurrentJCD.setJdbcLevel(level);

                        // set driver attribute
                        String driver = atts.getValue(tags.getTagById(DRIVER_NAME));
                        //logger.debug("     " + tags.getTagById(Driver_name) + ": " + driver);
                        m_CurrentJCD.setDriver(driver);

                        // set protocol attribute
                        String protocol = atts.getValue(tags.getTagById(URL_PROTOCOL));
                        //logger.debug("     " + tags.getTagById(Url_protocol) + ": " + protocol);
                        m_CurrentJCD.setProtocol(protocol);

                        // set subprotocol attribute
                        String subprotocol = atts.getValue(tags.getTagById(URL_SUBPROTOCOL));
                        //logger.debug("     " + tags.getTagById(Url_subprotocol) + ": " + subprotocol);
                        m_CurrentJCD.setSubProtocol(subprotocol);

                        // set the dbalias attribute
                        String dbalias = atts.getValue(tags.getTagById(URL_DBALIAS));
                        //logger.debug("     " + tags.getTagById(Url_dbalias) + ": " + dbalias);
                        m_CurrentJCD.setDbAlias(dbalias);

                        // set the datasource attribute
                        String datasource = atts.getValue(tags.getTagById(DATASOURCE_NAME));
                        //logger.debug("     " + tags.getTagById(Datasource_name) + ": " + datasource);
                        m_CurrentJCD.setDatasourceName(datasource);

                        // set the user attribute
                        String user = atts.getValue(tags.getTagById(USER_NAME));
                        //logger.debug("     " + tags.getTagById(User_name) + ": " + user);
                        m_CurrentJCD.setUserName(user);

                        // set the password attribute
                        String password = atts.getValue(tags.getTagById(USER_PASSWD));
                        //logger.debug("     " + tags.getTagById(User_passwd) + ": " + password);
                        m_CurrentJCD.setPassWord(password);

                        //connect to the database
                        m_dBUtility = getDBUtility(m_CurrentJCD);

                        break;
                    }
                case CLASS_DESCRIPTOR :
                    {
                        String isoLevel = atts.getValue(tags.getTagById(ISOLATION_LEVEL));

                        // set class attribute
                        String classname = atts.getValue(tags.getTagById(CLASS_NAME));

                        try
                        {
							m_currentClass = m_callingTask.loadClass(classname);
                        }
                        catch (ClassNotFoundException ex)
                        {
                            //logger.error(ex);
                            throw new MetadataException("Can't load class-descriptor class '" + classname + "'.");
                        }

                        m_CurrentTable = atts.getValue(tags.getTagById(TABLE_NAME));

                        if (m_CurrentTable != null)
                        {
                        	m_currentTableExists = m_dBUtility.exists(m_CurrentTable);
                            if(!m_currentTableExists)
                            {
                            	throw new MetadataException("The table '" + m_CurrentTable + "' does not exist in the database.");
                            }
                        }


                        break;
                    }

                case CLASS_EXTENT :
                    {

                        String classname = atts.getValue("class-ref");

                        try
                        {
							Class classExtent = m_callingTask.loadClass(classname);
                        }
                        catch (ClassNotFoundException ex)
                        {
                            //logger.error(ex);
                            throw new MetadataException("Can't load extent-class class '" + classname + "'.");
                        }
                        break;
                    }

                case FIELD_DESCRIPTOR :
                    {
			String strId = atts.getValue("id");
                        m_lastId  = (strId == null ? m_lastId + 1 : Integer.parseInt(strId));
                        m_CurrentFLD = new FieldDescriptor(null, m_lastId);

                        String fieldName = atts.getValue(tags.getTagById(FIELD_NAME));

                        if (m_currentClass != null)
                        {
                        	//this.m_callingTask.logWarning("Verifying " + fieldName + " in class " + m_currentClass.toString());
							confirmFieldExists(m_currentClass, fieldName);

	                        String columnName = atts.getValue(tags.getTagById(COLUMN_NAME));
	                        m_CurrentFLD.setColumnName(columnName);

	                        String jdbcType = atts.getValue(tags.getTagById(JDBC_TYPE));
	                        m_CurrentFLD.setColumnType(jdbcType);

	                        //check that the field exists in the database
	                        if(m_currentTableExists)
	                        {
	                        	if(this.m_callingTask.getUseStrictTypeChecking())
	                        	{
									m_dBUtility.exists(m_CurrentTable, columnName, jdbcType, m_callingTask.getIgnoreFieldNameCase());
	                        	}
	                        	else
	                        	{
									m_dBUtility.existsUseWarnings(m_CurrentTable, columnName, jdbcType, m_callingTask.getIgnoreFieldNameCase());
	                        	}
	                        }

                        }

                        break;
                    }
                case REFERENCE_DESCRIPTOR :
                    {
                        if (m_currentClass != null)
                        {
                        	//Confirm that this field exists in the class
                        	name = atts.getValue(tags.getTagById(FIELD_NAME));
	                        confirmFieldExists(m_currentClass, name);

                        	String classRef = atts.getValue(tags.getTagById(REFERENCED_CLASS));
                        	try
                        	{
                        		//Confirm that class 'class-ref' can be loaded.
                        		Class refClass = m_callingTask.loadClass(classRef);
                            }
	                        catch (ClassNotFoundException ex)
	                        {
	                            //logger.error(ex);
	                            throw new MetadataException("Can't find class-ref '" + classRef + "' in reference-descriptor '" + name + "'.");
	                        }

                        }

                        break;
                    }

                case COLLECTION_DESCRIPTOR :
                    {

                        if (m_currentClass != null)
                        {
                        	//Confirm that this field exists in the class
                        	name = atts.getValue(tags.getTagById(FIELD_NAME));
							confirmFieldExists(m_currentClass, name);

	                        String collectionClass = atts.getValue(tags.getTagById(COLLECTION_CLASS));
	                        //logger.debug("     " + tags.getTagById(Collection_class) + ": " + collectionClass);
	                        if (collectionClass != null)
	                        {
	                        	try
	                        	{
	                        		//Confirm that class 'class-ref' can be loaded.
	                        		Class oCollectionClass = m_callingTask.loadClass(collectionClass);
	                            }
		                        catch (ClassNotFoundException ex)
		                        {
		                            //logger.error(ex);
		                            throw new MetadataException("Can't find collection-class '" + collectionClass + "' in collection-descriptor '" + name + "'.");
		                        }
	                        }
	                        // set element-class-ref attribute
	                        String elementClassRef = atts.getValue(tags.getTagById(ITEMS_CLASS));
	                        //logger.debug("     " + tags.getTagById(Items_class) + ": " + elementClassRef);
	                        if (elementClassRef != null)
	                        {
	                        	try
	                        	{
	                        		//Confirm that class 'class-ref' can be loaded.
	                        		Class oElementClassRef = m_callingTask.loadClass(elementClassRef);
	                            }
		                        catch (ClassNotFoundException ex)
		                        {
		                            //logger.error(ex);
		                            throw new MetadataException("Can't find element-class-ref '" + elementClassRef + "' in collection-descriptor '" + name + "'.");
		                        }
		                     }
                        }


                        break;
                    }

                default :
                    {
                        // nop
                    }
            }
        }
        catch(MetadataException mde)
        {
        	this.m_callingTask.logWarning(" --> Mapping Error: " + mde.getMessage());
        	m_VerifyExceptions.add(mde);
        }
        catch(NullPointerException garbage)
        {
        	//eat it.
        }
        catch(SQLWarning sqlw)
        {
        	this.m_callingTask.logInfo(" --> DB Mapping Warning: " + sqlw.getMessage());
        	m_VerifyWarnings.add(sqlw);
        }
        catch(SQLException sqle)
        {
        	this.m_callingTask.logWarning(" --> DB Mapping Error: " + sqle.getMessage());
        	m_VerifyExceptions.add(sqle);
        }
        catch (Exception ex)
        {
            logger.error(ex);
            throw new PersistenceBrokerException(ex);
        }
    }


    public void endElement(String uri, String name, String qName)
    {
        try
        {
            switch (getLiteralId(qName))
            {
                case MAPPING_REPOSITORY :
                    {
                        //release the any db connections
                        if(m_dBUtility != null) m_dBUtility.release();
                        break;
                    }
                case JDBC_CONNECTION_DESCRIPTOR :
                    {
                        //logger.debug("   < " + tags.getTagById(JdbcConnectionDescriptor));
                        break;
                    }
                case CLASS_DESCRIPTOR :
                    {
                        //logger.debug("  < " + tags.getTagById(ClassDescriptor));
                        m_currentClass = null;
						m_CurrentTable = null;
						m_currentTableExists = false;
                        m_CurrentCLD = null;
                        break;
                    }
                case CLASS_EXTENT :
                    {
                        break;
                    }

                case FIELD_DESCRIPTOR :
                    {
                        //logger.debug("    < " + tags.getTagById(FIELDDESCRIPTOR));
                        m_CurrentFLD = null;
                        break;
                    }
                case REFERENCE_DESCRIPTOR :
                    {
                        //logger.debug("    < " + tags.getTagById(ReferenceDescriptor));
                        m_CurrentORD = null;
                        break;
                    }
                case FOREIGN_KEY :
                    {
                        //logger.debug("    < " + tags.getTagById(FOREIGN_KEY));
                    }

                case COLLECTION_DESCRIPTOR :
                    {
                        //logger.debug("    < " + tags.getTagById(CollectionDescriptor));
                        m_CurrentCOD = null;
                        break;
                    }

                case INVERSE_FK :
                    {
                        //logger.debug("    < " + tags.getTagById(Inverse_fk));
                        break;
                    }

                case FK_POINTING_TO_THIS_CLASS :
                    {
                        //logger.debug("    < " + tags.getTagById(Fk_pointing_to_this_class));
                        break;
                    }
                case FK_POINTING_TO_ITEMS_CLASS :
                    {
                        //logger.debug("    < " + tags.getTagById(Fk_pointing_to_items_class));
                        break;
                    }

                    // handle failure:
                default :
                    {
                        //logger.error("Ignoring unknown Element " + qName);
                    }
            }
        }
        catch (Exception ex)
        {
            //logger.error(ex);
            throw new PersistenceBrokerException(ex);
        }
    }

    public void characters(char ch[], int start, int length)
    {
        if (m_CurrentString == null)
            m_CurrentString = new String(ch, start, length);
        else
            m_CurrentString += new String(ch, start, length);
    }

    public void error(SAXParseException e) throws SAXException
    {
        //logger.error(e);
        //release the any db connections
        try
        {
        	if(m_dBUtility != null) m_dBUtility.release();
        }
        catch(Exception ex)
        {
        	ex.printStackTrace();
        }
        throw e;
    }

    public void fatalError(SAXParseException e) throws SAXException
    {
        //logger.fatal(e);
		//release the any db connections
        try
        {
        	if(m_dBUtility != null) m_dBUtility.release();
        }
        catch(Exception ex)
        {
        	ex.printStackTrace();
        }
        throw e;
    }

    public void warning(SAXParseException e) throws SAXException
    {
        //logger.warn(e);
        throw e;
    }

    /**
     * maps IsolationLevel literals to the corresponding id
     * @param isoLevel
     * @return the id
     */
    private int getIsoLevel(String isoLevel)
    {
        if (isoLevel.equalsIgnoreCase(LITERAL_IL_READ_UNCOMMITTED))
        {
            return IL_READ_UNCOMMITTED;
        }
        else if (isoLevel.equalsIgnoreCase(LITERAL_IL_READ_COMMITTED))
        {
            return IL_READ_COMMITTED;
        }
        else if (isoLevel.equalsIgnoreCase(LITERAL_IL_REPEATABLE_READ))
        {
            return IL_REPEATABLE_READ;
        }
        else if (isoLevel.equalsIgnoreCase(LITERAL_IL_SERIALIZABLE))
        {
            return IL_SERIALIZABLE;
        }
        else if (isoLevel.equalsIgnoreCase(LITERAL_IL_OPTIMISTIC))
        {
            return IL_OPTIMISTIC;
        }
        //logger.warn("unknown isolation-level: " + isoLevel + " using RW_UNCOMMITTED as default");
        return defIsoLevel;
    }


    public int getErrorCount()
    {
    	return m_VerifyExceptions.size();
    }

    public int getWarningCount()
    {
    	return m_VerifyWarnings.size();
    }


	private DBUtility getDBUtility(JdbcConnectionDescriptor jcd)
	throws MetadataException, MalformedURLException, ClassNotFoundException
	{
		DBUtility retval = null;
		String driver;
		String userName;
		String password;
		String url;
		//if the Tag provided the connection info use that, else
		//try to connect with the info in the connectionDescriptor
		if (m_callingTask.hasConnectionInfo())
		{
			m_callingTask.logWarning("Using DB conection info from Ant task.");
			driver = m_callingTask.getJdbcDriver();
			userName = m_callingTask.getLogon();
			password = m_callingTask.getPassword();
			url = m_callingTask.getUrl();
		}
		else
		{
			m_callingTask.logWarning("Using DB conection info from ojb repository connection descriptor.");
			driver = jcd.getDriver();
			userName = jcd.getUserName();
			password = jcd.getPassWord();
			url = jcd.getProtocol() + ":" + jcd.getSubProtocol() + ":" + jcd.getDbAlias();
		}

		try
		{
		    Class jdbcDriver = m_callingTask.loadClass(driver);

		    // not every jdbc driver registers itself with the driver manager
		    // so we do it explicitly
		    DriverManager.registerDriver((Driver)jdbcDriver.newInstance());
		    retval = new DBUtility(url, userName, password);
		}
		catch(ClassNotFoundException cnfe)
		{
			throw cnfe;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new MetadataException("Could not connect to database with url (" +
			url + "), driver (" + driver + "), logon (" + userName +
			"), password (" + password + ").", e);
		}

		return retval;
	}

	private Constructor m_persistConstructor = null;
	private Constructor getPersistenceClassConstructor() throws NoSuchMethodException
	{
		if(m_persistConstructor == null)
		{
			//load the persistent class specified in the OJB.properties file
			Class persistentClass = m_callingTask.getPersistentFieldClass();
			Class[] aConTypes = new Class[2];
			aConTypes[0] = Class.class;
			aConTypes[1] = String.class;
			m_persistConstructor = persistentClass.getConstructor(aConTypes);
		}
		return m_persistConstructor;
	}

	protected void confirmFieldExists(Class classToCheck, String fieldName)
	throws MetadataException, NoSuchMethodException,
			InstantiationException, IllegalAccessException
	{
    	Object[] aConParams = new Object[2];
    	aConParams[0] = classToCheck;
    	aConParams[1] = fieldName;

    	try
    	{
    		getPersistenceClassConstructor().newInstance(aConParams);
    	}
    	catch(InvocationTargetException ite)
    	{
    		throw new MetadataException(ite.getTargetException().getMessage());
    	}
	}

}
