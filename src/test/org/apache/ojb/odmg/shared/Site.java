package org.apache.ojb.odmg.shared;

import java.io.Serializable;

/**
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: Site.java,v 1.1 2007-08-24 22:17:29 ewestfal Exp $
 */
public class Site implements Serializable
{
    private Integer id;
    private String name;
    private Integer year;
    private Integer semester;

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
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Integer getYear()
    {
        return year;
    }

    public void setYear(Integer year)
    {
        this.year = year;
    }

    public Integer getSemester()
    {
        return semester;
    }

    public void setSemester(Integer semester)
    {
        this.semester = semester;
    }
}
