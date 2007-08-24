package xdoclet.modules.ojb.model;

import java.util.HashMap;

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

/**
 * Helper class for dealing with properties of the descriptor defs.
 */
public abstract class PropertyHelper
{
    // ojb repository properties
    public static final String OJB_PROPERTY_ACCEPT_LOCKS                    = "accept-locks";
    public static final String OJB_PROPERTY_ACCESS                          = "access";
    public static final String OJB_PROPERTY_ARGUMENTS                       = "arguments";
    public static final String OJB_PROPERTY_ARRAY_ELEMENT_CLASS_REF         = "array-element-class-ref";
    public static final String OJB_PROPERTY_ATTRIBUTES                      = "attributes";
    public static final String OJB_PROPERTY_AUTOINCREMENT                   = "autoincrement";
    public static final String OJB_PROPERTY_AUTO_DELETE                     = "auto-delete";
    public static final String OJB_PROPERTY_AUTO_RETRIEVE                   = "auto-retrieve";
    public static final String OJB_PROPERTY_AUTO_UPDATE                     = "auto-update";
    public static final String OJB_PROPERTY_CLASS                           = "class";
    public static final String OJB_PROPERTY_CLASS_REF                       = "class-ref";
    public static final String OJB_PROPERTY_COLLECTION_CLASS                = "collection-class";
    public static final String OJB_PROPERTY_COLUMN                          = "column";
    public static final String OJB_PROPERTY_COLUMN_DOCUMENTATION            = "column-documentation";
    public static final String OJB_PROPERTY_CONVERSION                      = "conversion";
    public static final String OJB_PROPERTY_DATABASE_FOREIGNKEY             = "database-foreignkey";
    public static final String OJB_PROPERTY_DEFAULT_CLASS_REF               = "default-class-ref";
    public static final String OJB_PROPERTY_DEFAULT_CONVERSION              = "default-conversion";
    public static final String OJB_PROPERTY_DEFAULT_FETCH                   = "default-fetch";
    public static final String OJB_PROPERTY_DEFAULT_JDBC_TYPE               = "default-jdbc-type";
    public static final String OJB_PROPERTY_DEFAULT_PRECISION               = "default-precision";
    public static final String OJB_PROPERTY_DEFAULT_SCALE                   = "default-scale";
    public static final String OJB_PROPERTY_DETERMINE_EXTENTS               = "determine-extents";
    public static final String OJB_PROPERTY_DOCUMENTATION                   = "documentation";
    public static final String OJB_PROPERTY_ELEMENT_CLASS_REF               = "element-class-ref";
    public static final String OJB_PROPERTY_FACTORY_CLASS                   = "factory-class";
    public static final String OJB_PROPERTY_FACTORY_METHOD                  = "factory-method";
    public static final String OJB_PROPERTY_FIELD_REF                       = "field-ref";
    public static final String OJB_PROPERTY_FIELDS                          = "fields";
    public static final String OJB_PROPERTY_FOREIGNKEY                      = "foreignkey";
    public static final String OJB_PROPERTY_FOREIGNKEY_DOCUMENTATION        = "foreignkey-documentation";
    public static final String OJB_PROPERTY_GENERATE_REPOSITORY_INFO        = "generate-repository-info";
    public static final String OJB_PROPERTY_GENERATE_TABLE_INFO             = "generate-table-info";
    public static final String OJB_PROPERTY_ID                              = "id";
    public static final String OJB_PROPERTY_INDEXED                         = "indexed";
    public static final String OJB_PROPERTY_IGNORE                          = "ignore";
    public static final String OJB_PROPERTY_INCLUDE_INHERITED               = "include-inherited";
    public static final String OJB_PROPERTY_INDIRECTION_TABLE               = "indirection-table";
    public static final String OJB_PROPERTY_INDIRECTION_TABLE_DOCUMENTATION = "indirection-table-documentation";
    public static final String OJB_PROPERTY_INDIRECTION_TABLE_PRIMARYKEYS   = "indirection-table-primarykeys";
    public static final String OJB_PROPERTY_INITIALIZATION_METHOD           = "initialization-method";
    public static final String OJB_PROPERTY_ISOLATION_LEVEL                 = "isolation-level";
    public static final String OJB_PROPERTY_JAVA_TYPE                       = "java-type";
    public static final String OJB_PROPERTY_JDBC_TYPE                       = "jdbc-type";
    public static final String OJB_PROPERTY_LENGTH                          = "length";
    public static final String OJB_PROPERTY_LOCKING                         = "locking";
    public static final String OJB_PROPERTY_NAME                            = "name";
    public static final String OJB_PROPERTY_NULLABLE                        = "nullable";
    public static final String OJB_PROPERTY_OJB_PERSISTENT                  = "ojb-persistent";
    public static final String OJB_PROPERTY_ORDERBY                         = "orderby";
    public static final String OJB_PROPERTY_OTM_DEPENDENT                   = "otm-dependent";
    public static final String OJB_PROPERTY_PRIMARYKEY                      = "primarykey";
    public static final String OJB_PROPERTY_PRECISION                       = "precision";
    public static final String OJB_PROPERTY_PROXY                           = "proxy";
    public static final String OJB_PROPERTY_PROXY_PREFETCHING_LIMIT         = "proxy-prefetching-limit";
    public static final String OJB_PROPERTY_QUERY_CUSTOMIZER                = "query-customizer";
    public static final String OJB_PROPERTY_QUERY_CUSTOMIZER_ATTRIBUTES     = "query-customizer-attributes";
    public static final String OJB_PROPERTY_REFRESH                         = "refresh";
    public static final String OJB_PROPERTY_REMOTE_FOREIGNKEY               = "remote-foreignkey";
    public static final String OJB_PROPERTY_REMOTE_FOREIGNKEY_DOCUMENTATION = "remote-foreignkey-documentation";
    public static final String OJB_PROPERTY_RETURN_FIELD_REF                = "return-field-ref";
    public static final String OJB_PROPERTY_ROW_READER                      = "row-reader";
    public static final String OJB_PROPERTY_SCALE                           = "scale";
    public static final String OJB_PROPERTY_SEQUENCE_NAME                   = "sequence-name";
    public static final String OJB_PROPERTY_TABLE                           = "table";
    public static final String OJB_PROPERTY_TABLE_DOCUMENTATION             = "table-documentation";
    public static final String OJB_PROPERTY_TYPE                            = "type";
    public static final String OJB_PROPERTY_UNIQUE                          = "unique";
    public static final String OJB_PROPERTY_UPDATE_LOCK                     = "update-lock";
    public static final String OJB_PROPERTY_VARIABLE_TYPE                   = "variable-type";
    public static final String OJB_PROPERTY_VIRTUAL_FIELD                   = "virtual-field";

