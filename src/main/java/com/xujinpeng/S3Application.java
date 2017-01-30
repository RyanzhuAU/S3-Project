package com.xujinpeng;

import javax.servlet.ServletRegistration;

import org.atmosphere.cpr.ApplicationConfig;
import org.atmosphere.cpr.AtmosphereServlet;
import org.atmosphere.cpr.BroadcasterFactory;

import com.xujinpeng.resources.KinesisResource;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class S3Application extends Application<S3ProjectConfiguration> {
    public static void main(String[] args) throws Exception {
        new S3Application().run(args);
    }

    /*@Override
    public String getName() {
        return "hello-world";
    }*/

    @Override
    public void initialize(Bootstrap<S3ProjectConfiguration> bootstrap) {
        // nothing to do yet
    }

    @Override
    public void run(S3ProjectConfiguration configuration,
                    Environment environment) {
        //Set up the Atmosphere servlet
        AtmosphereServlet atmosphereServlet = new AtmosphereServlet();
        atmosphereServlet.framework().addInitParameter(ApplicationConfig.WEBSOCKET_CONTENT_TYPE, "application/json");
        atmosphereServlet.framework().addInitParameter(ApplicationConfig.WEBSOCKET_SUPPORT, "true");
        ServletRegistration.Dynamic servletRegistration = environment.servlets().addServlet("atmosphereServlet", atmosphereServlet);
        servletRegistration.addMapping("/atmosphere/*");
        servletRegistration.setAsyncSupported(true);
        BroadcasterFactory broadcasterFactory = atmosphereServlet.framework().getBroadcasterFactory();

        final KinesisResource kinesisResource = new KinesisResource(broadcasterFactory);
        environment.jersey().register(kinesisResource);
    }

}
