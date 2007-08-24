package xdoclet.modules.ojb;

/* Copyright 2003-2005 The Apache Software Foundation
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

import xjavadoc.*;
import xdoclet.XDocletException;
import xdoclet.XDocletTagSupport;
import xdoclet.modules.ojb.constraints.*;
import xdoclet.modules.ojb.model.*;
import xdoclet.util.Translator;
import xdoclet.util.TypeConversionUtil;

/**
 * Provides functions for the XDoclet template.
 * 
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 * @xdoclet.taghandler namespace="Ojb"
 */
public class OjbTagsHandler extends XDocletTagSupport
{
    private static final String CONFIG_PARAM_CHECKS       = "checks";
    private static final String CONFIG_PARAM_VERBOSE      = "verbose";
    private static final String CONFIG_PARAM_DATABASENAME = "databaseName";

    private static final String ATTRIBUTE_CLASS           = "class";
    private static final String ATTRIBUTE_CONSTANT        = "constant";
    private static final String ATTRIBUTE_DEFAULT         = "default";
    private static final String ATTRIBUTE_DEFAULT_RIGHT   = "default-right";
    private static final String ATTRIBUTE_LEVEL           = "level";
    private static final String ATTRIBUTE_NAME            = "name";
    private static final String ATTRIBUTE_TYPE            = "type";
    private static final String ATTRIBUTE_UNIQUE          = "unique";
    private static final String ATTRIBUTE_VALUE           = "value";

    private static final String LEVEL_CLASS               = "class";
    private static final String LEVEL_COLLECTION          = "collection";
    private static final String LEVEL_COLUMN              = "column";
    private static final String LEVEL_FIELD               = "field";
    private static final String LEVEL_FOREIGNKEY          = "foreignkey";
    private static final String LEVEL_INDEX               = "index";
    private static final String LEVEL_INDEX_DESC          = "index-desc";
    private static final String LEVEL_OBJECT_CACHE        = "object-cache";
    private static final String LEVEL_PROCEDURE           = "procedure";
    private static final String LEVEL_PROCEDURE_ARGUMENT  = "procedure-argument";
    private static final String LEVEL_REFERENCE           = "reference";
    private static final String LEVEL_TABLE               = "table";
    
    /** The ojb model */
    private ModelDef _model = new ModelDef();
    /** The torque model */
    private TorqueModelDef _torqueModel = null;
    /** The current class definition */
    private ClassDescriptorDef _curClassDef = null;
    /** The current field definition */
    private FieldDescriptorDef _curFieldDef = null;
    /** The current reference definition */
    private ReferenceDescriptorDef _curReferenceDef = null;
    /** The current collection definition */
    private CollectionDescriptorDef _curCollectionDef = null;
    /** The current extending class of the current extent */
    private ClassDescriptorDef _curExtent = null;
    /** The current obejct cache definition */
    private ObjectCacheDef _curObjectCacheDef = null;
    /** The current index descriptor definition */
    private IndexDescriptorDef _curIndexDescriptorDef = null;
    /** The current procedure definition */
    private ProcedureDef _curProcedureDef = null;
    /** The current procedure argument definition */
    private ProcedureArgumentDef _curProcedureArgumentDef = null;
    /** The name of the current index column */
    private String  _curIndexColumn = null;
    /** The current table definition */
    private TableDef _curTableDef = null;
    /** The current column definition */
    private ColumnDef _curColumnDef = null;
    /** The current foreignkey definition */
    private ForeignkeyDef _curForeignkeyDef = null;
    /** The current index definition */
    private IndexDef _curIndexDef = null;
    /** The name of the current attribute */
    private String  _curPairLeft = null;
    /** The value of the current attribute */
    private String  _curPairRight = null;

    //
    // XDt methods
    //

    // Class-related
    
    /**
     * Sets the current class definition derived from the current class, and optionally some attributes.
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  if an error occurs
     * @doc.tag                     type="block"
     * @doc.param                   name="accept-locks" optional="true" description="The accept locks setting" values="true,false"
     * @doc.param                   name="attributes" optional="true" description="Attributes of the class as name-value pairs 'name=value',
     *      separated by commas"
     * @doc.param                   name="determine-extents" optional="true" description="Whether to determine
     *      persistent direct sub types automatically" values="true,false"
     * @doc.param                   name="documentation" optional="true" description="Documentation on the class"
     * @doc.param                   name="factory-method" optional="true" description="Specifies a no-argument factory method that is
     *      used to create instances (not yet implemented !)"
     * @doc.param                   name="factory-class" optional="true" description="Specifies a factory class to be used for creating
     *     objects of this class"
     * @doc.param                   name="factory-method" optional="true" description="Specifies a static no-argument method in the factory class"
     * @doc.param                   name="generate-repository-info" optional="true" description="Whether repository data should be
     *      generated for the class" values="true,false"
     * @doc.param                   name="generate-table-info" optional="true" description="Whether table data should be
     *      generated for the class" values="true,false"
     * @doc.param                   name="include-inherited" optional="true" description="Whether to include
     *      fields/references/collections of supertypes" values="true,false"
     * @doc.param                   name="initialization-method" optional="true" description="Specifies a no-argument instance method that is
     *      called right after an instance has been read from the database"
     * @doc.param                   name="isolation-level" optional="true" description="The isolation level setting"
     * @doc.param                   name="proxy" optional="true" description="The proxy setting for this class"
     * @doc.param                   name="proxy-prefetching-limit" optional="true" description="Specifies the amount of objects of
     *      objects of this class to prefetch in collections"
     * @doc.param                   name="refresh" optional="true" description="Can be set to force OJB to refresh instances when
     *      loaded from the cache" values="true,false"
     * @doc.param                   name="row-reader" optional="true" description="The row reader for the class"
     * @doc.param                   name="schema" optional="true" description="The schema for the type"
     * @doc.param                   name="table" optional="true" description="The table for the class"
     * @doc.param                   name="table-documentation" optional="true" description="Documentation on the table"
     */
    public void processClass(String template, Properties attributes) throws XDocletException
    {
        if (!_model.hasClass(getCurrentClass().getQualifiedName()))
        {
            // we only want to output the log message once
            LogHelper.debug(true, OjbTagsHandler.class, "processClass", "Type "+getCurrentClass().getQualifiedName());
        }

        ClassDescriptorDef classDef = ensureClassDef(getCurrentClass());
        String attrName;

        for (Enumeration attrNames = attributes.propertyNames(); attrNames.hasMoreElements(); )
        {
            attrName = (String)attrNames.nextElement();
            classDef.setProperty(attrName, attributes.getProperty(attrName));
        }
        _curClassDef = classDef;
        generate(template);
        _curClassDef = null;
    }

    /**
     * Processes the template for all class definitions.
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  if an error occurs
     * @doc.tag                     type="block"
     */
    public void forAllClassDefinitions(String template, Properties attributes) throws XDocletException
    {
        for (Iterator it = _model.getClasses(); it.hasNext(); )
        {
            _curClassDef = (ClassDescriptorDef)it.next();
            generate(template);
        }
        _curClassDef = null;

        LogHelper.debug(true, OjbTagsHandler.class, "forAllClassDefinitions", "Processed "+_model.getNumClasses()+" types");
    }

    /**
     * Processes the original class rather than the current class definition.
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  if an error occurs
     * @doc.tag                     type="block"
     */
    public void originalClass(String template, Properties attributes) throws XDocletException
    {
        pushCurrentClass(_curClassDef.getOriginalClass());
        generate(template);
        popCurrentClass();
    }

    /**
     * Processes all classes (flattens the hierarchy such that every class has declarations for all fields,
     * references,collections that it will have in the descriptor) and applies modifications (removes ignored
     * features, changes declarations).
     * 
     * @return  An empty string
     * @doc.tag type="content"
     */
    public String prepare() throws XDocletException
    {
        String             checkLevel = (String)getDocletContext().getConfigParam(CONFIG_PARAM_CHECKS);
        ArrayList          queue      = new ArrayList();
        ClassDescriptorDef classDef, baseDef;
        XClass             original;
        boolean            isFinished;

        // determine inheritance relationships
        for (Iterator it = _model.getClasses(); it.hasNext();)
        {
            classDef   = (ClassDescriptorDef)it.next();
            original   = classDef.getOriginalClass();
            isFinished = false;
            queue.clear();
            while (!isFinished)
            {
                if (original == null)
                {
                    isFinished = true;
                    for (Iterator baseIt = queue.iterator(); baseIt.hasNext();)
                    {
                        original = (XClass)baseIt.next();
                        baseDef  = _model.getClass(original.getQualifiedName());
                        baseIt.remove();
                        if (baseDef != null)
                        {
                            classDef.addDirectBaseType(baseDef);
                        }
                        else
                        {
                            isFinished = false;
                            break;
                        }
                    }
                }
                if (!isFinished)
                {    
                    if (original.getInterfaces() != null)
                    {
                        for (Iterator baseIt = original.getInterfaces().iterator(); baseIt.hasNext();)
                        {
                            queue.add(baseIt.next());
                        }
                    }
                    if (original.getSuperclass() != null)
                    {
                        queue.add(original.getSuperclass());
                    }
                    original = null;
                }
            }
        }
        try
        {
            _model.process();
            _model.checkConstraints(checkLevel);
        }
        catch (ConstraintException ex)
        {
            throw new XDocletException(ex.getMessage());
        }
        return "";
    }

