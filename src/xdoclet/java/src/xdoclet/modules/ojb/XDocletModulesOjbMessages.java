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

/**
 * Contains messages for the ojb xdoclet task.
 *
 * @author    <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 * @created   March 22, 2003
 */
public final class XDocletModulesOjbMessages
{
    /**
     * @msg.bundle   msg="The databaseName is required for the torque schema."
     */
    public final static String DATABASENAME_IS_REQUIRED = "DATABASENAME_IS_REQUIRED";

    /**
     * @msg.bundle   msg="Incompatible redefinition for column {0} encountered."
     */
    public final static String INCOMPATIBLE_COLUMN_REDEFINITION = "INCOMPATIBLE_COLUMN_REDEFINITION";

    /**
     * @msg.bundle   msg="The parameter {0} is required."
     */
    public final static String PARAMETER_IS_REQUIRED = "PARAMETER_IS_REQUIRED";

    /**
     * @msg.bundle   msg="The attribute {0} must be provided."
     */
    public final static String ATTRIBUTE_IS_REQUIRED = "ATTRIBUTE_IS_REQUIRED";

    /**
     * @msg.bundle   msg="The attribute {0} is not defined."
     */
    public final static String ATTRIBUTE_UNDEFINED = "ATTRIBUTE_UNDEFINED";

    /**
     * @msg.bundle   msg="Could not determine type of member {0}"
     */
    public final static String COULD_NOT_DETERMINE_TYPE_OF_MEMBER = "COULD_NOT_DETERMINE_TYPE_OF_MEMBER";

    /**
     * @msg.bundle   msg="The member {0} of type {1} cannot be a OJB reference."
     */
    public final static String MEMBER_CANNOT_BE_A_REFERENCE = "MEMBER_CANNOT_BE_A_REFERENCE";

    /**
     * @msg.bundle   msg="Could not find type {0}."
     */
    public final static String COULD_NOT_FIND_TYPE = "COULD_NOT_FIND_TYPE";

    /**
     * @msg.bundle   msg="Could not find type {0} of member {1} in type {2}."
     */
    public final static String COULD_NOT_FIND_MEMBER_TYPE = "COULD_NOT_FIND_MEMBER_TYPE";

    /**
     * @msg.bundle   msg="Could not find foreign key field {0} in type {1} (used in member {1} in type {2})."
     */
    public final static String COULD_NOT_FIND_FOREIGN_KEY_FIELD = "COULD_NOT_FIND_FOREIGN_KEY_FIELD";

    /**
     * @msg.bundle   msg="Collection element type {0} was not found or has no members implementing the association."
     */
    public final static String COLLECTION_ELEMENT_NOT_FOUND = "COLLECTION_ELEMENT_NOT_FOUND";

    /**
     * @msg.bundle   msg="No foreign keys specified for {0} in type {1}."
     */
    public final static String NO_FOREIGNKEYS = "NO_FOREIGNKEYS";

    /**
     * @msg.bundle   msg="The number of local foreign keys does not match the number of remote foreign keys for reference {0} in type {1}."
     */
    public final static String LOCAL_DOESNT_MATCH_REMOTE = "LOCAL_DOESNT_MATCH_REMOTE";

    /**
     * @msg.bundle   msg="The referenced type {0} does not have primary keys matching the foreign keys of reference {1} in type {2}."
     */
    public final static String NO_MATCHING_PRIMARYKEYS = "NO_MATCHING_PRIMARYKEYS";

    /**
     * @msg.bundle   msg="Could not find a primary key in type {0} that matches the foreignkey {1} used in the collection {2} in type {3}."
     */
    public final static String NO_PRIMARYKEY_FOR_FOREIGNKEY = "NO_PRIMARYKEY_FOR_FOREIGNKEY";

    /**
     * @msg.bundle   msg="The locking property can only be set for fields whose column is of type TIMESTAMP or INTEGER (field {1} in type {2})."
     */
    public final static String LOCKING_NOT_ALLOWED_HERE = "LOCKING_NOT_ALLOWED_HERE";

    /**
     * @msg.bundle   msg="The update-lock property can only be set for fields whose column is of type TIMESTAMP or INTEGER (field {1} in type {2})."
     */
    public final static String UPDATE_LOCK_NOT_ALLOWED_HERE = "UPDATE_LOCK_NOT_ALLOWED_HERE";

    /**
     * @msg.bundle   msg="The index defined via ojb.index in type {0} must have a non-empty name."
     */
    public final static String INDEX_NAME_MISSING = "INDEX_NAME_MISSING";

    /**
     * @msg.bundle   msg="The index field {0} for index {1} could not be found in type {2}."
     */
    public final static String INDEX_FIELD_MISSING = "INDEX_FIELD_MISSING";

    /**
     * @msg.bundle   msg="Could not find the class {0}. Perhaps it is not in the classpath ?"
     */
    public final static String CLASS_NOT_ON_CLASSPATH = "CLASS_NOT_ON_CLASSPATH";

    /**
     * @msg.bundle   msg="Only 'ASC' and 'DESC' are allowed as the sort order, not the specified value '{0}' for collection {1} in type {2}."
     */
    public final static String UNKNOWN_SORT_ORDER = "UNKNOWN_SORT_ORDER";

    /**
     * @msg.bundle   msg="The member {0} of type {1} cannot be a nested object."
     */
    public final static String MEMBER_CANNOT_BE_NESTED = "MEMBER_CANNOT_BE_NESTED";
}
