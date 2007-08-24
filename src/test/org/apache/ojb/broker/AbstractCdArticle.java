package org.apache.ojb.broker;


public abstract class AbstractCdArticle extends Article implements java.io.Serializable
{
    /** record label*/
    private String labelname;
    /** books the musicians*/
    private String musicians;

    /**
     * Insert the method's description here.
     * Creation date: (05.01.2001 19:31:04)
     */
    public AbstractCdArticle()
    {
    }

    /**
     * Gets the labelname.
     * @return Returns a String
     */
    public String getLabelname()
    {
        return labelname;
    }

    /**
     * Sets the labelname.
     * @param labelname The labelname to set
     */
    public void setLabelname(String labelname)
    {
        this.labelname = labelname;
    }

    /**
     * Gets the musicians.
     * @return Returns a String
     */
    public String getMusicians()
    {
        return musicians;
    }

    /**
     * Sets the musicians.
     * @param musicians The musicians to set
     */
    public void setMusicians(String musicians)
    {
        this.musicians = musicians;
    }

}
