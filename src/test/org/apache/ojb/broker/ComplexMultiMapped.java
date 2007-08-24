package org.apache.ojb.broker;

import java.io.Serializable;
import java.util.Date;

/**
 * Test case helper class.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: ComplexMultiMapped.java,v 1.1 2007-08-24 22:17:28 ewestfal Exp $
 */
public class ComplexMultiMapped
{
    //*******************************************************
    // Inner class
    //*******************************************************
    public static class PersistentA implements Serializable
    {
        private int ID = 0;
        private String value1;
        private int value2;
        private Date value3;
        private String ojbConcreteClass = PersistentA.class.getName();

        public int getID()
        {
            return ID;
        }

        public void setID(int ID)
        {
            this.ID = ID;
        }

        public String getValue1()
        {
            return value1;
        }

        public void setValue1(String value1)
        {
            this.value1 = value1;
        }

        public int getValue2()
        {
            return value2;
        }

        public void setValue2(int value2)
        {
            this.value2 = value2;
        }

        public Date getValue3()
        {
            return value3;
        }

        public void setValue3(Date value3)
        {
            this.value3 = value3;
        }

        public String getOjbConcreteClass()
        {
            return ojbConcreteClass;
        }

        public void setOjbConcreteClass(String ojbConcreteClass)
        {
            this.ojbConcreteClass = ojbConcreteClass;
        }
    }


    //*******************************************************
    // Inner class
    //*******************************************************
    public static class PersistentB implements Serializable
    {
        private int ID = 0;
        private String value4;
        private int value5;
        private Date value6;
        private String ojbConcreteClass = PersistentB.class.getName();

        public int getID()
        {
            return ID;
        }

        public void setID(int ID)
        {
            this.ID = ID;
        }

        public String getValue4()
        {
            return value4;
        }

        public void setValue4(String value4)
        {
            this.value4 = value4;
        }

        public int getValue5()
        {
            return value5;
        }

        public void setValue5(int value5)
        {
            this.value5 = value5;
        }

        public Date getValue6()
        {
            return value6;
        }

        public void setValue6(Date value6)
        {
            this.value6 = value6;
        }

        public String getOjbConcreteClass()
        {
            return ojbConcreteClass;
        }

        public void setOjbConcreteClass(String ojbConcreteClass)
        {
            this.ojbConcreteClass = ojbConcreteClass;
        }
    }


    //*******************************************************
    // Inner class
    //*******************************************************
    public static class PersistentC implements Serializable
    {
        private int ID = 0;
        private String value1;
        private int value2;
        private Date value3;
        private String value4;
        private int value5;
        private Date value6;
        private String value7;
        private String ojbConcreteClass = PersistentC.class.getName();

        public int getID()
        {
            return ID;
        }

        public void setID(int ID)
        {
            this.ID = ID;
        }

        public String getValue1()
        {
            return value1;
        }

        public void setValue1(String value1)
        {
            this.value1 = value1;
        }

        public int getValue2()
        {
            return value2;
        }

        public void setValue2(int value2)
        {
            this.value2 = value2;
        }

        public Date getValue3()
        {
            return value3;
        }

        public void setValue3(Date value3)
        {
            this.value3 = value3;
        }

        public String getValue4()
        {
            return value4;
        }

        public void setValue4(String value4)
        {
            this.value4 = value4;
        }

        public int getValue5()
        {
            return value5;
        }

        public void setValue5(int value5)
        {
            this.value5 = value5;
        }

        public Date getValue6()
        {
            return value6;
        }

        public void setValue6(Date value6)
        {
            this.value6 = value6;
        }

        public String getValue7()
        {
            return value7;
        }

        public void setValue7(String value7)
        {
            this.value7 = value7;
        }

        public String getOjbConcreteClass()
        {
            return ojbConcreteClass;
        }

        public void setOjbConcreteClass(String ojbConcreteClass)
        {
            this.ojbConcreteClass = ojbConcreteClass;
        }
    }

    //*******************************************************
    // Inner class
    //*******************************************************
    public static class PersistentD extends PersistentB
    {
        private int ID = 0;
        private String value1;
        private int value2;
        private Date value3;
        private String ojbConcreteClass = PersistentD.class.getName();

        public int getID()
        {
            return ID;
        }

        public void setID(int ID)
        {
            this.ID = ID;
        }

        public String getValue1()
        {
            return value1;
        }

        public void setValue1(String value1)
        {
            this.value1 = value1;
        }

        public int getValue2()
        {
            return value2;
        }

        public void setValue2(int value2)
        {
            this.value2 = value2;
        }

        public Date getValue3()
        {
            return value3;
        }

        public void setValue3(Date value3)
        {
            this.value3 = value3;
        }

        public String getOjbConcreteClass()
        {
            return ojbConcreteClass;
        }

        public void setOjbConcreteClass(String ojbConcreteClass)
        {
            this.ojbConcreteClass = ojbConcreteClass;
        }
    }

    //*******************************************************
    // Inner class
    //*******************************************************
    public static class PersistentE extends PersistentB
    {
        private int ID = 0;
        private String value1;
        private int value2;
        private Date value3;
        private String ojbConcreteClass =
                PersistentE.class.getName();

        public int getID()
        {
            return ID;
        }

        public void setID(int ID)
        {
            this.ID = ID;
        }

        public String getValue1()
        {
            return value1;
        }

        public void setValue1(String value1)
        {
            this.value1 = value1;
        }

        public int getValue2()
        {
            return value2;
        }

        public void setValue2(int value2)
        {
            this.value2 = value2;
        }

        public Date getValue3()
        {
            return value3;
        }

        public void setValue3(Date value3)
        {
            this.value3 = value3;
        }

        public String getOjbConcreteClass()
        {
            return ojbConcreteClass;
        }

        public void setOjbConcreteClass(String ojbConcreteClass)
        {
            this.ojbConcreteClass = ojbConcreteClass;
        }
    }


    //*******************************************************
    // Inner class
    //*******************************************************
    public static class PersistentF extends PersistentE
    {
    }
}
