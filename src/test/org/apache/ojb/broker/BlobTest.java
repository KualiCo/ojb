package org.apache.ojb.broker;

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

import org.apache.ojb.junit.PBTestCase;
import org.apache.ojb.broker.platforms.PlatformHsqldbImpl;
import org.apache.ojb.broker.platforms.PlatformPostgreSQLImpl;

import java.io.Serializable;

/**
 * This TestClass tests storing and retrieving Objects with BLOB/CLOB Attributes.
 * @version $Id: BlobTest.java,v 1.1 2007-08-24 22:17:27 ewestfal Exp $
 */
public class BlobTest extends PBTestCase
{
    private static final String SKIP_MESSAGE =
            "# Skip "+BlobTest.class.getName()+", DB does not support Blob/Clob #";

    /**
     * Known issue (to be fixed, warn in setup)
     * or DB lack of features (will never be fixed)?
     */
    private boolean knownIssue;
    private boolean skipTest;

    public BlobTest(String name)
    {
        super(name);
    }

    public static void main(String[] args)
    {
        String[] arr = {BlobTest.class.getName()};
        junit.textui.TestRunner.main(arr);
    }

    public void setUp() throws Exception
    {
        super.setUp();

        String platformClass = getPlatformClass();
        /*
        hsqldb<1.7.2 does not support Blob/Clob, so we skip test for this DB
        */
        knownIssue = false;
        if (platformClass.equals(PlatformHsqldbImpl.class.getName()))
        {
            skipTest = true;
        }
        else if (platformClass.equals(PlatformPostgreSQLImpl.class.getName())
            && ojbSkipKnownIssueProblem("LOB handling is not yet implemented for PostgreSQL"))
        {
            knownIssue = true;
            skipTest = true;
        }
    }

    public void testBlobInsertion() throws Exception
    {
        if (skipTest)
        {
            if (!knownIssue)
            {
                System.out.println(SKIP_MESSAGE);
            }
            return;
        }

        int size = 5000;

        ObjectWithBlob obj = new ObjectWithBlob();

        byte[] barr = new byte[size];
        char[] carr = new char[size];
        for (int i = 0; i < size; i++)
        {
            barr[i] = (byte) 'x';
            carr[i] = 'y';
        }

        // obj.setId(1); we use autoincrement
        obj.setBlob(barr);
        obj.setClob(new String(carr));
        broker.beginTransaction();
        broker.store(obj);
        broker.commitTransaction();
        broker.clearCache();

        Identity oid = new Identity(obj, broker);
        ObjectWithBlob obj1 = (ObjectWithBlob) broker.getObjectByIdentity(oid);
        assertNotNull("BLOB was not stored", obj1.getBlob());
        assertNotNull("CLOB was not stored", obj1.getClob());
        assertEquals(obj.getBlob().length, obj1.getBlob().length);
        assertEquals(obj.getClob().length(), obj1.getClob().length());

    }

    public void testReadNullBlob()
    {
        if (skipTest)
        {
            if (!knownIssue)
            {
                System.out.println(SKIP_MESSAGE);
            }
            return;
        }

        ObjectWithBlob obj = new ObjectWithBlob();

        // obj.setId(1); we use autoincrement
        obj.setBlob(null);
        obj.setClob(null);
        broker.beginTransaction();
        broker.store(obj);
        broker.commitTransaction();
        broker.clearCache();

        Identity oid = new Identity(obj, broker);
        ObjectWithBlob obj1 = (ObjectWithBlob) broker.getObjectByIdentity(oid);

        assertEquals(null, obj1.getBlob());
        assertEquals(null, obj1.getClob());
    }


    //*******************************************************
    // inner class - test class
    //*******************************************************
    public static class ObjectWithBlob implements Serializable
    {
        private int id;

        private byte[] blob;

        private String clob;


        /**
         * Gets the blob.
         * @return Returns a byte[]
         */
        public byte[] getBlob()
        {
            return blob;
        }

        /**
         * Sets the blob.
         * @param blob The blob to set
         */
        public void setBlob(byte[] blob)
        {
            this.blob = blob;
        }

        /**
         * Gets the clob.
         * @return Returns a char[]
         */
        public String getClob()
        {
            return clob;
        }

        /**
         * Sets the clob.
         * @param clob The clob to set
         */
        public void setClob(String clob)
        {
            this.clob = clob;
        }

        /**
         * Gets the id.
         * @return Returns a int
         */
        public int getId()
        {
            return id;
        }

        /**
         * Sets the id.
         * @param id The id to set
         */
        public void setId(int id)
        {
            this.id = id;
        }
    }

}
