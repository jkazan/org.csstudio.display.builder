/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.model.properties;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Information about a script.
 *
 *  <p>Script has a path and inputs.
 *  Info may also contain the script text,
 *  in which case the path is only used to identify the
 *  script type.
 *
 *  <p>PVs will be created for each input/output.
 *  The script is executed whenever one or
 *  more of the 'triggering' inputs receive
 *  a new value.
 *
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class ScriptInfo
{
    /** Script 'path' used to indicate an embedded python script */
    public final static String EMBEDDED_PYTHON = "EmbeddedPy";

    /** Script 'path' used to indicate an embedded java script */
    public final static String EMBEDDED_JAVASCRIPT = "EmbeddedJs";

    private final String path, text;
    private final List<ScriptPV> pvs;

    /**
     * @param path Path (excluding name and file separator) to the script
     * @param name Name of the script
     * @return <code>true</code> if Python; that is, if *.py file with "python"
     *         (as in #!/usr/bin/env python) in first line
     */
    public static boolean isPython(final String path, String name)
    {
        if (!name.endsWith(".py"))
            return false;
        String firstline = null;
        name = new File(name).getName();
        try
        (
                BufferedReader br = new BufferedReader(new FileReader(path + File.separator + name));)
        {
            firstline = br.readLine();
        }
        catch (IOException e)
        {
            //log exception here?
        }
        return firstline != null && firstline.contains("python");
    }

    /** @param path Path to the script.
     *  @return <code>true</code> if Jython
     */
    public static boolean isJython(final String path)
    {
        return path.endsWith(".py")  ||  EMBEDDED_PYTHON.equals(path);
    }

    /** @param path Path to the script.
     *  @return <code>true</code> if JavaScript
     */
    public static boolean isJavaScript(final String path)
    {
        return path.endsWith(".js")  ||  EMBEDDED_JAVASCRIPT.equals(path);
    }

    /** @param path Script path. May be URL, contain macros, or use magic EMBEDDED_* name.
     *  @param text Text or <code>null</code>
     *  @param pvs PVs
     */
    public ScriptInfo(final String path, final String text, final List<ScriptPV> pvs)
    {
        this.path = Objects.requireNonNull(path);
        this.text = text;
        this.pvs = Collections.unmodifiableList(Objects.requireNonNull(pvs));
    }

    public ScriptInfo(final String path, final ScriptPV... pvs)
    {
        this(path, null, Arrays.asList(pvs));
    }

    /** @return Path to the script. May be URL, or contain macros.
     *          File ending or magic EMBEDDED_* name determines type of script
     */
    public String getPath()
    {
        return path;
    }

    /** @return Script text, may be <code>null</code> */
    public String getText()
    {
        return text;
    }

    /** @return Input/Output PVs used by the script */
    public List<ScriptPV> getPVs()
    {
        return pvs;
    }

    @Override
    public String toString()
    {
        return "ScriptInfo('" + path + "', " + pvs + ")";
    }
}
