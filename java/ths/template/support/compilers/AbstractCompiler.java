/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.googlecode.httl.support.compilers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.googlecode.httl.Configurable;
import com.googlecode.httl.support.Compiler;
import com.googlecode.httl.support.util.ClassUtils;

/**
 * Abstract compiler. (SPI, Prototype, ThreadSafe)
 * 
 * @author Liang Fei (liangfei0201 AT gmail DOT com)
 */
public abstract class AbstractCompiler implements Compiler, Configurable {
    
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([_a-zA-Z][_a-zA-Z0-9\\.]*);");
    
    private static final Pattern CLASS_PATTERN = Pattern.compile("class\\s+([_a-zA-Z][_a-zA-Z0-9]*)\\s+");
    
    private File compileDirectory;
    
    public void configure(Map<String, String> config) {
        String directory = config.get(COMPILE_DIRECTORY);
        if (directory != null && directory.trim().length() > 0) {
            File file = new File(directory);
            if (file.exists() && file.isDirectory()) {
                this.compileDirectory = file;
            }
        }
    }
    
    protected void saveBytecode(String name, byte[] bytecode) throws IOException {
        // ClassUtils.checkBytecode(name, bytecode);
        if (compileDirectory != null && compileDirectory.exists()) {
            File file = new File(compileDirectory, name.replace('.', '/') + ".class");
            File dir = file.getParentFile();
            if (! dir.exists()) {
                boolean ok = dir.mkdirs();
                if (! ok) {
                    throw new IOException("Failed to create directory " + dir.getAbsolutePath());
                }
            }
            FileOutputStream out = new FileOutputStream(file);
            try {
                out.write(bytecode);
                out.flush();
            } finally {
                out.close();
            }
        }
    }

    public Class<?> compile(String code) throws ParseException {
        code = code.trim();
        Matcher matcher = PACKAGE_PATTERN.matcher(code);
        String pkg;
        if (matcher.find()) {
            pkg = matcher.group(1);
        } else {
            pkg = "";
        }
        matcher = CLASS_PATTERN.matcher(code);
        String cls;
        if (matcher.find()) {
            cls = matcher.group(1);
        } else {
            throw new IllegalArgumentException("No such class name in " + code);
        }
        String className = pkg != null && pkg.length() > 0 ? pkg + "." + cls : cls;
        try {
            return Class.forName(className, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            if (! code.endsWith("}")) {
                throw new ParseException("The java code not endsWith \"}\", code: \n" + code + "\n", code.length() - 1);
            }
            try {
                return doCompile(className, code);
            } catch (ParseException t) {
                throw t;
            } catch (Throwable t) {
                throw new ParseException("Failed to compile class, cause: " + t.getMessage() + ", class: " + className + ", code: \n" + code + "\n, stack: " + ClassUtils.toString(t), 0);
            }
        }
    }
    
    protected abstract Class<?> doCompile(String name, String source) throws Throwable;

}
