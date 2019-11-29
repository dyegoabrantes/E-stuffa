package saci.development.estufa001;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Debug;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import helpers.MqttHelper;
import java.util.Timer;
import java.util.TimerTask;


public class CultivoFragment extends Fragment {

    MainActivity mainActivity;
    MqttHelper mqttHelper;
    public int contador = 0;
    private Number umidade_do_ar = null;
    private Number umidade_do_solo = null;

    private boolean estado_iluminação = false;
    private boolean estado_ventilação = false;
    private boolean estado_rega = false;
    public boolean luz_branca = false;

    Switch trocaLuz;

    Button ilumina;
    Button rega;
    Button ventila;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragmet_cultivo, null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);
        startMqtt();

        ilumina = (Button) getView().findViewById(R.id.iluminaButton);
        ventila = (Button) getView().findViewById(R.id.ventilaButton);
        rega = (Button) getView().findViewById(R.id.regaButton);
        trocaLuz = (Switch) getView().findViewById(R.id.trocaLuz);

        ilumina.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Log.i("info", "publicando mensagem");
                String topic = "aplicativo/col";
                String payload = "ilumina";
                byte[] encodedPayload = new byte[0];
                try {
                    encodedPayload = payload.getBytes("UTF-8");
                    MqttMessage message = new MqttMessage(encodedPayload);
                    ((MainActivity)getActivity()).mqttHelper.mqttAndroidClient.publish(topic,message);
                } catch (UnsupportedEncodingException | MqttException e) {
                    e.printStackTrace();
                }
            }
        });

        ventila.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Log.i("info", "publicando mensagem");
                String topic = "aplicativo/vtl";
                String payload = "ventila";
                byte[] encodedPayload = new byte[0];
                try {
                    encodedPayload = payload.getBytes("UTF-8");
                    MqttMessage message = new MqttMessage(encodedPayload);
                    ((MainActivity)getActivity()).mqttHelper.mqttAndroidClient.publish(topic,message);
                } catch (UnsupportedEncodingException | MqttException e) {
                    e.printStackTrace();
                }
            }
        });

        rega.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Log.i("info", "publicando mensagem");
                String topic = "aplicativo/rg";
                String payload = "rega";
                byte[] encodedPayload = new byte[0];
                try {
                    encodedPayload = payload.getBytes("UTF-8");
                    MqttMessage message = new MqttMessage(encodedPayload);
                    ((MainActivity)getActivity()).mqttHelper.mqttAndroidClient.publish(topic,message);
                } catch (UnsupportedEncodingException | MqttException e) {
                    e.printStackTrace();
                }
            }
        });

        trocaLuz.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked == true){
                    try {
                        Log.i("info", "publicando mensagem");
                        String topic = "aplicativo/bco";
                        String payload = "true";
                        byte[] encodedPayload = new byte[0];
                        encodedPayload = payload.getBytes("UTF-8");
                        MqttMessage message = new MqttMessage(encodedPayload);
                        message.setQos(2);
                        ((MainActivity)getActivity()).mqttHelper.mqttAndroidClient.publish(topic,message);
                    } catch (UnsupportedEncodingException | MqttException e) {
                        e.printStackTrace();
                    }
                }else{
                        try {
                            Log.i("info", "publicando mensagem");
                            String topic = "aplicativo/bco";
                            String payload = "false";
                            byte[] encodedPayload = new byte[0];
                            encodedPayload = payload.getBytes("UTF-8");
                            MqttMessage message = new MqttMessage(encodedPayload);
                            message.setQos(2);
                            ((MainActivity)getActivity()).mqttHelper.mqttAndroidClient.publish(topic,message);
                        } catch (UnsupportedEncodingException | MqttException e) {
                            e.printStackTrace();
                        }
                }
            }
        });


        if (((MainActivity)getActivity()).luzBranca == false){
            Log.i("luz branca", "false");
            trocaLuz.setChecked(false);
        }else {
            Log.i("luz branca", "true");
            trocaLuz.setChecked(true);
        }

        TextView rega_status = (TextView) getView().findViewById(R.id.rega_status_texto);
        String regaStatusTexto = getArguments().getString("regaStatusTexto");
        rega_status.setText(regaStatusTexto);

        TextView ilumina_status = (TextView) getView().findViewById(R.id.ilumina_status_texto);
        String iluminaStatusTexto = getArguments().getString("iluminaStatusTexto");
        ilumina_status.setText(iluminaStatusTexto);

        TextView ventila_status = (TextView) getView().findViewById(R.id.ventila_status_texto);
        String ventilaStatusTexto = getArguments().getString("ventilaStatusTexto");
        ventila_status.setText(ventilaStatusTexto);
    }

    private void startMqtt(){
        mqttHelper = new MqttHelper(getContext());

        final String iluminaTopic = (String) "estufa/col";
        final String ventilaTopic = (String) "estufa/vtl";
        final String regaTopic = (String) "estufa/rg";

        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {

            }

            @Override
            public void connectionLost(Throwable throwable) {

            }

            @SuppressLint("ResourceAsColor")
            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                String msg = mqttMessage.toString();

                if (topic.toString().equals(iluminaTopic)){

                    if (msg.equals("false"))
                    {
                        ilumina.setBackgroundResource(R.drawable.ic_wb_sunny_black_24dp);
                    } else if (msg.equals("true")){
                        ilumina.setBackgroundResource(R.drawable.ic_wb_sunny_pink_24dp);
                    }
                }
                if (topic.toString().equals(ventilaTopic)){

                    if (msg.equals("false"))
                    {
                        ventila.setBackgroundResource(R.drawable.ic_toys_black_24dp);
                    } else if (msg.equals("true")){
                        ventila.setBackgroundResource(R.drawable.ic_toys_green_24dp);
                    }
                }
                if (topic.toString().equals(regaTopic)){

                    if (msg.equals("false"))
                    {
                        rega.setBackgroundResource(R.drawable.ic_invert_colors_off_24dp);
                    } else if (msg.equals("true")){
                        rega.setBackgroundResource(R.drawable.ic_invert_colors_azl_24dp);
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }

}
