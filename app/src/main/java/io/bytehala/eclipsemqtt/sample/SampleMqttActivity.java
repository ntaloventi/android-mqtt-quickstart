package io.bytehala.eclipsemqtt.sample;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * created by Iqbal Rizky on 1/12/2024
 * Cashlez Worldwide indonesia, PT
 * iqbal@cashlez.com
 */
public class SampleMqttActivity extends AppCompatActivity implements PropertyChangeListener {
    private static final String TAG = "IQ";

    private final static String mqttServer = "192.168.80.162";
    private final static int mqttPort = 1883;
    private final static String mqttClientId = "Android-" + (System.currentTimeMillis() /1000);
    private final static boolean useSsl = false;
    private final static String userName = "";
    private final static String passWord = "";
    private final static String topic = "Test Postman";
    private final static Boolean retained = false;
    private final Integer qos = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample_mqtt);

        attemptConnectMqtt();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void attemptConnectMqtt() {
        MqttConnectOptions conOpt = new MqttConnectOptions();
        String uri = "tcp://";
        if (useSsl){
            uri = "ssl://";
        }
        uri = uri + mqttServer + ":" + mqttPort;

        MqttAndroidClient androidClient = Connections.getInstance(this).createClient(this, uri, mqttClientId);

        String sslKeyPath = "";
        if (useSsl){
            try {
                if(sslKeyPath != null && !sslKeyPath.equalsIgnoreCase(""))
                {
                    FileInputStream key = new FileInputStream(sslKeyPath);
                    conOpt.setSocketFactory(androidClient.getSSLSocketFactory(key, "mqtttest"));
                }

            } catch (MqttSecurityException e) {
                Log.e(TAG, "MqttException Occured: ", e);
            } catch (FileNotFoundException e) {
                Log.e(TAG, "MqttException Occured: SSL Key file not found", e);
            }
        }
        String clientHandle = uri + mqttClientId;

        Connection connection = new Connection(clientHandle, mqttClientId, mqttServer, mqttPort, this, androidClient, useSsl);
        connection.registerChangeListener(this);

        connection.changeConnectionStatus(Connection.ConnectionStatus.CONNECTING);
        conOpt.setCleanSession(true);
        conOpt.setConnectionTimeout(1000); //in millisecond
        conOpt.setKeepAliveInterval(10);
        if (!userName.isEmpty()){
            conOpt.setUserName("");
        }
        if (!passWord.isEmpty()){
            conOpt.setPassword("".toCharArray());
        }

        String[] actionArgs = new String[1];
        actionArgs[0] = mqttClientId;
        final ActionListener actionListener = new ActionListener(this, ActionListener.Action.CONNECT, clientHandle, actionArgs);

        boolean doConnect = true;
        String message = new String();
        if ((!message.equals(ActivityConstants.empty)) || (!topic.equals(ActivityConstants.empty))) {
            // need to make a message since last will is set
            try {
                conOpt.setWill(topic, message.getBytes(), qos.intValue(),
                        retained.booleanValue());
            }
            catch (Exception e) {
                Log.e(TAG, "Exception Occured", e);
                doConnect = false;
                actionListener.onFailure(null, e);
            }
        }
        androidClient.setCallback(new MqttCallbackHandler(this, clientHandle));

        androidClient.setTraceCallback(new MqttTraceCallback());

        connection.addConnectionOptions(conOpt);
        Connections.getInstance(this).addConnection(connection);
        if (doConnect) {
            try {
                androidClient.connect(conOpt, null, actionListener);
            }
            catch (MqttException e) {
                Log.e(TAG, "MqttException Occured", e);
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("connectionStatus")){
            return;
        }
        Log.e(TAG, "propertyChange: " + evt.getPropertyName() );
    }
}
