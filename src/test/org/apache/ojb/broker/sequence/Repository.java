package org.apache.ojb.broker.sequence;

import java.io.Serializable;

/**
 * Repository class for sequence test classes.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: Repository.java,v 1.1 2007-08-24 22:17:37 ewestfal Exp $
 */
public class Repository
{
    public static class SMDatabaseSequence implements Serializable
    {
        private Integer seqId;
        private String name;

        public SMDatabaseSequence(Integer seqId, String name)
        {
            this.seqId = seqId;
            this.name = name;
        }

        public SMDatabaseSequence()
        {
        }

        public Integer getSeqId()
        {
            return seqId;
        }

        public void setSeqId(Integer seqId)
        {
            this.seqId = seqId;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }
    }


    public static interface SMInterface extends Serializable
    {
        public Integer getId();
        public void setId(Integer id);

        public String getName();
        public void setName(String name);
    }

    public static class SMInterfaceExtendA implements SMInterface
    {
        private Integer id;
        private String name;

        public Integer getId()
        {
            return id;
        }

        public void setId(Integer id)
        {
            this.id = id;
        }

        public String getName()
        {
            return this.name;
        }

        public void setName(String name)
        {
            this.name = name;
        }
    }

    public static class SMInterfaceExtendAA extends SMInterfaceExtendA
    {
    }

    public static class SMInterfaceExtendAAA extends SMInterfaceExtendAA
    {
    }

    public static class SMInterfaceExtendAB extends SMInterfaceExtendA
    {
    }

    public static class SMInterfaceExtendB implements SMInterface
    {
        private Integer id;
        private String name;

        public Integer getId()
        {
            return id;
        }

        public void setId(Integer id)
        {
            this.id = id;
        }

        public String getName()
        {
            return this.name;
        }

        public void setName(String name)
        {
            this.name = name;
        }
    }

    public static class SMInterfaceExtendBB extends SMInterfaceExtendB
    {
    }



    public static class SMKey implements Serializable
    {
        private int intKey;
        private String name;
        private String stringKey;
        private Long longKey;
        private Integer integerKey;

        public int getIntKey()
        {
            return intKey;
        }

        public void setIntKey(int intKey)
        {
            this.intKey = intKey;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String getStringKey()
        {
            return stringKey;
        }

        public void setStringKey(String stringKey)
        {
            this.stringKey = stringKey;
        }

        public Long getLongKey()
        {
            return longKey;
        }

        public void setLongKey(Long longKey)
        {
            this.longKey = longKey;
        }

        public Integer getIntegerKey()
        {
            return integerKey;
        }

        public void setIntegerKey(Integer integerKey)
        {
            this.integerKey = integerKey;
        }

        public String toString()
        {
            StringBuffer buf = new StringBuffer(this.getClass().getName());
            buf.append(": name="+getName());
            buf.append(", intKey="+getIntKey());
            buf.append(", longKey="+getLongKey());
            buf.append(", stringKey="+getStringKey());
            buf.append(", integerKey="+getIntegerKey());
            return buf.toString();
        }
    }


    public static interface SMMax extends Serializable
    {
        public Integer getId();
        public void setId(Integer id);

        public String getName();
        public void setName(String name);
    }

    public static class SMMaxA implements SMMax
    {
        private Integer id;
        private String name;

        public Integer getId()
        {
            return id;
        }

        public void setId(Integer id)
        {
            this.id = id;
        }

        public String getName()
        {
            return this.name;
        }

        public void setName(String name)
        {
            this.name = name;
        }
    }


    public static class SMMaxAA extends SMMaxA
    {
    }

    public static class SMMaxAAA extends SMMaxAA
    {
    }

    public static class SMMaxAB extends SMMaxA
    {
    }

    public static class SMMaxB implements SMMax
    {
        private Integer id;
        private String name;

        public Integer getId()
        {
            return id;
        }

        public void setId(Integer id)
        {
            this.id = id;
        }

        public String getName()
        {
            return this.name;
        }

        public void setName(String name)
        {
            this.name = name;
        }
    }

    public static class SMMaxBB extends SMMaxB
    {
    }

    public static class SMSameTableA implements Serializable
    {
        private Integer id;
        private String name;

        public Integer getId()
        {
            return id;
        }

        public void setId(Integer id)
        {
            this.id = id;
        }

        public String getName()
        {
            return this.name;
        }

        public void setName(String name)
        {
            this.name = name;
        }
    }

    public static class SMSameTableAA extends SMSameTableA
    {
    }

    public static class SMSameTableB implements Serializable
    {
        private Integer id;
        private String name;

        public Integer getId()
        {
            return id;
        }

        public void setId(Integer id)
        {
            this.id = id;
        }

        public String getName()
        {
            return this.name;
        }

        public void setName(String name)
        {
            this.name = name;
        }
    }

    public static class SMSameTableBB extends SMSameTableB
    {
    }

}
