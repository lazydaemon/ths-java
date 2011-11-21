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
package com.googlecode.httl.support.translators.expression;

import java.text.ParseException;
import java.util.Map;

import com.googlecode.httl.Expression;
import com.googlecode.httl.support.Translator;

/**
 * AbstractExpression
 * 
 * @author Liang Fei (liangfei0201 AT gmail DOT com)
 */
public abstract class AbstractExpression implements Expression {
    
    private final Translator resolver;

    private final String source;
    
    private final int offset;
    
    private final Map<String, Class<?>> parameterTypes;

    public AbstractExpression(Translator resolver, String source, int offset, Map<String, Class<?>> parameterTypes){
        this.resolver = resolver;
        this.source = source;
        this.offset = offset;
        this.parameterTypes = parameterTypes;
    }

    public Translator getResolver() {
        return resolver;
    }
    
    public String getSource() {
        return source;
    }
    
    public int getOffset() {
        return offset;
    }

    public Map<String, Class<?>> getParameterTypes() {
        return parameterTypes;
    }
    
    public Class<?>[] getReturnTypes() throws ParseException {
        Class<?> type = getReturnType();
        return type == null ? new Class<?>[0] : new Class<?>[] { type };
    }
    
    public Object evaluate(Map<String, Object> parameters) throws ParseException {
        return null;
    }
    
}
