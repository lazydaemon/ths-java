package ths.core.loaders;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import ths.core.Configurable;
import ths.core.Resource;
import ths.core.Loader;
import com.googlecode.httl.Constants;
import com.googlecode.httl.support.util.ClassUtils;

public class MultiLoader implements Loader, Configurable {

    private final List<Loader> templateLoaders = new CopyOnWriteArrayList<Loader>();
    
	public void configure(Map<String, String> config) {
	    String value = config.get(Constants.LOADERS);
        if (value != null && value.trim().length() > 0) {
            String[] values = value.trim().split("[\\s\\,]+");
            Loader[] loaders = new Loader[values.length];
            for (int i = 0; i < values.length; i ++) {
                loaders[i] = (Loader) ClassUtils.newInstance(values[i]);
                if (loaders[i] instanceof Configurable) {
                    ((Configurable)loaders[i]).configure(config);
                }
            }
            add(loaders);
        }
	}

    public void add(Loader... loaders) {
        if (loaders != null && loaders.length > 0) {
            for (Loader loader : loaders) {
                if (loader != null && ! templateLoaders.contains(loader)) {
                    templateLoaders.add(loader);
                }
            }
        }
    }
    
    public void remove(Loader... loaders) {
        if (loaders != null && loaders.length > 0) {
            for (Loader loader : loaders) {
                if (loader != null) {
                    templateLoaders.remove(loader);
                }
            }
        }
    }
    
    public void clear() {
        templateLoaders.clear();
    }

    public Resource load(String name, String encoding) throws IOException {
        for (Loader loader : templateLoaders) {
            try {
                return loader.load(name, encoding);
            } catch (Exception e) {
            }
        }
        throw new FileNotFoundException("No such template file: " + name);
    }

    public List<String> list() {
        List<String> all = new ArrayList<String>();
        for (Loader loader : templateLoaders) {
            try {
                List<String> list = loader.list();
                if (list != null && list.size() > 0) {
                    all.addAll(list);
                }
            } catch (Exception e) {
            }
        }
        return all;
    }

}
