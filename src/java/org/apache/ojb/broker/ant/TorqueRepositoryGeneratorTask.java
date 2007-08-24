package org.apache.ojb.broker.ant;

/* Copyright 2002-2005 The Apache Software Foundation
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

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.taskdefs.Java;

public class TorqueRepositoryGeneratorTask extends Task {

    private String inputFile;
    private String outputFile;
    private String database;

    // Optional parameters to the task
    private Reference classpathRef = null;
    private Path classpath = null;
    private String indexTablespace = "";
    private boolean useNativeIncrement = true;

    public void setInputFile(String inputFile) {
        this.inputFile = inputFile;
    }

    public String getInputFile() {
        return this.inputFile;
    }

    public void setOutputFile(String outputFile) {
        this.outputFile = outputFile;
    }

    public String getOutputFile() {
        return this.outputFile;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getDatabase() {
        return this.database;
    }

    public void setIndexTablespace(String indexTablespace) {
        this.indexTablespace = indexTablespace;
    }

    public String getIndexTablespace() {
        return this.indexTablespace;
    }

    public void setUseNativeIncrement(String useNativeIncrement) {
        if ("yes".equalsIgnoreCase(useNativeIncrement)) {
            this.useNativeIncrement = true;
        } else {
            this.useNativeIncrement = false;
        }
    }

    public String getUseNativeIncrement() {
        if (this.useNativeIncrement) {
            return "yes";
        } else {
            return "no";
        }
    }

    public Path getClasspath() {
        return this.classpath;
    }

    public void setClasspath(Path path) {
        this.classpath = path;
    }

    public Path createClasspath() {
        if (this.classpath == null) {
            this.classpath = new Path(getProject());
        }

        return this.classpath.createPath();
    }

    public Reference getClasspathRef() {
        return this.classpathRef;
    }

    public void setClasspathRef(Reference classpathRef) {
        this.classpathRef = classpathRef;
    }

    public void execute() throws BuildException {
        checkParameters();
        Java javaTask = (Java)project.createTask("java");
        javaTask.createArg().setLine(this.inputFile + " " +
                                     this.outputFile + " " +
                                     this.database + " " +
                                     this.indexTablespace + " " +
                                     this.useNativeIncrement);
        javaTask.setFork(true);
        javaTask.setClassname("org.apache.ojb.broker.metadata.torque.TorqueRepositoryGenerator");
        if (this.classpathRef != null) {
            javaTask.setClasspathRef(this.classpathRef);
        }
        if (this.classpath != null) {
            javaTask.setClasspath(this.classpath);
        }
        javaTask.execute();
    }

    protected boolean isEmpty(String string) {
        return (string == null || string.trim().length() == 0);
    }

    protected void checkParameters() throws BuildException {
        StringBuffer errorMessageBuffer = new StringBuffer();

        if (isEmpty(this.database)) {
            errorMessageBuffer.append("Database property not set.\n");
        }

        if (isEmpty(this.inputFile)) {
            errorMessageBuffer.append("Input file property not set.\n");
        }

        if (isEmpty(this.outputFile)) {
            errorMessageBuffer.append("Output file property not set.\n");
        }

        if (errorMessageBuffer.toString().length() > 0) {
            throw new BuildException(errorMessageBuffer.toString());
        }
    }
}
