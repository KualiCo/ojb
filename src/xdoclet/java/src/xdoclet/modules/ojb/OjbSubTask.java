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

/**
 * Generates the XML metadata for OJB-persistent classes and the descriptions of the associated database tables.
 *
 * @author        <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 * @created       March 22, 2003
 * @ant.element   display-name="OJB Repository MetaData" name="ojbrepository" parent="xdoclet.modules.ojb.OjbDocletTask"
 */
public class OjbSubTask extends XmlSubTask
{
    private static String OJB_TEMPLATE_FILE = "resources/ojb_xml.xdt";
    private static String OJB_REPOSITORY_FILE_NAME = "repository_user.xml";

    /**
     * Creates a new object.
     */
    public OjbSubTask()
    {
        setTemplateURL(getClass().getResource(OJB_TEMPLATE_FILE));
        setDestinationFile(OJB_REPOSITORY_FILE_NAME);
        setSubTaskName("ojbrepository");
    }

    /**
     * Whether to generate verbose output
     */
    private boolean _verbose;

    /**
     * Returns whether we generate verbose output.
     * 
     * @return <code>true</code> if we generate verbose output
     */
    public boolean getVerbose()
    {
        return _verbose;
    }

    /**
     * Specifies whether we generate verbose output.
     * 
     * @param beVerbose Whether we generate verbose output
     */
    public void setVerbose(boolean beVerbose)
    {
        _verbose = beVerbose;
    }

    /**
     * Called to validate configuration parameters.
     *
     * @exception XDocletException  Is not thrown
     */
    public void validateOptions() throws XDocletException
    {
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
        Log log = LogUtil.getLog(OjbSubTask.class, "startProcess");

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
        System.out.println("Generating ojb repository descriptor ("+getDestinationFile()+")");
    }
}
