package com.cheung.shadowsockets.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.GZip;
import org.apache.tools.ant.taskdefs.Tar;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileFilter;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by cheungrp on 18/9/7.
 */
@Slf4j
@Component
public class CleanUpLogs {

    private final static String LOGS_DIR_PATH = "/root/shadowsocks/logs";
    private final ReentrantLock lock = new ReentrantLock();

    @Async
    @Scheduled(cron = "${clean.cron}")
    public void exec() {
        log.info("定时任务启动");
        cleanUpLogs("netty_trace");
        cleanUpLogs("shadowsockets_error");
        cleanUpLogs("shadowsockets_trace");
        log.info("定时任务结束");
    }

    protected void cleanUpLogs(String logName) {
        try {
            lock.lockInterruptibly();
            File file = new File(LOGS_DIR_PATH);
            if (file.exists() && file.isDirectory()) {
                FileFilter fileFilter = new WildcardFileFilter(logName + ".log.*");
                File[] logs = file.listFiles(fileFilter);
                if (logs != null && logs.length != 0) {
                    for (File logFile : logs) {
                        log.info("删除 " + logFile.getName() + "结果为: " + logFile.delete());
                    }
                }
            } else {
                if (!file.exists()) {
                    log.error("指定文件夹不存在");
                } else if (!file.isDirectory()) {
                    log.error("指定文件夹不是一个文件夹");
                }
            }
        } catch (InterruptedException e) {
            log.error("中断异常", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

    }

    protected void packLogs(String logName) {

        try {
            lock.lockInterruptibly();
            File file = new File(LOGS_DIR_PATH);
            if (file.exists() && file.isDirectory()) {
                Project project = new Project();
                project.setName("packLogs");
                Tar tar = new Tar();
                tar.setTaskName("tar");

                tar.setProject(project);
                tar.setBasedir(file);
                tar.setExcludes(logName + ".log");
                File history = new File(new File(LOGS_DIR_PATH).getParent(), "history-logs.tar");
                tar.setDestFile(history);

                tar.execute();

                File historyPack = new File(new File(LOGS_DIR_PATH).getParent(), "history-logs.tar.gz");
                if (history.exists()) {
                    GZip gZip = new GZip();
                    gZip.setProject(project);
                    gZip.setTaskName("gzip");
                    gZip.setSrc(history);
                    gZip.setDestfile(historyPack);

                    gZip.execute();
                }
                history.delete();
            }
        } catch (InterruptedException e) {
            log.error("中断异常", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
