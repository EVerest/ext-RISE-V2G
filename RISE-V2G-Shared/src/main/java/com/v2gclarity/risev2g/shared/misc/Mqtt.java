// *** EVerest file ***
package com.v2gclarity.risev2g.shared.misc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONValue;

public class Mqtt {
    public interface CMD_HANDLER {
        public void handler(JSONObject args);
    }
    public interface READY_HANDLER {
        public void handler();
    }
    
    final Logger logger = LogManager.getLogger(Mqtt.class.getSimpleName());
    String basePath;
    MqttClient client;
    Map<String, CMD_HANDLER> cmd_handlers = new HashMap<>();
    READY_HANDLER ready_handler;
    
    private class OnMessageCallback implements MqttCallback {
        public void connectionLost(Throwable cause) {
            logger.fatal("mqtt unexpectedly disconnected, terminating");
            System.exit(2);
        }

        public void messageArrived(String topic, MqttMessage message) throws Exception {
            if (topic.equals(basePath+"/cmd")) {
                JSONObject data = (JSONObject)JSONValue.parse(new String(message.getPayload()));
                String key = (String) data.get("impl_id") + "|" + (String) data.get("cmd");
                if(cmd_handlers.containsKey(key)) {
                    cmd_handlers.get(key).handler((JSONObject)data.get("args"));
                }
            } else if (topic.equals(basePath+"/ready")) {
                boolean ready = (boolean)JSONValue.parse(new String(message.getPayload()));
                if(ready_handler != null && ready) {
                    ready_handler.handler();
                }
            }
        }

        public void deliveryComplete(IMqttDeliveryToken token) {
        }
    }
    
    public Mqtt(String host, String port, String basePath, String clientId) {
        try {
            this.basePath = basePath;
            client = new MqttClient("tcp://"+host+":"+port, clientId, new MemoryPersistence());
            MqttConnectOptions connOpts = new MqttConnectOptions();
            client.setCallback(new OnMessageCallback());
            client.connect(connOpts);
            client.subscribe(this.basePath+"/cmd");
            client.subscribe(this.basePath+"/ready");
        } catch(MqttException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            System.exit(3);
        }
    }
    
    public void publish_ready(boolean ready) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("ready", ready);
            client.publish(basePath+"/state", new MqttMessage(obj.toJSONString().getBytes()));
        } catch(MqttException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            System.exit(3);
        }
    }
    
    public void publish_var(String impl_id, String var_name, Object value) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("impl_id", impl_id);
            obj.put("var", var_name);
            obj.put("val", value);
            client.publish(basePath+"/var", new MqttMessage(obj.toJSONString().getBytes()));
        } catch(MqttException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            System.exit(3);
        }
    }
    
    public void handle_cmd(String impl_id, String cmd, CMD_HANDLER cmd_handler) {
        cmd_handlers.put(impl_id + "|" + cmd, cmd_handler);
    }
    
    public void handle_ready(READY_HANDLER ready_handler) {
        this.ready_handler = ready_handler;
    }

    public void mqtt_disconnect() {
        try {
            client.disconnect();
            client.close();
        } catch (MqttException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            System.exit(3);
        }
    }
}
