package ths.template;

import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

import ths.core.Configurable;
import ths.core.Resource;
import ths.core.Loader;
import ths.core.loaders.LoaderConfiguration;
import ths.core.loaders.StringLoader;

import ths.template.support.Cache;
import ths.template.support.Compiler;
import ths.template.support.EngineAware;
import ths.template.support.Filter;
import ths.template.support.Formatter;
import ths.template.support.Parser;
import ths.template.support.Translator;
import ths.template.support.sequences.StringSequence;
import ths.template.util.ClassUtils;
import ths.template.util.StringUtils;
import ths.template.util.UrlUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Engine implements Configurable<Configs> {
    
    /**
     * Default config path.
     */
    public static final String DEFAULT_CONFIG_LOCATION = "META-INF/template.properties";

    private static final ConcurrentMap<String, ReentrantLock> ENGINE_LOCKS = new ConcurrentHashMap<String, ReentrantLock>();

	private static final ConcurrentMap<String, Engine> ENGINES = new ConcurrentHashMap<String, Engine>();

    private final ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<String, ReentrantLock>();

    private final StringLoader literal = new StringLoader();

    private volatile Configs configuration;
    
    private volatile Logger logger = LoggerFactory.getLogger(getClass());

    private volatile Cache cache;
    
    private volatile Loader loader;

    private volatile Parser parser;

    private volatile Translator translator;

    private volatile Compiler compiler;

    private volatile Filter textFilter;
    
    private volatile Formatter<?> formatter;
    
    private volatile Filter filter;

    private volatile boolean reloadable;

    private final Map<Class<?>, Object> functions = new ConcurrentHashMap<Class<?>, Object>();

    private final List<StringSequence> sequences = new CopyOnWriteArrayList<StringSequence>();
    
    /**
     * Get template engine singleton.
     * 
     * @return template engine.
     */
	public static Engine getEngine() {
		return getEngine(DEFAULT_CONFIG_LOCATION);
	}

	/**
     * Get template engine singleton.
     * 
     * @param configPath config path.
     * @param configProperties config map.
     * @return template engine.
     */
	public static Engine getEngine(String configPath) {
		if (configPath == null || configPath.length() == 0) {
			throw new IllegalArgumentException("template config path == null");
		}
		ReentrantLock lock = ENGINE_LOCKS.get(configPath);
        if (lock == null) {
            ENGINE_LOCKS.putIfAbsent(configPath, new ReentrantLock());
            lock= ENGINE_LOCKS.get(configPath);
        }
        assert(lock != null);
        Engine engine = ENGINES.get(configPath);
        if (engine == null) { // double check
            lock.lock();
            try {
                engine = ENGINES.get(configPath);
                if (engine == null) { // double check
                    engine = new Engine(configPath);
                    ENGINES.put(configPath, engine);
                }
            } finally {
                lock.unlock();
            }
        }
        assert(engine != null);
        return engine;
	}
	
	/**
	 * Create template engine.
	 */
	public Engine() {
	    this(DEFAULT_CONFIG_LOCATION);
	}

	/**
	 * Create template engine.
	 * 
	 * @param configuration path
	 */
	public Engine(String configuration) {
        //this(ConfigUtils.loadProperties(configuration, DEFAULT_PATH.equals(configuration)));
		Configs config = new Configs();
		config.load(configuration);
		this.configure(config);
    }
	
	/**
	 * Get configuration.
	 * 
	 * @return configuration.
	 */
	public Configs getConfiguration() {
		return configuration;
	}
	
	@Override
    public synchronized void configure(Configs config) {
		this.configuration = config;
		
    	
		/*
        if (config == null || config.size() == 0) {
            return;
        }
        config = new HashMap<String, String>(config); // safe copy mutable argument
        for (Map.Entry<String, String> entry : new HashMap<String, String>(config).entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key.endsWith(Constants.PLUS)) {
                if (value != null && value.length() > 0) {
                    String k = key.substring(0, key.length() - Constants.PLUS.length());
                    String v = config.get(k);
                    if (v != null && v.length() > 0) {
                        v += "," + value;
                    } else {
                        v = value;
                    }
                    config.put(k, v);
                }
                config.remove(key);
            }
            if (value != null && value.startsWith(Constants.PLUS)) {
                value = value.substring(Constants.PLUS.length());
                String v = DEFAULT_CONFIGURATION.get(key);
                if (v != null && v.length() > 0) {
                    v += "," + value;
                } else {
                    v = value;
                }
                config.put(key, v);
            }
        }
        this.configuration = Collections.unmodifiableMap(config);
        */
		
        String cache = config.getCache();
        if (cache != null && cache.trim().length() > 0) {
            if (Constants.NULL.equals(cache.trim())) {
                setCache(null);
            } else {
                setCache((Cache) ClassUtils.newInstance(cache.trim()));
            }
        }
        
        String loader = config.getLoader();
        if (loader != null && loader.trim().length() > 0) {
            setLoader((Loader) ClassUtils.newInstance(loader.trim()));
        }
        
        String parser = config.getParser();
        if (parser != null && parser.trim().length() > 0) {
            setParser((Parser) ClassUtils.newInstance(parser.trim()));
        }
        
        String translator = config.getTranslator();
        if (translator != null && translator.trim().length() > 0) {
            setTranslator((Translator) ClassUtils.newInstance(translator.trim()));
        }
        String compiler = config.getCompiler();
        if (compiler != null && compiler.trim().length() > 0) {
            setCompiler((Compiler) ClassUtils.newInstance(compiler.trim()));
        }
        String rep = config.getTextFilter();
        if (rep != null && rep.trim().length() > 0) {
            setTextFilter((Filter) ClassUtils.newInstance(rep));
        }
        
        String fmt = config.getFormatter();
        if (fmt != null && fmt.trim().length() > 0) {
            setFormatter((Formatter<?>) ClassUtils.newInstance(fmt));
        }
        
        String flt = config.getFilter();
        if (flt != null && flt.trim().length() > 0) {
            setFilter((Filter) ClassUtils.newInstance(flt));
        }
        
        String fun = config.getFunctions();
        if (fun != null && fun.trim().length() > 0) {
            String[] funs = fun.trim().split("[\\s\\,]+");
            Object[] functions = new Object[funs.length];
            for (int i = 0; i < funs.length; i ++) {
                functions[i] = ClassUtils.newInstance(funs[i]);
            }
            setFunctions(functions);
        }
        
        String seq = config.getSequences();
        if (seq != null && seq.trim().length() > 0) {
            String[] ss = seq.trim().split(",");
            for (String s : ss) {
                s = s.trim();
                if (s.length() > 0) {
                    String[] ts = s.split("\\s+");
                    List<String> sequence = new ArrayList<String>();
                    for (String t : ts) {
                        t = t.trim();
                        if (t.length() > 0) {
                            sequence.add(t);
                        }
                    }
                    addSequence(sequence);
                }
            }
        }
        if (cache == null ) {
            throw new IllegalStateException("cache == null");
        }
        if (loader == null) {
            throw new IllegalStateException("loader == null");
        }
        if (parser == null) {
            throw new IllegalStateException("parser == null");
        }
        if (translator == null) {
            throw new IllegalStateException("translator == null");
        }
        if (compiler == null) {
            throw new IllegalStateException("compiler == null");
        }
        
        reloadable = "true".equalsIgnoreCase(config.getReloadable());
        boolean precompiled = "true".equalsIgnoreCase(config.getPrecompiled());
        if (precompiled) {
            try {
                List<String> list = getLoader().list();
                for (String name : list) {
                    try {
                        getTemplate(name);
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        
    }
    
    /**
     * Get expression.
     * 
     * @param source
     * @return
     * @throws ParseException
     */
    public Expression getExpression(String source) throws ParseException {
        return getExpression(source, null, 0);
    }

    /**
     * Get expression.
     * 
     * @param source
     * @param parameterTypes
     * @return
     * @throws ParseException
     */
    public Expression getExpression(String source, Map<String, Class<?>> parameterTypes) throws ParseException {
        return getExpression(source, parameterTypes, 0);
    }

    /**
     * Get expression.
     * 
     * @param source
     * @param parameterTypes
     * @return
     * @throws ParseException
     */
    public Expression getExpression(String source, Map<String, Class<?>> parameterTypes, int offset) throws ParseException {
        return getTranslator().translate(source, parameterTypes, offset);
    }
    
    /**
     * Get resource.
     * 
     * @param name
     * @return
     * @throws IOException
     * @throws ParseException
     */
    public Resource getResource(String name) throws IOException {
        return getResource(name, null);
    }

    /**
     * Get resource.
     * 
     * @param name
     * @param encoding
     * @return
     * @throws IOException
     * @throws ParseException
     */
    public Resource getResource(String name, String encoding) throws IOException {
        return getLoader().load(name, encoding);
    }

	/**
	 * Get template.
	 * 
	 * @param name
	 * @return template
	 * @throws IOException
	 * @throws ParseException
	 */
	public Template getTemplate(String name) throws IOException, ParseException {
		return getTemplate(name, null);
	}
	
	/**
	 * Get template.
	 * 
	 * @param name
	 * @param encoding
	 * @return template
	 * @throws IOException
	 * @throws ParseException
	 */
    public Template getTemplate(String name, String encoding) throws IOException, ParseException {
		if (name == null || name.trim().length() == 0) {
			throw new IllegalArgumentException("template name == null");
		}
		name = UrlUtils.cleanUrl(name.trim());
		int i = name.indexOf('#');
        if (i > 0) {
            getTemplate(name.substring(0, i), encoding);
        }
		Cache cache = this.cache; // safe copy reference
		if (cache == null) {
		    return parseTemplate(name, encoding);
		}
		ReentrantLock lock = locks.get(name);
        if (lock == null) {
            locks.putIfAbsent(name, new ReentrantLock());
            lock= locks.get(name);
        }
        assert(lock != null);
        Resource resource;
        if (reloadable) {
            resource = getLoader().load(name, encoding);
        } else {
            resource = null;
        }
		Template template = (Template) cache.get(name);
		if (template == null || (resource != null 
		        && resource.getLastModified() > template.getLastModified())) {
    		lock.lock();
    		try {
    			template = (Template) cache.get(name);
    			// double check
    			if (template == null || (resource != null 
    			        && resource.getLastModified() > template.getLastModified())) {
    				template = parseTemplate(name, encoding);
    				cache.put(name, template);
    			}
    		} finally {
    		    lock.unlock();
    		}
		}
		assert(template != null);
		return template;
	}

    /**
     * Parse the template. (No cache)
     * 
     * @param name - Template name
     * @return Template instance.
     * @throws IOException
     * @throws ParseException
     */
    public Template parseTemplate(String name) throws IOException, ParseException {
        return parseTemplate(name, null);
    }
    
    /**
     * Parse the template. (No cache)
     * 
     * @param name - Template name
     * @param encoding - Template encoding
     * @return Template instance.
     * @throws IOException
     * @throws ParseException
     */
    public Template parseTemplate(String name, String encoding) throws IOException, ParseException {
        if (name == null || name.trim().length() == 0) {
            throw new IllegalArgumentException("template name == null");
        }
        name = UrlUtils.cleanUrl(name.trim());
        Resource resource;
        if (literal.has(name)) {
            resource = literal.load(name, encoding);
        } else {
            resource = getLoader().load(name, encoding);
        }
        try {
            return getParser().parse(resource);
        } catch (ParseException e) {
            int offset = e.getErrorOffset();
            if (offset < 0) {
                offset = 0;
            }
            String location = null;
            if (offset > 0) {
                try {
                    Reader reader = resource.getReader();
                    try {
                        location = StringUtils.getLocationMessage(reader, offset);
                    } finally {
                        reader.close();
                    }
                } catch (Throwable t) {
                }
            }
            throw new ParseException("Failed to parse template " + name + ", cause: " + e.getMessage()  + ". occur to offset: " + offset + 
                                     (location == null || location.length() == 0 ? "" : ", " + location) 
                                     + ", stack: " + ClassUtils.toString(e), offset);
        }
    }
    
	/**
	 * Add literal template.
	 * 
	 * @param name - template name
	 * @param template
	 */
	public void addTemplate(String name, String template) {
	    literal.add(name, template);
	}
	
	/**
	 * Remove literal template.
	 * 
	 * @param name - template name
	 */
	public void removeTemplate(String name) {
        literal.remove(name);
    }
    
	/**
	 * Get template cache.
	 * 
	 * @return template cache.
	 */
    public Cache getCache() {
        return cache;
    }

    /**
     * Set template cache.
     * 
     * @param cache template cache.
     */
    public void setCache(Cache cache) {
        if (cache != null) {
            init(cache);
        }
        this.cache = cache;
	}
    
	/**
	 * Get template loader.
	 * 
	 * @return template loader.
	 */
	public Loader getLoader() {
		return loader;
	}

	/**
	 * Set template loader.
	 * 
	 * @param loader template loader.
	 */
	@SuppressWarnings("unchecked")
	public void setLoader(Loader loader) {
	    if (loader == null) {
	        throw new IllegalArgumentException("loader == null");
	    }
	    
        if (loader instanceof Configurable) {
            ((Configurable<LoaderConfiguration>) loader).configure(getConfiguration().getLoaderConfiguration());
        }
        
	    this.loader = loader;
	}

	/**
	 * Get template parser.
	 * 
	 * @return template parser.
	 */
	public Parser getParser() {
		return parser;
	}

	/**
	 * Set template parser.
	 * 
	 * @param parser template parser.
	 */
	public void setParser(Parser parser) {
	    if (parser == null) {
            throw new IllegalArgumentException("parser == null");
        }
	    init(parser);
		this.parser = parser;
	}

	/**
	 * Get expression resolver.
	 * 
	 * @return expression resolver.
	 */
	public Translator getTranslator() {
		return translator;
	}

	/**
	 * Set expression translator.
	 * 
	 * @param translator expression translator.
	 */
	public void setTranslator(Translator translator) {
	    if (translator == null) {
            throw new IllegalArgumentException("resolver == null");
        }
	    init(translator);
		this.translator = translator;
	}

	/**
	 * Get template compiler.
	 * 
	 * @return template compiler.
	 */
	public Compiler getCompiler() {
		return compiler;
	}

	/**
	 * Set template compiler.
	 * 
	 * @param compiler template compiler.
	 */
	public void setCompiler(Compiler compiler) {
	    if (compiler == null) {
            throw new IllegalArgumentException("compiler == null");
        }
	    init(compiler);
		this.compiler = compiler;
	}

	/**
	 * Get template formatter.
	 * 
	 * @return template formatter.
	 */
    public Formatter<?> getFormatter() {
        return formatter;
    }
    
    /**
     * Set template formatter.
     * 
     * @param formatter template formatter.
     */
    public void setFormatter(Formatter<?> formatter) {
        if (formatter == null) {
            throw new IllegalArgumentException("formatter == null");
        }
        init(formatter);
        this.formatter = formatter;
    }
    
    /**
     * Get template text filter.
     * 
     * @return template text filter.
     */
    public Filter getTextFilter() {
        return textFilter;
    }

    /**
     * Set template text filter.
     * 
     * @param filter template text filter.
     */
    public void setTextFilter(Filter filter) {
        if (filter == null) {
            throw new IllegalArgumentException("text filter == null");
        }
        init(filter);
        this.textFilter = filter;
    }
    
    /**
     * Get template filter.
     * 
     * @return template filter.
     */
    public Filter getFilter() {
        return filter;
    }

    /**
     * Set template filter.
     * 
     * @param filter template filters.
     */
    public void setFilter(Filter filter) {
        if (filter == null) {
            throw new IllegalArgumentException("filter == null");
        }
        init(filter);
        this.filter = filter;
    }

    /**
     * Get template functions.
     * 
     * @return template functions.
     */
    public Map<Class<?>, Object> getFunctions() {
        return Collections.unmodifiableMap(functions);
    }
    
    /**
     * Get function by type.
     * 
     * @param <T> - function type
     * @param type - function type
     * @return function
     */
    @SuppressWarnings("unchecked")
    public <T> T getFunction(Class<T> type) {
        return (T) functions.get(type);
    }

    /**
     * Set template functions.
     * 
     * @param functions template functions.
     */
    public void setFunctions(Object... functions) {
        this.functions.clear();
        addFunctions(functions);
    }
    
    /**
     * Add template functions.
     * 
     * @param functions
     */
    public void addFunctions(Object... functions) {
        if (functions != null && functions.length > 0) {
            for (Object function : functions) {
                if (function != null) {
                    Class<?> type = function.getClass();
                    init(function);
                    if (! this.functions.containsKey(type)) {
                        this.functions.put(type, function);
                    }
                }
            }
        }
    }
    
    /**
     * Remove template functions.
     * 
     * @param functions
     */
    public void removeFunctions(Object... functions) {
        if (functions != null && functions.length > 0) {
            for (Object function : functions) {
                if (function != null) {
                    Class<?> type = function.getClass();
                    this.functions.remove(type);
                }
            }
        }
    }
    
    /**
     * Get sequence.
     * 
     * @param begin - sequence begin
     * @param end - sequence end
     * @return sequence
     */
    public List<String> getSequence(String begin, String end) {
        for (StringSequence sequence : sequences) {
            if (sequence.containSequence(begin, end)) {
                return sequence.getSequence(begin, end);
            }
        }
        throw new IllegalStateException("No such sequence from \"" + begin + "\" to \"" + end + "\".");
    }
    
    /**
     * Add sequence.
     * 
     * @param sequence
     */
    public void addSequence(List<String> sequence) {
        if (sequence == null || sequence.size() == 0) {
            throw new IllegalArgumentException("sequence == null");
        }
        sequences.add(new StringSequence(sequence));
    }
    
    /**
     * Add sequence.
     * 
     * @param sequence
     */
    public void addSequence(String[] sequence) {
        addSequence(Arrays.asList(sequence));
    }
    
    @SuppressWarnings("unchecked")
	private void init(Object object) {
        if (object instanceof EngineAware) {
            ((EngineAware) object).setEngine(this);
        }
        
        if (object instanceof Configurable) {
            ((Configurable<Configs>) object).configure(getConfiguration());
        }
    }
}
