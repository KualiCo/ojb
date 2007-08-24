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
import xdoclet.tagshandler.AbstractProgramElementTagsHandler;
import xdoclet.tagshandler.ClassTagsHandler;
import xdoclet.tagshandler.MethodTagsHandler;
import xdoclet.tagshandler.XDocletTagshandlerMessages;
import xdoclet.util.Translator;
import xdoclet.util.TypeConversionUtil;

/**
 * @author               <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 * @created              March 22, 2003
 * @xdoclet.taghandler   namespace="OjbMember"
 */
public class OjbMemberTagsHandler extends AbstractProgramElementTagsHandler
{

    /**
     * Returns the name of the current member which is the name in the case of a field, or the property name for an
     * accessor method.
     *
     * @return                      The member name
     * @exception XDocletException  if an error occurs
     */
    public static String getMemberName() throws XDocletException
    {
        if (getCurrentField() != null) {
            return getCurrentField().getName();
        }
        else if (getCurrentMethod() != null) {
            return MethodTagsHandler.getPropertyNameFor(getCurrentMethod());
        }
        else {
            return null;
        }
    }

    /**
     * Returns the type of the current member which is the type in the case of a field, the return type for a getter
     * method, or the type of the parameter for a setter method.
     *
     * @return                      The member type
     * @exception XDocletException  if an error occurs
     */
    public static XClass getMemberType() throws XDocletException
    {
        if (getCurrentField() != null) {
            return getCurrentField().getType();
        }
        else if (getCurrentMethod() != null) {
            XMethod method = getCurrentMethod();

            if (MethodTagsHandler.isGetterMethod(method)) {
                return method.getReturnType().getType();
            }
            else if (MethodTagsHandler.isSetterMethod(method)) {
                XParameter param = (XParameter)method.getParameters().iterator().next();

                return param.getType();
            }
        }
        return null;
    }

    /**
     * Returns the dimension of the type of the current member.
     *
     * @return                      The member dimension
     * @exception XDocletException  if an error occurs
     * @see                         OjbMemberTagsHandler#getMemberType()
     */
    public static int getMemberDimension() throws XDocletException
    {
        if (getCurrentField() != null) {
            return getCurrentField().getDimension();
        }
        else if (getCurrentMethod() != null) {
            XMethod method = getCurrentMethod();

            if (MethodTagsHandler.isGetterMethod(method)) {
                return method.getReturnType().getDimension();
            }
            else if (MethodTagsHandler.isSetterMethod(method)) {
                XParameter param = (XParameter)method.getParameters().iterator().next();

                return param.getDimension();
            }
        }
        return 0;
    }

    /**
     * The <code>isField</code> processes the template body if the current member is a field.
     *
     * @param template              a <code>String</code> value
     * @param attributes            a <code>Properties</code> value
     * @exception XDocletException  if an error occurs
     * @doc:tag                     type="content"
     */
    public void isField(String template, Properties attributes) throws XDocletException
    {
        if (getCurrentField() != null) {
            generate(template);
        }
    }

    /**
     * The <code>isMethod</code> processes the template body if the current member is a method.
     *
     * @param template              a <code>String</code> value
     * @param attributes            a <code>Properties</code> value
     * @exception XDocletException  if an error occurs
     * @doc:tag                     type="block"
     */
    public void isMethod(String template, Properties attributes) throws XDocletException
    {
        if (getCurrentMethod() != null) {
            generate(template);
        }
    }

