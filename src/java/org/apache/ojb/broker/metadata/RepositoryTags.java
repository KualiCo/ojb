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

import org.apache.ojb.broker.util.DoubleHashtable;
import org.apache.ojb.broker.util.logging.LoggerFactory;
import org.apache.ojb.broker.util.pooling.PoolConfiguration;

/**
 * this class maintains a table mapping the xml-tags used in the
 * repository.dtd to their corresponding ids used within OJB.
 * This table is used in <br>
 * 1. the RepositoryXmlHandler to identify tags on parsing the
 * repository.xml in a large switch statement.
 * 2. in the RepositoryPersistor to get the proper tag for a
 * given XmlCapable id during assembling the repository.xml
 * for output.<br>
 * <b>Important note: This class is the only place where XML tags from the
 * repository.dtd are maintained.
 * All usages of these tags within OJB must use this table to ease
 * changes of the DTD.</b>
 * @author		Thomas Mahler
 */
public class RepositoryTags implements RepositoryElements
{
    /**
     * the two-way hashtable holding all entries.
     */
    private DoubleHashtable table;

    /**
     * the singleton instance of this class.
     */
    private static RepositoryTags instance = new RepositoryTags();

    /**
     * private Constructor, please use getInstance() to obtain
     * the singleton instance of this class.
     */
    private RepositoryTags()
    {
        // construct the mapping table
        table = new DoubleHashtable();
        table.put("descriptor-repository", new Integer(MAPPING_REPOSITORY));
        table.put("version", new Integer(REPOSITORY_VERSION));
        table.put("isolation-level", new Integer(ISOLATION_LEVEL));
        table.put("jdbc-connection-descriptor", new Integer(JDBC_CONNECTION_DESCRIPTOR));
        table.put("platform", new Integer(DBMS_NAME));
        table.put("schema", new Integer(SCHEMA_NAME));
        table.put("jcd-alias", new Integer(JCD_ALIAS));
        table.put("default-connection", new Integer(DEFAULT_CONNECTION));
        table.put("driver", new Integer(DRIVER_NAME));
        table.put("protocol", new Integer(URL_PROTOCOL));
        table.put("subprotocol", new Integer(URL_SUBPROTOCOL));
        table.put("dbalias", new Integer(URL_DBALIAS));
        table.put("username", new Integer(USER_NAME));
        table.put("password", new Integer(USER_PASSWD));
        table.put("eager-release", new Integer(EAGER_RELEASE));
        table.put("batch-mode", new Integer(BATCH_MODE));
        table.put("useAutoCommit", new Integer(USE_AUTOCOMMIT));
        table.put("ignoreAutoCommitExceptions", new Integer(IGNORE_AUTOCOMMIT_EXCEPTION));
        table.put("class-descriptor", new Integer(CLASS_DESCRIPTOR));
        table.put("class", new Integer(CLASS_NAME));
        table.put("proxy", new Integer(CLASS_PROXY));
        table.put("extent-class", new Integer(CLASS_EXTENT));
        table.put("extends", new Integer(EXTENDS));
        table.put("table", new Integer(TABLE_NAME));
        table.put("orderby", new Integer(ORDERBY));
        table.put("conversion", new Integer(FIELD_CONVERSION));
        table.put("row-reader", new Integer(ROW_READER));
        table.put("field-descriptor", new Integer(FIELD_DESCRIPTOR));
        table.put("name", new Integer(FIELD_NAME));
        table.put("column", new Integer(COLUMN_NAME));
        table.put("jdbc-type", new Integer(JDBC_TYPE));
        table.put("primarykey", new Integer(PRIMARY_KEY));
        table.put("autoincrement", new Integer(AUTO_INCREMENT));
        table.put("sequence-name", new Integer(SEQUENCE_NAME));
        table.put("nullable", new Integer(NULLABLE));
        table.put("indexed", new Integer(INDEXED));
        table.put("length", new Integer(LENGTH));
        table.put("precision", new Integer(PRECISION));
        table.put("scale", new Integer(SCALE));
        table.put(TAG_ACCESS, new Integer(ACCESS));

        table.put("reference-descriptor", new Integer(REFERENCE_DESCRIPTOR));
        table.put("class-ref", new Integer(REFERENCED_CLASS));
        table.put("foreignkey", new Integer(FOREIGN_KEY));
        table.put("auto-retrieve", new Integer(AUTO_RETRIEVE));
        table.put("auto-update", new Integer(AUTO_UPDATE));
        table.put("auto-delete", new Integer(AUTO_DELETE));
        table.put("collection-descriptor", new Integer(COLLECTION_DESCRIPTOR));
        table.put("element-class-ref", new Integer(ITEMS_CLASS));
        table.put("inverse-foreignkey", new Integer(INVERSE_FK));
        table.put("collection-class", new Integer(COLLECTION_CLASS));
        table.put("indirection-table", new Integer(INDIRECTION_TABLE));
        table.put("fk-pointing-to-element-class", new Integer(FK_POINTING_TO_ITEMS_CLASS));
        table.put("fk-pointing-to-this-class", new Integer(FK_POINTING_TO_THIS_CLASS));
        table.put("jndi-datasource-name", new Integer(DATASOURCE_NAME));
        table.put("jdbc-level", new Integer(JDBC_LEVEL));
        table.put("locking", new Integer(LOCKING));
        table.put("update-lock", new Integer(UPDATE_LOCK));
        table.put("refresh", new Integer(REFRESH));
        table.put("proxy", new Integer(PROXY_REFERENCE));
        table.put("sort", new Integer(SORT));
        table.put("otm-dependent", new Integer(OTM_DEPENDENT));

        table.put("index-descriptor", new Integer(INDEX_DESCRIPTOR));
        table.put("index-column", new Integer(INDEX_COLUMN));
        table.put("unique", new Integer(UNIQUE));
        table.put("name", new Integer(NAME));

        table.put(PoolConfiguration.MAX_ACTIVE, new Integer(CON_MAX_ACTIVE));
        table.put(PoolConfiguration.MAX_IDLE, new Integer(CON_MAX_IDLE));
        table.put(PoolConfiguration.MAX_WAIT, new Integer(CON_MAX_WAIT));
        table.put(
            PoolConfiguration.MIN_EVICTABLE_IDLE_TIME_MILLIS,
            new Integer(CON_MIN_EVICTABLE_IDLE_TIME_MILLIS));
        table.put(
            PoolConfiguration.NUM_TESTS_PER_EVICTION_RUN,
            new Integer(CON_NUM_TESTS_PER_EVICTION_RUN));
        table.put(PoolConfiguration.TEST_ON_BORROW, new Integer(CON_TEST_ON_BORROW));
        table.put(PoolConfiguration.TEST_ON_RETURN, new Integer(CON_TEST_ON_RETURN));
        table.put(PoolConfiguration.TEST_WHILE_IDLE, new Integer(CON_TEST_WHILE_IDLE));
        table.put(
            PoolConfiguration.TIME_BETWEEN_EVICTION_RUNS_MILLIS,
            new Integer(CON_TIME_BETWEEN_EVICTION_RUNS_MILLIS));
        table.put(PoolConfiguration.WHEN_EXHAUSTED_ACTION, new Integer(CON_WHEN_EXHAUSTED_ACTION));
        table.put(PoolConfiguration.VALIDATION_QUERY, new Integer(VALIDATION_QUERY));

        table.put(PoolConfiguration.LOG_ABANDONED, new Integer(CON_LOG_ABANDONED));
        table.put(PoolConfiguration.REMOVE_ABANDONED, new Integer(CON_REMOVE_ABANDONED));
        table.put(
            PoolConfiguration.REMOVE_ABANDONED_TIMEOUT,
            new Integer(CON_REMOVE_ABANDONED_TIMEOUT));

        table.put("connectionFactory", new Integer(CONNECTION_FACTORY));
        table.put("connection-pool", new Integer(CONNECTION_POOL));
        table.put("class-ref", new Integer(CLASS_REF));
        table.put("id", new Integer(ID));
        table.put("field-id-ref", new Integer(FIELD_ID_REF));
        table.put("field-ref", new Integer(FIELD_REF));
        table.put("attribute", new Integer(ATTRIBUTE));
        table.put("attribute-name", new Integer(ATTRIBUTE_NAME));
        table.put("attribute-value", new Integer(ATTRIBUTE_VALUE));
        table.put("documentation", new Integer(DOCUMENTATION));
        table.put("accept-locks", new Integer(ACCEPT_LOCKS));

        table.put("sequence-manager", new Integer(SEQUENCE_MANAGER));
        table.put("className", new Integer(SEQUENCE_MANAGER_CLASS));

        table.put("query-customizer", new Integer(QUERY_CUSTOMIZER));
        table.put("initialization-method", new Integer(INITIALIZATION_METHOD));
        table.put("factory-class", new Integer(FACTORY_CLASS));
        table.put("factory-method", new Integer(FACTORY_METHOD));

        table.put("insert-procedure", new Integer(INSERT_PROCEDURE));
        table.put("update-procedure", new Integer(UPDATE_PROCEDURE));
        table.put("delete-procedure", new Integer(DELETE_PROCEDURE));
        table.put("constant-argument", new Integer(CONSTANT_ARGUMENT));
        table.put("runtime-argument", new Integer(RUNTIME_ARGUMENT));
        table.put("return", new Integer(RETURN));
        table.put("value", new Integer(VALUE));
        table.put("return-field-ref", new Integer(RETURN_FIELD_REF));
        table.put("include-pk-only", new Integer(INCLUDE_PK_FIELDS_ONLY));
        table.put("include-all-fields", new Integer(INCLUDE_ALL_FIELDS));

        table.put("object-cache", new Integer(OBJECT_CACHE));

        table.put("proxy-prefetching-limit", new Integer(PROXY_PREFETCHING_LIMIT));

        // add new tags here !
    }

