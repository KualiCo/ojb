package xdoclet.modules.ojb;

/* Copyright 2003-2005 The Apache Software Foundation
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

import org.apache.commons.logging.Log;
import xdoclet.XDocletException;
import xdoclet.XmlSubTask;
import xdoclet.util.LogUtil;
import xdoclet.util.Translator;

/**
 * Generates the XML schema for torque.
 *
 * @author        <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 * @created       April 9, 2003
 * @ant.element   display-name="Torque Schema" name="torqueschema" parent="xdoclet.modules.ojb.OjbDocletTask"
 */
public class TorqueSubTask extends XmlSubTask
{
    private static String TORQUE_TEMPLATE_FILE = "resources/torque_xml.xdt";
    private static String TORQUE_DEFAULT_FILE_NAME = "project-schema.xml";

    /** Whether to generate verbose output */
    private boolean _isVerbose = false;
    /** The database name */
    private String  _databaseName;
    /** The torque dtd url */
    private String  _dtdUrl = "http://jakarta.apache.org/turbine/dtd/database.dtd";
    /** Whether to generate foreignkey tags in the torque schema */
    private boolean _isGeneratingForeignkeys = true;
    
    
    /**
     * Creates a new object.
     */
    public TorqueSubTask()
    {
        setTemplateURL(getClass().getResource(TORQUE_TEMPLATE_FILE));
        setDestinationFile(TORQUE_DEFAULT_FILE_NAME);
        setSubTaskName("torqueschema");
    }

    /**
     * Returns whether we generate verbose output.
     * 
     * @return <code>true</code> if we generate verbose output
     */
    public boolean getVerbose()
    {
        return _isVerbose;
    }

    /**
     * Specifies whether we generate verbose output.
     * 
     * @param beVerbose Whether we generate verbose output
     */
    public void setVerbose(boolean beVerbose)
    {
        _isVerbose = beVerbose;
    }

    /**
     * Sets the dtd used for the torque schema.
     *
     * @param url The dtd url
     */
    public void setDtdUrl(String url)
    {
        _dtdUrl = url;
    }

    /**
     * Returns the dtd url.
     *
     * @return The url
     */
    public String getDtdUrl()
    {
        return _dtdUrl;
    }

    /**
     * Returns the database name.
     *
     * @return   The database name
     */
    public String getDatabaseName()
    {
        return _databaseName;
    }

    /**
     * Sets the databaseName used for the torque schema.
     *
     * @param databaseName  The database name
     */
    public void setDatabaseName(String databaseName)
    {
        _databaseName = databaseName;
    }

    /**
     * Specifies whether we should generate foreignkey tags.
     * 
     * @param generateForeignkeys <code>true</code> if we will generate foreignkey tags
     */
    public void setGenerateForeignkeys(boolean generateForeignkeys)
    {
        _isGeneratingForeignkeys = generateForeignkeys;
    }

    /**
     * Returns whether we generate foreignkey tags.
     * 
     * @return <code>true</code> if we generate foreignkey tags
     */
    public boolean getGenerateForeignkeys()
    {
        return _isGeneratingForeignkeys;
    }
    
    /**
     * Called to validate configuration parameters.
     *
     * @exception XDocletException  Is not thrown
     */
    public void validateOptions() throws XDocletException
    {
        if ((_databaseName == null) || (_databaseName.length() == 0)) {
            throw new XDocletException(Translator.getString(XDocletModulesOjbMessages.class,
                XDocletModulesOjbMessages.DATABASENAME_IS_REQUIRED));
        }
    }

    /**
     * Executes the task.
     *
     * @exception XDocletException  If an error occurs.
     */
    public void execute() throws XDocletException
    {
        startProcess();
    }

    public void startProcess() throws XDocletException
    {
        Log log = LogUtil.getLog(TorqueSubTask.class, "startProcess");

        if (log.isDebugEnabled()) {
            log.debug("destDir.toString()=" + getDestDir());
            log.debug("getTemplateURL()=" + getTemplateURL());
            log.debug("getDestinationfile()=" + getDestinationFile());
            log.debug("getOfType()=" + getOfType());
            log.debug("getExtent()=" + getExtent());
            log.debug("getHavingClassTag()=" + getHavingClassTag());
        }

        startProcessForAll();
    }

    /**
     * Describe what the method does
     *
     * @exception XDocletException
     */
    protected void engineStarted() throws XDocletException
    {
        System.out.println("Generating torque schema ("+getDestinationFile()+")");
    }
}