    // Extent-related
    
    /**
     * The <code>forAllSubClasses</code> method iterates through all sub types of the current type (classes if it is a
     * class or classes and interfaces for an interface).
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  if an error occurs
     * @doc.tag                     type="block"
     */
    public void forAllSubClasses(String template, Properties attributes) throws XDocletException
    {
        ArrayList subTypes = new ArrayList();
        XClass    type     = getCurrentClass();

        addDirectSubTypes(type, subTypes);

        int                pos = 0;
        ClassDescriptorDef classDef;

        while (pos < subTypes.size())
        {
            type     = (XClass)subTypes.get(pos);
            classDef = _model.getClass(type.getQualifiedName());
            if ((classDef != null) && classDef.hasProperty(PropertyHelper.OJB_PROPERTY_OJB_PERSISTENT))
            {
                pos++;
            }
            else
            {
                subTypes.remove(pos);
                addDirectSubTypes(type, subTypes);
            }
        }
        for (Iterator it = subTypes.iterator(); it.hasNext(); )
        {
            pushCurrentClass((XClass)it.next());
            generate(template);
            popCurrentClass();
        }
    }

    /**
     * Adds an extent relation to the current class definition.
     *
     * @param attributes            The attributes of the tag
     * @return                      An empty string
     * @exception XDocletException  If an error occurs
     * @doc.tag                     type="content"
     * @doc.param                   name="name" optional="false" description="The fully qualified name of the extending
     *      class"
     */
    public String addExtent(Properties attributes) throws XDocletException
    {
        String name = attributes.getProperty(ATTRIBUTE_NAME);

        if (!_model.hasClass(name))
        {
            throw new XDocletException(Translator.getString(XDocletModulesOjbMessages.class,
                                       XDocletModulesOjbMessages.COULD_NOT_FIND_TYPE,
                                       new String[]{name}));
        }
        _curClassDef.addExtentClass(_model.getClass(name));
        return "";
    }

    /**
     * Processes the template for all extents of the current class.
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  if an error occurs
     * @doc.tag                     type="block"
     */
    public void forAllExtents(String template, Properties attributes) throws XDocletException
    {
        for (Iterator it = _curClassDef.getExtentClasses(); it.hasNext(); )
        {
            _curExtent = (ClassDescriptorDef)it.next();
            generate(template);
        }
        _curExtent = null;
    }

    /**
     * Returns the qualified name of the current extent.
     *
     * @param attributes            The attributes of the tag
     * @return                      The qualified name of the extent class
     * @exception XDocletException  If an error occurs
     * @doc.tag                     type="content"
     */
    public String extent(Properties attributes) throws XDocletException
    {
        return _curExtent.getName();
    }

    // Index-related

    /**
     * Processes an index descriptor tag.
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  If an error occurs
     * @doc.tag                     type="content"
     * @doc.param                   name="documentation" optional="true" description="Documentation on the index"
     * @doc.param                   name="fields" optional="false" description="The fields making up the index separated by commas"
     * @doc.param                   name="name" optional="false" description="The name of the index descriptor"
     * @doc.param                   name="unique" optional="true" description="Whether the index descriptor is unique" values="true,false"
     */
    public String processIndexDescriptor(Properties attributes) throws XDocletException
    {
        String             name     = attributes.getProperty(ATTRIBUTE_NAME);
        IndexDescriptorDef indexDef = _curClassDef.getIndexDescriptor(name);
        String             attrName;
        
        if (indexDef == null)
        {    
            indexDef = new IndexDescriptorDef(name);
            _curClassDef.addIndexDescriptor(indexDef);
        }

        if ((indexDef.getName() == null) || (indexDef.getName().length() == 0))
        {
            throw new XDocletException(Translator.getString(XDocletModulesOjbMessages.class,
                                       XDocletModulesOjbMessages.INDEX_NAME_MISSING,
                                       new String[]{_curClassDef.getName()}));
        }
        attributes.remove(ATTRIBUTE_NAME);
        for (Enumeration attrNames = attributes.propertyNames(); attrNames.hasMoreElements(); )
        {
            attrName = (String)attrNames.nextElement();
            indexDef.setProperty(attrName, attributes.getProperty(attrName));
        }
        return "";
    }

    /**
     * Processes the template for all index descriptors of the current class definition.
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  if an error occurs
     * @doc.tag                     type="block"
     */
    public void forAllIndexDescriptorDefinitions(String template, Properties attributes) throws XDocletException
    {
        for (Iterator it = _curClassDef.getIndexDescriptors(); it.hasNext(); )
        {
            _curIndexDescriptorDef = (IndexDescriptorDef)it.next();
            generate(template);
        }
        _curIndexDescriptorDef = null;
    }

    /**
     * Processes the template for all index columns for the current index descriptor.
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  if an error occurs
     * @doc.tag                     type="block"
     */
    public void forAllIndexDescriptorColumns(String template, Properties attributes) throws XDocletException
    {
        String             fields = _curIndexDescriptorDef.getProperty(PropertyHelper.OJB_PROPERTY_FIELDS);
        FieldDescriptorDef fieldDef;
        String             name;

        for (CommaListIterator it = new CommaListIterator(fields); it.hasNext();)
        {
            name     = it.getNext();
            fieldDef = _curClassDef.getField(name);
            if (fieldDef == null)
            {
                throw new XDocletException(Translator.getString(XDocletModulesOjbMessages.class,
                                           XDocletModulesOjbMessages.INDEX_FIELD_MISSING,
                                           new String[]{name, _curIndexDescriptorDef.getName(), _curClassDef.getName()}));
            }
            _curIndexColumn = fieldDef.getProperty(PropertyHelper.OJB_PROPERTY_COLUMN);
            generate(template);
        }
        _curIndexColumn = null;
    }

    /**
     * Returns the current index column.
     *
     * @param attributes            The attributes of the tag
     * @return                      The index column
     * @exception XDocletException  If an error occurs
     * @doc.tag                     type="content"
     */
    public String indexColumn(Properties attributes) throws XDocletException
    {
        return _curIndexColumn;
    }


    // Object-cache related

    /**
     * Processes an object cache tag.
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  If an error occurs
     * @doc.tag                     type="content"
     * @doc.param                   name="attributes" optional="true" description="Attributes of the object-cache as name-value pairs 'name=value',
     * @doc.param                   name="class" optional="false" description="The object cache implementation"
     * @doc.param                   name="documentation" optional="true" description="Documentation on the object cache"
     */
    public String processObjectCache(Properties attributes) throws XDocletException
    {
        ObjectCacheDef objCacheDef = _curClassDef.setObjectCache(attributes.getProperty(ATTRIBUTE_CLASS));
        String         attrName;

        attributes.remove(ATTRIBUTE_CLASS);
        for (Enumeration attrNames = attributes.propertyNames(); attrNames.hasMoreElements(); )
        {
            attrName = (String)attrNames.nextElement();
            objCacheDef.setProperty(attrName, attributes.getProperty(attrName));
        }
        return "";
    }

    /**
     * Processes the template for the object cache of the current class definition.
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  if an error occurs
     * @doc.tag                     type="block"
     */
    public void forObjectCache(String template, Properties attributes) throws XDocletException
    {
        _curObjectCacheDef = _curClassDef.getObjectCache();
        if (_curObjectCacheDef != null)
        {
            generate(template);
            _curObjectCacheDef = null;
        }
    }

    // Procedure-related

