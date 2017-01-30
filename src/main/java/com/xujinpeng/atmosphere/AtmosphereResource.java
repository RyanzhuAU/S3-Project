package com.xujinpeng.atmosphere;

import java.io.IOException;

import org.atmosphere.config.service.AtmosphereHandlerService;
import org.atmosphere.cpr.AtmosphereHandler;
import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;

import com.xujinpeng.Constants;

@AtmosphereHandlerService(path = "/")
public class AtmosphereResource implements AtmosphereHandler {
    /**
     * Name of the round broadcaster.
     */

    @Override
    public void onRequest(org.atmosphere.cpr.AtmosphereResource atmosphereResource) throws IOException {

        AtmosphereRequest req = atmosphereResource.getRequest();

        BroadcasterFactory broadcasterFactory = atmosphereResource.getAtmosphereConfig().getBroadcasterFactory();
        Broadcaster broadcaster = broadcasterFactory.lookup(Constants.APPLICATION_NAME, true);

        atmosphereResource.setBroadcaster(broadcaster);

        if (req.getMethod().equalsIgnoreCase("GET")) {
            // use HTTP long-polling with an invite timeout
            atmosphereResource.suspend();

        } else if (req.getMethod().equalsIgnoreCase("POST")) {
            // broadcast message to all connected users.
            broadcaster.broadcast(req.getReader().readLine().trim());
        }
    }

    private boolean isBroadcast(AtmosphereResourceEvent event) {
        return event.getMessage() != null && !event.isCancelled() && !event.isClosedByClient() && !event.isClosedByApplication();
    }

    @Override
    public void onStateChange(AtmosphereResourceEvent event) throws IOException {
        org.atmosphere.cpr.AtmosphereResource resource = event.getResource();

        if (isBroadcast(event)) {
            resource.write(event.getMessage().toString());

            switch (resource.transport()) {
                case WEBSOCKET:
                case STREAMING:
                    resource.getResponse().flushBuffer();
                    break;
                default:
                    resource.resume();
                    break;
            }
        }
    }

    @Override
    public void destroy() {
    }

}
