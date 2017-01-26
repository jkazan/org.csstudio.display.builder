/*******************************************************************************
 * Copyright (c) 2015-2016 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.csstudio.display.builder.runtime;

import static org.csstudio.display.builder.runtime.RuntimePlugin.logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/** Execute external command
 *
 *  <p>Logs standard output of the command as INFO,
 *  error output as warning.
 *
 *  <p>For briefly running commands, it awaits the exit status
 *  and then returns the exit code.
 *
 *  <p>For longer running commands, the logging remains active
 *  but the call returns <code>null</code> since the exit code is not known.
 *
 *  @author Kay Kasemir
 */
public class CommandExecutor implements Callable<Integer>
{
    /** Seconds to wait for a launched program */
    private static final int WAIT_SECONDS = 5;

    /** Thread that writes data from stream to log */
    private static class LogWriter extends Thread
    {
        private final BufferedReader reader;
        private final String cmd;
        private Level level;

        public LogWriter(final InputStream stream, final String cmd, final Level level)
        {
            super("LogWriter " + level.getName() + " " + cmd);
            reader = new BufferedReader(new InputStreamReader(stream));
            this.cmd = cmd;
            this.level = level;
            setDaemon(true);
        }

        @Override
        public void run()
        {
            try
            {
                String line;
                while ((line = reader.readLine()) != null)
                    logger.log(level, "Cmd {0}: {1}", new Object[] { cmd, line });
            }
            catch (Exception ex)
            {
                logger.log(Level.WARNING, "Error reading cmd output", ex);
            }
        }
    };

    private final ProcessBuilder process_builder;
    private volatile Process process;

    public CommandExecutor(final String cmd, final File directory)
    {
        process_builder = new ProcessBuilder(splitCmd(cmd)).directory(directory);
    }

    /** Split command into items, honoring double quotes
     *  (no 'escape', no single quotes)
     *  @param cmd "cmd arg1 \"another arg\""
     *  @return [ "cmd", "arg1", "another arg" ]
     */
    public static List<String> splitCmd(final String cmd)
    {
        final List<String> items = new ArrayList<>();
        final int len = cmd.length();
        int i = 0;
        final StringBuilder line = new StringBuilder();
        while (i < len)
        {
            char c = cmd.charAt(i);
            if (c == ' '  ||  c == '\t')
            {
                items.add(line.toString());
                line.delete(0, line.length());
                do
                    ++i;
                while (i < len  &&
                       (cmd.charAt(i) == ' '  ||  cmd.charAt(i) == '\t'));
            }
            else if (c == '"')
            {
                ++i;
                while (i < len  &&  cmd.charAt(i) != '"')
                    line.append(cmd.charAt(i++));
                if (i < len  &&  cmd.charAt(i) == '"')
                    ++i;
            }
            else
            {
                line.append(c);
                ++i;
            }
        }
        if (line.length() > 0)
            items.add(line.toString());
        return items;
    }

    @Override
    public Integer call() throws Exception
    {
        // Get 'basename' of command
        String cmd = process_builder.command().get(0);
        final int sep = cmd.lastIndexOf('/');
        if (sep >= 0)
            cmd = cmd.substring(sep+1);

        process = process_builder.start();
        // Send stdout and error output to log
        final Thread stdout = new LogWriter(process.getInputStream(), cmd, Level.INFO);
        final Thread stderr = new LogWriter(process.getErrorStream(), cmd, Level.WARNING);
        stdout.start();
        stderr.start();

        // Wait for some time...
        if (process.waitFor(WAIT_SECONDS, TimeUnit.SECONDS))
        {   // Process completed, await exit of log watching threads
            stderr.join();
            stdout.join();
            // Check exit code
            final int status = process.exitValue();
            if (status != 0)
                logger.log(Level.WARNING, "Command {0} exited with status {1}",  new Object[] { process_builder.command(), status });
            return status;
        }

        // Leave running, continuing to log outputs, but no longer checking status
        return null;
    }

    @Override
    public String toString()
    {
        final Process p = process;
        if (p == null)
            return "CommandExecutor (idle): " +  process_builder.command().get(0);
        else if (p.isAlive())
            return "CommandExecutor (running): " +  process_builder.command().get(0);
        else
            return "CommandExecutor (" + p.exitValue() + "): " +  process_builder.command().get(0);
    }
}