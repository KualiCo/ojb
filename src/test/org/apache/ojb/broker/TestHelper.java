package org.apache.ojb.broker;

/**
 * Centralise the most important constants used in the test cases.
 *
 * @author <a href="mailto:armin@codeAuLait.de">Armin Waibel</a>
 * @version $Id: TestHelper.java,v 1.1 2007-08-24 22:17:27 ewestfal Exp $
 */
public class TestHelper
{
    public static final String DEF_REPOSITORY = "repository.xml";

    public static final String DEF_JCD_ALIAS = PersistenceBrokerFactory.getDefaultKey().getAlias();
    public static final String DEF_USER = PersistenceBrokerFactory.getDefaultKey().getUser();
    public static final String DEF_PASSWORD = PersistenceBrokerFactory.getDefaultKey().getPassword();
    public static final String DEF_DATABASE_NAME;
    static
    {
        DEF_DATABASE_NAME = buildDefDatabase();
    }

    public static final PBKey DEF_KEY = new PBKey(DEF_JCD_ALIAS, DEF_USER, DEF_PASSWORD);


    public static final String FAR_AWAY_CONNECTION_REPOSITORY = "Test_ConnectionDescriptor.xml";
    public static final String FAR_AWAY_JCD_ALIAS = "farAway";
    public static final String FAR_AWAY_DATABASE_NAME = "farAway#sa#";
    public static final PBKey FAR_AWAY_KEY = new PBKey(FAR_AWAY_JCD_ALIAS, "sa", "");

    public static final String DATABASE_REPOSITORY = "repository_database.xml";


    protected static String buildDefDatabase()
    {
        StringBuffer buf = new StringBuffer();
        PBKey defKey = PersistenceBrokerFactory.getDefaultKey();
        buf.append(defKey.getAlias());
        if ((defKey.getUser() != null && !(defKey.getUser().trim().equals(""))))
        {
            buf.append("#" + defKey.getUser());
        }
        if (defKey.getPassword() != null) buf.append("#" + defKey.getPassword());
        return buf.toString();
    }
}
