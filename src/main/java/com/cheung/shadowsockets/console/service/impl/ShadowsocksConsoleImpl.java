package com.cheung.shadowsockets.console.service.impl;

import com.cheung.shadowsockets.config.Config;
import com.cheung.shadowsockets.config.User;
import com.cheung.shadowsockets.console.service.ShadowsocksConsole;
import org.springframework.stereotype.Component;

/**
 * Created by cheungrp on 18/5/26.
 */
@Component("shadowsocksConsole")
public class ShadowsocksConsoleImpl implements ShadowsocksConsole {


    @Override
    public void addUserServer(Config config, User user) {

    }

    @Override
    public void closeUserServer(User user) {

    }

    @Override
    public void closeLogger() {

    }
}
