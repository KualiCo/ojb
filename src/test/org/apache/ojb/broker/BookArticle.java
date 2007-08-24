package org.apache.ojb.broker;

public class BookArticle extends Article implements java.io.Serializable
{

    /** books author*/
    private String author;
    /** ISBN No of Book*/
    private String isbn;

    public BookArticle()
    {
    }

    /**
     * Gets the author.
     * @return Returns a String
     */
    public String getAuthor()
    {
        return author;
    }

    /**
     * Sets the author.
     * @param author The author to set
     */
    public void setAuthor(String author)
    {
        this.author = author;
    }

    /**
     * Gets the isbn.
     * @return Returns a String
     */
    public String getIsbn()
    {
        return isbn;
    }

    /**
     * Sets the isbn.
     * @param isbn The isbn to set
     */
    public void setIsbn(String isbn)
    {
        this.isbn = isbn;
    }


}
