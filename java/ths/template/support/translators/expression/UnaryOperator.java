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

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import com.googlecode.httl.Expression;
import com.googlecode.httl.Template;
import com.googlecode.httl.support.Translator;
import com.googlecode.httl.support.util.ClassUtils;
import com.googlecode.httl.support.util.StringUtils;

/**
 * UnaryOperator
 * 
 * @author Liang Fei (liangfei0201 AT gmail DOT com)
 */
public final class UnaryOperator extends Operator {

    private AbstractExpression parameter;

    public UnaryOperator(Translator resolver, String source, int offset, Map<String, Class<?>> parameterTypes, 
                         Collection<Class<?>> functions, String[] packages, String name, int priority) {
        super(resolver, source, offset, parameterTypes, functions, packages, name, priority);
    }

    public Expression getParameter() {
        return parameter;
    }

    public void setParameter(AbstractExpression parameter) {
        this.parameter = parameter;
    }

    public Class<?> getReturnType() throws ParseException {
        if (getName().startsWith("new ")) {
            String type = getName().substring(4);
            return ClassUtils.forName(getPackages(), type);
        } else if (StringUtils.isTyped(getName())) {
            String type = getName();
            return ClassUtils.forName(getPackages(), type);
        } else if (StringUtils.isFunction(getName())) {
            String name = getName().substring(1);
            Class<?> t = getParameterTypes().get(name);
            if (t != null && Template.class.isAssignableFrom(t)) {
                return String.class;
            } else {
                Class<?>[] types = parameter.getReturnTypes();
                Collection<Class<?>> functions = getFunctions();
                if (functions != null && functions.size() > 0) {
                    for (Class<?> function : functions) {
                        try {
                            Method method = ClassUtils.searchMethod(function, name, types);
                            if (Object.class.equals(method.getDeclaringClass())) {
                                break;
                            }
                            return method.getReturnType();
                        } catch (NoSuchMethodException e) {
                        }
                    }
                }
                throw new ParseException("No such macro \"" + name + "\" or static method \"" + name + "\" with parameters " + Arrays.toString(types) + " in functions!", getOffset());
            }
        } else if (getName().equals("[")) {
            Class<?>[] types = parameter.getReturnTypes();
            if (Map.Entry.class.isAssignableFrom(types[0])) {
                return Map.class;
            } else {
                Object array = Array.newInstance(types[0], 0);
                return array.getClass();
            }
        } else {
            return parameter.getReturnType();
        }
    }

    public String getCode() throws ParseException {
        if (getName().startsWith("new ")) {
            return getName() + "(" + parameter.getCode() + ")";
        } else if (StringUtils.isTyped(getName())) {
            return "(" + getName() + ")(" + parameter.getCode() + ")";
        } else if (StringUtils.isFunction(getName())) {
            String name = getName().substring(1);
            Class<?> t = getParameterTypes().get(name);
            if (t != null && Template.class.isAssignableFrom(t)) {
                return name + ".render(new Object[] {" + parameter.getCode()+ "})";
            } else {
                Class<?>[] types = parameter.getReturnTypes();
                Collection<Class<?>> functions = getFunctions();
                if (functions != null && functions.size() > 0) {
                    for (Class<?> function : functions) {
                        try {
                            Method method = ClassUtils.searchMethod(function, name, types);
                            if (Object.class.equals(method.getDeclaringClass())) {
                                break;
                            }
                            return "((" + function.getName() + ")getEngine().getFunction(" + function.getName() + ".class))." + method.getName() + "(" + parameter.getCode() + ")";
                        } catch (NoSuchMethodException e) {
                        }
                    }
                }
                throw new ParseException("No such macro \"" + name + "\" or static method \"" + name + "\" with parameters " + Arrays.toString(types) + " in functions!", getOffset());
            }
        } else if (getName().equals("[")) {
            Class<?>[] types = parameter.getReturnTypes();
            if (Map.Entry.class.isAssignableFrom(types[0])) {
                return ClassUtils.class.getName() + ".toMap(new " + Map.Entry.class.getCanonicalName() + "[] {" + parameter.getCode() + "})";
            } else {
                return "new " + types[0].getCanonicalName() + "[] {" + parameter.getCode() + "}";
            }
        } else {
            if (parameter instanceof Operator
                    && ((Operator) parameter).getPriority() < getPriority()) {
                return getName() + " (" + parameter.getCode() + ")";
            }
            return getName() + " " + parameter.getCode();
        }
    }

}
