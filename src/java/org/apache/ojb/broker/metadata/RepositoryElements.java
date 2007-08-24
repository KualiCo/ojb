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

/**
 * This interface contains integer constants representing the
 * elements of a DescriptorRepository.
 * This constants are used in marshalling and unmarshalling a
 * DescriptorRepository to identify all its constituent elements.
 * @author		Thomas Mahler
 * @version $Id: RepositoryElements.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public interface RepositoryElements
{

    public static final int MAPPING_REPOSITORY = 0;
    public static final int JDBC_CONNECTION_DESCRIPTOR = 1;
    public static final int DBMS_NAME = 2;
    public static final int SORT = 3;
    public static final int SCHEMA_NAME = 5;
    public static final int JCD_ALIAS = 82;
    public static final int DEFAULT_CONNECTION = 80;
    public static final int DRIVER_NAME = 6;
    public static final int URL_PROTOCOL = 7;
    public static final int URL_SUBPROTOCOL = 8;
    public static final int URL_DBALIAS = 9;
    public static final int USER_NAME = 10;
    public static final int USER_PASSWD = 11;
    public static final int EAGER_RELEASE = 74;
    public static final int BATCH_MODE = 83;
    public static final int USE_AUTOCOMMIT = 90;
    public static final int IGNORE_AUTOCOMMIT_EXCEPTION = 91;
    public static final int CLASS_DESCRIPTOR = 12;
    public static final int CLASS_NAME = 13;
    public static final int CLASS_PROXY = 35;
    public static final int CLASS_EXTENT = 33;
    public static final int EXTENDS = 76;
    public static final int TABLE_NAME = 14;
    public static final int ORDERBY = 36;
    public static final int FIELD_CONVERSION = 30;
    public static final int ROW_READER = 32;
    public static final int FIELD_DESCRIPTOR = 15;
    public static final int FIELD_NAME = 16;
    public static final int COLUMN_NAME = 17;
    public static final int JDBC_TYPE = 18;
    public static final int PRIMARY_KEY = 19;
    public static final int AUTO_INCREMENT = 31;
    public static final int SEQUENCE_NAME = 77;
    public static final int REFERENCE_DESCRIPTOR = 20;
    public static final int REFERENCED_CLASS = 22;
    public static final int AUTO_RETRIEVE = 24;
    public static final int AUTO_UPDATE = 25;
    public static final int AUTO_DELETE = 26;
    public static final int OTM_DEPENDENT = 102;
    public static final int COLLECTION_DESCRIPTOR = 27;
    public static final int ITEMS_CLASS = 29;
    public static final int INVERSE_FK = 38;
    public static final int COLLECTION_CLASS = 37;
    public static final int INDIRECTION_TABLE = 39;
    public static final int FK_POINTING_TO_ITEMS_CLASS = 40;
    public static final int FK_POINTING_TO_THIS_CLASS = 41;
    public static final int DATASOURCE_NAME = 44;
    public static final int JDBC_LEVEL = 45;
    public static final int LOCKING = 46;
    public static final int UPDATE_LOCK = 98;
    public static final int REFRESH = 47;
    public static final int PROXY_REFERENCE = 48;
    public static final int ISOLATION_LEVEL = 34;
    public static final int FOREIGN_KEY = 49;
    public static final int NULLABLE = 50;
    public static final int INDEXED = 51;
    public static final int LENGTH = 52;
    public static final int PRECISION = 53;
    public static final int SCALE = 54;
    public static final int ACCESS = 99;

    public static final int CON_MAX_ACTIVE = 55;
    public static final int CON_MAX_IDLE = 56;
    public static final int CON_MAX_WAIT = 57;
    public static final int CON_MIN_EVICTABLE_IDLE_TIME_MILLIS = 58;
    public static final int CON_NUM_TESTS_PER_EVICTION_RUN = 59;
    public static final int CON_TEST_ON_BORROW = 60;
    public static final int CON_TEST_ON_RETURN = 61;
    public static final int CON_TEST_WHILE_IDLE = 62;
    public static final int CON_TIME_BETWEEN_EVICTION_RUNS_MILLIS = 63;
    public static final int CON_WHEN_EXHAUSTED_ACTION = 64;
    public static final int CON_LOG_ABANDONED = 87;
    public static final int CON_REMOVE_ABANDONED = 85;
    public static final int CON_REMOVE_ABANDONED_TIMEOUT = 86;

    public static final int CONNECTION_POOL = 65;
    public static final int CONNECTION_FACTORY = 66;
    public static final int VALIDATION_QUERY = 79;
    public static final int SEQUENCE_MANAGER = 88;
    public static final int SEQUENCE_MANAGER_CLASS = 89;

    public static final int REPOSITORY_VERSION = 67;
    public static final int CLASS_REF = 68;
    public static final int ID = 69;
    public static final int FIELD_ID_REF = 70;
    public static final int FIELD_REF = 84;
    public static final int ATTRIBUTE = 71;
    public static final int ATTRIBUTE_NAME = 72;
    public static final int ATTRIBUTE_VALUE = 73;
    public static final int DOCUMENTATION = 75;
    public static final int ACCEPT_LOCKS = 78;
    public static final int QUERY_CUSTOMIZER = 92;
    public static final int INITIALIZATION_METHOD = 93;
    public static final int FACTORY_CLASS = 100;
    public static final int FACTORY_METHOD = 101;

    public static final int INDEX_DESCRIPTOR = 94;
    public static final int INDEX_COLUMN = 95;
    public static final int UNIQUE = 96;
    public static final int NAME = 97;

    public static final int INSERT_PROCEDURE = 103;
    public static final int UPDATE_PROCEDURE = 104;
    public static final int DELETE_PROCEDURE = 105;
    public static final int CONSTANT_ARGUMENT = 106;
    public static final int RUNTIME_ARGUMENT = 107;
    public static final int RETURN_FIELD_REF = 108;
    public static final int INCLUDE_ALL_FIELDS = 109;
    public static final int INCLUDE_PK_FIELDS_ONLY = 110;
    public static final int RETURN = 111;
    public static final int VALUE = 112;

    public static final int OBJECT_CACHE = 113;

    public static final int PROXY_PREFETCHING_LIMIT = 114;

    // maintain a next id to keep track where we are
    static final int _NEXT = 115;

    // String constants
    public static final String TAG_ACCESS = "access";

    public static final String TAG_ACCESS_ANONYMOUS = "anonymous";
    public static final String TAG_ACCESS_READONLY = "readonly";
    public static final String TAG_ACCESS_READWRITE = "readwrite";

    public static final String TAG_SUPER = "super";

    public static final String CASCADE_NONE_STR = "none";
    public static final String CASCADE_LINK_STR = "link";
    public static final String CASCADE_OBJECT_STR = "object";

}
