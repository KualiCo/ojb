package org.apache.ojb.broker.ant;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.beanutils.DynaBean;
import org.apache.ddlutils.model.Column;
import org.apache.ddlutils.model.Database;
import org.apache.ddlutils.model.Table;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import org.apache.ojb.broker.metadata.CollectionDescriptor;
import org.apache.ojb.broker.metadata.DescriptorRepository;
import org.apache.ojb.broker.metadata.FieldDescriptor;

/**
 * Provides a model derived from {@link org.apache.ojb.broker.metadata.DescriptorRepository} that
 * is preprocessed for data handling (inserting data, generating data dtd).
 * 
 * @author Thomas Dudziak
 */
public class PreparedModel
{
    /** The database model. */
    private Database _schema;
    /** Maps dtd elements to tables */
    private TreeMap _elementToTable                 = new TreeMap();
    /** Maps dtd elements to lists of class descriptors (which all map to the same table) */
    private HashMap _elementToClassDescriptors      = new HashMap();
    /** Maps dtd elements to colum maps which in turn map attribute names to columns */
    private HashMap _elementToColumnMap             = new HashMap();
    /** Maps dtd elements to maps that specify which attributes are required */
    private HashMap _elementToRequiredAttributesMap = new HashMap();

    public PreparedModel(DescriptorRepository model, Database schema)
    {
        _schema = schema;
        prepareModel(model);
    }

    public Iterator getElementNames()
    {
        return _elementToTable.keySet().iterator();
    }

    public Iterator getAttributeNames(String elementName)
    {
        Map columns = getColumnsFor(elementName);

        return columns == null ? null : columns.keySet().iterator();
    }

    public Map getRequiredAttributes(String elementName)
    {
        return (Map)_elementToRequiredAttributesMap.get(elementName);
    }

    public boolean isRequired(String elementName, String attributeName)
    {
        Map requiredAttributes = getRequiredAttributes(elementName);

        if (requiredAttributes == null)
        {
            return false;
        }
        else
        {
            Boolean status = (Boolean)requiredAttributes.get(attributeName);

            return status == null ? false : status.booleanValue();
        }
    }

    public Table getTableFor(String elementName)
    {
        return (Table)_elementToTable.get(elementName);
    }

    /**
     * Creates a dyna bean for the table associated to the given element.
     * 
     * @param elementName The element name
     * @return The dyna bean
     */
    public DynaBean createBeanFor(String elementName)
    {
        return _schema.createDynaBeanFor(getTableFor(elementName));
    }
    
    public List getClassDescriptorsMappingTo(String elementName)
    {
        return (List)_elementToClassDescriptors.get(elementName);
    }

    public Map getColumnsFor(String elementName)
    {
        return (Map)_elementToColumnMap.get(elementName);
    }

    public Column getColumnFor(String elementName, String attrName)
    {
        Map columns = getColumnsFor(elementName);

        if (columns == null)
        {
            return null;
        }
        else
        {
            return (Column)columns.get(attrName);
        }
    }

    /**
     * Prepares a representation of the model that is easier accessible for our purposes.
     * 
     * @param model  The original model
     * @return The model representation
     */
    private void prepareModel(DescriptorRepository model)
    {
        TreeMap result = new TreeMap();

        for (Iterator it = model.getDescriptorTable().values().iterator(); it.hasNext();)
        {
            ClassDescriptor classDesc = (ClassDescriptor)it.next();

            if (classDesc.getFullTableName() == null)
            {
                // not mapped to a database table
                continue;
            }

            String elementName        = getElementName(classDesc);
            Table  mappedTable        = getTableFor(elementName);
            Map    columnsMap         = getColumnsFor(elementName);
            Map    requiredAttributes = getRequiredAttributes(elementName);
            List   classDescs         = getClassDescriptorsMappingTo(elementName);

            if (mappedTable == null)
            {
                mappedTable = _schema.findTable(classDesc.getFullTableName());
                if (mappedTable == null)
                {
                    continue;
                }
                columnsMap         = new TreeMap();
                requiredAttributes = new HashMap();
                classDescs         = new ArrayList();
                _elementToTable.put(elementName, mappedTable);
                _elementToClassDescriptors.put(elementName, classDescs);
                _elementToColumnMap.put(elementName, columnsMap);
                _elementToRequiredAttributesMap.put(elementName, requiredAttributes);
            }
            classDescs.add(classDesc);
            extractAttributes(classDesc, mappedTable, columnsMap, requiredAttributes);
        }
        extractIndirectionTables(model, _schema);
    }

