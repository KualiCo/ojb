package org.apache.ojb.broker;

/**
 * 
 * @author <a href="leandro@ibnetwork.com.br">Leandro Rodrigo Saad Cruz</a>
 */
public class Topic 
    extends BaseQualifierImpl
{
    private String importance;

    public String getImportance()
    {
        return importance;
    }

    public void setImportance(String value)
    {
        importance = value;
    }
}
