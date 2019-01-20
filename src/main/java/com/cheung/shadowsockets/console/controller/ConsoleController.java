package com.cheung.shadowsockets.console.controller;

import com.cheung.shadowsockets.console.service.ShadowsocksConsole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@RestController
public class ConsoleController {

    @Autowired
    @Qualifier("shadowsocksConsole")
    ShadowsocksConsole console;

    @RequestMapping("/test/cookie")
    public String getNum(HttpServletRequest request) {

        return "index";
    }
}
