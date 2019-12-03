package edu.ilstu.cartelematics;

import android.content.Context;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPolicyRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateRequest;
import com.amazonaws.services.iot.model.CreateKeysAndCertificateResult;

import java.security.KeyStore;
import java.util.StringTokenizer;
import java.util.UUID;

public class AWSConnection {

    static final String LOG_TAG = AWSConnection.class.getCanonicalName();
    private static String[] PiData = new String[7];
    private AWSIotClient iotClient;
    private KeyStore clientKeyStore;
    private String keystorePath;
    private String keystoreName;
    private String keystorePassword;
    private String certificateId;
    private AWSIotMqttManager mqtt;
    private CameraModeActivity cameraModeActivity;
    private DataModeActivity dataModeActivity;
    private Context context;
    private StringTokenizer tokenizer;
    private int mode;

    public static AWSConnection newInstance(Context context){
        return new AWSConnection(context);
    }

    private AWSConnection(Context context){
        if(context instanceof CameraModeActivity){
            cameraModeActivity = (CameraModeActivity) context;
            mode = 2;
        }else if(context instanceof DataModeActivity){
            dataModeActivity = (DataModeActivity) context;
            mode = 1;
        }
        this.context = context;
    }

    public void AWSConnect() {

        // Initialize the Amazon Cognito credentials provider
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                context,
                "us-east-2:75501553-00b5-4346-a6a1-fa3d2bd9c133", // Identity pool ID
                Regions.US_EAST_2 // Region
        );

        //connects to AWS IoT
        String clientID = UUID.randomUUID().toString();
        mqtt = new AWSIotMqttManager(clientID, "a29oyasplq5kn5-ats.iot.us-east-2.amazonaws.com");
        mqtt.setKeepAlive(10);
        mqtt.setAutoReconnect(false);

        // IoT Client (for creation of certificate if needed)
        iotClient = new AWSIotClient(credentialsProvider);
        iotClient.setRegion(Region.getRegion(Regions.US_EAST_2));

        keystorePath = context.getFilesDir().getPath();
        keystoreName = "iot_keystore";
        keystorePassword = "password";
        certificateId = "default";

        // To load cert/key from keystore on filesystem
        try {
            if (AWSIotKeystoreHelper.isKeystorePresent(keystorePath, keystoreName)) {
                if (AWSIotKeystoreHelper.keystoreContainsAlias(certificateId, keystorePath,
                        keystoreName, keystorePassword)) {
                    Log.i(LOG_TAG, "Certificate " + certificateId
                            + " found in keystore - using for MQTT. ");
                    // load keystore from file into memory to pass on connection
                    clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                            keystorePath, keystoreName, keystorePassword);
                    subscribeToTopic();
                } else {
                    Log.i(LOG_TAG, "Key/cert " + certificateId + " not found in keystore.");
                }
            } else {
                Log.i(LOG_TAG, "Keystore " + keystorePath + "/" + keystoreName + " not found.");
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "An error occurred retrieving cert/key from keystore.", e);
        }

        if (clientKeyStore == null) {
            Log.i(LOG_TAG, "Cert/key was not found in keystore - creating new key and certificate.");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Create a new private key and certificate. This call
                        // creates both on the server and returns them to the
                        // device.
                        CreateKeysAndCertificateRequest createKeysAndCertificateRequest =
                                new CreateKeysAndCertificateRequest();
                        createKeysAndCertificateRequest.setSetAsActive(true);
                        final CreateKeysAndCertificateResult createKeysAndCertificateResult;
                        createKeysAndCertificateResult =
                                iotClient.createKeysAndCertificate(
                                        createKeysAndCertificateRequest);
                        Log.i(LOG_TAG,
                                "Cert ID: " +
                                        createKeysAndCertificateResult.getCertificateId() +
                                        " created.");

                        // store in keystore for use in MQTT client
                        // saved as alias "default" so a new certificate isn't
                        // generated each run of this application
                        AWSIotKeystoreHelper.saveCertificateAndPrivateKey(certificateId,
                                createKeysAndCertificateResult.getCertificatePem(),
                                createKeysAndCertificateResult.getKeyPair().getPrivateKey(),
                                keystorePath, keystoreName, keystorePassword);

                        // load keystore from file into memory to pass on
                        // connection
                        clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                                keystorePath, keystoreName, keystorePassword);

                        // Attach a policy to the newly created certificate.
                        // This flow assumes the policy was already created in
                        // AWS IoT and we are now just attaching it to the
                        // certificate.
                        AttachPolicyRequest policyAttachRequest = new AttachPolicyRequest();
                        policyAttachRequest.setPolicyName("telemetry_policy");
                        policyAttachRequest.setTarget(createKeysAndCertificateResult.getCertificateArn());
                        iotClient.attachPolicy(policyAttachRequest);
                        try {
                            if (clientKeyStore != null) {
                                subscribeToTopic();
                            }
                        } catch (final Exception e) {
                            Log.e(LOG_TAG, "Connection error.", e);
                        }

                    } catch (Exception e) {
                        Log.e(LOG_TAG,
                                "Exception occurred when generating new private key and certificate.",
                                e);
                    }
                }
            }).start();
        }
    }

    // used for updating the PiData variable inside the onMessageArrived method so AWSConnect and return the data
    private void updateData(String data){
        tokenizer = new StringTokenizer(data, ",");
        int i = 0;
        while(tokenizer.hasMoreTokens()){
            PiData[i] = tokenizer.nextToken();
            i++;
        }
        IoT.setData(PiData);
    }

    //subscribe to topic
    private void subscribeToTopic(){
        mqtt.connect(clientKeyStore, new AWSIotMqttClientStatusCallback() {
            @Override
            public void onStatusChanged(final AWSIotMqttClientStatus status,
                                        final Throwable throwable) {
                Log.d(LOG_TAG, "Status = " + String.valueOf(status));
                if (status == AWSIotMqttClientStatus.Connected) {
                    //subscribes to the data topic which is the topic the PI sends the data on
                    mqtt.subscribeToTopic("PiTelemetry/data", AWSIotMqttQos.QOS0, new AWSIotMqttNewMessageCallback() {
                        @Override
                        public void onMessageArrived(String topic, byte[] data) {
                            String topicData = new String(data);
                            updateData(topicData);
                        }
                    });
                }
            }
        });
    }
}
