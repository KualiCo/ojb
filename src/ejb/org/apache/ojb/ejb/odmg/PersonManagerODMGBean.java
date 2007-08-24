package org.apache.ojb.ejb.odmg;

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

import javax.ejb.SessionBean;
import java.util.Collection;

import org.apache.ojb.broker.OJBRuntimeException;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.ejb.PersonVO;
import org.odmg.OQLQuery;

/**
 * Simple example bean for manage persons using the ODMG-api
 * by subclassing {@link org.apache.ojb.ejb.odmg.ODMGBaseBeanImpl}
 *
 * @ejb:bean
 * 			type="Stateless"
 * 			name="PersonManagerODMGBean"
 * 			jndi-name="org.apache.ojb.ejb.odmg.PersonManagerODMGBean"
 * 			local-jndi-name="org.apache.ojb.ejb.odmg.PersonManagerODMGBeanLocal"
 * 			view-type="both"
 * 			transaction-type="Container"
 *
 * @ejb:interface
 * 		remote-class="org.apache.ojb.ejb.odmg.PersonManagerODMGRemote"
 * 		local-class="org.apache.ojb.ejb.odmg.PersonManagerODMGLocal"
 * 		extends="javax.ejb.EJBObject"
 *
 * @ejb:home
 * 		remote-class="org.apache.ojb.ejb.odmg.PersonManagerODMGHome"
 * 		local-class="org.apache.ojb.ejb.odmg.PersonManagerODMGLocalHome"
 * 		extends="javax.ejb.EJBHome"
 *
 * @ejb:transaction
 * 		type="Required"
 *
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: PersonManagerODMGBean.java,v 1.1 2007-08-24 22:17:30 ewestfal Exp $
 */
public class PersonManagerODMGBean extends ODMGBaseBeanImpl implements SessionBean
{
    private Logger log = LoggerFactory.getLogger(PersonManagerODMGBean.class);

    public PersonManagerODMGBean()
    {
    }

    /**
     * @ejb:interface-method
     */
    public PersonVO storePerson(PersonVO person)
    {
        return (PersonVO) this.storeObject(person);
    }

    /**
     * @ejb:interface-method
     */
    public Collection storePersons(Collection persons)
    {
        return this.storeObjects(persons);
    }

    /**
     * @ejb:interface-method
     */
    public void deletePerson(PersonVO person)
    {
        this.deleteObject(person);
    }

    /**
     * @ejb:interface-method
     */
    public void deletePersons(Collection persons)
    {
        this.deleteObjects(persons);
    }

    /**
     * @ejb:interface-method
     */
    public int countPersons()
    {
        return this.getCount(PersonVO.class);
    }

    /**
     * @ejb:interface-method
     */
    public Collection getAllPersons()
    {
        return this.getAllObjects(PersonVO.class);
    }

    /**
     * @ejb:interface-method
     */
    public Collection getPersons(String firstname, String lastname)
    {
        OQLQuery query = getImplementation().newOQLQuery();
        try
        {
            StringBuffer buf = new StringBuffer("select allObjects from " + PersonVO.class.getName());
            buf.append(" where id not null");
            if (firstname != null) buf.append(" and firstname = " + firstname);
            if (lastname != null) buf.append(" and lastname = " + lastname);
            query.create(buf.toString());
            return (Collection) query.execute();
        }
        catch (Exception e)
        {
            log.error("OQLQuery failed", e);
            throw new OJBRuntimeException("OQLQuery failed", e);
        }
    }
}