    /**
     * Processes a procedure tag.
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  If an error occurs
     * @doc.tag                     type="content"
     * @doc.param                   name="arguments" optional="true" description="The arguments of the procedure as a comma-separated
     *      list of names of procedure attribute tags"
     * @doc.param                   name="attributes" optional="true" description="Attributes of the procedure as name-value pairs 'name=value',
     *      separated by commas"
     * @doc.param                   name="documentation" optional="true" description="Documentation on the procedure"
     * @doc.param                   name="include-all-fields" optional="true" description="For insert/update: whether all fields of the current
     *      class shall be included (arguments is ignored then)" values="true,false"
     * @doc.param                   name="include-pk-only" optional="true" description="For delete: whether all primary key fields
     *      shall be included (arguments is ignored then)" values="true,false"
     * @doc.param                   name="name" optional="false" description="The name of the procedure"
     * @doc.param                   name="return-field-ref" optional="true" description="Identifies the field that receives the return value"
     * @doc.param                   name="type" optional="false" description="The type of the procedure" values="delete,insert,update"
     */
    public String processProcedure(Properties attributes) throws XDocletException
    {
        String       type    = attributes.getProperty(ATTRIBUTE_TYPE);
        ProcedureDef procDef = _curClassDef.getProcedure(type);
        String       attrName;

        if (procDef == null)
        {    
            procDef = new ProcedureDef(type);
            _curClassDef.addProcedure(procDef);
        }

        for (Enumeration attrNames = attributes.propertyNames(); attrNames.hasMoreElements(); )
        {
            attrName = (String)attrNames.nextElement();
            procDef.setProperty(attrName, attributes.getProperty(attrName));
        }
        return "";
    }

    /**
     * Processes the template for all procedures of the current class definition.
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  if an error occurs
     * @doc.tag                     type="block"
     */
    public void forAllProcedures(String template, Properties attributes) throws XDocletException
    {
        for (Iterator it = _curClassDef.getProcedures(); it.hasNext(); )
        {
            _curProcedureDef = (ProcedureDef)it.next();
            generate(template);
        }
        _curProcedureDef = null;
    }

    /**
     * Processes a runtime procedure argument tag.
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  If an error occurs
     * @doc.tag                     type="content"
     * @doc.param                   name="attributes" optional="true" description="Attributes of the procedure as name-value pairs 'name=value',
     *      separated by commas"
     * @doc.param                   name="documentation" optional="true" description="Documentation on the procedure"
     * @doc.param                   name="field-ref" optional="true" description="Identifies the field that provides the value
     *      if a runtime argument; if not set, then null is used"
     * @doc.param                   name="name" optional="false" description="The identifier of the argument tag"
     * @doc.param                   name="return" optional="true" description="Whether this is a return value (if a runtime argument)"
     *      values="true,false"
     * @doc.param                   name="value" optional="false" description="The value if a constant argument"
     */
    public String processProcedureArgument(Properties attributes) throws XDocletException
    {
        String               id     = attributes.getProperty(ATTRIBUTE_NAME);
        ProcedureArgumentDef argDef = _curClassDef.getProcedureArgument(id);
        String               attrName;
        
        if (argDef == null)
        {    
            argDef = new ProcedureArgumentDef(id);
            _curClassDef.addProcedureArgument(argDef);
        }

        attributes.remove(ATTRIBUTE_NAME);
        for (Enumeration attrNames = attributes.propertyNames(); attrNames.hasMoreElements(); )
        {
            attrName = (String)attrNames.nextElement();
            argDef.setProperty(attrName, attributes.getProperty(attrName));
        }
        return "";
    }

    /**
     * Processes the template for all procedure arguments of the current procedure.
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  if an error occurs
     * @doc.tag                     type="block"
     */
    public void forAllProcedureArguments(String template, Properties attributes) throws XDocletException
    {
        String argNameList = _curProcedureDef.getProperty(PropertyHelper.OJB_PROPERTY_ARGUMENTS);

        for (CommaListIterator it = new CommaListIterator(argNameList); it.hasNext();)
        {
            _curProcedureArgumentDef = _curClassDef.getProcedureArgument(it.getNext());
            generate(template);
        }
        _curProcedureArgumentDef = null;
    }

    // Field-related

    /**
     * Processes an anonymous field definition specified at the class level.
     *
     * @param attributes            The attributes of the tag
     * @exception XDocletException  if an error occurs
     * @doc.tag                     type="content"
     * @doc.param                   name="attributes" optional="true" description="Attributes of the field as name-value pairs 'name=value',
     *      separated by commas"
     * @doc.param                   name="autoincrement" optional="true" description="Whether the field is
     *      auto-incremented" values="true,false"
     * @doc.param                   name="column" optional="true" description="The column for the field"
     * @doc.param                   name="conversion" optional="true" description="The fully qualified name of the
     *      conversion for the field"
     * @doc.param                   name="default-fetch" optional="true" description="The default-fetch setting"
     *      values="true,false"
     * @doc.param                   name="documentation" optional="true" description="Documentation on the field"
     * @doc.param                   name="id" optional="true" description="The position of the field in the class
     *      descriptor"
     * @doc.param                   name="indexed" optional="true" description="Whether the field is indexed"
     *      values="true,false"
     * @doc.param                   name="jdbc-type" optional="true" description="The jdbc type of the column"
     * @doc.param                   name="length" optional="true" description="The length of the column"
     * @doc.param                   name="locking" optional="true" description="Whether the field supports locking"
     *      values="true,false"
     * @doc.param                   name="name" optional="false" description="The name of the field"
     * @doc.param                   name="nullable" optional="true" description="Whether the field is nullable"
     *      values="true,false"
     * @doc.param                   name="precision" optional="true" description="The precision of the column"
     * @doc.param                   name="primarykey" optional="true" description="Whether the field is a primarykey"
     *      values="true,false"
     * @doc.param                   name="scale" optional="true" description="The scale of the column"
     * @doc.param                   name="sequence-name" optional="true" description="The name of the sequence for
     *      incrementing the field"
     * @doc.param                   name="table" optional="true" description="The table of the field (not implemented
     *      yet)"
     * @doc.param                   name="update-lock" optional="true" description="Can be set to false if the persistent attribute is
     *      used for optimistic locking AND the dbms should update the lock column itself (default is true). Can only be set for
     *      TIMESTAMP and INTEGER columns" values="true,false"
     */
    public void processAnonymousField(Properties attributes) throws XDocletException
    {
        if (!attributes.containsKey(ATTRIBUTE_NAME))
        {
            throw new XDocletException(Translator.getString(XDocletModulesOjbMessages.class,
                                       XDocletModulesOjbMessages.PARAMETER_IS_REQUIRED,
                                       new String[]{ATTRIBUTE_NAME}));
        }

        String             name     = attributes.getProperty(ATTRIBUTE_NAME);
        FieldDescriptorDef fieldDef = _curClassDef.getField(name);
        String             attrName;

        if (fieldDef == null)
        {
            fieldDef = new FieldDescriptorDef(name);
            _curClassDef.addField(fieldDef);
        }
        fieldDef.setAnonymous();
        LogHelper.debug(false, OjbTagsHandler.class, "processAnonymousField", "  Processing anonymous field "+fieldDef.getName());

        attributes.remove(ATTRIBUTE_NAME);
        for (Enumeration attrNames = attributes.propertyNames(); attrNames.hasMoreElements(); )
        {
            attrName = (String)attrNames.nextElement();
            fieldDef.setProperty(attrName, attributes.getProperty(attrName));
        }
        fieldDef.setProperty(PropertyHelper.OJB_PROPERTY_ACCESS, "anonymous");
    }

