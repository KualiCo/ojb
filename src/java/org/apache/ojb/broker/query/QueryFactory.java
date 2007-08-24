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

import java.util.Collection;
import java.util.HashSet;
import java.util.Vector;

import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.metadata.MetadataManager;

/**
 * Insert the type's description here.
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @version $Id: QueryFactory.java,v 1.1 2007-08-24 22:17:36 ewestfal Exp $
 */
public final class QueryFactory
{
    private static DescriptorRepository getRepository()
    {
        return MetadataManager.getInstance().getRepository();
    }

    /**
     * create a new ReportQueryByCriteria
     * @param classToSearchFrom
     * @param criteria
     * @param distinct
     * @return ReportQueryByCriteria
     */
    public static ReportQueryByCriteria newReportQuery(Class classToSearchFrom, String[] columns, Criteria criteria, boolean distinct)
    {
        criteria = addCriteriaForOjbConcreteClasses(getRepository().getDescriptorFor(classToSearchFrom), criteria);
        return new ReportQueryByCriteria(classToSearchFrom, columns, criteria, distinct);
    }

    /**
     * create a new ReportQueryByCriteria
     * @param classToSearchFrom
     * @param criteria
     * @param distinct
     * @return ReportQueryByCriteria
     */
    public static ReportQueryByCriteria newReportQuery(Class classToSearchFrom, Criteria criteria, boolean distinct)
    {
        criteria = addCriteriaForOjbConcreteClasses(getRepository().getDescriptorFor(classToSearchFrom), criteria);
        return newReportQuery(classToSearchFrom, null, criteria, distinct);
    }

    /**
     * create a new ReportQueryByCriteria
     * @param classToSearchFrom
     * @param criteria
     * @return ReportQueryByCriteria
     */
    public static ReportQueryByCriteria newReportQuery(Class classToSearchFrom, Criteria criteria)
    {
        return newReportQuery(classToSearchFrom, criteria, false);
    }

    /**
     * Method declaration
     * @param classToSearchFrom
     * @param criteria
     * @param distinct
     * @return QueryByCriteria
     */
    public static QueryByCriteria newQuery(Class classToSearchFrom, Criteria criteria, boolean distinct)
    {
        criteria = addCriteriaForOjbConcreteClasses(getRepository().getDescriptorFor(classToSearchFrom), criteria);
        return new QueryByCriteria(classToSearchFrom, criteria, distinct);
    }

    /**
     * Method declaration
     * @param classToSearchFrom
     * @param criteria
     * @return QueryByCriteria
     */
    public static QueryByCriteria newQuery(Class classToSearchFrom, Criteria criteria)
    {
        return newQuery(classToSearchFrom, criteria, false);
    }

    /**
     * Return a QueryByIdentity for example_or_identity
     * @param example_or_identity
     * @return QueryByIdentity
     */
    public static QueryByIdentity newQuery(Object example_or_identity)
    {
        return newQueryByIdentity(example_or_identity);
    }

    /**
     * Return a QueryByIdentity for example_or_identity
     * @param example_or_identity
     * @return QueryByIdentity
     */
    public static QueryByIdentity newQueryByIdentity(Object example_or_identity)
    {
        return new QueryByIdentity(example_or_identity);
    }

    /**
     * Return a QueryByCriteria for example
     * <br>Use with care because building of Query is not foolproof !!!
     * @param example
     * @return QueryByCriteria
     */
    public static QueryByCriteria newQueryByExample(Object example)
    {
        return new QueryByCriteria(example);
    }

    /**
     * @param classToSearchFrom
     * @param indirectionTable
     * @param criteria
     * @param distinct
     * @return QueryByMtoNCriteria
     */
    public static QueryByMtoNCriteria newQuery(Class classToSearchFrom, String indirectionTable, Criteria criteria, boolean distinct)
    {
        criteria = addCriteriaForOjbConcreteClasses(getRepository().getDescriptorFor(classToSearchFrom), criteria);
        return new QueryByMtoNCriteria(classToSearchFrom, indirectionTable, criteria, distinct);
    }