    /**
     * returns the singleton instance.
     */
    public static RepositoryTags getInstance()
    {
        return instance;
    }

    /**
     * returns the xml-tag literal associated with the repository element with
     * id <code>elementId</code>.
     * @return the resulting tag
     */
    public String getTagById(int elementId)
    {
        return (String) table.getKeyByValue(new Integer(elementId));
    }

    /**
     * returns the opening xml-tag associated with the repository element with
     * id <code>elementId</code>.
     * @return the resulting tag
     */
    public String getOpeningTagById(int elementId)
    {
        return "<" + table.getKeyByValue(new Integer(elementId)) + ">";
    }

    /**
     * returns the opening but non-closing xml-tag
     * associated with the repository element with
     * id <code>elementId</code>.
     * @return the resulting tag
     */
    public String getOpeningTagNonClosingById(int elementId)
    {
        return "<" + table.getKeyByValue(new Integer(elementId));
    }

    /**
     * returns the opening xml-tag associated with the repository element with
     * id <code>elementId</code>.
     * @return the resulting tag
     */
    public String getOpeningTagById(int elementId, String attributes)
    {
        return "<" + table.getKeyByValue(new Integer(elementId)) + " " + attributes + ">";
    }

    /**
     * returns the opening but non-closing xml-tag
     * associated with the repository element with
     * id <code>elementId</code>.
     * @return the resulting tag
     */
    public String getAttribute(int elementId, String value)
    {
        return table.getKeyByValue(new Integer(elementId)) + "=\"" + value + "\"";
    }

    /**
     * returns the closing xml-tag associated with the repository element with
     * id <code>elementId</code>.
     * @return the resulting tag
     */
    public String getClosingTagById(int elementId)
    {
        return "</" + table.getKeyByValue(new Integer(elementId)) + ">";
    }

    /**
     * returns the repository element id associated with the xml-tag
     * literal <code>tag</code>.
     * @return the resulting repository element id.
     * @throws NullPointerException if no value was found for <b>tag</b>
     */
    public int getIdByTag(String tag)
    {
        Integer value = (Integer) table.getValueByKey(tag);
        if (value == null)
            LoggerFactory.getDefaultLogger().error(
                "** " + this.getClass().getName() + ": Tag '" + tag + "' is not defined. **");
        return value.intValue();
    }

    /**
     * returns the opening xml-tag associated with the repository element with
     * id <code>elementId</code>.
     * @return the resulting tag
     */
    public String getCompleteTagById(int elementId, String characters)
    {
        String result = getOpeningTagById(elementId);
        result += characters;
        result += getClosingTagById(elementId);
        return result;
    }
}
