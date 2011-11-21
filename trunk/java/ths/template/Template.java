package ths.template;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Map;

import ths.core.Resource;
import ths.template.base.Statement;

public interface Template extends Resource, Statement {

    /**
     * Render the template to a string with thread local context.
     * 
     * @see com.googlecode.httl.Context#getContext()
     * @see com.googlecode.httl.Context#getParameters()
     * @return Template render result
     */
    String render();
    
    /**
     * Render the template to output stream with thread local context.
     * 
     * @see com.googlecode.httl.Context#getContext()
     * @see com.googlecode.httl.Context#getParameters()
     * @param output - Template output stream
     * @throws IOException Failed to output
     */
    void render(OutputStream output) throws IOException;
    
    /**
     * Render the template to writer with thread local context.
     * 
     * @see com.googlecode.httl.Context#getContext()
     * @see com.googlecode.httl.Context#getParameters()
     * @param writer - Template writer
     * @throws IOException Failed to writer
     */
    void render(Writer writer) throws IOException;

    /**
     * Render the template to a string use getParameters() names.
     * 
     * @see #getParameterTypes()
     * @param parameters - Template parameters
     * @return Template render result
     */
    String render(Object[] parameters);
    
    /**
     * Render the template to output stream use getParameters() names.
     * 
     * @see #getParameterTypes()
     * @param parameters - Template parameters
     * @param output - Template output stream
     * @throws IOException Failed to output
     */
    void render(Object[] parameters, OutputStream output) throws IOException;
    
    /**
     * Render the template to writer use getParameters() names.
     * 
     * @see #getParameterTypes()
     * @param parameters - Template parameters
     * @param writer - Template writer
     * @throws IOException Failed to writer
     */
    void render(Object[] parameters, Writer writer) throws IOException;
    
    /**
     * Render the template to a string.
     * 
     * @param parameters - Template parameters
     * @return Template render result
     */
    String render(Map<String, Object> parameters);

    /**
     * Render the template to output stream.
     * 
     * @param parameters - Template parameters
     * @param output - Template output stream
     * @throws IOException Failed to output
     */
    void render(Map<String, Object> parameters, OutputStream output) throws IOException;
    
    /**
     * Render the template to writer.
     * 
     * @param parameters - Template parameters
     * @param writer - Template writer
     * @throws IOException Failed to writer
     */
    void render(Map<String, Object> parameters, Writer writer) throws IOException;
    
    /**
     * Get the template engine.
     * 
     * @return Template engine.
     */
    Engine getEngine();
    
    /**
     * Get return types.
     * 
     * @return return types.
     */
    Map<String, Class<?>> getReturnTypes();
    
}
