package com.cheung.shadowsocks.config;

import com.google.common.collect.Lists;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.ClassLoaderReference;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.io.xml.XmlFriendlyNameCoder;

import java.io.InputStream;
import java.util.List;

/**
 * 加载Config配置xml
 *
 * @author zhaohui
 */
public enum ConfigXmlLoader {

    loader;

    List<Config> cache = Lists.newCopyOnWriteArrayList();

    public Config load() {
        InputStream inputStream = ClassLoader.getSystemResourceAsStream("config.xml");
        XStream stream = new XStream(null, new DomDriver("UTF-8", new XmlFriendlyNameCoder("__", "_")),
                new ClassLoaderReference(ClassLoader.getSystemClassLoader()));
        stream.setMode(XStream.NO_REFERENCES);

        stream.alias("config", Config.class);

        if (cache.size() == 0) {
            cache.add((Config) stream.fromXML(inputStream));
            return cache.get(0);
        } else {
            return cache.get(0);
        }
    }

}
