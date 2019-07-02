package com.bbn.akbc.neolearnit.server.handlers;

import org.eclipse.jetty.server.Server;

public class ShutdownHandler extends SimpleJSONHandler {

  private final Server server;

  public ShutdownHandler(Server server) {
    this.server = server;
  }

  @JettyMethod("/shutdown")
  public void shutdown() {
    // TODO: @hqiu: According to `https://bugs.eclipse.org/bugs/show_bug.cgi?id=420142`, `setGracefulShutdown` or `setStopTimeout`. Change to `StatisticsHandler` when possible!
    server.setStopTimeout(10000);
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
