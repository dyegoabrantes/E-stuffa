package saci.development.estufa001;

import android.os.Bundle;
import android.os.Debug;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.util.Timer;
import java.util.TimerTask;


public class CultivoFragment extends Fragment {

    MainActivity mainActivity;
    public int contador = 0;
    private Number umidade_do_ar = null;
    private Number umidade_do_solo = null;

    private boolean estado_iluminação = false;
    private boolean estado_ventilação = false;
    private boolean estado_rega = false;

    public boolean luz_branca = false;
    Switch trocaLuz;



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragmet_cultivo, null);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        trocaLuz = (Switch) getView().findViewById(R.id.trocaLuz);
        trocaLuz.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked == true){
                    final Timer timer = new Timer();
                    timer.scheduleAtFixedRate(new TimerTask() {
                                                  @Override
                                                  public void run() {
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
                                                          contador++;
                                                          if (contador>=4){
                                                              contador=0;
                                                              timer.cancel();
                                                          }
                                                  }
                                              },
                            0, 1000);
                }else{
                    final Timer timer2 = new Timer();
                    timer2.scheduleAtFixedRate(new TimerTask() {
                                                  @Override
                                                  public void run() {
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
                                                      contador++;
                                                      if (contador>=4){
                                                          contador=0;
                                                          timer2.cancel();
                                                      }
                                                  }
                                              },
                            0, 1000);
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
}
