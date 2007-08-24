package org.apache.ojb.broker.accesslayer;

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
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.NoSuchElementException;

import org.apache.ojb.broker.Identity;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.PersistenceBrokerSQLException;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.util.ConstructorHelper;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * this class can be used to create enumerations of PrimaryKey objects.
 * This is interesting for EJB finder methods
 * in BMP entity beans which must return such enumerations.
 * @author Thomas Mahler
 * @version $Id: PkEnumeration.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class PkEnumeration implements Enumeration
{
	static final long serialVersionUID = -834955711995869884L;
    protected boolean hasCalledCheck = false;
    protected boolean hasNext = false;

    protected PersistenceBroker broker;

    /**
     * The underlying jdbc resultset produced by select statement
     */
    protected ResultSetAndStatement resultSetAndStatment;

    /**
     * descriptor for the class of which items are to be found
     */
    protected ClassDescriptor classDescriptor;

    /**
     * the Constructor that is needed to build the PrimaryKey Objects
     */
    protected Constructor constructor;

    /**
     * PkEnumeration constructor.
     * @param query the SELECT statement gerating the underlying resultset
     * @param cld classDescriptor of the target entity class (say Article)
     * @param primaryKeyClass the entity classes PrimaryKey class (say ArticleKey).
     * this key-class MUST have a constructor with one argument of type org.apache.ojb.broker.Identity !
     */
    public PkEnumeration(Query query, ClassDescriptor cld, Class primaryKeyClass, PersistenceBroker broker)
    {
        this.resultSetAndStatment = broker.serviceJdbcAccess().executeQuery(query, cld);
        this.classDescriptor = cld;
        this.broker = broker;
        // get a contructor object that can be used to build instances of class primaryKeyClass
        try
        {
            Class[] argArray = {Identity.class};
            this.constructor = primaryKeyClass.getConstructor(argArray);
        }
        catch (NoSuchMethodException e)
        {
            LoggerFactory.getDefaultLogger().error(primaryKeyClass.getName()
                    + " must implement a Constructor with one argument of type org.apache.ojb.broker.Identity");
            throw new PersistenceBrokerException(e);
        }
        catch (SecurityException e)
        {
            LoggerFactory.getDefaultLogger().error(e);
            throw new PersistenceBrokerException(e);
        }
    }

    /**
     * returns an Identity object representing the current resultset row
     */
    private Identity getIdentityFromResultSet()
    {

        try
        {
            // 1. get an empty instance of the target class
            Constructor con = classDescriptor.getZeroArgumentConstructor();
            Object obj = ConstructorHelper.instantiate(con);

            // 2. fill only primary key values from Resultset
            Object colValue;
            FieldDescriptor fld;
            FieldDescriptor[] pkfields = classDescriptor.getPkFields();
            for (int i = 0; i < pkfields.length; i++)
            {
                fld = pkfields[i];
                colValue = fld.getJdbcType().getObjectFromColumn(resultSetAndStatment.m_rs, fld.getColumnName());
                fld.getPersistentField().set(obj, colValue);
            }
            // 3. return the representing identity object
            return broker.serviceIdentity().buildIdentity(classDescriptor, obj);
        }
        catch (SQLException e)
        {
            throw new PersistenceBrokerSQLException("Error reading object from column", e);
        }
        catch (Exception e)
        {
            throw new PersistenceBrokerException("Error reading Identity from result set", e);
        }
    }

    /**
     * Tests if this enumeration contains more elements.
     * @return  <code>true</code> if and only if this enumeration object
     * contains at least one more element to provide;
     * <code>false</code> otherwise.
     */
    public boolean hasMoreElements()
    {
        try
        {
            if (!hasCalledCheck)
            {
                hasCalledCheck = true;
                hasNext = resultSetAndStatment.m_rs.next();
            }
        }
        catch (SQLException e)
        {
            LoggerFactory.getDefaultLogger().error(e);
            //releaseDbResources();
            hasNext = false;
        }
        finally
        {
            if(!hasNext)
            {
                releaseDbResources();
            }
        }
        return hasNext;
    }

    private void releaseDbResources()
    {
        resultSetAndStatment.close();
        resultSetAndStatment = null;
    }

    /**
     * Returns the next element of this enumeration if this enumeration
     * object has at least one more element to provide.
     * @return     the next element of this enumeration.
     * @exception  NoSuchElementException  if no more elements exist.
     */
    public Object nextElement()
    {
        try
        {
            if (!hasCalledCheck)
            {    
                hasMoreElements();
            }    
            hasCalledCheck = false;
            if (hasNext)
            {
                Identity oid = getIdentityFromResultSet();
                Identity[] args = {oid};
                return this.constructor.newInstance(args);
            }
            else
                throw new NoSuchElementException();
        }
        catch (Exception ex)
        {
            LoggerFactory.getDefaultLogger().error(ex);
            throw new NoSuchElementException();
        }
    }

	/**
	 * protection just in case someone leaks.
	 */
	protected void finalize()
	{
		if(resultSetAndStatment != null)
        {
            LoggerFactory.getDefaultLogger().error("["+PkEnumeration.class.getName()+"] Found unclosed resources while finalize");
            releaseDbResources();
        }
	}
}
