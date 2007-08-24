package org.apache.ojb.broker.query;

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

import java.util.*;

/**
 * This Class contains utility functions for Criterias.
 *
 * @author <a href="mailto:on@ibis.odessa.ua">Oleg Nitz</a>
 * @version $Id: CriteriaUtils.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public class CriteriaUtils
{

    /**
     * Disjunctive Normal Form: list of Criteria, which don't contain ORs,
     * the elements of the list joined by ORs give the condition equivalent
     * to the original Criteria.
     */
    public static List getDNF(Criteria crit)
    {
        List dnf = new ArrayList();
        Enumeration e = crit.getElements();
        Criteria tmpCrit;

        while (e.hasMoreElements())
        {
            Object o = e.nextElement();
            if (o instanceof Criteria)
            {
                Criteria pc = (Criteria) o;
                switch (pc.getType())
                {
                    case (Criteria.OR):
                        {
                            dnf.addAll(getDNF(pc));
                            break;
                        }
                    case (Criteria.AND):
                        {
                            dnf = getDnfAndDnf(dnf, getDNF(pc));
                            break;
                        }
                }
            }
            else
            {
                SelectionCriteria c = (SelectionCriteria) o;
                tmpCrit = new Criteria();
                tmpCrit.getCriteria().add(c);
                if (dnf.isEmpty())
                {
                    dnf.add(tmpCrit);
                }
                else
                {
//#ifdef JDK13
					dnf = getDnfAndDnf(dnf, Collections.singletonList(tmpCrit));
//#else
/*
					Vector singletonList = new Vector(1);
					singletonList.add(tmpCrit);
					dnf = getDnfAndDnf(dnf,singletonList); 
*/

//#endif                	
                	
                    
                }
            }
        } // while

        return dnf;
    }

    /**
     * (a OR b) AND (c OR d) -> (a AND c) OR (a AND d) OR (b AND c) OR (b AND d)
     */
    private static List getDnfAndDnf(List dnf1, List dnf2)
    {
        ArrayList dnf = new ArrayList();

        for (Iterator it1 = dnf1.iterator(); it1.hasNext(); )
        {
            Criteria crit1 = (Criteria) it1.next();

            for (Iterator it2 = dnf2.iterator(); it2.hasNext(); )
            {
                Criteria crit2 = (Criteria) it2.next();
                Criteria crit = new Criteria();
                crit.getCriteria().addAll(crit1.getCriteria());
                crit.getCriteria().addAll(crit2.getCriteria());
                dnf.add(crit);
            }
        }

        return dnf;
    }
}

