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

import xdoclet.DocletTask;
import xdoclet.modules.ojb.constraints.ConstraintsBase;

/**
 * A task that executes various OJB-specific sub-tasks.
 *
 * @author        <a href="mailto:tomdz@users.sourceforge.net">Thomas Dudziak (tomdz@users.sourceforge.net)</a>
 * @created       March 22, 2003
 * @ant.element   name="ojbdoclet" display-name="OJB Task"
 */
public class OjbDocletTask extends DocletTask
{
    /** The amount of checks we perform */
    private String _checkingLevel = ConstraintsBase.CHECKLEVEL_STRICT;

    public void setChecks(String level)
    {
        if (ConstraintsBase.CHECKLEVEL_NONE.equals(level))
        {
            LogHelper.warn(true,
                           OjbDocletTask.class,
                           "setChecks",
                           "Disabling checks. Please use this level only if you have problems with the other check levels. In this case, please post the problem to the ojb-user mailing list.");
            _checkingLevel = level;
        }
        else if (ConstraintsBase.CHECKLEVEL_BASIC.equals(level))
        {
            LogHelper.warn(true,
                           OjbDocletTask.class,
                           "setChecks",
                           "Disabling strict checks.");
            _checkingLevel = level;
        }
        else if (ConstraintsBase.CHECKLEVEL_STRICT.equals(level))
        {
            _checkingLevel = level;
        }
        else
        {
            LogHelper.warn(true,
                           OjbDocletTask.class,
                           "setChecks",
                           "Unknown checks value: "+level+". Using default level "+ConstraintsBase.CHECKLEVEL_STRICT+" instead.");
            _checkingLevel = ConstraintsBase.CHECKLEVEL_STRICT;
        }
    }

    public String getChecks()
    {
        return _checkingLevel;
    }
}