    // torque schema properties
    public static final String TORQUE_PROPERTY_AUTOINCREMENT                = "autoIncrement";
    public static final String TORQUE_PROPERTY_FOREIGNTABLE                 = "foreignTable";
    public static final String TORQUE_PROPERTY_ID                           = "id";
    public static final String TORQUE_PROPERTY_JAVANAME                     = "javaName";
    public static final String TORQUE_PROPERTY_PRIMARYKEY                   = "primaryKey";
    public static final String TORQUE_PROPERTY_RELATION_NAME                = "relation-name";
    public static final String TORQUE_PROPERTY_INV_RELATION_NAME            = "inv-relation-name";
    public static final String TORQUE_PROPERTY_REQUIRED                     = "required";
    public static final String TORQUE_PROPERTY_SIZE                         = "size";
    public static final String TORQUE_PROPERTY_TYPE                         = "type";
    
    /** Contains which properties are defined for the various defs */ 
    private static HashMap _properties = new HashMap();

    static
    {
        HashMap classProperties = new HashMap();

        classProperties.put(OJB_PROPERTY_ACCEPT_LOCKS, null);
        classProperties.put(OJB_PROPERTY_ATTRIBUTES, null);
        classProperties.put(OJB_PROPERTY_DETERMINE_EXTENTS, null);
        classProperties.put(OJB_PROPERTY_DOCUMENTATION, null);
        classProperties.put(OJB_PROPERTY_FACTORY_CLASS, null);
        classProperties.put(OJB_PROPERTY_FACTORY_METHOD, null);
        classProperties.put(OJB_PROPERTY_GENERATE_REPOSITORY_INFO, null);
        classProperties.put(OJB_PROPERTY_GENERATE_TABLE_INFO, null);
        classProperties.put(OJB_PROPERTY_INCLUDE_INHERITED, null);
        classProperties.put(OJB_PROPERTY_INITIALIZATION_METHOD, null);
        classProperties.put(OJB_PROPERTY_ISOLATION_LEVEL, null);
        classProperties.put(OJB_PROPERTY_OJB_PERSISTENT, null);
        classProperties.put(OJB_PROPERTY_PROXY, null);
        classProperties.put(OJB_PROPERTY_PROXY_PREFETCHING_LIMIT, null);
        classProperties.put(OJB_PROPERTY_REFRESH, null);
        classProperties.put(OJB_PROPERTY_ROW_READER, null);
        classProperties.put(OJB_PROPERTY_TABLE, null);
        classProperties.put(OJB_PROPERTY_TABLE_DOCUMENTATION, null);
        
        _properties.put(ClassDescriptorDef.class, classProperties);

        HashMap fieldProperties = new HashMap();

        fieldProperties.put(OJB_PROPERTY_ACCESS, null);
        fieldProperties.put(OJB_PROPERTY_ATTRIBUTES, null);
        fieldProperties.put(OJB_PROPERTY_AUTOINCREMENT, null);
        fieldProperties.put(OJB_PROPERTY_COLUMN, null);
        fieldProperties.put(OJB_PROPERTY_COLUMN_DOCUMENTATION, null);
        fieldProperties.put(OJB_PROPERTY_CONVERSION, null);
        fieldProperties.put(OJB_PROPERTY_DEFAULT_CONVERSION, null);
        fieldProperties.put(OJB_PROPERTY_DEFAULT_FETCH, null);
        fieldProperties.put(OJB_PROPERTY_DEFAULT_JDBC_TYPE, null);
        fieldProperties.put(OJB_PROPERTY_DEFAULT_PRECISION, null);
        fieldProperties.put(OJB_PROPERTY_DEFAULT_SCALE, null);
        fieldProperties.put(OJB_PROPERTY_DOCUMENTATION, null);
        fieldProperties.put(OJB_PROPERTY_ID, null);
        fieldProperties.put(OJB_PROPERTY_IGNORE, null);
        fieldProperties.put(OJB_PROPERTY_INDEXED, null);
        fieldProperties.put(OJB_PROPERTY_JDBC_TYPE, null);
        fieldProperties.put(OJB_PROPERTY_LENGTH, null);
        fieldProperties.put(OJB_PROPERTY_LOCKING, null);
        fieldProperties.put(OJB_PROPERTY_NULLABLE, null);
        fieldProperties.put(OJB_PROPERTY_PRECISION, null);
        fieldProperties.put(OJB_PROPERTY_PRIMARYKEY, null);
        fieldProperties.put(OJB_PROPERTY_SCALE, null);
        fieldProperties.put(OJB_PROPERTY_SEQUENCE_NAME, null);
        fieldProperties.put(OJB_PROPERTY_UPDATE_LOCK, null);
        fieldProperties.put(OJB_PROPERTY_VIRTUAL_FIELD, null);

        _properties.put(FieldDescriptorDef.class, fieldProperties);

        HashMap referenceProperties = new HashMap();

        referenceProperties.put(OJB_PROPERTY_ATTRIBUTES, null);
        referenceProperties.put(OJB_PROPERTY_AUTO_DELETE, null);
        referenceProperties.put(OJB_PROPERTY_AUTO_RETRIEVE, null);
        referenceProperties.put(OJB_PROPERTY_AUTO_UPDATE, null);
        referenceProperties.put(OJB_PROPERTY_CLASS_REF, null);
        referenceProperties.put(OJB_PROPERTY_DATABASE_FOREIGNKEY, null);
        referenceProperties.put(OJB_PROPERTY_DEFAULT_CLASS_REF, null);
        referenceProperties.put(OJB_PROPERTY_DOCUMENTATION, null);
        referenceProperties.put(OJB_PROPERTY_FOREIGNKEY, null);
        referenceProperties.put(OJB_PROPERTY_IGNORE, null);
        referenceProperties.put(OJB_PROPERTY_OTM_DEPENDENT, null);
        referenceProperties.put(OJB_PROPERTY_PROXY, null);
        referenceProperties.put(OJB_PROPERTY_PROXY_PREFETCHING_LIMIT, null);
        referenceProperties.put(OJB_PROPERTY_REFRESH, null);
        referenceProperties.put(OJB_PROPERTY_VARIABLE_TYPE, null);

        _properties.put(ReferenceDescriptorDef.class, referenceProperties);

        HashMap collectionProperties = new HashMap();

        collectionProperties.put(OJB_PROPERTY_ARRAY_ELEMENT_CLASS_REF, null);
        collectionProperties.put(OJB_PROPERTY_ATTRIBUTES, null);
        collectionProperties.put(OJB_PROPERTY_AUTO_DELETE, null);
        collectionProperties.put(OJB_PROPERTY_AUTO_RETRIEVE, null);
        collectionProperties.put(OJB_PROPERTY_AUTO_UPDATE, null);
        collectionProperties.put(OJB_PROPERTY_COLLECTION_CLASS, null);
        collectionProperties.put(OJB_PROPERTY_DATABASE_FOREIGNKEY, null);
        collectionProperties.put(OJB_PROPERTY_DOCUMENTATION, null);
        collectionProperties.put(OJB_PROPERTY_ELEMENT_CLASS_REF, null);
        collectionProperties.put(OJB_PROPERTY_FOREIGNKEY, null);
        collectionProperties.put(OJB_PROPERTY_FOREIGNKEY_DOCUMENTATION, null);
        collectionProperties.put(OJB_PROPERTY_IGNORE, null);
        collectionProperties.put(OJB_PROPERTY_INDIRECTION_TABLE, null);
        collectionProperties.put(OJB_PROPERTY_INDIRECTION_TABLE_DOCUMENTATION, null);
        collectionProperties.put(OJB_PROPERTY_INDIRECTION_TABLE_PRIMARYKEYS, null);
        collectionProperties.put(OJB_PROPERTY_ORDERBY, null);
        collectionProperties.put(OJB_PROPERTY_OTM_DEPENDENT, null);
        collectionProperties.put(OJB_PROPERTY_PROXY, null);
        collectionProperties.put(OJB_PROPERTY_PROXY_PREFETCHING_LIMIT, null);
        collectionProperties.put(OJB_PROPERTY_QUERY_CUSTOMIZER, null);
        collectionProperties.put(OJB_PROPERTY_QUERY_CUSTOMIZER_ATTRIBUTES, null);
        collectionProperties.put(OJB_PROPERTY_REFRESH, null);
        collectionProperties.put(OJB_PROPERTY_REMOTE_FOREIGNKEY, null);
        collectionProperties.put(OJB_PROPERTY_REMOTE_FOREIGNKEY_DOCUMENTATION, null);
        collectionProperties.put(OJB_PROPERTY_VARIABLE_TYPE, null);

        _properties.put(CollectionDescriptorDef.class, collectionProperties);

        HashMap indexProperties = new HashMap();

        indexProperties.put(OJB_PROPERTY_DOCUMENTATION, null);
        indexProperties.put(OJB_PROPERTY_FIELDS, null);
        indexProperties.put(OJB_PROPERTY_UNIQUE, null);

        _properties.put(IndexDescriptorDef.class, indexProperties);

        HashMap cacheProperties = new HashMap();

        cacheProperties.put(OJB_PROPERTY_ATTRIBUTES, null);
        cacheProperties.put(OJB_PROPERTY_DOCUMENTATION, null);
        cacheProperties.put(OJB_PROPERTY_CLASS, null);

        _properties.put(ObjectCacheDef.class, cacheProperties);
    }

    /**
     * Checks whether the property of the given name is allowed for the model element.
     * 
     * @param defClass     The class of the model element
     * @param propertyName The name of the property
     * @return <code>true</code> if the property is allowed for this type of model elements
     */
    public static boolean isPropertyAllowed(Class defClass, String propertyName)
    {
        HashMap props = (HashMap)_properties.get(defClass);

        return (props == null ? true : props.containsKey(propertyName));
    }

    /**
     * Determines whether the boolean value of the given string value.
     * 
     * @param value        The value
     * @param defaultValue The boolean value to use if the string value is neither 'true' nor 'false'
     * @return The boolean value of the string
     */
    public static boolean toBoolean(String value, boolean defaultValue)
    {
        return "true".equals(value) ? true : ("false".equals(value) ? false : defaultValue);
    }
}
