package com.cheung.shadowsockets.config;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.core.ClassLoaderReference;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.io.xml.XmlFriendlyNameCoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 加载Config配置xml
 *
 * @author zhaohui
 */
public enum ConfigXmlLoader {

    loader;

    private Logger logger = LoggerFactory.getLogger(ConfigXmlLoader.class);

    private final static Map<Integer, String> nameMap = new ConcurrentHashMap<>(16);
    private final static Map<Integer, Thread> threadIdMap = new ConcurrentHashMap<>(16);

    public Config load() {
        InputStream inputStream = ClassLoader.getSystemResourceAsStream("config.xml");
        XStream stream = new XStream(null, new DomDriver("UTF-8", new XmlFriendlyNameCoder("__", "_")),
                new ClassLoaderReference(ClassLoader.getSystemClassLoader()));
        stream.setMode(XStream.NO_REFERENCES);

        stream.alias("user", User.class);
        stream.alias("config", Config.class);

        return (Config) stream.fromXML(inputStream);
    }

    public void addUser(User user) {
        nameMap.put(user.getServerPort(), user.getName());
    }

    public void clear() {
        nameMap.clear();
    }

    public Map<Integer, String> getUserMapper() {
        return nameMap;
    }

    public void addThreadId(Integer serverPort, Thread thread) {
        threadIdMap.put(serverPort, thread);
    }

    public Map<Integer, Thread> getThreadIdMapper() {
        return threadIdMap;
    }

}