    /**
     * The <code>forAllMembers</code> method iterates through all fields of the current class. In contrast to the <code>FieldTagsHandler.forAllFields</code>
     * method, this method operates on both fields and accessors (get/set/is-methods). In addition, the method
     * automatically includes all supertype-fields (i.e., accessors for base interfaces) if the corresponding attribute
     * is set (superclasses). The fields can optionally be limited to those posessing a specified tag.
     *
     * @param template              a <code>String</code> value
     * @param attributes            a <code>Properties</code> value
     * @exception XDocletException  if an error occurs
     * @doc:tag                     type="block"
     * @doc.param                   name="class" optional="true" description="Specifies the type to be searched. If not
     *      specified, then the current type is used."
     * @doc.param                   name="superclasses" optional="true" description="Specifies whether super types shall
     *      be processed as well."
     * @doc.param                   name="sort" optional="true" values="true,false" description="If true then sort the
     *      fields list."
     * @doc.param                   name="tagName" optional="true" description="Specifies a tag that all fields must
     *      posess in order to be processed."
     * @doc.param                   name="paramName" optional="true" description="Specifies a param that the given tag
     *      must have."
     * @doc.param                   name="value" optional="true" description="Specifies the value that the param must
     *      have."
     */
    public void forAllMembers(String template, Properties attributes) throws XDocletException
    {
        if (getCurrentClass() == null) {
            return;
        }

        String className = attributes.getProperty("class");
        XClass type = null;

        if ((className == null) || (className.length() == 0)) {
            type = getCurrentClass();
        }
        else {
            XClass curType;

            for (Iterator it = ClassTagsHandler.getAllClasses().iterator(); it.hasNext(); ) {
                curType = (XClass)it.next();
                if (className.equals(curType.getQualifiedName())) {
                    type = curType;
                    break;
                }
            }
            if (type == null) {
                throw new XDocletException(Translator.getString(XDocletModulesOjbMessages.class,
                    XDocletModulesOjbMessages.COULD_NOT_FIND_TYPE,
                    new String[]{className}));
            }
        }

        String tagName = attributes.getProperty("tagName");
        String paramName = attributes.getProperty("paramName");
        String paramValue = attributes.getProperty("value");
        boolean superTypes = TypeConversionUtil.stringToBoolean(attributes.getProperty("superclasses"), true);
        boolean sort = TypeConversionUtil.stringToBoolean(attributes.getProperty("sort"), true);
        ArrayList allMemberNames = new ArrayList();
        HashMap allMembers = new HashMap();

        if (superTypes) {
            addMembersInclSupertypes(allMemberNames, allMembers, type, tagName, paramName, paramValue);
        }
        else {
            addMembers(allMemberNames, allMembers, type, tagName, paramName, paramValue);
        }
        if (sort) {
            Collections.sort(allMemberNames);
        }
        for (Iterator it = allMemberNames.iterator(); it.hasNext(); ) {
            XMember member = (XMember) allMembers.get(it.next());

            if (member instanceof XField) {
                setCurrentField((XField) member);
            }
            else if (member instanceof XMethod) {
                setCurrentMethod((XMethod) member);
            }
            generate(template);
            if (member instanceof XField) {
                setCurrentField(null);
            }
            else if (member instanceof XMethod) {
                setCurrentMethod(null);
            }
        }
    }

    /**
     * Iterates over all tags of current member and evaluates the template for each one.
     *
     * @param template              The template to be evaluated
     * @param attributes            The attributes of the template tag
     * @exception XDocletException  If an error occurs
     * @doc.tag                     type="block"
     * @doc.param                   name="tagName" optional="false" description="The tag name."
     * @doc.param                   name="paramName" optional="true" description="The parameter name."
     */
    public void forAllMemberTags(String template, Properties attributes) throws XDocletException
    {
        if (getCurrentField() != null) {
            forAllMemberTags(template, attributes, FOR_FIELD, XDocletTagshandlerMessages.ONLY_CALL_FIELD_NOT_NULL, new String[]{"forAllMemberTags"});
        }
        else if (getCurrentMethod() != null) {
            forAllMemberTags(template, attributes, FOR_METHOD, XDocletTagshandlerMessages.ONLY_CALL_METHOD_NOT_NULL, new String[]{"forAllMemberTags"});
        }
    }

    /**
     * Iterates over all tokens in current member tag with the name tagName and evaluates the body for every token.
     *
     * @param template              The body of the block tag
     * @param attributes            The attributes of the template tag
     * @exception XDocletException  If an error occurs
     * @doc.tag                     type="block"
     * @doc.param                   name="tagName" optional="false" description="The tag name."
     * @doc.param                   name="delimiter" description="delimiter for the StringTokenizer. consult javadoc for
     *      java.util.StringTokenizer default is ','"
     * @doc.param                   name="skip" description="how many tokens to skip on start"
     */
    public void forAllMemberTagTokens(String template, Properties attributes) throws XDocletException
    {
        if (getCurrentField() != null) {
            forAllMemberTagTokens(template, attributes, FOR_FIELD);
        }
        else if (getCurrentMethod() != null) {
            forAllMemberTagTokens(template, attributes, FOR_METHOD);
        }
    }

    /**
     * Returns the name of the member which is the name in the case of a field, or the property name for an accessor
     * method.
     *
     * @param attributes            The attributes of the template tag
     * @return                      The member name
     * @exception XDocletException  if an error occurs
     * @doc.tag                     type="content"
     */
    public String memberName(Properties attributes) throws XDocletException
    {
        return getMemberName();
    }

