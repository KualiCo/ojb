package org.apache.ojb.broker.util.sequence;

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

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerException;
import org.apache.ojb.broker.accesslayer.StatementManagerIF;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.FieldDescriptor;
import org.apache.ojb.broker.query.Query;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;

/**
 * Helper class for SequenceManager implementations.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: SequenceManagerHelper.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class SequenceManagerHelper
{
    private static Logger log = LoggerFactory.getLogger(SequenceManagerHelper.class);

    /**
     * Property name used to configure sequence manager implementations.
     */
    public static final String PROP_SEQ_AS = "seq.as";
    /**
     * Property name used to configure sequence manager implementations.
     * @deprecated use {@link #PROP_SEQ_START} instead.
     */
    public static final String PROP_SEQ_START_OLD = "sequenceStart";
    /**
     * Property name used to configure sequence manager implementations.
     */
    public static final String PROP_SEQ_START = "seq.start";
    /**
     * Property name used to configure sequence manager implementations.
     */
    public static final String PROP_SEQ_INCREMENT_BY = "seq.incrementBy";
    /**
     * Property name used to configure sequence manager implementations.
     */
    public static final String PROP_SEQ_MAX_VALUE = "seq.maxValue";
    /**
     * Property name used to configure sequence manager implementations.
     */
    public static final String PROP_SEQ_MIN_VALUE = "seq.minValue";
    /**
     * Property name used to configure sequence manager implementations.
     */
    public static final String PROP_SEQ_CYCLE = "seq.cycle";
    /**
     * Property name used to configure sequence manager implementations.
     */
    public static final String PROP_SEQ_CACHE = "seq.cache";
    /**
     * Property name used to configure sequence manager implementations.
     */
    public static final String PROP_SEQ_ORDER = "seq.order";

    private static final String SEQ_PREFIX = "SEQ_";
    private static final String SEQ_UNASSIGNED = "UNASSIGNED";
    private static final String SM_SELECT_MAX = "SELECT MAX(";
    private static final String SM_FROM = ") FROM ";

    /**
     * Prefix for global sequence names.
     */

    /**
     * Returns a unique sequence name (unique across all extents).
     * <br/>
     * If we found a non null value for the 'sequence-name' attribute in
     * the field descriptor, we use the 'sequence-name' value as sequence name.
     * <br/>
     * Else if the top-level class of the target class has extents,
     * we take the first extent class table name of the extents as
     * sequence name.
     * <br/>
     * Else we take the table name of the target class.
     * <p>
     * If the method argument 'autoNaming' is true, the generated
     * sequence name will be set in the given field descriptor
     * using {@link org.apache.ojb.broker.metadata.FieldDescriptor#setSequenceName}
     * to speed up sequence name lookup in future calls.
     * </p>
     * @param brokerForClass current used PB instance
     * @param field target field
     * @param autoNaming if 'false' no auto sequence name was build and
     * a exception was throw if none could be found in field.
     */
    public static String buildSequenceName(PersistenceBroker brokerForClass,
                                           FieldDescriptor field, boolean autoNaming)
            throws SequenceManagerException
    {
        String seqName = field.getSequenceName();
        /*
        if we found a sequence name bound to the field descriptor
        via 'sequence-name' attribute we use that name
        */
        if (seqName != null && seqName.trim().length() != 0)
        {
            return seqName;
        }
        else if (!autoNaming)
        {
            /*
            arminw:
            we don't find a sequence name and we should not automatic build one,
            thus we throw an exception
            */
            throw new SequenceManagerException("Could not find sequence-name for field '" +
                    field + "' of class '" + field.getClassDescriptor().getClassNameOfObject() +
                    "', property 'autoNaming' in sequence-manager element in repository was '" +
                    autoNaming + "'. Set autoNaming true in sequence-descriptor or define a " +
                    " sequence-name in field-descriptor.");
        }

        ClassDescriptor cldTargetClass = field.getClassDescriptor();
        /*
        check for inheritance on multiple table
        */
        cldTargetClass = findInheritanceRoot(cldTargetClass);
        Class topLevel = brokerForClass.getTopLevelClass(cldTargetClass.getClassOfObject());
        ClassDescriptor cldTopLevel = brokerForClass.getClassDescriptor(topLevel);
        /**
         *
         * MBAIRD
         * Should not use classname for the sequenceName as we will end up
         * re-using sequence numbers for classes mapped to the same table.
         * Instead, make the FullTableName the discriminator since it will
         * always be unique for that table, and hence that class.
         *
         * arminw:
         * If the found top-level class has extents, we take the first
         * found extent class table name as sequence name. Else we take
         * the table name of the 'targetClass'.
         *
         */
        if (cldTopLevel.isExtent())
        {
            /*
            arminw:
            this is a little critical, because we do not know if the extent classes
            will change by and by and the first found extent class may change, thus the
            returned table name could change!
            But I don't know a way to resolve this problem. I put a comment to the
            sequence manager docs
            TODO: find better solution
            */
//            seqName = brokerForClass.getClassDescriptor(((Class) cldTopLevel.getExtentClasses().
//                    get(0))).getFullTableName();
            seqName = firstFoundTableName(brokerForClass, cldTopLevel);
        }
        else
        {
            seqName = cldTargetClass.getFullTableName();
        }
//        log.info("* targetClass: "+targetClass +", toplevel: "+topLevel+ " seqName: "+seqName);
        if (seqName == null)
        {
            seqName = SEQ_UNASSIGNED;
            log.warn("Too complex structure, can not assign automatic sequence name for field '" +
                    field.getAttributeName() + "' in class '" +
                    field.getClassDescriptor().getClassNameOfObject() +
                    "'. Use a default sequence name instead: " + (SEQ_PREFIX + seqName));
        }
//        System.out.println("* targetClass: " + cldTargetClass.getClassNameOfObject() + ", toplevel: " + topLevel + " seqName: " + seqName);
        seqName = SEQ_PREFIX + seqName;
        if (log.isDebugEnabled())
                log.debug("Set automatic generated sequence-name for field '" +
                        field.getAttributeName() + "' in class '" +
                        field.getClassDescriptor().getClassNameOfObject() +
                        "'.");
        field.setSequenceName(seqName);
        return seqName;
    }

    /**
     * Returns the root {@link org.apache.ojb.broker.metadata.ClassDescriptor} of the inheriatance
     * hierachy of the given descriptor or the descriptor itself if no inheriatance on multiple table is
     * used.
     */
    private static ClassDescriptor findInheritanceRoot(ClassDescriptor cld)
    {
        ClassDescriptor result = cld;
        if(cld.getSuperClassDescriptor() != null)
        {
            result = findInheritanceRoot(cld.getSuperClassDescriptor());
        }
        return result;
    }

    /**
     * try to find the first none null table name for the given class-descriptor.
     * If cld has extent classes, all of these cld's searched for the first none null
     * table name.
     */
    private static String firstFoundTableName(PersistenceBroker brokerForClass, ClassDescriptor cld)
    {
        String name = null;
        if (!cld.isInterface() && cld.getFullTableName() != null)
        {
            return cld.getFullTableName();
        }
        if (cld.isExtent())
        {
            Collection extentClasses = cld.getExtentClasses();
            for (Iterator iterator = extentClasses.iterator(); iterator.hasNext();)
            {
                name = firstFoundTableName(brokerForClass, brokerForClass.getClassDescriptor((Class) iterator.next()));
                // System.out.println("## " + cld.getClassNameOfObject()+" - name: "+name);
                if (name != null) break;
            }
        }
        return name;
    }

    /**
     * Lookup all tables associated with given class (search all extent classes)
     * to find the current maximum value for the given field.
     * <br><b>Note:</b> Only works for <code>long</code> autoincrement fields.
     * @param brokerForClass persistence broker instance match the database of the
     * given field/class
     * @param field the target field
     */
    public static long getMaxForExtent(PersistenceBroker brokerForClass, FieldDescriptor field) throws PersistenceBrokerException
    {
        if (field == null)
        {
            log.error("Given FieldDescriptor was null, could not detect max value across all extents");
            return 0;
            // throw new PersistenceBrokerException("Given FieldDescriptor was null");
        }
        // first lookup top-level class
        Class topLevel = brokerForClass.getTopLevelClass(field.getClassDescriptor().getClassOfObject());
        return getMaxId(brokerForClass, topLevel, field);
    }

    /**
     * Search down all extent classes and return max of all found
     * PK values.
     */
    public static long getMaxId(PersistenceBroker brokerForClass, Class topLevel, FieldDescriptor original) throws PersistenceBrokerException
    {
        long max = 0;
        long tmp;
        ClassDescriptor cld = brokerForClass.getClassDescriptor(topLevel);

        // if class is not an interface / not abstract we have to search its directly mapped table
        if (!cld.isInterface() && !cld.isAbstract())
        {
            tmp = getMaxIdForClass(brokerForClass, cld, original);
            if (tmp > max)
            {
                max = tmp;
            }
        }
        // if class is an extent we have to search through its subclasses
        if (cld.isExtent())
        {
            Vector extentClasses = cld.getExtentClasses();
            for (int i = 0; i < extentClasses.size(); i++)
            {
                Class extentClass = (Class) extentClasses.get(i);
                if (cld.getClassOfObject().equals(extentClass))
                {
                    throw new PersistenceBrokerException("Circular extent in " + extentClass +
                            ", please check the repository");
                }
                else
                {
                    // fix by Mark Rowell
                    // Call recursive
                    tmp = getMaxId(brokerForClass, extentClass, original);
                }
                if (tmp > max)
                {
                    max = tmp;
                }
            }
        }
        return max;
    }

    /**
     * lookup current maximum value for a single field in
     * table the given class descriptor was associated.
     */
    public static long getMaxIdForClass(
            PersistenceBroker brokerForClass, ClassDescriptor cldForOriginalOrExtent, FieldDescriptor original)
            throws PersistenceBrokerException
    {
        FieldDescriptor field = null;
        if (!original.getClassDescriptor().equals(cldForOriginalOrExtent))
        {
            // check if extent match not the same table
            if (!original.getClassDescriptor().getFullTableName().equals(
                    cldForOriginalOrExtent.getFullTableName()))
            {
                // we have to look for id's in extent class table
                field = cldForOriginalOrExtent.getFieldDescriptorByName(original.getAttributeName());
            }
        }
        else
        {
            field = original;
        }
        if (field == null)
        {
            // if null skip this call
            return 0;
        }

        String column = field.getColumnName();
        long result = 0;
        ResultSet rs = null;
        Statement stmt = null;
        StatementManagerIF sm = brokerForClass.serviceStatementManager();
        String table = cldForOriginalOrExtent.getFullTableName();
        // String column = cld.getFieldDescriptorByName(fieldName).getColumnName();
        String sql = SM_SELECT_MAX + column + SM_FROM + table;
        try
        {
            //lookup max id for the current class
            stmt = sm.getGenericStatement(cldForOriginalOrExtent, Query.NOT_SCROLLABLE);
            rs = stmt.executeQuery(sql);
            rs.next();
            result = rs.getLong(1);
        }
        catch (Exception e)
        {
            log.warn("Cannot lookup max value from table " + table + " for column " + column +
                    ", PB was " + brokerForClass + ", using jdbc-descriptor " +
                    brokerForClass.serviceConnectionManager().getConnectionDescriptor(), e);
        }
        finally
        {
            try
            {
                sm.closeResources(stmt, rs);
            }
            catch (Exception ignore)
            {
                // ignore it
           }
        }
        return result;
    }

    /**
     * Database sequence properties helper method.
     * Return sequence <em>start value</em> or <em>null</em>
     * if not set.
     *
     * @param prop The {@link java.util.Properties} instance to use.
     * @return The found expression or <em>null</em>.
     */
    public static Long getSeqStart(Properties prop)
    {
        String result = prop.getProperty(PROP_SEQ_START, null);
        if(result == null)
        {
            result = prop.getProperty(PROP_SEQ_START_OLD, null);
        }
        if(result != null)
        {
            return new Long(Long.parseLong(result));
        }
        else
        {
            return null;
        }
    }

    /**
     * Database sequence properties helper method.
     * Return sequence <em>increment by value</em> or <em>null</em>
     * if not set.
     *
     * @param prop The {@link java.util.Properties} instance to use.
     * @return The found expression or <em>null</em>.
     */
    public static Long getSeqIncrementBy(Properties prop)
    {
        String result = prop.getProperty(PROP_SEQ_INCREMENT_BY, null);
        if(result != null)
        {
            return new Long(Long.parseLong(result));
        }
        else
        {
            return null;
        }
    }

    /**
     * Database sequence properties helper method.
     * Return sequence <em>max value</em> or <em>null</em>
     * if not set.
     *
     * @param prop The {@link java.util.Properties} instance to use.
     * @return The found expression or <em>null</em>.
     */
    public static Long getSeqMaxValue(Properties prop)
    {
        String result = prop.getProperty(PROP_SEQ_MAX_VALUE, null);
        if(result != null)
        {
            return new Long(Long.parseLong(result));
        }
        else
        {
            return null;
        }
    }

    /**
     * Database sequence properties helper method.
     * Return sequence <em>min value</em> or <em>null</em>
     * if not set.
     *
     * @param prop The {@link java.util.Properties} instance to use.
     * @return The found expression or <em>null</em>.
     */
    public static Long getSeqMinValue(Properties prop)
    {
        String result = prop.getProperty(PROP_SEQ_MIN_VALUE, null);
        if(result != null)
        {
            return new Long(Long.parseLong(result));
        }
        else
        {
            return null;
        }
    }

    /**
     * Database sequence properties helper method.
     * Return sequence <em>cache value</em> or <em>null</em>
     * if not set.
     *
     * @param prop The {@link java.util.Properties} instance to use.
     * @return The found expression or <em>null</em>.
     */
    public static Long getSeqCacheValue(Properties prop)
    {
        String result = prop.getProperty(PROP_SEQ_CACHE, null);
        if(result != null)
        {
            return new Long(Long.parseLong(result));
        }
        else
        {
            return null;
        }
    }

    /**
     * Database sequence properties helper method.
     * Return sequence <em>cycle</em> Booelan or <em>null</em>
     * if not set.
     *
     * @param prop The {@link java.util.Properties} instance to use.
     * @return The found expression or <em>null</em>.
     */
    public static Boolean getSeqCycleValue(Properties prop)
    {
        String result = prop.getProperty(PROP_SEQ_CYCLE, null);
        if(result != null)
        {
            return Boolean.valueOf(result);
        }
        else
        {
            return null;
        }
    }

    /**
     * Database sequence properties helper method.
     * Return sequence <em>order</em> Booelan or <em>null</em>
     * if not set.
     *
     * @param prop The {@link java.util.Properties} instance to use.
     * @return The found expression or <em>null</em>.
     */
    public static Boolean getSeqOrderValue(Properties prop)
    {
        String result = prop.getProperty(PROP_SEQ_ORDER, null);
        if(result != null)
        {
            return Boolean.valueOf(result);
        }
        else
        {
            return null;
        }
    }

    /**
     * Database sequence properties helper method.
     * Return the datatype to set for the sequence or <em>null</em>
     * if not set.
     *
     * @param prop The {@link java.util.Properties} instance to use.
     * @return The found expression or <em>null</em>.
     */
    public static String getSeqAsValue(Properties prop)
    {
        return prop.getProperty(PROP_SEQ_AS, null);
    }
}
