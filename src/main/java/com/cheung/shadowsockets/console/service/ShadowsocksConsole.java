package com.cheung.shadowsockets.console.service;

import com.cheung.shadowsockets.config.Config;
import com.cheung.shadowsockets.config.User;

/**
 * Created by cheungrp on 18/5/26.
 */
public interface ShadowsocksConsole {

    void addUserServer(Config config, User user);

    void closeUserServer(User user);

    void closeLogger();
}