    /**
     * Sets the current field definition derived from the current member, and optionally some attributes.
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  if an error occurs
     * @doc.tag                     type="block"
     * @doc.param                   name="access" optional="true" description="The accessibility of the column" values="readonly,readwrite"
     * @doc.param                   name="attributes" optional="true" description="Attributes of the field as name-value pairs 'name=value',
     *      separated by commas"
     * @doc.param                   name="autoincrement" optional="true" description="Whether the field is
     *      auto-incremented" values="none,ojb,database"
     * @doc.param                   name="column" optional="true" description="The column for the field"
     * @doc.param                   name="column-documentation" optional="true" description="Documentation on the column"
     * @doc.param                   name="conversion" optional="true" description="The fully qualified name of the
     *      conversion for the field"
     * @doc.param                   name="default-fetch" optional="true" description="The default-fetch setting"
     *      values="true,false"
     * @doc.param                   name="documentation" optional="true" description="Documentation on the field"
     * @doc.param                   name="id" optional="true" description="The position of the field in the class
     *      descriptor"
     * @doc.param                   name="indexed" optional="true" description="Whether the field is indexed"
     *      values="true,false"
     * @doc.param                   name="jdbc-type" optional="true" description="The jdbc type of the column"
     * @doc.param                   name="length" optional="true" description="The length of the column"
     * @doc.param                   name="locking" optional="true" description="Whether the field supports locking"
     *      values="true,false"
     * @doc.param                   name="nullable" optional="true" description="Whether the field is nullable"
     *      values="true,false"
     * @doc.param                   name="precision" optional="true" description="The precision of the column"
     * @doc.param                   name="primarykey" optional="true" description="Whether the field is a primarykey"
     *      values="true,false"
     * @doc.param                   name="scale" optional="true" description="The scale of the column"
     * @doc.param                   name="sequence-name" optional="true" description="The name of the sequence for
     *      incrementing the field"
     * @doc.param                   name="table" optional="true" description="The table of the field (not implemented
     *      yet)"
     * @doc.param                   name="update-lock" optional="true" description="Can be set to false if the persistent attribute is
     *      used for optimistic locking AND the dbms should update the lock column itself (default is true). Can only be set for
     *      TIMESTAMP and INTEGER columns" values="true,false"
     */
    public void processField(String template, Properties attributes) throws XDocletException
    {
        String             name              = OjbMemberTagsHandler.getMemberName();
        String             defaultType       = getDefaultJdbcTypeForCurrentMember();
        String             defaultConversion = getDefaultJdbcConversionForCurrentMember();
        FieldDescriptorDef fieldDef          = _curClassDef.getField(name);
        String             attrName;

        if (fieldDef == null)
        {
            fieldDef = new FieldDescriptorDef(name);
            _curClassDef.addField(fieldDef);
        }
        LogHelper.debug(false, OjbTagsHandler.class, "processField", "  Processing field "+fieldDef.getName());

        for (Enumeration attrNames = attributes.propertyNames(); attrNames.hasMoreElements(); )
        {
            attrName = (String)attrNames.nextElement();
            fieldDef.setProperty(attrName, attributes.getProperty(attrName));
        }
        // storing additional info for later use
        fieldDef.setProperty(PropertyHelper.OJB_PROPERTY_JAVA_TYPE,
                             OjbMemberTagsHandler.getMemberType().getQualifiedName());
        fieldDef.setProperty(PropertyHelper.OJB_PROPERTY_DEFAULT_JDBC_TYPE, defaultType);
        if (defaultConversion != null)
        {    
            fieldDef.setProperty(PropertyHelper.OJB_PROPERTY_DEFAULT_CONVERSION, defaultConversion);
        }

        _curFieldDef = fieldDef;
        generate(template);
        _curFieldDef = null;
    }

