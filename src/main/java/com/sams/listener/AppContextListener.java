package com.sams.listener;

import com.sams.util.Config;
import com.sams.util.DBUtil;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Collections;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/** Starts the connection pool when the web-app starts and releases everything on shutdown. */
@WebListener
public class AppContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent e) {
        DBUtil.init();
        e.getServletContext().setAttribute("appName", "Smart Attendance Management System");
        e.getServletContext().setAttribute("appVersion", "1.0");
        e.getServletContext().setAttribute("threshold", (int) Config.threshold());
        e.getServletContext().log("[SAMS] started - minimum attendance " + Config.threshold() + "%");
    }

    @Override
    public void contextDestroyed(ServletContextEvent e) {
        DBUtil.shutdown();
        for (Driver d : Collections.list(DriverManager.getDrivers())) {
            if (d.getClass().getClassLoader() == getClass().getClassLoader()) {
                try { DriverManager.deregisterDriver(d); } catch (Exception ignored) { }
            }
        }
    }
}
