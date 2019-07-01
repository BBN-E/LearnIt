package com.bbn.akbc.neolearnit.server.handlers;

import org.eclipse.jetty.server.Server;

public class ShutdownHandler extends SimpleJSONHandler {

  private final Server server;

  public ShutdownHandler(Server server) {
    this.server = server;
  }

  @JettyMethod("/shutdown")
  public void shutdown() {
    server.setGracefulShutdown(10000);
    try {
      new Thread() {
        @Override
        public void run() {
          try {
            server.stop();
          } catch (Exception ex) {
            System.out.println("Failed to stop Jetty");
          }
        }
      }.start();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
