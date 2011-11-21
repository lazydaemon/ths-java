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

import java.text.ParseException;
import java.util.Map;

import com.googlecode.httl.Configurable;
import com.googlecode.httl.support.Compiler;
import com.googlecode.httl.support.util.ClassUtils;

/**
 * AdaptiveCompiler. (SPI, Singleton, ThreadSafe)
 * 
 * @see com.googlecode.httl.Engine#setCompiler(Compiler)
 * 
 * @author Liang Fei (liangfei0201 AT gmail DOT com)
 */
public class AdaptiveCompiler implements Compiler, Configurable {
    
    private Compiler compiler;

    public void configure(Map<String, String> config) {
        String version = config.get(JAVA_VERSION);
        if (version == null || ClassUtils.isBeforeJava6(version.trim())) {
            compiler = new JavassistCompiler();
        } else {
            compiler = new JdkCompiler();
        }
        if (compiler instanceof Configurable) {
            ((Configurable)compiler).configure(config);
        }
    }

    public Class<?> compile(String code) throws ParseException {
        return compiler.compile(code);
    }

}
