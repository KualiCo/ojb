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

import java.io.Serializable;

/**
 * Test case for java.lang.Character/java.lang.String to CHAR/VARCHAR mappings.
 *
 * @author <a href="mailto:mkalen@apache.org">Martin Kal&eacute;n</a>
 * @version $Id: CharacterTest.java,v 1.1 2007-08-24 22:17:27 ewestfal Exp $
 * @since 1.0.2
 */
public class CharacterTest extends PBTestCase
{

    public void testJavaCharacterToJdbcCharMapping() {
        final Character x = new Character('x');

        ObjectWithCharField obj = new ObjectWithCharField();
        obj.setCharacterCharField(x);
        pbPersist(obj);

        broker.clearCache();

        Identity oid = new Identity(obj, broker);
        ObjectWithCharField dbObj;
        assertNotNull(dbObj = (ObjectWithCharField) broker.getObjectByIdentity(oid));
        assertEquals(obj, dbObj);
    }

    public void testJavaCharacterToJdbcVarcharMapping() {
        final Character x = new Character('x');

        ObjectWithCharField obj = new ObjectWithCharField();
        obj.setCharacterVarcharField(x);
        pbPersist(obj);

        broker.clearCache();

        Identity oid = new Identity(obj, broker);
        ObjectWithCharField dbObj;
        assertNotNull(dbObj = (ObjectWithCharField) broker.getObjectByIdentity(oid));
        assertEquals(obj, dbObj);
    }

    public void testJavaStringToJdbcCharMapping() {
        final String strIn = "12345";

        ObjectWithCharField obj = new ObjectWithCharField();
        obj.setStringCharField(strIn);
        pbPersist(obj);

        broker.clearCache();

        Identity oid = new Identity(obj, broker);
        ObjectWithCharField dbObj;
        assertNotNull(dbObj = (ObjectWithCharField) broker.getObjectByIdentity(oid));
        assertEquals(obj.getId(), dbObj.getId());

        // mkalen: different JDBC-drivers do readback of CHAR(x) => java.lang.String differently,
        //          Oracle pads String with spaces to exactly x characters
        //          hsqldb clears trailing spaces
        //          PostgreSQL keeps some(v7)/all(v8) spaces
        // We can't assert much here, other than that the start of string is not mutated.
        final String strOut;
        assertNotNull(strOut = dbObj.getStringCharField());
        assertTrue(strOut, strOut.startsWith(strIn));
    }

    public void testJavaStringToJdbcVarcharMapping() {
        final String str = "12345";

        ObjectWithCharField obj = new ObjectWithCharField();
        obj.setStringVarcharField(str);
        pbPersist(obj);

        broker.clearCache();

        Identity oid = new Identity(obj, broker);
        ObjectWithCharField dbObj;
        assertNotNull(dbObj = (ObjectWithCharField) broker.getObjectByIdentity(oid));
        assertEquals(obj, dbObj);
    }

    public static void main(String[] args)
    {
        String[] testClasses = new String[]{ CharacterTest.class.getName() };
        junit.textui.TestRunner.main(testClasses);
    }

    public static class ObjectWithCharField implements Serializable
    {
        private int id;
        private Object characterCharField;
        private Object characterVarcharField;
        private String stringCharField;
        private String stringVarcharField;

        public ObjectWithCharField()
        {
        }

        public int getId()
        {
            return id;
        }

        public void setId(int id)
        {
            this.id = id;
        }

        public Object getCharacterCharField()
        {
            return characterCharField;
        }

        public void setCharacterCharField(Object characterCharField)
        {
            this.characterCharField = characterCharField;
        }

        public void setCharacterCharField(Character characterCharField)
        {
            this.characterCharField = characterCharField;
        }

        public Object getCharacterVarcharField()
        {
            return characterVarcharField;
        }

        public void setCharacterVarcharField(Object characterVarcharField)
        {
            this.characterVarcharField = characterVarcharField;
        }

        public void setCharacterVarcharField(Character characterVarcharField)
        {
            this.characterVarcharField = characterVarcharField;
        }

        public String getStringCharField()
        {
            return stringCharField;
        }

        public void setStringCharField(String stringCharField)
        {
            this.stringCharField = stringCharField;
        }

        public String getStringVarcharField()
        {
            return stringVarcharField;
        }

        public void setStringVarcharField(String stringVarcharField)
        {
            this.stringVarcharField = stringVarcharField;
        }

        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (!(o instanceof ObjectWithCharField)) return false;

            final ObjectWithCharField objectWithCharField = (ObjectWithCharField) o;

            if (id != objectWithCharField.id) return false;
            if (stringCharField != null ? !stringCharField.equals(objectWithCharField.stringCharField) : objectWithCharField.stringCharField != null) return false;
            if (stringVarcharField != null ? !stringVarcharField.equals(objectWithCharField.stringVarcharField) : objectWithCharField.stringVarcharField != null) return false;
            // Special comparison for the Object fields; use toString-representations if != null:
            final String myCharacterCharField = characterCharField != null ? characterCharField.toString() : null;
            final String myCharacterVarcharField = characterVarcharField != null ? characterVarcharField.toString() : null;
            final String otherCharacterCharField = objectWithCharField.characterCharField != null ? objectWithCharField.characterCharField.toString() : null;
            final String otherCharacterVarcharField = objectWithCharField.characterVarcharField != null ? objectWithCharField.characterVarcharField.toString() : null;
            if (myCharacterCharField != null ? !myCharacterCharField.equals(otherCharacterCharField) : otherCharacterCharField != null) return false;
            if (myCharacterVarcharField != null ? !myCharacterVarcharField.equals(otherCharacterVarcharField) : otherCharacterVarcharField != null) return false;

            return true;
        }

        public int hashCode()
        {
            int result;
            result = id;
            result = 29 * result + (characterCharField != null ? characterCharField.hashCode() : 0);
            result = 29 * result + (characterVarcharField != null ? characterVarcharField.hashCode() : 0);
            result = 29 * result + (stringCharField != null ? stringCharField.hashCode() : 0);
            result = 29 * result + (stringVarcharField != null ? stringVarcharField.hashCode() : 0);
            return result;
        }

        public String toString()
        {
            return "CharacterTest$ObjectWithCharField (id=" + id +
                   ", characterCharField=[" + characterCharField +
                   "], characterVarcharField=[" + characterVarcharField +
                   "], stringCharField=[" + stringCharField +
                   "], stringVarcharField=[" + stringVarcharField +
                   "])";
        }

    }

}