    /**
     * Evaluates the body if current member has no tag with the specified name.
     *
     * @param template              The body of the block tag
     * @param attributes            The attributes of the template tag
     * @exception XDocletException  Description of Exception
     * @doc.tag                     type="block"
     * @doc.param                   name="tagName" optional="false" description="The tag name."
     * @doc.param                   name="paramName" description="The parameter name. If not specified, then the raw
     *      content of the tag is returned."
     * @doc.param                   name="paramNum" description="The zero-based parameter number. It's used if the user
     *      used the space-separated format for specifying parameters."
     * @doc.param                   name="error" description="Show this error message if no tag found."
     */
    public void ifDoesntHaveMemberTag(String template, Properties attributes) throws XDocletException
    {
        boolean result = false;

        if (getCurrentField() != null) {
            if (!hasTag(attributes, FOR_FIELD)) {
                result = true;
                generate(template);
            }
        }
        else if (getCurrentMethod() != null) {
            if (!hasTag(attributes, FOR_METHOD)) {
                result = true;
                generate(template);
            }
        }
        if (!result) {
            String error = attributes.getProperty("error");

            if (error != null) {
                getEngine().print(error);
            }
        }
    }

    /**
     * Evaluates the body if the current class has at least one member with at least one tag with the specified name.
     *
     * @param template              The body of the block tag
     * @param attributes            The attributes of the template tag
     * @exception XDocletException  Description of Exception
     * @doc.tag                     type="block"
     * @doc.param                   name="tagName" optional="false" description="The tag name."
     * @doc.param                   name="paramName" description="The parameter name. If not specified, then the raw
     *      content of the tag is returned."
     * @doc.param                   name="error" description="Show this error message if no tag found."
     */
    public void ifHasMemberWithTag(String template, Properties attributes) throws XDocletException
    {
        ArrayList allMemberNames = new ArrayList();
        HashMap allMembers = new HashMap();
        boolean hasTag = false;

        addMembers(allMemberNames, allMembers, getCurrentClass(), null, null, null);
        for (Iterator it = allMemberNames.iterator(); it.hasNext(); ) {
            XMember member = (XMember) allMembers.get(it.next());

            if (member instanceof XField) {
                setCurrentField((XField)member);
                if (hasTag(attributes, FOR_FIELD)) {
                    hasTag = true;
                }
                setCurrentField(null);
            }
            else if (member instanceof XMethod) {
                setCurrentMethod((XMethod)member);
                if (hasTag(attributes, FOR_METHOD)) {
                    hasTag = true;
                }
                setCurrentMethod(null);
            }
            if (hasTag) {
                generate(template);
                break;
            }
        }
    }

    /**
     * Evaluates the body if current member has at least one tag with the specified name.
     *
     * @param template              The body of the block tag
     * @param attributes            The attributes of the template tag
     * @exception XDocletException  Description of Exception
     * @doc.tag                     type="block"
     * @doc.param                   name="tagName" optional="false" description="The tag name."
     * @doc.param                   name="paramName" description="The parameter name. If not specified, then the raw
     *      content of the tag is returned."
     * @doc.param                   name="paramNum" description="The zero-based parameter number. It's used if the user
     *      used the space-separated format for specifying parameters."
     * @doc.param                   name="error" description="Show this error message if no tag found."
     */
    public void ifHasMemberTag(String template, Properties attributes) throws XDocletException
    {
        boolean result = false;

        if (getCurrentField() != null) {
            if (hasTag(attributes, FOR_FIELD)) {
                result = true;
                generate(template);
            }
        }
        else if (getCurrentMethod() != null) {
            if (hasTag(attributes, FOR_METHOD)) {
                result = true;
                generate(template);
            }
        }
        if (!result) {
            String error = attributes.getProperty("error");

            if (error != null) {
                getEngine().print(error);
            }
        }
    }

    /**
     * Returns the value of the tag/parameter combination for the current member tag
     *
     * @param attributes            The attributes of the template tag
     * @return                      Description of the Returned Value
     * @exception XDocletException  Description of Exception
     * @doc.tag                     type="content"
     * @doc.param                   name="tagName" optional="false" description="The tag name."
     * @doc.param                   name="paramName" description="The parameter name. If not specified, then the raw
     *      content of the tag is returned."
     * @doc.param                   name="paramNum" description="The zero-based parameter number. It's used if the user
     *      used the space-separated format for specifying parameters."
     * @doc.param                   name="values" description="The valid values for the parameter, comma separated. An
     *      error message is printed if the parameter value is not one of the values."
     * @doc.param                   name="default" description="The default value is returned if parameter not specified
     *      by user for the tag."
     */
    public String memberTagValue(Properties attributes) throws XDocletException
    {
        if (getCurrentField() != null) {
            // setting field to true will override the for_class value.
            attributes.setProperty("field", "true");
            return getExpandedDelimitedTagValue(attributes, FOR_FIELD);
        }
        else if (getCurrentMethod() != null) {
            return getExpandedDelimitedTagValue(attributes, FOR_METHOD);
        }
        else {
            return null;
        }
    }

