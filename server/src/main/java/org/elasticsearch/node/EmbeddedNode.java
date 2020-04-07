package org.elasticsearch.node;

import org.elasticsearch.Version;
import org.elasticsearch.env.Environment;
import org.elasticsearch.node.Node;
import org.elasticsearch.plugins.Plugin;

import java.util.Collection;

public abstract class EmbeddedNode extends Node {

    private Collection<Class<? extends Plugin>> plugins;

    public EmbeddedNode(Environment environment,Collection<Class<? extends Plugin>> classpathPlugins) {
        super(environment,  classpathPlugins,true);
        this.plugins = classpathPlugins;
    }

    public Collection<Class<? extends Plugin>> getPlugins() {
        return plugins;
    }

}
