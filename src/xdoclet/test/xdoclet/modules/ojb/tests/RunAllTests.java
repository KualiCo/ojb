package xdoclet.modules.ojb.tests;

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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test runner for the XDoclet OJB module tests.
 * Note: You should not run this class but rather the junit target in the ANT build file
 *       as there is a memory leak somewhere in the combination ant+xdoclet which results
 *       in an out-of-memory error when the tests are run after another within the same vm.
 *       This class rather serves as a helper to run specific tests (by commenting the other ones).
 *
 * @author <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 */
public class RunAllTests extends TestCase
{

    public RunAllTests(String name)
    {
        super(name);
    }

    public static void main(String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite("XDoclet OJB module tests");
/*
        suite.addTest(new TestSuite(ClassTagSimpleTests.class));
        suite.addTest(new TestSuite(ClassTagAcceptLocksAttributeTests.class));
        suite.addTest(new TestSuite(ClassTagAttributesAttributeTests.class));
        suite.addTest(new TestSuite(ClassTagDetermineExtendsAttributeTests.class));
        suite.addTest(new TestSuite(ClassTagDocumentationAttributeTests.class));
        suite.addTest(new TestSuite(ClassTagFactoryClassAndMethodAttributeTests.class));
        suite.addTest(new TestSuite(ClassTagGenerateRepositoryInfoAttributeTests.class));
        suite.addTest(new TestSuite(ClassTagGenerateTableInfoAttributeTests.class));
        suite.addTest(new TestSuite(ClassTagIncludeInheritedAttributeTests.class));
        suite.addTest(new TestSuite(ClassTagInitializationMethodAttributeTests.class));
        suite.addTest(new TestSuite(ClassTagIsolationLevelAttributeTests.class));
        suite.addTest(new TestSuite(ClassTagProxyAttributeTests.class));
        suite.addTest(new TestSuite(ClassTagRefreshAttributeTests.class));
        suite.addTest(new TestSuite(ClassTagRowReaderAttributeTests.class));
        suite.addTest(new TestSuite(ClassTagTableAttributeTests.class));
        suite.addTest(new TestSuite(ClassTagTableDocumentationAttributeTests.class));
        suite.addTest(new TestSuite(ExtentClassTagTests.class));
        suite.addTest(new TestSuite(ObjectCacheTagTests.class));
        suite.addTest(new TestSuite(IndexTagTests.class));
        suite.addTest(new TestSuite(FieldTagSimpleTests.class));
        suite.addTest(new TestSuite(FieldTagAccessAttributeTests.class));
        suite.addTest(new TestSuite(FieldTagAttributesAttributeTests.class));
        suite.addTest(new TestSuite(FieldTagAutoincrementAttributeTests.class));
        suite.addTest(new TestSuite(FieldTagColumnAttributeTests.class));
        suite.addTest(new TestSuite(FieldTagColumnDocumentationAttributeTests.class));
        suite.addTest(new TestSuite(FieldTagDocumentationAttributeTests.class));
        suite.addTest(new TestSuite(FieldTagConversionAttributeTests.class));
        suite.addTest(new TestSuite(FieldTagDefaultFetchAttributeTests.class));
        suite.addTest(new TestSuite(FieldTagIdAttributeTests.class));
        suite.addTest(new TestSuite(FieldTagIndexedAttributeTests.class));
        suite.addTest(new TestSuite(FieldTagJdbcTypeAttributeTests.class));
        suite.addTest(new TestSuite(FieldTagLengthAttributeTests.class));
        suite.addTest(new TestSuite(FieldTagLockingAttributeTests.class));
        suite.addTest(new TestSuite(FieldTagNameAttributeTests.class));
        suite.addTest(new TestSuite(FieldTagNullableAttributeTests.class));
        suite.addTest(new TestSuite(FieldTagPrecisionAndScaleAttributesTests.class));
        suite.addTest(new TestSuite(FieldTagPrimarykeyAttributeTests.class));
        suite.addTest(new TestSuite(FieldTagSequenceNameAttributeTests.class));
        suite.addTest(new TestSuite(FieldTagUpdateLockAttributeTests.class));
        suite.addTest(new TestSuite(AnonymousFieldTagTests.class));
        suite.addTest(new TestSuite(ReferenceTagAttributesAttributeTests.class));
        suite.addTest(new TestSuite(ReferenceTagAutoDeleteAttributeTests.class));
        suite.addTest(new TestSuite(ReferenceTagAutoRetrieveAttributeTests.class));
        suite.addTest(new TestSuite(ReferenceTagAutoUpdateAttributeTests.class));
        suite.addTest(new TestSuite(ReferenceTagClassRefAttributeTests.class));
        suite.addTest(new TestSuite(ReferenceTagDatabaseForeignkeyAttributeTests.class));
        suite.addTest(new TestSuite(ReferenceTagDocumentationAttributeTests.class));
        suite.addTest(new TestSuite(ReferenceTagForeignkeyAttributeTests.class));
        suite.addTest(new TestSuite(ReferenceTagOtmDependentAttributeTests.class));
        suite.addTest(new TestSuite(ReferenceTagProxyAttributeTests.class));
        suite.addTest(new TestSuite(ReferenceTagRefreshAttributeTests.class));
        suite.addTest(new TestSuite(AnonymousReferenceTagTests.class));
        suite.addTest(new TestSuite(CollectionTagSimpleTests.class));
        suite.addTest(new TestSuite(CollectionTagAttributesAttributeTests.class));
        suite.addTest(new TestSuite(CollectionTagAutoDeleteAttributeTests.class));
        suite.addTest(new TestSuite(CollectionTagAutoRetrieveAttributeTests.class));
        suite.addTest(new TestSuite(CollectionTagAutoUpdateAttributeTests.class));
        suite.addTest(new TestSuite(CollectionTagCollectionClassAttributeTests.class));
        suite.addTest(new TestSuite(CollectionTagDocumentationAttributeTests.class));
        suite.addTest(new TestSuite(CollectionTagElementClassRefAttributeTests.class));
        suite.addTest(new TestSuite(CollectionTagForeignkeyAttributeTests.class));
        suite.addTest(new TestSuite(CollectionTagIndirectionTableAttributeTests.class));
        suite.addTest(new TestSuite(CollectionTagIndirectionTableDocumentationAttributesTests.class));
        suite.addTest(new TestSuite(CollectionTagIndirectionTablePrimarykeysAttributeTests.class));
        suite.addTest(new TestSuite(CollectionTagOrderbyAttributeTests.class));
        suite.addTest(new TestSuite(CollectionTagOtmDependentAttributeTests.class));
        suite.addTest(new TestSuite(CollectionTagProxyAttributeTests.class));
        suite.addTest(new TestSuite(CollectionTagQueryCustomizerAttributeTests.class));
        suite.addTest(new TestSuite(CollectionTagRefreshAttributeTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagSimpleTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagAccessAttributeTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagAttributesAttributeTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagAutoDeleteAttributeTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagAutoIncrementAttributeTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagAutoRetrieveAttributeTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagAutoUpdateAttributeTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagClassRefAttributeTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagCollectionClassAttributeTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagColumnAttributeTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagColumnDocumentationAttributeTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagConversionAttributeTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagDatabaseForeignkeyAttributeTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagDefaultFetchAttributeTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagDocumentationAttributeTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagElementClassRefAttributeTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagForeignkeyAttributeTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagIdAttributeTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagIgnoreAttributeTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagJdbcTypeAttributeTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagLengthAttributeTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagLockingAttributeTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagNullableAttributeTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagOrderbyAttributeTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagOtmDependentAttributeTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagPrecisionAndScaleAttributesTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagPrimarykeyAttributeTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagProxyAttributeTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagQueryCustomizerAttributeTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagRefreshAttributeTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagSequenceNameAttributeTests.class));
        suite.addTest(new TestSuite(ModifyInheritedTagUpdateLockAttributeTests.class));
        suite.addTest(new TestSuite(NestedTagSimpleTests.class));
        suite.addTest(new TestSuite(ModifyNestedTagSimpleTests.class));
        suite.addTest(new TestSuite(DeleteProcedureTagTests.class));
        suite.addTest(new TestSuite(InsertProcedureTagTests.class));
        suite.addTest(new TestSuite(UpdateProcedureTagTests.class));
        suite.addTest(new TestSuite(ProcedureArgumentTagTests.class));
*/
        
        return suite;
    }
}
