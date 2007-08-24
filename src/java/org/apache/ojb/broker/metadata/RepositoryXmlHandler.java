package org.apache.ojb.broker.metadata;

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

import org.apache.ojb.broker.accesslayer.QueryCustomizer;
import org.apache.ojb.broker.locking.IsolationLevels;
import org.apache.ojb.broker.locking.LockHelper;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentField;
import org.apache.ojb.broker.metadata.fieldaccess.PersistentFieldFactory;
import org.apache.ojb.broker.util.ClassHelper;
import org.apache.ojb.broker.util.logging.Logger;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * The handler catches Parsing events raised by the xml-parser
 * and builds up the DescriptorRepository that is used within the
 * OJB PersistenceBroker System.
 * <p>
 * TODO: Reading of metadata are split in two classes {@link RepositoryXmlHandler} and
 * {@link ConnectionDescriptorXmlHandler}. Thus we should only read relevant tags in this
 * classes. In further versions we should split repository.dtd in two parts, one for connetion
 * metadata, one for pc object metadata.
 * </p>
 * @author <a href="mailto:thma@apache.org">Thomas Mahler<a>
 * @author Jakob Br?uchi
 * @version $Id: RepositoryXmlHandler.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class RepositoryXmlHandler
        extends DefaultHandler
        implements RepositoryElements, IsolationLevels
{
    private Logger logger = LoggerFactory.getLogger(RepositoryXmlHandler.class);

    private DescriptorRepository m_repository;
    private ClassDescriptor m_CurrentCLD;
    private ProcedureDescriptor m_CurrentProcedure;
    private FieldDescriptor m_CurrentFLD;
    private ObjectReferenceDescriptor m_CurrentORD;
    private CollectionDescriptor m_CurrentCOD;
    private IndexDescriptor m_CurrentIndexDescriptor;
    private String m_CurrentString;
    /** holds custom attributes */
    private AttributeContainer m_CurrentAttrContainer;
    /** the default proxy prefetching limit*/
    private int defProxyPrefetchingLimit = 50;
    /**
     * Allows not to specify field id
     */
    private int m_lastId;

    /**
     * All known xml tags are kept in this table.
     * The tags table allows lookup from literal to id
     * and from id to literal.
     */
    private RepositoryTags tags = RepositoryTags.getInstance();

    /**
     * returns the XmlCapable id associated with the literal.
     * OJB maintains a RepositoryTags table that provides
     * a mapping from xml-tags to XmlCapable ids.
     * @param literal the literal to lookup
     * @return the int value representing the XmlCapable
     *
     * @throws MetadataException if no literal was found in tags mapping
     */
    private int getLiteralId(String literal) throws MetadataException
    {
        //logger.debug("lookup: " + literal);
        try
        {
            return tags.getIdByTag(literal);
        }
        catch (NullPointerException t)
        {
            throw new MetadataException("Found Unknown literal '" + literal +
                    "' in the repository file. Check repository file or/and RepositoryTags.java", t);
        }

    }

    /**
     * build a handler that fills the given repository
     * from an XML file.
     */
    public RepositoryXmlHandler(DescriptorRepository dr)
    {
        if (dr != null)
        {
            m_repository = dr;
        }
        else
        {
            throw new MetadataException("Given DescriptorRepository argument was null");
        }
    }

    /**
     * startDocument callback, nothing to do here.
     */
    public void startDocument()
    {
        logger.debug("startDoc");
    }

    /**
     * endDocument callback, nothing to do here.
     */
    public void endDocument()
    {
        // arminw: no longer needed since SuperReferenceDescriptor was used
        // AnonymousPersistentFieldHelper.computeInheritedPersistentFields(m_repository);
        logger.debug("endDoc");
    }

    /**
     * startElement callback.
     * Only some Elements need special start operations.
     * @throws MetadataException indicating mapping errors
     */
    public void startElement(String uri, String name, String qName, Attributes atts)
    {
        boolean isDebug = logger.isDebugEnabled();

        m_CurrentString = null;
        try
        {
            switch (getLiteralId(qName))
            {
                case MAPPING_REPOSITORY:
                    {
                        if (isDebug) logger.debug(" > " + tags.getTagById(MAPPING_REPOSITORY));
                        this.m_CurrentAttrContainer = m_repository;

                        String defIso = atts.getValue(tags.getTagById(ISOLATION_LEVEL));
                        this.m_repository.setDefaultIsolationLevel(LockHelper.getIsolationLevelFor(defIso));
                        if (isDebug) logger.debug("     " + tags.getTagById(ISOLATION_LEVEL) + ": " + defIso);


                        String proxyPrefetchingLimit = atts.getValue(tags.getTagById(PROXY_PREFETCHING_LIMIT));
                        if (isDebug) logger.debug("     " + tags.getTagById(PROXY_PREFETCHING_LIMIT) + ": " + proxyPrefetchingLimit);
                        if (proxyPrefetchingLimit != null)
                        {
                            defProxyPrefetchingLimit = Integer.parseInt(proxyPrefetchingLimit);
                        }

                        // check repository version:
                        String version = atts.getValue(tags.getTagById(REPOSITORY_VERSION));
                        if (DescriptorRepository.getVersion().equals(version))
                        {
                            if (isDebug) logger.debug("     " + tags.getTagById(REPOSITORY_VERSION) + ": " + version);
                        }
                        else
                        {
                            throw new MetadataException("Repository version does not match. expected " +
                                    DescriptorRepository.getVersion() + " but found: " +
                                    version+". Please update your repository.dtd and your repository.xml"+
                                    " version attribute entry");
                        }
                        break;
                    }
                case CLASS_DESCRIPTOR:
                    {
                        if (isDebug) logger.debug("  > " + tags.getTagById(CLASS_DESCRIPTOR));
                        m_CurrentCLD = new ClassDescriptor(m_repository);

                        // prepare for custom attributes
                        this.m_CurrentAttrContainer = this.m_CurrentCLD;

                        // set isolation-level attribute
                        String isoLevel = atts.getValue(tags.getTagById(ISOLATION_LEVEL));
                        if (isDebug) logger.debug("     " + tags.getTagById(ISOLATION_LEVEL) + ": " + isoLevel);
                        /*
                        arminw:
                        only when an isolation-level is set in CLD, set it.
                        Else the CLD use the default iso-level defined in the repository
                        */
                        if(checkString(isoLevel)) m_CurrentCLD.setIsolationLevel(LockHelper.getIsolationLevelFor(isoLevel));

                        // set class attribute
                        String classname = atts.getValue(tags.getTagById(CLASS_NAME));
                        if (isDebug) logger.debug("     " + tags.getTagById(CLASS_NAME) + ": " + classname);
                        try
                        {
                            m_CurrentCLD.setClassOfObject(ClassHelper.getClass(classname));
                        }
                        catch (ClassNotFoundException e)
                        {
                            m_CurrentCLD = null;
                            throw new MetadataException("Class "+classname+" could not be found"
                                    +" in the classpath. This could cause unexpected behaviour of OJB,"+
                                    " please remove or comment out this class descriptor" +
                                    " in the repository.xml file.", e);
                        }

                        // set schema attribute
                        String schema = atts.getValue(tags.getTagById(SCHEMA_NAME));
                        if (schema != null)
                        {
                            if (isDebug) logger.debug("     " + tags.getTagById(SCHEMA_NAME) + ": " + schema);
                            m_CurrentCLD.setSchema(schema);
                        }

                        // set proxy attribute
                        String proxy = atts.getValue(tags.getTagById(CLASS_PROXY));
                        if (isDebug) logger.debug("     " + tags.getTagById(CLASS_PROXY) + ": " + proxy);
                        if (checkString(proxy))
                        {
                            if (proxy.equalsIgnoreCase(ClassDescriptor.DYNAMIC_STR))
                            {
                                m_CurrentCLD.setProxyClassName(ClassDescriptor.DYNAMIC_STR);
                            }
                            else
                            {
                                m_CurrentCLD.setProxyClassName(proxy);
                            }
                        }

                        // set proxyPrefetchingLimit attribute
                        String proxyPrefetchingLimit = atts.getValue(tags.getTagById(PROXY_PREFETCHING_LIMIT));
                        if (isDebug) logger.debug("     " + tags.getTagById(PROXY_PREFETCHING_LIMIT) + ": " + proxyPrefetchingLimit);
                        if (proxyPrefetchingLimit == null)
                        {
                            m_CurrentCLD.setProxyPrefetchingLimit(defProxyPrefetchingLimit);
                        }
                        else
                        {
                            m_CurrentCLD.setProxyPrefetchingLimit(Integer.parseInt(proxyPrefetchingLimit));
                        }

                        // set table attribute:
                        String table = atts.getValue(tags.getTagById(TABLE_NAME));
                        if (isDebug) logger.debug("     " + tags.getTagById(TABLE_NAME) + ": " + table);
                        m_CurrentCLD.setTableName(table);
                        if (table == null)
                        {
                            m_CurrentCLD.setIsInterface(true);
                        }

                        // set row-reader attribute
                        String rowreader = atts.getValue(tags.getTagById(ROW_READER));
                        if (isDebug) logger.debug("     " + tags.getTagById(ROW_READER) + ": " + rowreader);
                        if (rowreader != null)
                        {
                            m_CurrentCLD.setRowReader(rowreader);
                        }

                        // set if extends
// arminw: TODO: this feature doesn't work, remove this stuff?
                        String extendsAtt = atts.getValue(tags.getTagById(EXTENDS));
                        if (isDebug) logger.debug("     " + tags.getTagById(EXTENDS) + ": " + extendsAtt);
                        if (checkString(extendsAtt))
                        {
                            m_CurrentCLD.setSuperClass(extendsAtt);
                        }

                        //set accept-locks attribute
                        String acceptLocks = atts.getValue(tags.getTagById(ACCEPT_LOCKS));
                        if (acceptLocks==null)
                            acceptLocks="true"; // default is true
                        logger.debug("     " + tags.getTagById(ACCEPT_LOCKS) + ": " + acceptLocks);
                        if (isDebug) logger.debug("     " + tags.getTagById(ACCEPT_LOCKS) + ": " + acceptLocks);
                        boolean b = (Boolean.valueOf(acceptLocks)).booleanValue();
                        m_CurrentCLD.setAcceptLocks(b);

                        //set initializationMethod attribute
                        String initializationMethod = atts.getValue(tags.getTagById(INITIALIZATION_METHOD));
                        if (isDebug) logger.debug("     " + tags.getTagById(INITIALIZATION_METHOD) + ": " + initializationMethod);
                        if (initializationMethod != null)
                        {
                            m_CurrentCLD.setInitializationMethod(initializationMethod);
                        }

                        // set factoryClass attribute
                        String factoryClass = atts.getValue(tags.getTagById(FACTORY_CLASS));
                        if (isDebug)
                            logger.debug("     " + tags.getTagById(FACTORY_CLASS) + ": " + factoryClass);
                        if (factoryClass != null)
                        {
                            m_CurrentCLD.setFactoryClass(factoryClass);
                        }

                        //set factoryMethod attribute
                        String factoryMethod = atts.getValue(tags.getTagById(FACTORY_METHOD));
                        if (isDebug)
                            logger.debug("     " + tags.getTagById(FACTORY_METHOD) + ": " + factoryMethod);
                        if (factoryMethod != null)
                        {
                            m_CurrentCLD.setFactoryMethod(factoryMethod);
                        }

                        // set refresh attribute
                        String refresh = atts.getValue(tags.getTagById(REFRESH));
                        if (isDebug) logger.debug("     " + tags.getTagById(REFRESH) + ": " + refresh);
                        b = (Boolean.valueOf(refresh)).booleanValue();
                        m_CurrentCLD.setAlwaysRefresh(b);

                        // TODO: remove this or make offical feature
                        // persistent field
						String pfClassName = atts.getValue("persistent-field-class");
						if (isDebug) logger.debug("     persistent-field-class: " + pfClassName);
						m_CurrentCLD.setPersistentFieldClassName(pfClassName);

                        // put cld to the metadata repository
                        m_repository.put(classname, m_CurrentCLD);
                        break;
                    }
                case OBJECT_CACHE:
                    {
                        // we only interessted in object-cache tags declared within
                        // an class-descriptor
                        if(m_CurrentCLD != null)
                        {
                            String className = atts.getValue(tags.getTagById(CLASS_NAME));
                            if(checkString(className))
                            {
                                if (isDebug) logger.debug("     > " + tags.getTagById(OBJECT_CACHE));
                                ObjectCacheDescriptor ocd = new ObjectCacheDescriptor();
                                this.m_CurrentAttrContainer = ocd;
                                ocd.setObjectCache(ClassHelper.getClass(className));
                                if(m_CurrentCLD != null)
                                {
                                    m_CurrentCLD.setObjectCacheDescriptor(ocd);
                                }
                                if (isDebug) logger.debug("     " + tags.getTagById(CLASS_NAME) + ": " + className);
                            }
                        }
                        break;
                    }
                case CLASS_EXTENT:
                    {
                        String classname = atts.getValue("class-ref");
                        if (isDebug) logger.debug("     " + tags.getTagById(CLASS_EXTENT) + ": " + classname);
                        m_CurrentCLD.addExtentClass(classname);
                        break;
                    }

                case FIELD_DESCRIPTOR:
                    {
                        if (isDebug) logger.debug("    > " + tags.getTagById(FIELD_DESCRIPTOR));

                        String strId = atts.getValue(tags.getTagById(ID));
                        m_lastId = (strId == null ? m_lastId + 1 : Integer.parseInt(strId));

                        String strAccess = atts.getValue(tags.getTagById(ACCESS));

                        if (RepositoryElements.TAG_ACCESS_ANONYMOUS.equalsIgnoreCase(strAccess))
                        {
                            m_CurrentFLD = new AnonymousFieldDescriptor(m_CurrentCLD, m_lastId);
                        }
                        else
                        {
                            m_CurrentFLD = new FieldDescriptor(m_CurrentCLD, m_lastId);
                        }
                        m_CurrentFLD.setAccess(strAccess);
                        m_CurrentCLD.addFieldDescriptor(m_CurrentFLD);

                        // prepare for custom attributes
                        this.m_CurrentAttrContainer = this.m_CurrentFLD;

                        String fieldName = atts.getValue(tags.getTagById(FIELD_NAME));
                        if (isDebug) logger.debug("     " + tags.getTagById(FIELD_NAME) + ": " + fieldName);

                        if (RepositoryElements.TAG_ACCESS_ANONYMOUS.equalsIgnoreCase(strAccess))
                        {
							AnonymousFieldDescriptor anonymous = (AnonymousFieldDescriptor) m_CurrentFLD;
                            anonymous.setPersistentField(null,fieldName);
                        }
                        else
                        {
                            String classname = m_CurrentCLD.getClassNameOfObject();
							PersistentField pf = PersistentFieldFactory.createPersistentField(m_CurrentCLD.getPersistentFieldClassName(),ClassHelper.getClass(classname),fieldName);
                            m_CurrentFLD.setPersistentField(pf);
                        }

                        String columnName = atts.getValue(tags.getTagById(COLUMN_NAME));
                        if (isDebug) logger.debug("     " + tags.getTagById(COLUMN_NAME) + ": " + columnName);
                        m_CurrentFLD.setColumnName(columnName);

                        String jdbcType = atts.getValue(tags.getTagById(JDBC_TYPE));
                        if (isDebug) logger.debug("     " + tags.getTagById(JDBC_TYPE) + ": " + jdbcType);
                        m_CurrentFLD.setColumnType(jdbcType);

                        String primaryKey = atts.getValue(tags.getTagById(PRIMARY_KEY));
                        if (isDebug) logger.debug("     " + tags.getTagById(PRIMARY_KEY) + ": " + primaryKey);
                        boolean b = (Boolean.valueOf(primaryKey)).booleanValue();
                        m_CurrentFLD.setPrimaryKey(b);

                        String nullable = atts.getValue(tags.getTagById(NULLABLE));
                        if (nullable != null)
                        {
                            if (isDebug) logger.debug("     " + tags.getTagById(NULLABLE) + ": " + nullable);
                            b = !(Boolean.valueOf(nullable)).booleanValue();
                            m_CurrentFLD.setRequired(b);
                        }

                        String indexed = atts.getValue(tags.getTagById(INDEXED));
                        if (isDebug) logger.debug("     " + tags.getTagById(INDEXED) + ": " + indexed);
                        b = (Boolean.valueOf(indexed)).booleanValue();
                        m_CurrentFLD.setIndexed(b);

                        String autoincrement = atts.getValue(tags.getTagById(AUTO_INCREMENT));
                        if (isDebug) logger.debug("     " + tags.getTagById(AUTO_INCREMENT) + ": " + autoincrement);
                        b = (Boolean.valueOf(autoincrement)).booleanValue();
                        m_CurrentFLD.setAutoIncrement(b);

                        String sequenceName = atts.getValue(tags.getTagById(SEQUENCE_NAME));
                        if (isDebug) logger.debug("     " + tags.getTagById(SEQUENCE_NAME) + ": " + sequenceName);
                        m_CurrentFLD.setSequenceName(sequenceName);

                        String locking = atts.getValue(tags.getTagById(LOCKING));
                        if (isDebug) logger.debug("     " + tags.getTagById(LOCKING) + ": " + locking);
                        b = (Boolean.valueOf(locking)).booleanValue();
                        m_CurrentFLD.setLocking(b);

                        String updateLock = atts.getValue(tags.getTagById(UPDATE_LOCK));
                        if (isDebug) logger.debug("     " + tags.getTagById(UPDATE_LOCK) + ": " + updateLock);
                        if(checkString(updateLock))
                        {
                            b = (Boolean.valueOf(updateLock)).booleanValue();
                            m_CurrentFLD.setUpdateLock(b);
                        }

                        String fieldConversion = atts.getValue(tags.getTagById(FIELD_CONVERSION));
                        if (isDebug) logger.debug("     " + tags.getTagById(FIELD_CONVERSION) + ": " + fieldConversion);
                        if (fieldConversion != null)
                        {
                            m_CurrentFLD.setFieldConversionClassName(fieldConversion);
                        }

                        // set length attribute
                        String length = atts.getValue(tags.getTagById(LENGTH));
                        if (length != null)
                        {
                            int i = Integer.parseInt(length);
                            if (isDebug) logger.debug("     " + tags.getTagById(LENGTH) + ": " + i);
                            m_CurrentFLD.setLength(i);
                            m_CurrentFLD.setLengthSpecified(true);
                        }

                        // set precision attribute
                        String precision = atts.getValue(tags.getTagById(PRECISION));
                        if (precision != null)
                        {
                            int i = Integer.parseInt(precision);
                            if (isDebug) logger.debug("     " + tags.getTagById(PRECISION) + ": " + i);
                            m_CurrentFLD.setPrecision(i);
                            m_CurrentFLD.setPrecisionSpecified(true);
                        }

                        // set scale attribute
                        String scale = atts.getValue(tags.getTagById(SCALE));
                        if (scale != null)
                        {
                            int i = Integer.parseInt(scale);
                            if (isDebug) logger.debug("     " + tags.getTagById(SCALE) + ": " + i);
                            m_CurrentFLD.setScale(i);
                            m_CurrentFLD.setScaleSpecified(true);
                        }

                        break;
                    }

                case REFERENCE_DESCRIPTOR:
                    {
                        if (isDebug) logger.debug("    > " + tags.getTagById(REFERENCE_DESCRIPTOR));
                        // set name attribute
                        name = atts.getValue(tags.getTagById(FIELD_NAME));
                        if (isDebug) logger.debug("     " + tags.getTagById(FIELD_NAME) + ": " + name);

                        // set class-ref attribute
                        String classRef = atts.getValue(tags.getTagById(REFERENCED_CLASS));
                        if (isDebug) logger.debug("     " + tags.getTagById(REFERENCED_CLASS) + ": " + classRef);

                        ObjectReferenceDescriptor ord;
                        if (name.equals(TAG_SUPER))
                        {
                            // no longer needed sine SuperReferenceDescriptor was used
//                            checkThis(classRef);
//                            AnonymousObjectReferenceDescriptor aord =
//                                new AnonymousObjectReferenceDescriptor(m_CurrentCLD);
//                            aord.setPersistentField(null, TAG_SUPER);
//                            ord = aord;

                            ord = new SuperReferenceDescriptor(m_CurrentCLD);
                        }
                        else
                        {
                            ord = new ObjectReferenceDescriptor(m_CurrentCLD);
                            PersistentField pf = PersistentFieldFactory.createPersistentField(m_CurrentCLD.getPersistentFieldClassName(),m_CurrentCLD.getClassOfObject(),name);
                            ord.setPersistentField(pf);
                        }
                        m_CurrentORD = ord;

                        // now we add the new descriptor
                        m_CurrentCLD.addObjectReferenceDescriptor(m_CurrentORD);
                        m_CurrentORD.setItemClass(ClassHelper.getClass(classRef));

                        // prepare for custom attributes
                        this.m_CurrentAttrContainer = m_CurrentORD;

                        // set proxy attribute
                        String proxy = atts.getValue(tags.getTagById(PROXY_REFERENCE));
                        if (isDebug) logger.debug("     " + tags.getTagById(PROXY_REFERENCE) + ": " + proxy);
                        boolean b = (Boolean.valueOf(proxy)).booleanValue();
                        m_CurrentORD.setLazy(b);

                        // set proxyPrefetchingLimit attribute
                        String proxyPrefetchingLimit = atts.getValue(tags.getTagById(PROXY_PREFETCHING_LIMIT));
                        if (isDebug) logger.debug("     " + tags.getTagById(PROXY_PREFETCHING_LIMIT) + ": " + proxyPrefetchingLimit);
                        if (proxyPrefetchingLimit == null)
                        {
                            m_CurrentORD.setProxyPrefetchingLimit(defProxyPrefetchingLimit);
                        }
                        else
                        {
                            m_CurrentORD.setProxyPrefetchingLimit(Integer.parseInt(proxyPrefetchingLimit));
                        }

                        // set refresh attribute
                        String refresh = atts.getValue(tags.getTagById(REFRESH));
                        if (isDebug) logger.debug("     " + tags.getTagById(REFRESH) + ": " + refresh);
                        b = (Boolean.valueOf(refresh)).booleanValue();
                        m_CurrentORD.setRefresh(b);

                        // set auto-retrieve attribute
                        String autoRetrieve = atts.getValue(tags.getTagById(AUTO_RETRIEVE));
                        if (isDebug) logger.debug("     " + tags.getTagById(AUTO_RETRIEVE) + ": " + autoRetrieve);
                        b = (Boolean.valueOf(autoRetrieve)).booleanValue();
                        m_CurrentORD.setCascadeRetrieve(b);

                        // set auto-update attribute
                        String autoUpdate = atts.getValue(tags.getTagById(AUTO_UPDATE));
                        if (isDebug) logger.debug("     " + tags.getTagById(AUTO_UPDATE) + ": " + autoUpdate);
                        if(autoUpdate != null)
                        {
                            m_CurrentORD.setCascadingStore(autoUpdate);
                        }

                        //set auto-delete attribute
                        String autoDelete = atts.getValue(tags.getTagById(AUTO_DELETE));
                        if (isDebug) logger.debug("     " + tags.getTagById(AUTO_DELETE) + ": " + autoDelete);

                        if(autoDelete != null)
                        {
                            m_CurrentORD.setCascadingDelete(autoDelete);
                        }

                        //set otm-dependent attribute
                        String otmDependent = atts.getValue(tags.getTagById(OTM_DEPENDENT));
                        if (isDebug) logger.debug("     " + tags.getTagById(OTM_DEPENDENT) + ": " + otmDependent);
                        b = (Boolean.valueOf(otmDependent)).booleanValue();
                        m_CurrentORD.setOtmDependent(b);

                        break;
                    }

                case FOREIGN_KEY:
                    {
                        if (isDebug) logger.debug("    > " + tags.getTagById(FOREIGN_KEY));
                        String fieldIdRef = atts.getValue(tags.getTagById(FIELD_ID_REF));

                        if (fieldIdRef != null)
                        {
                            if (isDebug) logger.debug("      " + tags.getTagById(FIELD_ID_REF) + ": " + fieldIdRef);

                            try
                            {
                                int fieldId;
                                fieldId = Integer.parseInt(fieldIdRef);
                                m_CurrentORD.addForeignKeyField(fieldId);
                            }
                            catch (NumberFormatException rex)
                            {
                                throw new MetadataException(tags.getTagById(FIELD_ID_REF)
                                        + " attribute must be an int. Found: "
                                        + fieldIdRef + ". Please check your repository file.", rex);
                            }
                        }
                        else
                        {
                            String fieldRef = atts.getValue(tags.getTagById(FIELD_REF));
                            if (isDebug) logger.debug("      " + tags.getTagById(FIELD_REF) + ": " + fieldRef);
                            m_CurrentORD.addForeignKeyField(fieldRef);
                        }
                        break;
                    }

                case COLLECTION_DESCRIPTOR:
                    {
                        if (isDebug) logger.debug("    > " + tags.getTagById(COLLECTION_DESCRIPTOR));
                        m_CurrentCOD = new CollectionDescriptor(m_CurrentCLD);


                        // prepare for custom attributes
                        this.m_CurrentAttrContainer = m_CurrentCOD;

                        // set name attribute
                        name = atts.getValue(tags.getTagById(FIELD_NAME));
                        if (isDebug) logger.debug("     " + tags.getTagById(FIELD_NAME) + ": " + name);
						PersistentField pf = PersistentFieldFactory.createPersistentField(m_CurrentCLD.getPersistentFieldClassName(),m_CurrentCLD.getClassOfObject(),name);
                        m_CurrentCOD.setPersistentField(pf);

                        // set collection-class attribute
                        String collectionClassName = atts.getValue(tags.getTagById(COLLECTION_CLASS));
                        if (collectionClassName != null)
                        {
                            if (isDebug) logger.debug("     " + tags.getTagById(COLLECTION_CLASS) + ": " + collectionClassName);
                            m_CurrentCOD.setCollectionClass(ClassHelper.getClass(collectionClassName));
                        }
                        // set element-class-ref attribute
                        String elementClassRef = atts.getValue(tags.getTagById(ITEMS_CLASS));
                        if (isDebug) logger.debug("     " + tags.getTagById(ITEMS_CLASS) + ": " + elementClassRef);
                        if (elementClassRef != null)
                        {
                            m_CurrentCOD.setItemClass(ClassHelper.getClass(elementClassRef));
                        }

                        //set orderby and sort attributes:
                        String orderby = atts.getValue(tags.getTagById(ORDERBY));
                        String sort = atts.getValue(tags.getTagById(SORT));
                        if (isDebug) logger.debug("     " + tags.getTagById(SORT) + ": " + orderby + ", " + sort);
                        if (orderby != null)
                        {
                            m_CurrentCOD.addOrderBy(orderby, "ASC".equalsIgnoreCase(sort));
                        }

                        // set indirection-table attribute
                        String indirectionTable = atts.getValue(tags.getTagById(INDIRECTION_TABLE));
                        if (isDebug) logger.debug("     " + tags.getTagById(INDIRECTION_TABLE) + ": " + indirectionTable);
                        m_CurrentCOD.setIndirectionTable(indirectionTable);

                        // set proxy attribute
                        String proxy = atts.getValue(tags.getTagById(PROXY_REFERENCE));
                        if (isDebug) logger.debug("     " + tags.getTagById(PROXY_REFERENCE) + ": " + proxy);
                        boolean b = (Boolean.valueOf(proxy)).booleanValue();
                        m_CurrentCOD.setLazy(b);

                        // set proxyPrefetchingLimit attribute
                        String proxyPrefetchingLimit = atts.getValue(tags.getTagById(PROXY_PREFETCHING_LIMIT));
                        if (isDebug) logger.debug("     " + tags.getTagById(PROXY_PREFETCHING_LIMIT) + ": " + proxyPrefetchingLimit);
                        if (proxyPrefetchingLimit == null)
                        {
                            m_CurrentCOD.setProxyPrefetchingLimit(defProxyPrefetchingLimit);
                        }
                        else
                        {
                            m_CurrentCOD.setProxyPrefetchingLimit(Integer.parseInt(proxyPrefetchingLimit));
                        }

                        // set refresh attribute
                        String refresh = atts.getValue(tags.getTagById(REFRESH));
                        if (isDebug) logger.debug("     " + tags.getTagById(REFRESH) + ": " + refresh);
                        b = (Boolean.valueOf(refresh)).booleanValue();
                        m_CurrentCOD.setRefresh(b);

                        // set auto-retrieve attribute
                        String autoRetrieve = atts.getValue(tags.getTagById(AUTO_RETRIEVE));
                        if (isDebug) logger.debug("     " + tags.getTagById(AUTO_RETRIEVE) + ": " + autoRetrieve);
                        b = (Boolean.valueOf(autoRetrieve)).booleanValue();
                        m_CurrentCOD.setCascadeRetrieve(b);

                        // set auto-update attribute
                        String autoUpdate = atts.getValue(tags.getTagById(AUTO_UPDATE));
                        if (isDebug) logger.debug("     " + tags.getTagById(AUTO_UPDATE) + ": " + autoUpdate);
                        if(autoUpdate != null)
                        {
                            m_CurrentCOD.setCascadingStore(autoUpdate);
                        }

                        //set auto-delete attribute
                        String autoDelete = atts.getValue(tags.getTagById(AUTO_DELETE));
                        if (isDebug) logger.debug("     " + tags.getTagById(AUTO_DELETE) + ": " + autoDelete);
                        if(autoDelete != null)
                        {
                            m_CurrentCOD.setCascadingDelete(autoDelete);
                        }

                        //set otm-dependent attribute
                        String otmDependent = atts.getValue(tags.getTagById(OTM_DEPENDENT));
                        if (isDebug) logger.debug("     " + tags.getTagById(OTM_DEPENDENT) + ": " + otmDependent);
                        b = (Boolean.valueOf(otmDependent)).booleanValue();
                        m_CurrentCOD.setOtmDependent(b);

                        m_CurrentCLD.addCollectionDescriptor(m_CurrentCOD);

                        break;
                    }
                case ORDERBY :
                    {
                        if (isDebug) logger.debug("    > " + tags.getTagById(ORDERBY));
                        name = atts.getValue(tags.getTagById(FIELD_NAME));
                        if (isDebug) logger.debug("     " + tags.getTagById(FIELD_NAME) + ": " + name);
                        String sort = atts.getValue(tags.getTagById(SORT));
                        if (isDebug) logger.debug("     " + tags.getTagById(SORT) + ": " + name + ", " + sort);

                        m_CurrentCOD.addOrderBy(name, "ASC".equalsIgnoreCase(sort));
                        break;
                    }
                case INVERSE_FK:
                    {
                        if (isDebug) logger.debug("    > " + tags.getTagById(INVERSE_FK));
                        String fieldIdRef = atts.getValue(tags.getTagById(FIELD_ID_REF));

                        if (fieldIdRef != null)
                        {
                            if (isDebug) logger.debug("      " + tags.getTagById(FIELD_ID_REF) + ": " + fieldIdRef);

                            try
                            {
                                int fieldId;
                                fieldId = Integer.parseInt(fieldIdRef);
                                m_CurrentCOD.addForeignKeyField(fieldId);
                            }
                            catch (NumberFormatException rex)
                            {
                                throw new MetadataException(tags.getTagById(FIELD_ID_REF)
                                        + " attribute must be an int. Found: "
                                        + fieldIdRef + " Please check your repository file.", rex);
                            }
                        }
                        else
                        {
                            String fieldRef = atts.getValue(tags.getTagById(FIELD_REF));
                            if (isDebug) logger.debug("      " + tags.getTagById(FIELD_REF) + ": " + fieldRef);
                            m_CurrentCOD.addForeignKeyField(fieldRef);
                        }
                        break;
                    }

                case FK_POINTING_TO_THIS_CLASS:
                    {
                        if (isDebug) logger.debug("    > " + tags.getTagById(FK_POINTING_TO_THIS_CLASS));
                        String column = atts.getValue("column");
                        if (isDebug) logger.debug("      " + "column" + ": " + column);
                        m_CurrentCOD.addFkToThisClass(column);
                        break;
                    }

                case FK_POINTING_TO_ITEMS_CLASS:
                    {
                        if (isDebug) logger.debug("    > " + tags.getTagById(FK_POINTING_TO_ITEMS_CLASS));
                        String column = atts.getValue("column");
                        if (isDebug) logger.debug("      " + "column" + ": " + column);
                        m_CurrentCOD.addFkToItemClass(column);
                        break;
                    }
                case ATTRIBUTE:
                    {
                        //handle custom attributes
                        String attributeName = atts.getValue(tags.getTagById(ATTRIBUTE_NAME));
                        String attributeValue = atts.getValue(tags.getTagById(ATTRIBUTE_VALUE));
                        // If we have a container to store this attribute in, then do so.
                        if (this.m_CurrentAttrContainer != null)
                        {
                            if (isDebug) logger.debug("      > " + tags.getTagById(ATTRIBUTE));
                            if (isDebug) logger.debug("       " + tags.getTagById(ATTRIBUTE_NAME) + ": " + attributeName);
                            if (isDebug) logger.debug("       " + tags.getTagById(ATTRIBUTE_VALUE) + ": " + attributeValue);
                            this.m_CurrentAttrContainer.addAttribute(attributeName, attributeValue);
                        }
                        else
                        {
//                            logger.debug("Found attribute (name="+attributeName+", value="+attributeValue+
//                                    ") but I can not assign them to a descriptor");
                        }
                        break;
                    }
//                    case SEQUENCE_MANAGER:
//                    {
//                        if (isDebug) logger.debug("    > " + tags.getTagById(SEQUENCE_MANAGER));
//                        // currently it's not possible to specify SM on class-descriptor level
//                        // thus we use a dummy object to prevent ATTRIBUTE container report
//                        // unassigned attributes
//                        this.m_CurrentAttrContainer = new SequenceDescriptor(null);
//                        break;
//                    }
                    case QUERY_CUSTOMIZER:
                        {
                            // set collection-class attribute
                            String className = atts.getValue("class");
                            QueryCustomizer queryCust;

                            if (className != null)
                            {
                                if (isDebug) logger.debug("     " + "class" + ": " + className);
                                queryCust = (QueryCustomizer)ClassHelper.newInstance(className);
                                m_CurrentAttrContainer = queryCust;
                                m_CurrentCOD.setQueryCustomizer(queryCust);
                            }
                            break;
                        }
                    case INDEX_DESCRIPTOR:
                        {
                            m_CurrentIndexDescriptor = new IndexDescriptor();
                            m_CurrentIndexDescriptor.setName(atts.getValue(tags.getTagById(NAME)));
                            m_CurrentIndexDescriptor.setUnique(Boolean.valueOf(atts.getValue(tags.getTagById(UNIQUE))).booleanValue());
                            break;
                        }
                    case INDEX_COLUMN:
                        {
                            m_CurrentIndexDescriptor.getIndexColumns().add(atts.getValue(tags.getTagById(NAME)));
                            break;
                        }
                    case INSERT_PROCEDURE:
                    {
                        if (isDebug) logger.debug("    > " + tags.getTagById(INSERT_PROCEDURE));

                        // Get the proc name and the 'include all fields' setting
                        String procName = atts.getValue(tags.getTagById(NAME));
                        String includeAllFields = atts.getValue(tags.getTagById(INCLUDE_ALL_FIELDS));
                        if (isDebug) logger.debug("     " + tags.getTagById(NAME) +
                                                  ": " + procName);
                        if (isDebug) logger.debug("     " + tags.getTagById(INCLUDE_ALL_FIELDS) +
                                                  ": " + includeAllFields);

                        // create the procedure descriptor
                        InsertProcedureDescriptor proc =
                            new InsertProcedureDescriptor(m_CurrentCLD,
                                                          procName,
                                                          Boolean.valueOf(includeAllFields).booleanValue());
                        m_CurrentProcedure = proc;

                        // Get the name of the field ref that will receive the
                        // return value.
                        String returnFieldRefName = atts.getValue(tags.getTagById(RETURN_FIELD_REF));
                        if (isDebug) logger.debug("     " + tags.getTagById(RETURN_FIELD_REF) +
                                                  ": " + returnFieldRefName);
                        proc.setReturnValueFieldRef(returnFieldRefName);

                        break;
                    }
                case UPDATE_PROCEDURE:
                    {
                        if (isDebug) logger.debug("    > " + tags.getTagById(UPDATE_PROCEDURE));

                        // Get the proc name and the 'include all fields' setting
                        String procName = atts.getValue(tags.getTagById(NAME));
                        String includeAllFields = atts.getValue(tags.getTagById(INCLUDE_ALL_FIELDS));
                        if (isDebug) logger.debug("     " + tags.getTagById(NAME) +
                                                  ": " + procName);
                        if (isDebug) logger.debug("     " + tags.getTagById(INCLUDE_ALL_FIELDS) +
                                                  ": " + includeAllFields);

                        // create the procedure descriptor
                        UpdateProcedureDescriptor proc =
                            new UpdateProcedureDescriptor(m_CurrentCLD,
                                                          procName,
                                                          Boolean.valueOf(includeAllFields).booleanValue());
                        m_CurrentProcedure = proc;

                        // Get the name of the field ref that will receive the
                        // return value.
                        String returnFieldRefName = atts.getValue(tags.getTagById(RETURN_FIELD_REF));
                        if (isDebug) logger.debug("     " + tags.getTagById(RETURN_FIELD_REF) +
                                                  ": " + returnFieldRefName);
                        proc.setReturnValueFieldRef(returnFieldRefName);

                        break;
                    }
                case DELETE_PROCEDURE:
                    {
                        if (isDebug) logger.debug("    > " + tags.getTagById(DELETE_PROCEDURE));

                        // Get the proc name and the 'include all fields' setting
                        String procName = atts.getValue(tags.getTagById(NAME));
                        String includeAllPkFields = atts.getValue(tags.getTagById(INCLUDE_PK_FIELDS_ONLY));
                        if (isDebug) logger.debug("     " + tags.getTagById(NAME) +
                                                  ": " + procName);
                        if (isDebug) logger.debug("     " + tags.getTagById(INCLUDE_PK_FIELDS_ONLY) +
                                                  ": " + includeAllPkFields);

                        // create the procedure descriptor
                        DeleteProcedureDescriptor proc =
                            new DeleteProcedureDescriptor(m_CurrentCLD,
                                                          procName,
                                                          Boolean.valueOf(includeAllPkFields).booleanValue());
                        m_CurrentProcedure = proc;

                        // Get the name of the field ref that will receive the
                        // return value.
                        String returnFieldRefName = atts.getValue(tags.getTagById(RETURN_FIELD_REF));
                        if (isDebug) logger.debug("     " + tags.getTagById(RETURN_FIELD_REF) +
                                                  ": " + returnFieldRefName);
                        proc.setReturnValueFieldRef(returnFieldRefName);

                        break;

                    }
                case CONSTANT_ARGUMENT:
                    {
                        if (isDebug) logger.debug("    > " + tags.getTagById(CONSTANT_ARGUMENT));
                        ArgumentDescriptor arg = new ArgumentDescriptor(m_CurrentProcedure);

                        // Get the value
                        String value = atts.getValue(tags.getTagById(VALUE));
                        if (isDebug) logger.debug("     " + tags.getTagById(VALUE) + ": " + value);

                        // Set the value for the argument
                        arg.setValue(value);

                        // Add the argument to the procedure.
                        m_CurrentProcedure.addArgument(arg);
                        break;
                    }
                case RUNTIME_ARGUMENT:
                    {
                        if (isDebug) logger.debug("    > " + tags.getTagById(RUNTIME_ARGUMENT));
                        ArgumentDescriptor arg = new ArgumentDescriptor(m_CurrentProcedure);

                        // Get the name of the field ref
                        String fieldRefName = atts.getValue(tags.getTagById(FIELD_REF));
                        if (isDebug) logger.debug("     " + tags.getTagById(FIELD_REF) +
                                                  ": " + fieldRefName);

                        // Get the 'return' value.
                        String returnValue = atts.getValue(tags.getTagById(RETURN));
                        if (isDebug) logger.debug("     " + tags.getTagById(RETURN) +
                                                  ": " + returnValue);

                        // Set the value for the argument.
                        if ((fieldRefName != null) && (fieldRefName.trim().length() != 0)) {
                            arg.setValue(fieldRefName,
                                         Boolean.valueOf(returnValue).booleanValue());
                        }

                        // Add the argument to the procedure.
                        m_CurrentProcedure.addArgument(arg);
                        break;
                    }

                default :
                    {
                        // nop
                    }
            }
        }
        catch (Exception ex)
        {
            logger.error("Exception while read metadata", ex);
            if(ex instanceof MetadataException) throw (MetadataException)ex;
            else throw new MetadataException("Exception when reading metadata information,"+
                    " please check your repository.xml file", ex);
        }
    }

    /**
     * endElement callback. most elements are build up from here.
     */
    public void endElement(String uri, String name, String qName)
    {
        boolean isDebug = logger.isDebugEnabled();
        try
        {
            switch (getLiteralId(qName))
            {
                case MAPPING_REPOSITORY:
                    {
                        if (isDebug) logger.debug(" < " + tags.getTagById(MAPPING_REPOSITORY));
                        this.m_CurrentAttrContainer = null;
                        m_CurrentCLD = null;
                        break;
                    }
                case CLASS_DESCRIPTOR:
                    {
                        if (isDebug) logger.debug("  < " + tags.getTagById(CLASS_DESCRIPTOR));
                        m_CurrentCLD = null;
                        this.m_CurrentAttrContainer = null;
                        break;
                    }
                case OBJECT_CACHE:
                    {
                        if(m_CurrentAttrContainer != null)
                        {
                            if (isDebug) logger.debug("     < " + tags.getTagById(OBJECT_CACHE));
                        }
                        this.m_CurrentAttrContainer = m_CurrentCLD;
                        break;
                    }
                case CLASS_EXTENT:
                    {
                        break;
                    }
                case FIELD_DESCRIPTOR:
                    {
                        if (isDebug) logger.debug("    < " + tags.getTagById(FIELD_DESCRIPTOR));
                        m_CurrentFLD = null;
                        m_CurrentAttrContainer = m_CurrentCLD;
                        break;
                    }
                case REFERENCE_DESCRIPTOR:
                    {
                        if (isDebug) logger.debug("    < " + tags.getTagById(REFERENCE_DESCRIPTOR));
                        m_CurrentORD = null;
                        m_CurrentAttrContainer = m_CurrentCLD;
                        break;
                    }
                case FOREIGN_KEY:
                    {
                        if (isDebug) logger.debug("    < " + tags.getTagById(FOREIGN_KEY));
                        break;
                    }
                case COLLECTION_DESCRIPTOR:
                    {
                        if (isDebug) logger.debug("    < " + tags.getTagById(COLLECTION_DESCRIPTOR));
                        m_CurrentCOD = null;
                        m_CurrentAttrContainer = m_CurrentCLD;
                        break;
                    }
                case INVERSE_FK:
                    {
                        if (isDebug) logger.debug("    < " + tags.getTagById(INVERSE_FK));
                        break;
                    }
                case ORDERBY :
                    {
                        if (isDebug) logger.debug("    < " + tags.getTagById(ORDERBY));
                        break;
                    }
                case FK_POINTING_TO_THIS_CLASS:
                    {
                        if (isDebug) logger.debug("    < " + tags.getTagById(FK_POINTING_TO_THIS_CLASS));
                        break;
                    }
                case FK_POINTING_TO_ITEMS_CLASS:
                    {
                        if (isDebug) logger.debug("    < " + tags.getTagById(FK_POINTING_TO_ITEMS_CLASS));
                        break;
                    }
                case ATTRIBUTE:
                    {
                        if(m_CurrentAttrContainer != null)
                        {
                            if (isDebug) logger.debug("      < " + tags.getTagById(ATTRIBUTE));
                        }
                        break;
                    }
                case DOCUMENTATION:
                    {
                        if (isDebug) logger.debug("    < " + tags.getTagById(DOCUMENTATION));
                        break;
                    }
//                case SEQUENCE_MANAGER:
//                    {
//                        // currently not used on class-descriptor level
//                        // if (isDebug) logger.debug("    < " + tags.getTagById(SEQUENCE_MANAGER));
//                        this.m_CurrentAttrContainer = null;
//                        break;
//                    }
//                case CONNECTION_POOL:
//                    {
//                        // not used on class-descriptor level
//                        // if (isDebug) logger.debug("    < " + tags.getTagById(CONNECTION_POOL));
//                        this.m_CurrentAttrContainer = null;
//                        break;
//                    }
//                case JDBC_CONNECTION_DESCRIPTOR:
//                    {
//                        // not used on class-descriptor level
//                        // if (isDebug) logger.debug("    < " + tags.getTagById(JDBC_CONNECTION_DESCRIPTOR));
//                        this.m_CurrentAttrContainer = null;
//                        break;
//                    }
                case QUERY_CUSTOMIZER:
                    {
                        m_CurrentAttrContainer = m_CurrentCOD;
                        break;
                    }
                case INDEX_DESCRIPTOR:
                    {
                        m_CurrentCLD.getIndexes().add(m_CurrentIndexDescriptor);
                        m_CurrentIndexDescriptor = null;
                        break;
                    }
                case INDEX_COLUMN:
                    {
                        // ignore; all processing done in startElement
                        break;
                    }
                case INSERT_PROCEDURE:
                {
                    if (isDebug) logger.debug("    < " + tags.getTagById(INSERT_PROCEDURE));
                    m_CurrentCLD.setInsertProcedure((InsertProcedureDescriptor)m_CurrentProcedure);
                    m_CurrentProcedure = null;
                    break;
                }
                case UPDATE_PROCEDURE:
                    {
                        if (isDebug) logger.debug("    < " + tags.getTagById(UPDATE_PROCEDURE));
                        m_CurrentCLD.setUpdateProcedure((UpdateProcedureDescriptor)m_CurrentProcedure);
                        m_CurrentProcedure = null;
                        break;
                    }
                case DELETE_PROCEDURE:
                    {
                        if (isDebug) logger.debug("    < " + tags.getTagById(DELETE_PROCEDURE));
                        m_CurrentCLD.setDeleteProcedure((DeleteProcedureDescriptor)m_CurrentProcedure);
                        m_CurrentProcedure = null;
                        break;
                    }
                case CONSTANT_ARGUMENT:
                    {
                        if (isDebug) logger.debug("    < " + tags.getTagById(CONSTANT_ARGUMENT));
                        break;
                    }
                case RUNTIME_ARGUMENT:
                    {
                        if (isDebug) logger.debug("    < " + tags.getTagById(RUNTIME_ARGUMENT));
                        break;
                    }

                    // handle failure:
                default :
                    {
                        logger.debug("Ignoring unused Element " + qName);
                    }
            }
        }
        catch (Exception ex)
        {
            if(ex instanceof MetadataException) throw (MetadataException) ex;
            else throw new MetadataException("Exception when reading metadata information,"+
                    " please check your repository.xml file", ex);
        }
    }

    /**
     * characters callback.
     */
    public void characters(char ch[], int start, int length)
    {
        if (m_CurrentString == null)
            m_CurrentString = new String(ch, start, length);
        else
            m_CurrentString += new String(ch, start, length);
    }

    /**
     * Error callback.
     */
    public void error(SAXParseException e) throws SAXException
    {
        logger.error(e);
        throw e;
    }

    /**
     * fatal error callback.
     */
    public void fatalError(SAXParseException e) throws SAXException
    {
        logger.fatal(e);
        throw e;
    }

    /**
     * warning callback.
     */
    public void warning(SAXParseException e) throws SAXException
    {
        logger.warn(e);
        throw e;
    }



    private boolean checkString(String str)
    {
        return (str != null && !str.trim().equals(""));
    }
}