    /**
     * Processes the template for all field definitions of the current class definition (including inherited ones if
     * required)
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  if an error occurs
     * @doc.tag                     type="block"
     */
    public void forAllFieldDefinitions(String template, Properties attributes) throws XDocletException
    {
        for (Iterator it = _curClassDef.getFields(); it.hasNext(); )
        {
            _curFieldDef = (FieldDescriptorDef)it.next();
            if (!isFeatureIgnored(LEVEL_FIELD) &&
                !_curFieldDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_IGNORE, false))
            {
                generate(template);
            }
        }
        _curFieldDef = null;
    }

    /**
     * Returns the constraint (length or precision+scale) for the jdbc type of the current field.
     *
     * @param attributes            The attributes of the tag
     * @return                      The constraint of the field
     * @exception XDocletException  If an error occurs
     * @doc.tag                     type="content"
     */
    public String fieldConstraint(Properties attributes) throws XDocletException
    {
        return _curFieldDef.getSizeConstraint();
    }

    // Reference-related

    /**
     * Processes an anonymous reference definition.
     *
     * @param attributes            The attributes of the tag
     * @exception XDocletException  If an error occurs
     * @doc.tag                     type="content"
     * @doc.param                   name="attributes" optional="true" description="Attributes of the reference as name-value pairs 'name=value',
     *      separated by commas"
     * @doc.param                   name="auto-delete" optional="true" description="Whether to automatically delete the
     *      referenced object on object deletion"
     * @doc.param                   name="auto-retrieve" optional="true" description="Whether to automatically retrieve
     *      the referenced object"
     * @doc.param                   name="auto-update" optional="true" description="Whether to automatically update the
     *      referenced object"
     * @doc.param                   name="class-ref" optional="false" description="The fully qualified name of the class
     *      owning the referenced field"
     * @doc.param                   name="documentation" optional="true" description="Documentation on the reference"
     * @doc.param                   name="foreignkey" optional="true" description="The fields in the current type used for
     * implementing the reference"
     * @doc.param                   name="otm-dependent" optional="true" description="Whether the reference is dependent on otm"
     * @doc.param                   name="proxy" optional="true" description="Whether to use a proxy for the reference"
     * @doc.param                   name="proxy-prefetching-limit" optional="true" description="Specifies the amount of objects to prefetch"
     * @doc.param                   name="refresh" optional="true" description="Whether to automatically refresh the
     *      reference"
     * @doc.param                   name="remote-foreignkey" optional="true" description="The fields in the referenced type
     * corresponding to the local fields (is only used for the table definition)"
     */
    public void processAnonymousReference(Properties attributes) throws XDocletException
    {
        ReferenceDescriptorDef refDef = _curClassDef.getReference("super");
        String                 attrName;

        if (refDef == null)
        {
            refDef = new ReferenceDescriptorDef("super");
            _curClassDef.addReference(refDef);
        }
        refDef.setAnonymous();
        LogHelper.debug(false, OjbTagsHandler.class, "processAnonymousReference", "  Processing anonymous reference");

        for (Enumeration attrNames = attributes.propertyNames(); attrNames.hasMoreElements(); )
        {
            attrName = (String)attrNames.nextElement();
            refDef.setProperty(attrName, attributes.getProperty(attrName));
        }
    }

    /**
     * Sets the current reference definition derived from the current member, and optionally some attributes.
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  If an error occurs
     * @doc.tag                     type="block"
     * @doc.param                   name="attributes" optional="true" description="Attributes of the reference as name-value pairs 'name=value',
     *      separated by commas"
     * @doc.param                   name="auto-delete" optional="true" description="Whether to automatically delete the
     *      referenced object on object deletion"
     * @doc.param                   name="auto-retrieve" optional="true" description="Whether to automatically retrieve
     *      the referenced object"
     * @doc.param                   name="auto-update" optional="true" description="Whether to automatically update the
     *      referenced object"
     * @doc.param                   name="class-ref" optional="true" description="The fully qualified name of the class
     *      owning the referenced field"
     * @doc.param                   name="database-foreignkey" optional="true" description="Whether a database foreignkey shall be created"
     *      values="true,false"
     * @doc.param                   name="documentation" optional="true" description="Documentation on the reference"
     * @doc.param                   name="foreignkey" optional="true" description="The fields in the current type used for
     * implementing the reference"
     * @doc.param                   name="otm-dependent" optional="true" description="Whether the reference is dependent on otm"
     * @doc.param                   name="proxy" optional="true" description="Whether to use a proxy for the reference"
     * @doc.param                   name="proxy-prefetching-limit" optional="true" description="Specifies the amount of objects to prefetch"
     * @doc.param                   name="refresh" optional="true" description="Whether to automatically refresh the
     *      reference"
     * @doc.param                   name="remote-foreignkey" optional="true" description="The fields in the referenced type
     * corresponding to the local fields (is only used for the table definition)"
     */
    public void processReference(String template, Properties attributes) throws XDocletException
    {
        String                 name   = OjbMemberTagsHandler.getMemberName();
        XClass                 type   = OjbMemberTagsHandler.getMemberType();
        int                    dim    = OjbMemberTagsHandler.getMemberDimension();
        ReferenceDescriptorDef refDef = _curClassDef.getReference(name);
        String                 attrName;

        if (refDef == null)
        {
            refDef = new ReferenceDescriptorDef(name);
            _curClassDef.addReference(refDef);
        }
        LogHelper.debug(false, OjbTagsHandler.class, "processReference", "  Processing reference "+refDef.getName());

        for (Enumeration attrNames = attributes.propertyNames(); attrNames.hasMoreElements(); )
        {
            attrName = (String)attrNames.nextElement();
            refDef.setProperty(attrName, attributes.getProperty(attrName));
        }
        // storing default info for later use
        if (type == null)
        {
            throw new XDocletException(Translator.getString(XDocletModulesOjbMessages.class,
                                       XDocletModulesOjbMessages.COULD_NOT_DETERMINE_TYPE_OF_MEMBER,
                                       new String[]{name}));
        }
        if (dim > 0)
        {
            throw new XDocletException(Translator.getString(XDocletModulesOjbMessages.class,
                                       XDocletModulesOjbMessages.MEMBER_CANNOT_BE_A_REFERENCE,
                                       new String[]{name, _curClassDef.getName()}));
        }

        refDef.setProperty(PropertyHelper.OJB_PROPERTY_VARIABLE_TYPE, type.getQualifiedName());

        // searching for default type
        String typeName = searchForPersistentSubType(type);

        if (typeName != null)
        {
            refDef.setProperty(PropertyHelper.OJB_PROPERTY_DEFAULT_CLASS_REF, typeName);
        }

        _curReferenceDef = refDef;
        generate(template);
        _curReferenceDef = null;
    }

    /**
     * Processes the template for all reference definitions of the current class definition.
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  if an error occurs
     * @doc.tag                     type="block"
     */
    public void forAllReferenceDefinitions(String template, Properties attributes) throws XDocletException
    {
        for (Iterator it = _curClassDef.getReferences(); it.hasNext(); )
        {
            _curReferenceDef = (ReferenceDescriptorDef)it.next();
            // first we check whether it is an inherited anonymous reference
            if (_curReferenceDef.isAnonymous() && (_curReferenceDef.getOwner() != _curClassDef))
            {
                continue;
            }
            if (!isFeatureIgnored(LEVEL_REFERENCE) &&
                !_curReferenceDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_IGNORE, false))
            {
                generate(template);
            }
        }
        _curReferenceDef = null;
    }

    // Collection-related
    
    /**
     * Sets the current collection definition derived from the current member, and optionally some attributes.
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  If an error occurs
     * @doc.tag                     type="block"
     * @doc.param                   name="attributes" optional="true" description="Attributes of the collection as name-value pairs 'name=value',
     *      separated by commas"
     * @doc.param                   name="auto-delete" optional="true" description="Whether to automatically delete the
     *      collection on object deletion"
     * @doc.param                   name="auto-retrieve" optional="true" description="Whether to automatically retrieve
     *      the collection"
     * @doc.param                   name="auto-update" optional="true" description="Whether to automatically update the
     *      collection"
     * @doc.param                   name="collection-class" optional="true" description="The type of the collection if not a
     * java.util type or an array"
     * @doc.param                   name="database-foreignkey" optional="true" description="Whether a database foreignkey shall be created"
     *      values="true,false"
     * @doc.param                   name="documentation" optional="true" description="Documentation on the collection"
     * @doc.param                   name="element-class-ref" optional="true" description="The fully qualified name of
     *      the element type"
     * @doc.param                   name="foreignkey" optional="true" description="The name of the
     *      foreign keys (columns when an indirection table is given)"
     * @doc.param                   name="foreignkey-documentation" optional="true" description="Documentation
     *      on the foreign keys as a comma-separated list if using an indirection table"
     * @doc.param                   name="indirection-table" optional="true" description="The name of the indirection
     *      table for m:n associations"
     * @doc.param                   name="indirection-table-documentation" optional="true" description="Documentation
     *      on the indirection table"
     * @doc.param                   name="indirection-table-primarykeys" optional="true" description="Whether the
     *      fields referencing the collection and element classes, should also be primarykeys"
     * @doc.param                   name="otm-dependent" optional="true" description="Whether the collection is dependent on otm"
     * @doc.param                   name="proxy" optional="true" description="Whether to use a proxy for the collection"
     * @doc.param                   name="proxy-prefetching-limit" optional="true" description="Specifies the amount of objects to prefetch"
     * @doc.param                   name="query-customizer" optional="true" description="The query customizer for this collection"
     * @doc.param                   name="query-customizer-attributes" optional="true" description="Attributes for the query customizer"
     * @doc.param                   name="refresh" optional="true" description="Whether to automatically refresh the
     *      collection"
     * @doc.param                   name="remote-foreignkey" optional="true" description="The name of the
     *      foreign key columns pointing to the elements if using an indirection table"
     * @doc.param                   name="remote-foreignkey-documentation" optional="true" description="Documentation
     *      on the remote foreign keys as a comma-separated list if using an indirection table"
     */
    public void processCollection(String template, Properties attributes) throws XDocletException
    {
        String                  name    = OjbMemberTagsHandler.getMemberName();
        CollectionDescriptorDef collDef = _curClassDef.getCollection(name);
        String                  attrName;

        if (collDef == null)
        {
            collDef = new CollectionDescriptorDef(name);
            _curClassDef.addCollection(collDef);
        }
        LogHelper.debug(false, OjbTagsHandler.class, "processCollection", "  Processing collection "+collDef.getName());

        for (Enumeration attrNames = attributes.propertyNames(); attrNames.hasMoreElements(); )
        {
            attrName = (String)attrNames.nextElement();
            collDef.setProperty(attrName, attributes.getProperty(attrName));
        }
        if (OjbMemberTagsHandler.getMemberDimension() > 0)
        {
            // we store the array-element type for later use
            collDef.setProperty(PropertyHelper.OJB_PROPERTY_ARRAY_ELEMENT_CLASS_REF,
                                OjbMemberTagsHandler.getMemberType().getQualifiedName());
        }
        else
        {    
            collDef.setProperty(PropertyHelper.OJB_PROPERTY_VARIABLE_TYPE,
                                OjbMemberTagsHandler.getMemberType().getQualifiedName());
        }

        _curCollectionDef = collDef;
        generate(template);
        _curCollectionDef = null;
    }

    /**
     * Processes the template for all collection definitions of the current class definition.
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  if an error occurs
     * @doc.tag                     type="block"
     */
    public void forAllCollectionDefinitions(String template, Properties attributes) throws XDocletException
    {
        for (Iterator it = _curClassDef.getCollections(); it.hasNext(); )
        {
            _curCollectionDef = (CollectionDescriptorDef)it.next();
            if (!isFeatureIgnored(LEVEL_COLLECTION) &&
                !_curCollectionDef.getBooleanProperty(PropertyHelper.OJB_PROPERTY_IGNORE, false))
            {
                generate(template);
            }
        }
        _curCollectionDef = null;
    }

    /**
     * Addes the current member as a nested object.
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  If an error occurs
     * @doc.tag                     type="content"
     */
    public String processNested(Properties attributes) throws XDocletException
    {
        String    name      = OjbMemberTagsHandler.getMemberName();
        XClass    type      = OjbMemberTagsHandler.getMemberType();
        int       dim       = OjbMemberTagsHandler.getMemberDimension();
        NestedDef nestedDef = _curClassDef.getNested(name);

        if (type == null)
        {
            throw new XDocletException(Translator.getString(XDocletModulesOjbMessages.class,
                                          XDocletModulesOjbMessages.COULD_NOT_DETERMINE_TYPE_OF_MEMBER,
                                          new String[]{name}));
        }
        if (dim > 0)
        {
            throw new XDocletException(Translator.getString(XDocletModulesOjbMessages.class,
                                          XDocletModulesOjbMessages.MEMBER_CANNOT_BE_NESTED,
                                          new String[]{name, _curClassDef.getName()}));
        }

        ClassDescriptorDef nestedTypeDef = _model.getClass(type.getQualifiedName());

        if (nestedTypeDef == null)
        {
            throw new XDocletException(Translator.getString(XDocletModulesOjbMessages.class,
                                          XDocletModulesOjbMessages.COULD_NOT_DETERMINE_TYPE_OF_MEMBER,
                                          new String[]{name}));
        }
        if (nestedDef == null)
        {
            nestedDef = new NestedDef(name, nestedTypeDef);
            _curClassDef.addNested(nestedDef);
        }
        LogHelper.debug(false, OjbTagsHandler.class, "processNested", "  Processing nested object "+nestedDef.getName()+" of type "+nestedTypeDef.getName());

        String attrName;
        
        for (Enumeration attrNames = attributes.propertyNames(); attrNames.hasMoreElements(); )
        {
            attrName = (String)attrNames.nextElement();
            nestedDef.setProperty(attrName, attributes.getProperty(attrName));
        }
        return "";
    }
    
    // Modifications
    
    /**
     * Processes a modification tag containing changes to properties of an inherited field/reference/collection.
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  If an error occurs
     * @doc.tag                     type="content"
     * @doc.param                   name="attributes" optional="true" description="Attributes of the field as name-value pairs 'name=value',
     *      separated by commas"
     * @doc.param                   name="autoincrement" optional="true" description="Whether the field is
     *      auto-incremented" values="true,false"
     * @doc.param                   name="auto-delete" optional="true" description="Whether to automatically delete the
     *      referenced object/the collection on object deletion"
     * @doc.param                   name="auto-retrieve" optional="true" description="Whether to automatically retrieve
     *      the referenced object/the collection"
     * @doc.param                   name="auto-update" optional="true" description="Whether to automatically update the
     *      referenced object/the collection"
     * @doc.param                   name="class-ref" optional="true" description="The fully qualified name of the class
     *      owning the referenced field"
     * @doc.param                   name="collection-class" optional="true" description="The type of the collection if not a
     * java.util type or an array"
     * @doc.param                   name="column" optional="true" description="The column for the field"
     * @doc.param                   name="column-documentation" optional="true" description="Documentation on the column"
     * @doc.param                   name="conversion" optional="true" description="The fully qualified name of the
     *      conversion for the field"
     * @doc.param                   name="database-foreignkey" optional="true" description="Whether a database foreignkey shall be created"
     *      values="true,false"
     * @doc.param                   name="default-fetch" optional="true" description="The default-fetch setting"
     *      values="true,false"
     * @doc.param                   name="documentation" optional="true" description="Documentation on the field"
     * @doc.param                   name="element-class-ref" optional="true" description="The fully qualified name of
     *      the element type"
     * @doc.param                   name="foreignkey" optional="true" description="The name of the
     *      foreign key (a column when an indirection table is given)"
     * @doc.param                   name="id" optional="true" description="The position of the field in the class
     *      descriptor"
     * @doc.param                   name="ignore" optional="true" description="Whether the feature shall be ignored"
     *      values="true,false"
     * @doc.param                   name="indexed" optional="true" description="Whether the field is indexed"
     *      values="true,false"
     * @doc.param                   name="jdbc-type" optional="true" description="The jdbc type of the column"
     * @doc.param                   name="length" optional="true" description="The length of the column"
     * @doc.param                   name="locking" optional="true" description="Whether the field supports locking"
     *      values="true,false"
     * @doc.param                   name="name" optional="false" description="The name of the inherited field, reference or collection"
     * @doc.param                   name="nullable" optional="true" description="Whether the field is nullable"
     *      values="true,false"
     * @doc.param                   name="precision" optional="true" description="The precision of the column"
     * @doc.param                   name="primarykey" optional="true" description="Whether the field is a primarykey"
     *      values="true,false"
     * @doc.param                   name="proxy" optional="true" description="Whether to use a proxy for the reference/collection"
     * @doc.param                   name="query-customizer" optional="true" description="The query customizer for the collection"
     * @doc.param                   name="query-customizer-attributes" optional="true" description="Attributes for the query customizer for the collection"
     * @doc.param                   name="refresh" optional="true" description="Whether to automatically refresh the
     *      reference/collection"
     * @doc.param                   name="scale" optional="true" description="The scale of the column"
     * @doc.param                   name="sequence-name" optional="true" description="The name of the sequence for
     *      incrementing the field"
     * @doc.param                   name="table" optional="true" description="The table of the field (not implemented
     *      yet)"
     */
    public String processModification(Properties attributes) throws XDocletException
    {
        String     name = attributes.getProperty(ATTRIBUTE_NAME);
        Properties mods = _curClassDef.getModification(name);
        String     key;
        String     value;

        if (mods == null)
        {
            mods = new Properties();
            _curClassDef.addModification(name, mods);
        }

        attributes.remove(ATTRIBUTE_NAME);
        for (Enumeration en = attributes.keys(); en.hasMoreElements();)
        {
            key   = (String)en.nextElement();
            value = attributes.getProperty(key);
            mods.setProperty(key, value);
        }
        return "";
    }

    /**
     * Processes a modification tag containing changes to properties of an nested field/reference/collection.
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  If an error occurs
     * @doc.tag                     type="content"
     * @doc.param                   name="attributes" optional="true" description="Attributes of the field as name-value pairs 'name=value',
     *      separated by commas"
     * @doc.param                   name="autoincrement" optional="true" description="Whether the field is
     *      auto-incremented" values="true,false"
     * @doc.param                   name="auto-delete" optional="true" description="Whether to automatically delete the
     *      referenced object/the collection on object deletion"
     * @doc.param                   name="auto-retrieve" optional="true" description="Whether to automatically retrieve
     *      the referenced object/the collection"
     * @doc.param                   name="auto-update" optional="true" description="Whether to automatically update the
     *      referenced object/the collection"
     * @doc.param                   name="class-ref" optional="true" description="The fully qualified name of the class
     *      owning the referenced field"
     * @doc.param                   name="collection-class" optional="true" description="The type of the collection if not a
     * java.util type or an array"
     * @doc.param                   name="column" optional="true" description="The column for the field"
     * @doc.param                   name="column-documentation" optional="true" description="Documentation on the column"
     * @doc.param                   name="conversion" optional="true" description="The fully qualified name of the
     *      conversion for the field"
     * @doc.param                   name="database-foreignkey" optional="true" description="Whether a database foreignkey shall be created"
     *      values="true,false"
     * @doc.param                   name="default-fetch" optional="true" description="The default-fetch setting"
     *      values="true,false"
     * @doc.param                   name="documentation" optional="true" description="Documentation on the field"
     * @doc.param                   name="element-class-ref" optional="true" description="The fully qualified name of
     *      the element type"
     * @doc.param                   name="foreignkey" optional="true" description="The name of the
     *      foreign key (a column when an indirection table is given)"
     * @doc.param                   name="id" optional="true" description="The position of the field in the class
     *      descriptor"
     * @doc.param                   name="ignore" optional="true" description="Whether the feature shall be ignored"
     *      values="true,false"
     * @doc.param                   name="indexed" optional="true" description="Whether the field is indexed"
     *      values="true,false"
     * @doc.param                   name="jdbc-type" optional="true" description="The jdbc type of the column"
     * @doc.param                   name="length" optional="true" description="The length of the column"
     * @doc.param                   name="locking" optional="true" description="Whether the field supports locking"
     *      values="true,false"
     * @doc.param                   name="name" optional="false" description="The name of the inherited field, reference or collection"
     * @doc.param                   name="nullable" optional="true" description="Whether the field is nullable"
     *      values="true,false"
     * @doc.param                   name="precision" optional="true" description="The precision of the column"
     * @doc.param                   name="primarykey" optional="true" description="Whether the field is a primarykey"
     *      values="true,false"
     * @doc.param                   name="proxy" optional="true" description="Whether to use a proxy for the reference/collection"
     * @doc.param                   name="query-customizer" optional="true" description="The query customizer for the collection"
     * @doc.param                   name="query-customizer-attributes" optional="true" description="Attributes for the query customizer for the collection"
     * @doc.param                   name="refresh" optional="true" description="Whether to automatically refresh the
     *      reference/collection"
     * @doc.param                   name="scale" optional="true" description="The scale of the column"
     * @doc.param                   name="sequence-name" optional="true" description="The name of the sequence for
     *      incrementing the field"
     * @doc.param                   name="table" optional="true" description="The table of the field (not implemented
     *      yet)"
     */
    public String processNestedModification(Properties attributes) throws XDocletException
    {
        String     prefix = OjbMemberTagsHandler.getMemberName() + "::";
        String     name   = prefix + attributes.getProperty(ATTRIBUTE_NAME);
        Properties mods   = _curClassDef.getModification(name);
        String     key;
        String     value;

        if (mods == null)
        {
            mods = new Properties();
            _curClassDef.addModification(name, mods);
        }

        attributes.remove(ATTRIBUTE_NAME);
        for (Enumeration en = attributes.keys(); en.hasMoreElements();)
        {
            key   = (String)en.nextElement();
            value = attributes.getProperty(key);
            mods.setProperty(key, value);
        }
        return "";
    }

    // Table related
    
    /**
     * Generates a torque schema for the model.
     *
     * @param attributes            The attributes of the tag
     * @return                      The property value
     * @exception XDocletException  If an error occurs
     * @doc.tag                     type="content"
     */
    public String createTorqueSchema(Properties attributes) throws XDocletException
    {
        String dbName = (String)getDocletContext().getConfigParam(CONFIG_PARAM_DATABASENAME);

        _torqueModel = new TorqueModelDef(dbName, _model);
        return "";
    }

    /**
     * Processes the template for all table definitions in the torque model.
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  if an error occurs
     * @doc.tag                     type="block"
     */
    public void forAllTables(String template, Properties attributes) throws XDocletException
    {
        for (Iterator it = _torqueModel.getTables(); it.hasNext(); )
        {
            _curTableDef = (TableDef)it.next();
            generate(template);
        }
        _curTableDef = null;
    }

    /**
     * Processes the template for all column definitions of the current table.
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  if an error occurs
     * @doc.tag                     type="block"
     */
    public void forAllColumns(String template, Properties attributes) throws XDocletException
    {
        for (Iterator it = _curTableDef.getColumns(); it.hasNext(); )
        {
            _curColumnDef = (ColumnDef)it.next();
            generate(template);
        }
        _curColumnDef = null;
    }

    /**
     * Processes the template for all foreignkeys of the current table.
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  if an error occurs
     * @doc.tag                     type="block"
     */
    public void forAllForeignkeys(String template, Properties attributes) throws XDocletException
    {
        for (Iterator it = _curTableDef.getForeignkeys(); it.hasNext(); )
        {
            _curForeignkeyDef = (ForeignkeyDef)it.next();
            generate(template);
        }
        _curForeignkeyDef = null;
    }

    /**
     * Processes the template for all column pairs of the current foreignkey.
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  if an error occurs
     * @doc.tag                     type="block"
     */
    public void forAllForeignkeyColumnPairs(String template, Properties attributes) throws XDocletException
    {
        for (int idx = 0; idx < _curForeignkeyDef.getNumColumnPairs(); idx++)
        {
            _curPairLeft  = _curForeignkeyDef.getLocalColumn(idx);
            _curPairRight = _curForeignkeyDef.getRemoteColumn(idx);
            generate(template);
        }
        _curPairLeft  = null;
        _curPairRight = null;
    }

    /**
     * Processes the template for all indices of the current table.
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  if an error occurs
     * @doc.tag                     type="block"
     * @doc.param                   name="unique" optional="true" description="Whether to process the unique indices or not"
     *      values="true,false"
     */
    public void forAllIndices(String template, Properties attributes) throws XDocletException
    {
        boolean processUnique = TypeConversionUtil.stringToBoolean(attributes.getProperty(ATTRIBUTE_UNIQUE), false);

        // first the default index
        _curIndexDef = _curTableDef.getIndex(null);
        if ((_curIndexDef != null) && (processUnique == _curIndexDef.isUnique()))
        {
            generate(template);
        }
        for (Iterator it = _curTableDef.getIndices(); it.hasNext(); )
        {
            _curIndexDef = (IndexDef)it.next();
            if (!_curIndexDef.isDefault() && (processUnique == _curIndexDef.isUnique()))
            {    
                generate(template);
            }
        }
        _curIndexDef = null;
    }

    /**
     * Processes the template for all columns of the current table index.
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  if an error occurs
     * @doc.tag                     type="block"
     */
    public void forAllIndexColumns(String template, Properties attributes) throws XDocletException
    {
        for (Iterator it = _curIndexDef.getColumns(); it.hasNext(); )
        {
            _curColumnDef = _curTableDef.getColumn((String)it.next());
            generate(template);
        }
        _curColumnDef = null;
    }

    // Other

    /**
     * Returns the name of the current object on the specified level.
     *
     * @param attributes            The attributes of the tag
     * @return                      The property value
     * @exception XDocletException  If an error occurs
     * @doc.tag                     type="content"
     * @doc.param                   name="level" optional="false" description="The level for the current object"
     *      values="class,field,reference,collection"
     */
    public String name(Properties attributes) throws XDocletException
    {
        return getDefForLevel(attributes.getProperty(ATTRIBUTE_LEVEL)).getName();
    }

    /**
     * Processes the template if the current object on the specified level has a non-empty name.
     *
     * @param attributes            The attributes of the tag
     * @return                      The property value
     * @exception XDocletException  If an error occurs
     * @doc.tag                     type="block"
     * @doc.param                   name="level" optional="false" description="The level for the current object"
     *      values="class,field,reference,collection"
     */
    public void ifHasName(String template, Properties attributes) throws XDocletException
    {
        String name =  getDefForLevel(attributes.getProperty(ATTRIBUTE_LEVEL)).getName();

        if ((name != null) && (name.length() > 0))
        {
            generate(template);
        }
    }

    /**
     * Determines whether the current object on the specified level has a specific property, and if so, processes the
     * template
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  If an error occurs
     * @doc.tag                     type="block"
     * @doc.param                   name="level" optional="false" description="The level for the current object"
     *      values="class,field,reference,collection"
     * @doc.param                   name="name" optional="false" description="The name of the property"
     */
    public void ifHasProperty(String template, Properties attributes) throws XDocletException
    {
        String value = getPropertyValue(attributes.getProperty(ATTRIBUTE_LEVEL), attributes.getProperty(ATTRIBUTE_NAME));

        if (value != null)
        {
            generate(template);
        }
    }

    /**
     * Determines whether the current object on the specified level does not have a specific property, and if so,
     * processes the template
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  If an error occurs
     * @doc.tag                     type="block"
     * @doc.param                   name="level" optional="false" description="The level for the current object"
     *      values="class,field,reference,collection"
     * @doc.param                   name="name" optional="false" description="The name of the property"
     */
    public void ifDoesntHaveProperty(String template, Properties attributes) throws XDocletException
    {
        String value = getPropertyValue(attributes.getProperty(ATTRIBUTE_LEVEL), attributes.getProperty(ATTRIBUTE_NAME));

        if (value == null)
        {
            generate(template);
        }
    }

    /**
     * Returns the value of a property of the current object on the specified level.
     *
     * @param attributes            The attributes of the tag
     * @return                      The property value
     * @exception XDocletException  If an error occurs
     * @doc.tag                     type="content"
     * @doc.param                   name="level" optional="false" description="The level for the current object"
     *      values="class,field,reference,collection"
     * @doc.param                   name="name" optional="false" description="The name of the property"
     * @doc.param                   name="default" optional="true" description="A default value to use if the property
     *      is not defined"
     */
    public String propertyValue(Properties attributes) throws XDocletException
    {
        String value = getPropertyValue(attributes.getProperty(ATTRIBUTE_LEVEL), attributes.getProperty(ATTRIBUTE_NAME));

        if (value == null)
        {
            value = attributes.getProperty(ATTRIBUTE_DEFAULT);
        }
        return value;
    }

    /**
     * Processes the template if the property value of the current object on the specified level equals the given value.
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  If an error occurs
     * @doc.tag                     type="block"
     * @doc.param                   name="level" optional="false" description="The level for the current object"
     *      values="class,field,reference,collection"
     * @doc.param                   name="name" optional="false" description="The name of the property"
     * @doc.param                   name="value" optional="false" description="The value to check for"
     * @doc.param                   name="default" optional="true" description="A default value to use if the property
     *      is not defined"
     */
    public void ifPropertyValueEquals(String template, Properties attributes) throws XDocletException
    {
        String value    = getPropertyValue(attributes.getProperty(ATTRIBUTE_LEVEL), attributes.getProperty(ATTRIBUTE_NAME));
        String expected = attributes.getProperty(ATTRIBUTE_VALUE);

        if (value == null)
        {
            value = attributes.getProperty(ATTRIBUTE_DEFAULT);
        }
        if (expected.equals(value))
        {
            generate(template);
        }
    }

    /**
     * Processes the template if the property value of the current object on the specified level does not equal the given value.
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  If an error occurs
     * @doc.tag                     type="block"
     * @doc.param                   name="level" optional="false" description="The level for the current object"
     *      values="class,field,reference,collection"
     * @doc.param                   name="name" optional="false" description="The name of the property"
     * @doc.param                   name="value" optional="false" description="The value to check for"
     * @doc.param                   name="default" optional="true" description="A default value to use if the property
     *      is not defined"
     */
    public void ifPropertyValueDoesntEqual(String template, Properties attributes) throws XDocletException
    {
        String value    = getPropertyValue(attributes.getProperty(ATTRIBUTE_LEVEL), attributes.getProperty(ATTRIBUTE_NAME));
        String expected = attributes.getProperty(ATTRIBUTE_VALUE);

        if (value == null)
        {
            value = attributes.getProperty(ATTRIBUTE_DEFAULT);
        }
        if (!expected.equals(value))
        {
            generate(template);
        }
    }

    /**
     * Processes the template for the comma-separated value pairs in an attribute of the current object on the specified level.
     *
     * @param template              The template
     * @param attributes            The attributes of the tag
     * @exception XDocletException  if an error occurs
     * @doc.tag                     type="block"
     * @doc.param                   name="level" optional="false" description="The level for the current object"
     *      values="class,field,reference,collection"
     * @doc.param                   name="name" optional="true" description="The name of the attribute containg attributes (defaults to 'attributes')"
     * @doc.param                   name="default-right" optional="true" description="The default right value if none is given (defaults to empty value)"
     */
    public void forAllValuePairs(String template, Properties attributes) throws XDocletException
    {
        String name           = attributes.getProperty(ATTRIBUTE_NAME, "attributes");
        String defaultValue   = attributes.getProperty(ATTRIBUTE_DEFAULT_RIGHT, "");
        String attributePairs = getPropertyValue(attributes.getProperty(ATTRIBUTE_LEVEL), name);

        if ((attributePairs == null) || (attributePairs.length() == 0))
        {
            return;
        }

        String token;
        int    pos;

        for (CommaListIterator it = new CommaListIterator(attributePairs); it.hasNext();)
        {
            token = it.getNext();
            pos   = token.indexOf('=');
            if (pos >= 0)
            {
                _curPairLeft  = token.substring(0, pos);
                _curPairRight = (pos < token.length() - 1 ? token.substring(pos + 1) : defaultValue);
            }
            else
            {
                _curPairLeft  = token;
                _curPairRight = defaultValue;
            }
            if (_curPairLeft.length() > 0)
            {
                generate(template);
            }
        }
        _curPairLeft = null;
        _curPairRight = null;
    }

    /**
     * Returns the left part of the current pair.
     *
     * @param attributes            The attributes of the tag
     * @return                      The property value
     * @exception XDocletException  If an error occurs
     * @doc.tag                     type="content"
     */
    public String pairLeft(Properties attributes) throws XDocletException
    {
        return _curPairLeft;
    }

    /**
     * Returns the right part of the current pair.
     *
     * @param attributes            The attributes of the tag
     * @return                      The property value
     * @exception XDocletException  If an error occurs
     * @doc.tag                     type="content"
     */
    public String pairRight(Properties attributes) throws XDocletException
    {
        return _curPairRight;
    }

    //
    // Helper methods
    //

    // Class-related and Modification-related
    
    /**
     * Makes sure that there is a class definition for the given qualified name, and returns it.
     *
     * @param original The XDoclet class object 
     * @return The class definition
     */
    private ClassDescriptorDef ensureClassDef(XClass original)
    {
        String             name     = original.getQualifiedName();
        ClassDescriptorDef classDef = _model.getClass(name);

        if (classDef == null)
        {
            classDef = new ClassDescriptorDef(original);
            _model.addClass(classDef);
        }
        return classDef;
    }

    /**
     * Adds all direct subtypes to the given list.
     * 
     * @param type     The type for which to determine the direct subtypes
     * @param subTypes The list to receive the subtypes
     */
    private void addDirectSubTypes(XClass type, ArrayList subTypes)
    {
        if (type.isInterface())
        {
            if (type.getExtendingInterfaces() != null)
            {
                subTypes.addAll(type.getExtendingInterfaces());
            }
            // we have to traverse the implementing classes as these array contains all classes that
            // implement the interface, not only those who have an "implement" declaration
            // note that for whatever reason the declared interfaces are not exported via the XClass interface
            // so we have to get them via the underlying class which is hopefully a subclass of AbstractClass
            if (type.getImplementingClasses() != null)
            {
                Collection declaredInterfaces = null;
                XClass     subType;

                for (Iterator it = type.getImplementingClasses().iterator(); it.hasNext(); )
                {
                    subType = (XClass)it.next();
                    if (subType instanceof AbstractClass)
                    {
                        declaredInterfaces = ((AbstractClass)subType).getDeclaredInterfaces();
                        if ((declaredInterfaces != null) && declaredInterfaces.contains(type))
                        {
                            subTypes.add(subType);
                        }
                    }
                    else
                    {
                        // Otherwise we have to live with the bug
                        subTypes.add(subType);
                    }
                }
            }
        }
        else
        {
            subTypes.addAll(type.getDirectSubclasses());
        }
    }


    // Field-related

    /**
     * Determines the default mapping for the type of the current member. If the current member is a field, the type of
     * the field is used. If the current member is an accessor, then the return type (get/is) or parameter type (set) is
     * used.
     *
     * @return                      The jdbc type
     * @exception XDocletException  If an error occurs
     */
    public static String getDefaultJdbcTypeForCurrentMember() throws XDocletException
    {
        if (OjbMemberTagsHandler.getMemberDimension() > 0)
        {
            return JdbcTypeHelper.JDBC_DEFAULT_TYPE_FOR_ARRAY;
        }

        String type = OjbMemberTagsHandler.getMemberType().getQualifiedName();

        return JdbcTypeHelper.getDefaultJdbcTypeFor(type);
    }

    /**
     * Determines the default conversion for the type of the current member. If the current member is a field, the type of
     * the field is used. If the current member is an accessor, then the return type (get/is) or parameter type (set) is
     * used.
     *
     * @return                      The jdbc type
     * @exception XDocletException  If an error occurs
     */
    private static String getDefaultJdbcConversionForCurrentMember() throws XDocletException
    {
        if (OjbMemberTagsHandler.getMemberDimension() > 0)
        {
            return JdbcTypeHelper.JDBC_DEFAULT_CONVERSION;
        }

        String type = OjbMemberTagsHandler.getMemberType().getQualifiedName();

        return JdbcTypeHelper.getDefaultConversionFor(type);
    }

    /**
     * Searches the type and its sub types for the nearest ojb-persistent type and returns its name.
     *
     * @param type  The type to search
     * @return      The qualified name of the found type or <code>null</code> if no type has been found
     */
    private String searchForPersistentSubType(XClass type)
    {
        ArrayList queue = new ArrayList();
        XClass    subType;

        queue.add(type);
        while (!queue.isEmpty())
        {
            subType = (XClass)queue.get(0);
            queue.remove(0);
            if (_model.hasClass(subType.getQualifiedName()))
            {
                return subType.getQualifiedName();
            }
            addDirectSubTypes(subType, queue);
        }
        return null;
    }

    
    /**
     * Returns the current definition on the indicated level.
     *
     * @param level  The level
     * @return       The definition
     */
    private DefBase getDefForLevel(String level)
    {
        if (LEVEL_CLASS.equals(level))
        {
            return _curClassDef;
        }
        else if (LEVEL_FIELD.equals(level))
        {
            return _curFieldDef;
        }
        else if (LEVEL_REFERENCE.equals(level))
        {
            return _curReferenceDef;
        }
        else if (LEVEL_COLLECTION.equals(level))
        {
            return _curCollectionDef;
        }
        else if (LEVEL_OBJECT_CACHE.equals(level))
        {
            return _curObjectCacheDef;
        }
        else if (LEVEL_INDEX_DESC.equals(level))
        {
            return _curIndexDescriptorDef;
        }
        else if (LEVEL_TABLE.equals(level))
        {
            return _curTableDef;
        }
        else if (LEVEL_COLUMN.equals(level))
        {
            return _curColumnDef;
        }
        else if (LEVEL_FOREIGNKEY.equals(level))
        {
            return _curForeignkeyDef;
        }
        else if (LEVEL_INDEX.equals(level))
        {
            return _curIndexDef;
        }
        else if (LEVEL_PROCEDURE.equals(level))
        {
            return _curProcedureDef;
        }
        else if (LEVEL_PROCEDURE_ARGUMENT.equals(level))
        {
            return _curProcedureArgumentDef;
        }
        else
        {
            return null;
        }
    }

    /**
     * Determines whether the current feature on the given level is ignored.
     * 
     * @param level The level
     * @return <code>true</code> if this feature is ignored
     */
    private boolean isFeatureIgnored(String level)
    {
        return getDefForLevel(level).getBooleanProperty(PropertyHelper.OJB_PROPERTY_IGNORE, false);
    }

    /**
     * Returns the value of the indicated property of the current object on the specified level.
     *
     * @param level  The level
     * @param name   The name of the property
     * @return       The property value
     */
    private String getPropertyValue(String level, String name)
    {
        return getDefForLevel(level).getProperty(name);
    }

}
