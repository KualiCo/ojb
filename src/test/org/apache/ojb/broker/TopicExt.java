package org.apache.ojb.broker;

/**
 * Extension of {@link Topic}.
 */
public class TopicExt extends Topic
{
    private String description;

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }
}