    private void extractAttributes(ClassDescriptor classDesc, Table mappedTable, Map columnsMap, Map requiredColumnsMap)
    {
        FieldDescriptor[] fieldDescs = classDesc.getFieldDescriptions();

        if (fieldDescs != null)
        {
            for (int idx = 0; idx < fieldDescs.length; idx++)
            {
                Column column = mappedTable.findColumn(fieldDescs[idx].getColumnName());

                if (column != null)
                {
                    // we'll check whether another field (of not necessarily the same name)
                    // already maps to this column; if this is the case, we're ignoring
                    // this field
                    boolean alreadyMapped = false;

                    for (Iterator mappedColumnsIt = columnsMap.values().iterator(); mappedColumnsIt.hasNext();)
                    {
                        if (column.equals(mappedColumnsIt.next()))
                        {
                            alreadyMapped = true;
                            break;
                        }
                    }
                    if (!alreadyMapped)
                    {
                        String shortAttrName = getShortAttributeName(fieldDescs[idx].getAttributeName());
        
                        columnsMap.put(shortAttrName, column);
                        requiredColumnsMap.put(shortAttrName,
                                               fieldDescs[idx].isPrimaryKey() ? Boolean.TRUE : Boolean.FALSE);
                    }
                }
            }
        }
    }

    /**
     * Extracts indirection tables from the given class descriptor, and adds elements
     * for them. In contrast to normal elements, for indirection tables the element name
     * matches the table name, and the attribute names match the column names.
     * 
     * @param model    The model
     * @param elements The elements
     */
    private void extractIndirectionTables(DescriptorRepository model, Database schema)
    {
        HashMap indirectionTables = new HashMap();

        // first we gather all participants for each m:n relationship
        for (Iterator classDescIt = model.getDescriptorTable().values().iterator(); classDescIt.hasNext();)
        {
            ClassDescriptor classDesc = (ClassDescriptor)classDescIt.next();

            for (Iterator collDescIt = classDesc.getCollectionDescriptors().iterator(); collDescIt.hasNext();)
            {
                CollectionDescriptor collDesc   = (CollectionDescriptor)collDescIt.next();
                String               indirTable = collDesc.getIndirectionTable();

                if ((indirTable != null) && (indirTable.length() > 0))
                {
                    Set columns = (Set)indirectionTables.get(indirTable);

                    if (columns == null)
                    {
                        columns = new HashSet();
                        indirectionTables.put(indirTable, columns);
                    }
                    columns.addAll(Arrays.asList(collDesc.getFksToThisClass()));
                    columns.addAll(Arrays.asList(collDesc.getFksToItemClass()));
                }
            }
        }
        if (indirectionTables.isEmpty())
        {
            // nothing to do
            return;
        }

        for (Iterator it = indirectionTables.keySet().iterator(); it.hasNext();)
        {
            String tableName   = (String)it.next();
            Set    columns     = (Set)indirectionTables.get(tableName);
            String elementName = tableName;

            for (Iterator classDescIt = model.getDescriptorTable().values().iterator(); classDescIt.hasNext();)
            {
                ClassDescriptor classDesc = (ClassDescriptor)classDescIt.next();

                if (tableName.equals(classDesc.getFullTableName()))
                {
                    elementName = getElementName(classDesc);

                    FieldDescriptor[] fieldDescs = classDesc.getFieldDescriptions();

                    if (fieldDescs != null)
                    {
                        for (int idx = 0; idx < fieldDescs.length; idx++)
                        {
                            columns.remove(fieldDescs[idx].getColumnName());
                        }
                    }
                }
            }

            Table mappedTable        = getTableFor(elementName);
            Map   columnsMap         = getColumnsFor(elementName);
            Map   requiredAttributes = getRequiredAttributes(elementName);
    
            if (mappedTable == null)
            {
                mappedTable = schema.findTable(elementName);
                if (mappedTable == null)
                {
                    continue;
                }
                columnsMap         = new TreeMap();
                requiredAttributes = new HashMap();
                _elementToTable.put(elementName, mappedTable);
                _elementToColumnMap.put(elementName, columnsMap);
                _elementToRequiredAttributesMap.put(elementName, requiredAttributes);
            }
            for (Iterator columnIt = columns.iterator(); columnIt.hasNext();)
            {
                String columnName = (String)columnIt.next();
                Column column     = mappedTable.findColumn(columnName);

                if (column != null)
                {
                    columnsMap.put(columnName, column);
                    requiredAttributes.put(columnName, Boolean.TRUE);
                }
            }
        }
    }

    /**
     * Returns the element name for the class descriptor which is the adjusted short (unqualified) class
     * name. Also takes care that the element name does not clash with another class of the same short
     * name that maps to a different table though.
     * 
     * @param classDesc The class descriptor
     * @return The element name
     */
    private String getElementName(ClassDescriptor classDesc)
    {
        String elementName = classDesc.getClassNameOfObject().replace('$', '_');

        elementName = elementName.substring(elementName.lastIndexOf('.') + 1);

        Table table  = getTableFor(elementName);
        int   suffix = 0;

        while ((table != null) && !table.getName().equals(classDesc.getFullTableName()))
        {
            ++suffix;
            table = getTableFor(elementName + "-" + suffix);
        }
        if (suffix > 0)
        {
            elementName += "-" + suffix;
        }

        return elementName;
    }

    /**
     * Adjusts the local attribute name (the part after the last '::' for nested fields).
     * 
     * @param attrName The original attribute name
     * @return The local attribute name
     */
    private String getShortAttributeName(String attrName)
    {
        return attrName.substring(attrName.lastIndexOf(':') + 1);
    }
}
