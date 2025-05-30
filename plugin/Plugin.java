    package org.nms.plugin;

    import io.vertx.core.AbstractVerticle;
    import io.vertx.core.Future;
    import io.vertx.core.json.JsonArray;
    import io.vertx.core.json.JsonObject;

    import static org.nms.App.LOGGER;
    import org.nms.constants.Config;
    import org.nms.constants.Fields;

    import java.io.BufferedReader;
    import java.io.InputStreamReader;

    public class Plugin extends AbstractVerticle
    {
        @Override
        public void start()
        {
            vertx.eventBus().<JsonObject>localConsumer(Fields.EventBus.PLUGIN_SPAWN_ADDRESS, message ->
            {
                var request = message.body();

                spawnPlugin(request)
                        .onComplete(pluginResponse -> message.reply(pluginResponse.result()));
            });

            LOGGER.info("✅ Plugin Verticle Deployed");
        }


        @Override
        public void stop()
        {
            LOGGER.info("⚠ Plugin Verticle Stopped");
        }

        /**
         * Spawns plugin, Sends request and returns Json Response
         */
        private Future<JsonArray> spawnPlugin(JsonObject request)
        {
            return vertx.executeBlocking(() ->
            {
                try
                {
                    // Calculate timeout based on request type
                    var timeout = calculateTimeout(request);

                    var encodedRequest = request.encode();

                    // Prepare go command
                    var goCommand = new String[] {Config.PLUGIN_PATH, encodedRequest};

                    LOGGER.debug("Spawning plugin with request: " + encodedRequest);

                    var builder = new ProcessBuilder(goCommand);

                    // Run command
                    var process = builder.start();

                    long timerId = vertx.setTimer( timeout * 1000L, (tId)->
                    {
                        if(process.isAlive())
                        {
                            LOGGER.info("Timeout for proccess " + process.pid() + ", terminating...");

                            process.destroyForcibly();
                        }
                    });

                    // Else Read output
                    var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                    var response = new JsonArray();

                    String line;

                    while ((line = reader.readLine()) != null)
                    {
                        try
                        {
                            response.add(new JsonObject(line));
                        }
                        catch (Exception e)
                        {
                            LOGGER.warn("❌ Error Parsing Json: " + line);
                        }

                        LOGGER.debug("Plugin response: " + line);
                    }

                    reader.close();

                    if(!vertx.timer(timerId).isComplete())
                    {
                        vertx.timer(timerId).cancel();
                    }

                    return response;
                }
                catch (Exception exception)
                {
                    LOGGER.error("❌ Error spawning plugin: " + exception.getMessage());

                    // Send empty response
                    return new JsonArray();
                }
            });
        }

        /**
         * Calculates timeout based on request type and content
         */
        private int calculateTimeout(JsonObject request)
        {
            var type = request.getString("type", "");

            // Calculate timeout for discovery
            if ("discovery".equals(type))
            {
                var ips = request.getJsonArray(Fields.PluginDiscoveryRequest.IPS, null);

                var ipsCount = (ips != null) ? ips.size() : 0;

                return Config.BASE_TIME + (ipsCount * Config.DISCOVERY_TIMEOUT_PER_IP);
            }

            // Calculate timeout for polling
            else if ("polling".equals(type))
            {
                var metricGroups = request.getJsonArray(Fields.PluginPollingRequest.METRIC_GROUPS, null);

                var metricGroupCount = (metricGroups != null) ? metricGroups.size() : 0;

                return Config.BASE_TIME + (metricGroupCount * Config.POLLING_TIMEOUT_PER_METRIC_GROUP);
            }

            // Default timeout
            return Config.BASE_TIME;
        }
    }