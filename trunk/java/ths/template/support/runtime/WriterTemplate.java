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
package com.googlecode.httl.support.runtime;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import com.googlecode.httl.Context;
import com.googlecode.httl.Engine;
import com.googlecode.httl.Resource;
import com.googlecode.httl.Template;
import com.googlecode.httl.support.util.ClassUtils;
import com.googlecode.httl.support.util.UnsafeStringWriter;

/**
 * Writer template. (SPI, Prototype, ThreadSafe)
 * 
 * @see com.googlecode.httl.Engine#getTemplate(String)
 * 
 * @author Liang Fei (liangfei0201 AT gmail DOT com)
 */
public abstract class WriterTemplate extends AbstractTemplate {
    
    private static final long serialVersionUID = 7127901461769617745L;
    
    public WriterTemplate(Engine engine, Resource resource){
        super(engine, resource);
    }
    
    public String render(Map<String, Object> parameters) {
        UnsafeStringWriter output = new UnsafeStringWriter();
        try {
            render(parameters, output);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return output.toString();
    }
    
    public void render(Map<String, Object> parameters, OutputStream output) throws IOException {
        render(parameters, new OutputStreamWriter(output));
    }
    
    public void render(Map<String, Object> parameters, Writer writer) throws IOException {
        if(parameters == null) {
            parameters = new HashMap<String, Object>();
        }
        Context context = Context.getContext();
        Template preTemplate = context.getTemplate();
        Map<String, Object> preParameters = context.getParameters();
        context.setTemplate(this).setParameters(parameters);
        try {
            doRender(parameters, writer);
        } catch (RuntimeException e) {
            throw (RuntimeException) e;
        } catch (IOException e) {
            throw (IOException) e;
        } catch (Exception e) {
            throw new IllegalStateException(ClassUtils.toString(e), e);
        } finally {
            context.setTemplate(preTemplate).setParameters(preParameters);
        }
    }
    
    protected abstract void doRender(Map<String, Object> parameters, Writer output) throws Exception;
    
}
