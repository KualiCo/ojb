package org.apache.ojb.ejb.pb;

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

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.ejb.PersonVO;

/**
 * Simple example bean for manage persons using the PB-api
 * by subclassing {@link org.apache.ojb.ejb.pb.PBBaseBeanImpl}
 *
 * @ejb:bean
 * 			type="Stateless"
 * 			name="PersonManagerPBBean"
 * 			jndi-name="org.apache.ojb.ejb.pb.pb.PersonManagerPBBean"
 * 			local-jndi-name="org.apache.ojb.ejb.pb.PersonManagerPBBeanLocal"
 * 			view-type="both"
 * 			transaction-type="Container"
 *
 * @ejb:interface
 * 		remote-class="org.apache.ojb.ejb.pb.PersonManagerPBRemote"
 * 		local-class="org.apache.ojb.ejb.pb.PersonManagerPBLocal"
 * 		extends="javax.ejb.EJBObject"
 *
 * @ejb:home
 * 		remote-class="org.apache.ojb.ejb.pb.PersonManagerPBHome"
 * 		local-class="org.apache.ojb.ejb.pb.PersonManagerPBLocalHome"
 * 		extends="javax.ejb.EJBHome"
 *
 * @ejb:transaction
 * 		type="Required"
 *
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: PersonManagerPBBean.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public class PersonManagerPBBean extends PBBaseBeanImpl implements SessionBean
{
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
        PersistenceBroker broker = getBroker();
        Criteria criteria = new Criteria();
        if (firstname != null) criteria.addEqualTo("firstname", firstname);
        if (lastname != null) criteria.addEqualTo("firstname", lastname);
        Query q = new QueryByCriteria(PersonVO.class);
        Collection result = broker.getCollectionByQuery(q);
        broker.close();
        return result;
    }
}