    /**
     * @param classToSearchFrom
     * @param indirectionTable
     * @param criteria
     * @return QueryByCriteria
     */
    public static QueryByCriteria newQuery(Class classToSearchFrom, String indirectionTable, Criteria criteria)
    {
        criteria = addCriteriaForOjbConcreteClasses(getRepository().getDescriptorFor(classToSearchFrom), criteria);
        return new QueryByMtoNCriteria(classToSearchFrom, indirectionTable, criteria);
    }

    /**
     * Factory method for QueryBySQL
     * @param classToSearchFrom
     * @param anSqlStatement
     * @return QueryBySQL
     */
    public static QueryBySQL newQuery(Class classToSearchFrom, String anSqlStatement)
    {
        return new QueryBySQL(classToSearchFrom, anSqlStatement);
    }

    /**
     * Searches the class descriptor for the ojbConcrete class attribute
     * if it finds the concrete class attribute, append a where clause which
     * specifies we can load all classes that are this type or extents of this type.
     * @param cld
     * @return the extent classes
     */
    private static Collection getExtentClasses(ClassDescriptor cld)
    {
        /**
         * 1. check if this class has a ojbConcreteClass attribute
         */
        FieldDescriptor fd = cld.getFieldDescriptorByName(ClassDescriptor.OJB_CONCRETE_CLASS);
        Collection classes = new HashSet();  // use same class only once
        if (fd != null)
        {
            classes.add(cld.getClassOfObject().getName());
        }

        /**
         * 2. if this class has extents/is an extent search for all extents
         */
        if (cld.isExtent())
        {
            Vector extentClasses = cld.getExtentClasses();

            /**
             * 3. get all extents for this class
             */
            for (int i = 0; i < extentClasses.size(); i++)
            {
                Class ec = (Class) extentClasses.get(i);
                ClassDescriptor extCld = cld.getRepository().getDescriptorFor(ec);
                classes.addAll(getExtentClasses(extCld));
            }
        }

        return classes;
    }

    /**
     * Searches the class descriptor for the ojbConcrete class attribute
     * if it finds the concrete class attribute, append a where clause which
     * specifies we can load all classes that are this type or extents of this type.
     * @param cld
     * @param crit
     * @return the passed in Criteria object + optionally and'ed criteria with OR'd class
     * type discriminators.
     */
    private static Criteria addCriteriaForOjbConcreteClasses(ClassDescriptor cld, Criteria crit)
    {
        /**
         * 1. check if this class has a ojbConcreteClass attribute
         */
        Criteria concreteClassDiscriminator = null;
        Collection classes = getExtentClasses(cld);

        /**
         * 1. create a new Criteria for objConcreteClass
         */
        if (!classes.isEmpty())
        {
            concreteClassDiscriminator = new Criteria();
            if (classes.size() > 1)
            {
                concreteClassDiscriminator = new Criteria();
                concreteClassDiscriminator.addIn(ClassDescriptor.OJB_CONCRETE_CLASS, classes);
            }
            else
            {
                concreteClassDiscriminator.addEqualTo(ClassDescriptor.OJB_CONCRETE_CLASS, classes.toArray()[0]);
            }
        }

        /**
         * 2. only add the AND (objConcreteClass = "some.class" OR....) if we've actually found concrete
         * classes.
         */
        if (concreteClassDiscriminator != null)
        {
            /**
             * it's possible there is no criteria attached to the query, and in this
             * case we still have to add the IN/EqualTo criteria for the concrete class type
             * so check if the crit is null and then create a blank one if needed.
             */
            if (crit == null)
            {
                crit = new Criteria();
            }
 
            crit.addAndCriteria(concreteClassDiscriminator);
        }
        /**
         * will just return the passed in criteria if no OJB concrete class is attribute is found.
         */
        return crit;
    }

}
