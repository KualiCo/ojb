package org.apache.ojb.broker;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.ojb.broker.cache.LocalCacheTest;
import org.apache.ojb.broker.cache.ObjectCacheTest;
import org.apache.ojb.broker.locking.LockTestCommitedReads;
import org.apache.ojb.broker.locking.LockTestRepeatableReads;
import org.apache.ojb.broker.locking.LockTestSerializable;
import org.apache.ojb.broker.locking.LockTestUncommitedReads;
import org.apache.ojb.broker.locking.CommonsLockTestSerializable;
import org.apache.ojb.broker.locking.CommonsLockTestRepeatableReads;
import org.apache.ojb.broker.locking.CommonsLockTestCommittedReads;
import org.apache.ojb.broker.locking.CommonsLockTestUncommittedReads;
import org.apache.ojb.broker.metadata.CustomAttributesTest;
import org.apache.ojb.broker.metadata.MetadataMultithreadedTest;
import org.apache.ojb.broker.metadata.MetadataTest;
import org.apache.ojb.broker.metadata.PersistentFieldTest;
import org.apache.ojb.broker.metadata.ReadonlyTest;
import org.apache.ojb.broker.metadata.RepositoryElementsTest;
import org.apache.ojb.broker.metadata.RepositoryPersistorTest;
import org.apache.ojb.broker.sequence.AutoIncrementTest;
import org.apache.ojb.broker.sequence.NativeIdentifierTest;
import org.apache.ojb.broker.sequence.SMMultiThreadedTest;
import org.apache.ojb.broker.sequence.SequenceManagerTest;

/**
 * the facade to all TestCases in this package.
 *
 * @author Thomas Mahler
 * @version $Id: AllTests.java,v 1.1 2007-08-24 22:17:27 ewestfal Exp $
 */
public class AllTests extends junit.framework.TestSuite
{
    /**
     * runs the suite in a junit.textui.TestRunner.
     */
    public static void main(String[] args)
    {
        String[] arr = {AllTests.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    /** build a TestSuite from all the TestCases in this package*/
    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(QueryTest.class);
        suite.addTestSuite(EmptyTableTest.class);
        suite.addTestSuite(PersistenceBrokerTest.class);
        suite.addTestSuite(BrokerExamples.class);
        suite.addTestSuite(ProxyExamples.class);
        suite.addTestSuite(PolymorphicExtents.class);
        suite.addTestSuite(TreeTest.class);
        suite.addTestSuite(TypedCollectionsTest.class);
        suite.addTestSuite(AutomaticForeignKeys.class);
        suite.addTestSuite(OptimisticLockingTest.class);
        suite.addTestSuite(GraphTest.class);
        suite.addTestSuite(PBListenerTest.class);
        suite.addTestSuite(ContractVersionEffectivenessTest.class);
        suite.addTestSuite(ComplexMultiMappedTableTest.class);
        suite.addTestSuite(ComplexMultiMappedTableWithCollectionByQueryTest.class);
        suite.addTestSuite(CollectionTest.class);
        suite.addTestSuite(BidirectionalAssociationTest.class);
        suite.addTestSuite(AutoIncrementWithRelatedObjectTest.class);
        suite.addTestSuite(OneToManyTest.class);
        suite.addTestSuite(PBRollbackTest.class);
        suite.addTestSuite(TransactionDemarcationTest.class);
        suite.addTestSuite(MultipleDBTest.class);
        suite.addTestSuite(RepositoryPersistorTest.class);
        suite.addTestSuite(CustomAttributesTest.class);
        suite.addTestSuite(SequenceManagerTest.class);
        suite.addTestSuite(SMMultiThreadedTest.class);
        suite.addTestSuite(KeyConstraintViolationTest.class);
        suite.addTestSuite(RsIteratorTest.class);
        suite.addTestSuite(BlobTest.class);
        suite.addTestSuite(CharacterTest.class);
        suite.addTestSuite(LogServiceTest.class);
        suite.addTestSuite(MetaDataSerializationTest.class);
        suite.addTestSuite(MetadataTest.class);
        suite.addTestSuite(MetadataMultithreadedTest.class);
        suite.addTestSuite(FieldConversionTest.class);
        suite.addTestSuite(FieldConversionTest_2.class);
        suite.addTestSuite(FieldConversionTest_3.class);
        suite.addTestSuite(FieldTypeTest.class);
        suite.addTestSuite(BatchModeTest.class);
        suite.addTestSuite(ObjectCacheTest.class);
        suite.addTestSuite(LocalCacheTest.class);
        suite.addTestSuite(ReferenceTest.class);
        suite.addTestSuite(ComplexReferenceTest.class);
        suite.addTestSuite(ExtentAwarePathExpressionsTest.class);
        suite.addTestSuite(MultipleTableExtentAwareQueryTest.class);
        suite.addTestSuite(RepositoryElementsTest.class);
        suite.addTestSuite(ConnectionFactoryTest.class);
        suite.addTestSuite(NativeIdentifierTest.class);
        suite.addTestSuite(AnonymousFieldsTest.class);
        suite.addTestSuite(AbstractExtentClassTest.class);
        suite.addTestSuite(NestedFieldsTest.class);
        suite.addTestSuite(ReadonlyTest.class);
        // arminw: this test doesn't pass without failure on all machines
        // because the behavior of the JVM gc is not predetermined.
        // suite.addTestSuite(ReferenceMapTest.class);
        suite.addTestSuite(MultithreadedReadTest.class);
        suite.addTestSuite(CollectionTest2.class);
        suite.addTestSuite(NumberAccuracyTest.class);
        suite.addTestSuite(AutoIncrementTest.class);
        suite.addTestSuite(PathTest.class);
        suite.addTestSuite(PrimaryKeyForeignKeyTest.class);
        suite.addTestSuite(PersistentFieldTest.class);
        suite.addTestSuite(InheritanceMultipleTableTest.class);
        suite.addTestSuite(M2NGraphTest.class);
        suite.addTestSuite(MtoNMapping.class);
        suite.addTestSuite(MtoNTest.class);
        suite.addTestSuite(M2NTest.class);
        suite.addTestSuite(LockTestSerializable.class);
        suite.addTestSuite(LockTestRepeatableReads.class);
        suite.addTestSuite(LockTestCommitedReads.class);
        suite.addTestSuite(LockTestUncommitedReads.class);
        suite.addTestSuite(CommonsLockTestSerializable.class);
        suite.addTestSuite(CommonsLockTestRepeatableReads.class);
        suite.addTestSuite(CommonsLockTestCommittedReads.class);
        suite.addTestSuite(CommonsLockTestUncommittedReads.class);
        suite.addTestSuite(OptimisticLockingMultithreadedTest.class);

        // BRJ: ensure shutdown of hsqldb
        suite.addTestSuite(HsqldbShutdown.class);
        return suite;
    }

}