    /**
     * Evaluates the body if value for the member tag equals the specified value.
     *
     * @param template              The body of the block tag
     * @param attributes            The attributes of the template tag
     * @exception XDocletException  If an error occurs
     * @doc.tag                     type="block"
     * @doc.param                   name="tagName" optional="false" description="The tag name."
     * @doc.param                   name="paramName" description="The parameter name. If not specified, then the raw
     *      content of the tag is returned."
     * @doc.param                   name="paramNum" description="The zero-based parameter number. It's used if the user
     *      used the space-separated format for specifying parameters."
     * @doc.param                   name="value" optional="false" description="The expected value."
     */
    public void ifMemberTagValueEquals(String template, Properties attributes) throws XDocletException
    {
        if (getCurrentField() != null) {
            if (isTagValueEqual(attributes, FOR_FIELD)) {
                generate(template);
            }
        }
        else if (getCurrentMethod() != null) {
            if (isTagValueEqual(attributes, FOR_METHOD)) {
                generate(template);
            }
        }
    }

    /**
     * Retrieves the members of the type and of its super types.
     *
     * @param memberNames        Will receive the names of the members (for sorting)
     * @param members            Will receive the members
     * @param type               The type to process
     * @param tagName            An optional tag for filtering the types
     * @param paramName          The feature to be added to the MembersInclSupertypes attribute
     * @param paramValue         The feature to be added to the MembersInclSupertypes attribute
     * @throws XDocletException  If an error occurs
     */
    private void addMembersInclSupertypes(Collection memberNames, HashMap members, XClass type, String tagName, String paramName, String paramValue) throws XDocletException
    {
        addMembers(memberNames, members, type, tagName, paramName, paramValue);
        if (type.getInterfaces() != null) {
            for (Iterator it = type.getInterfaces().iterator(); it.hasNext(); ) {
                addMembersInclSupertypes(memberNames, members, (XClass)it.next(), tagName, paramName, paramValue);
            }
        }
        if (!type.isInterface() && (type.getSuperclass() != null)) {
            addMembersInclSupertypes(memberNames, members, type.getSuperclass(), tagName, paramName, paramValue);
        }
    }

    /**
     * Retrieves the members of the given type.
     *
     * @param memberNames        Will receive the names of the members (for sorting)
     * @param members            Will receive the members
     * @param type               The type to process
     * @param tagName            An optional tag for filtering the types
     * @param paramName          The feature to be added to the Members attribute
     * @param paramValue         The feature to be added to the Members attribute
     * @throws XDocletException  If an error occurs
     */
    private void addMembers(Collection memberNames, HashMap members, XClass type, String tagName, String paramName, String paramValue) throws XDocletException
    {
        if (!type.isInterface() && (type.getFields() != null)) {
            XField field;

            for (Iterator it = type.getFields().iterator(); it.hasNext(); ) {
                field = (XField)it.next();
                if (!field.isFinal() && !field.isStatic() && !field.isTransient()) {
                    if (checkTagAndParam(field.getDoc(), tagName, paramName, paramValue)) {
                        // already processed ?
                        if (!members.containsKey(field.getName())) {
                            memberNames.add(field.getName());
                            members.put(field.getName(), field);
                        }
                    }
                }
            }
        }

        if (type.getMethods() != null) {
            XMethod method;
            String propertyName;

            for (Iterator it = type.getMethods().iterator(); it.hasNext(); ) {
                method = (XMethod)it.next();
                if (!method.isConstructor() && !method.isNative() && !method.isStatic()) {
                    if (checkTagAndParam(method.getDoc(), tagName, paramName, paramValue)) {
                        if (MethodTagsHandler.isGetterMethod(method) || MethodTagsHandler.isSetterMethod(method)) {
                            propertyName = MethodTagsHandler.getPropertyNameFor(method);
                            if (!members.containsKey(propertyName)) {
                                memberNames.add(propertyName);
                                members.put(propertyName, method);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Determines whether the given documentation part contains the specified tag with the given parameter having the
     * given value.
     *
     * @param doc         The documentation part
     * @param tagName     The tag to be searched for
     * @param paramName   The parameter that the tag is required to have
     * @param paramValue  The value of the parameter
     * @return            boolean Whether the documentation part has the tag and parameter
     */
    private boolean checkTagAndParam(XDoc doc, String tagName, String paramName, String paramValue)
    {
        if (tagName == null) {
            return true;
        }
        if (!doc.hasTag(tagName)) {
            return false;
        }
        if (paramName == null) {
            return true;
        }
        if (!doc.getTag(tagName).getAttributeNames().contains(paramName)) {
            return false;
        }
        return (paramValue == null) || paramValue.equals(doc.getTagAttributeValue(tagName, paramName));
    }
}
