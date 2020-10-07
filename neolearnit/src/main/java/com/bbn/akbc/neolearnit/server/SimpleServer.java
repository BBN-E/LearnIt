package com.bbn.akbc.neolearnit.server;

import com.bbn.akbc.neolearnit.common.LearnItConfig;
import com.bbn.akbc.neolearnit.server.handlers.ShutdownHandler;

import com.google.common.collect.ImmutableList;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;

import javax.servlet.annotation.MultipartConfig;
import java.io.File;

public class SimpleServer {

    private final String homePage;
    private final int port;
    private final Iterable<Handler> handlers;

    private String introMessage;

    public SimpleServer(Handler handler, String homePage, int port) {
        this(ImmutableList.of(handler), homePage, port);
    }

    public SimpleServer(Iterable<Handler> handlers, String homePage, int port) {
        this.port = port;
        this.homePage = homePage;
        this.handlers = handlers;
        this.introMessage = "Running server on port " + port + "...";
    }

    public SimpleServer withIntroMessage(String message) {
        this.introMessage = message;
        return this;
    }

    public void run() throws Exception {
        System.out.println("Starting server...");

        Server server = new Server(port);

        ResourceHandler resource_handler = new ResourceHandler();
        resource_handler.setDirectoriesListed(true);
        resource_handler.setWelcomeFiles(new String[]{this.homePage});
        resource_handler.setResourceBase(LearnItConfig.get("learnit_root") + File.separator + "static");

        HandlerList handlerList = new HandlerList();

        handlerList.addHandler(resource_handler);
        for (Handler handler : handlers) {
            handlerList.addHandler(handler);
        }

        handlerList.addHandler(new ShutdownHandler(server));
        handlerList.addHandler(new DefaultHandler());

        server.setHandler(handlerList);

        server.start();
        System.out.println("----------------------------------------------");
        System.out.println(this.introMessage);
        System.out.println("----------------------------------------------");

        server.join();

        System.out.println("Server stopped!");
    }

}
