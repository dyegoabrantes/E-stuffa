package saci.development.estufa001;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;

import helpers.MqttHelper;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity
        implements BottomNavigationView.OnNavigationItemSelectedListener {

    MqttHelper mqttHelper;
    CultivoFragment cultivoFragment;

    private boolean estufa_conectada = false;

    private TextView mTextMessage;
    private TextView modoDeOperacao;
    private TextView statusDeConexao;

    private String modo_de_operacao = null;

    private String nome_do_SmartVaso = null;

    private boolean regando = false;
    private boolean iluminando = false;
    private boolean ventilando = false;

    public boolean luzBranca = false;

    TextView umidSoloValue;

    @SuppressLint("ResourceType")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startMqtt();

        mTextMessage = (TextView) findViewById(R.id.message);
        modoDeOperacao = (TextView) findViewById(R.id.StatusModo);
        statusDeConexao = (TextView) findViewById(R.id.StatusConexao);

        umidSoloValue = (TextView) findViewById(R.id.UmidadeSoloValor);

        updateView();
        BottomNavigationView navigation = findViewById(R.id.navigation);

        navigation.setOnNavigationItemSelectedListener(this);

        if(estufa_conectada)
        {
            loadFragment(new CultivoFragment());
        }else{
            loadFragment(new ConexaoFragment());
            mTextMessage.setText("Conexão");
        }
    }


    private void updateView(){
        if(estufa_conectada)
        {
            statusDeConexao.setText("E-Stuffa conectada :)");
            statusDeConexao.setTextColor(Color.parseColor("#FF679A02"));
        }else{
            statusDeConexao.setText("E-Stuffa desconectada :(");
            statusDeConexao.setTextColor(Color.parseColor("#ffcc0000"));
        }

        if (modo_de_operacao != null)
        {
            modoDeOperacao.setText(modo_de_operacao);
        }else {
            modoDeOperacao.setText("Nada Conectado");
        }

        if (nome_do_SmartVaso != null)
        {
            TextView nome_do_vaso = (TextView) findViewById(R.id.nomeDoSmartVaso);
            nome_do_vaso.setText(nome_do_SmartVaso);
        }
    }

    private void startMqtt(){
        mqttHelper = new MqttHelper(getApplicationContext());

        final String modoTopic = (String) "estufa/modo";
        final String conexaoTopic = (String) "estufa/conecta";
        final String nomeVasoTopic = (String) "estufa/nomeVaso";
        final String luzBrancaTopic = (String) "estufa/lb";

        final String tempTopic = (String) "estufa/temp";
        final String umidArTopic = (String) "estufa/umid";
        final String umidSoloTopic = (String) "estufa/umidSo";

        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {

            }

            @Override
            public void connectionLost(Throwable throwable) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                String msg = mqttMessage.toString();
                if(topic.toString().equals(modoTopic)){
                    if (msg.equals("false")){
                        modo_de_operacao = "manual";
                    }else {
                        if (msg.equals("true")){
                            modo_de_operacao = "automatico";
                        }
                    }
                    updateView();
                }else {
                    if(topic.toString().equals(conexaoTopic)){

                        if (msg.equals("true"))
                        {
                            estufa_conectada = true;
                            Fragment fragment = new CultivoFragment();
                            loadFragment(fragment);
                        } else {
                            if (msg.equals("false"))
                            {
                                estufa_conectada = false;
                                Fragment fragment = new ConexaoFragment();
                                loadFragment(fragment);
                            }
                        }
                        updateView();
                    }else {
                        if (topic.toString().equals(nomeVasoTopic))
                        {
                            nome_do_SmartVaso = msg;
                        }
                        updateView();
                    }
                    if(topic.toString().equals(tempTopic)){
                        //int result = Integer.parseInt(tempString);
                        //temperatura = result;
                        TextView tempValue = (TextView) findViewById(R.id.TemperaturaValor);
                        tempValue.setText(mqttMessage.toString());
                    }else {
                        if(topic.toString().equals(umidSoloTopic)){
                            TextView umidSoilValue = (TextView) findViewById(R.id.UmidadeSoloValor);
                            umidSoilValue.setText(mqttMessage.toString());
                        }else {
                            if (topic.toString().equals(umidArTopic))
                            {
                                TextView umidArValue = (TextView) findViewById(R.id.UmidadeArValor);
                                umidArValue.setText(mqttMessage.toString());
                            }
                        }
                    }
                }
                if (topic.toString().equals(luzBrancaTopic)){
                    //cultivoFragment.timer.cancel();
                    if (msg.equals("false"))
                    {
                        cultivoFragment.luz_branca = false;
                        luzBranca = false;
                        Log.d("chegou msg", "false");
                    } else if (msg.equals("true")){
                        cultivoFragment.luz_branca = true;
                        luzBranca = true;
                        Log.d("chegou msg", "false");
                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }

    private boolean loadFragment(Fragment fragment){
        if (fragment!= null){
            Bundle bundle = new Bundle();
            if(iluminando)
            {
                bundle.putString("iluminaStatusTexto", "Ligado");
            } else {
                bundle.putString("iluminaStatusTexto", "Desligado");
            }
            if(ventilando)
            {
                bundle.putString("ventilaStatusTexto", "Ligado");
            }else {
                bundle.putString("ventilaStatusTexto", "Desligado");
            }
            if (regando)
            {
                bundle.putString("regaStatusTexto", "Ligado");
            }else {
                bundle.putString("regaStatusTexto", "Desligado");
            }

            fragment.setArguments(bundle);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }

        return false;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        final int qos1 = (int) 1;
        Fragment fragment = null;
        if (estufa_conectada)
        {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    fragment = new CultivoFragment();
                    mTextMessage.setText("Cultivo");
                    String topic = "aplicativo/get";
                    String payload = "manda";
                    byte[] encodedPayload = new byte[0];
                    try {
                        encodedPayload = payload.getBytes("UTF-8");
                        MqttMessage message = new MqttMessage(encodedPayload);
                        mqttHelper.mqttAndroidClient.publish(topic, message);
                    } catch (UnsupportedEncodingException | MqttException e) {
                        e.printStackTrace();
                    }
                    break;
                case R.id.navigation_dashboard:
                    if (modo_de_operacao.equals("automatico"))
                    {
                        fragment = new ControleAutomaticoFragment();
                    }
                    if (modo_de_operacao.equals("manual"))
                    {
                        fragment = new ControleFragment();
                    }

                    mTextMessage.setText("Controle");
                    break;
                case R.id.navigation_notifications:
                    fragment = new ConexaoFragment();
                    mTextMessage.setText("Conexão");
                    break;
            }
            return loadFragment(fragment);
        } else {
            fragment = new ConexaoFragment();
            return loadFragment(fragment);
        }
    }
}
