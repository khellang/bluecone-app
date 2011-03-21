package com.bluecone.connect;

import java.util.Set;

import com.bluecone.R;
import com.bluecone.intent.Bluecone_intent;

import debug.Debug;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class DeviceFinder extends Activity implements OnItemClickListener{
	
    
 
    
    //Blåtannadapteret som hentes fra systemet
    private BluetoothAdapter bluetoothAdapter;
    
    // To Arrayadapteret som vil holde på parede enheter og funnede enheter
    private ArrayAdapter<String> pairedAdapter;
    private ArrayAdapter<String> foundAdapter;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		if(Debug.D)Log.d(Debug.TAG_FINDER, "onCreate...");
		//Vinduet får en progressbar 
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.device_layout);
		
		if(Debug.D)Log.d(Debug.TAG_FINDER, "bluetoothAdapter...");
		//instansier blåtannadapteret
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(Debug.D)Log.d(Debug.TAG_FINDER, "...bluetoothAdapter");

		//Dersom brukeren angrer returneres resultatkoden kansellert
		setResult(RESULT_CANCELED);
	
		if(Debug.D)Log.d(Debug.TAG_FINDER, "Arrayadapter...");
		pairedAdapter = new ArrayAdapter<String>(this, R.layout.device_name_layout);
		foundAdapter = new ArrayAdapter<String>(this, R.layout.device_name_layout);
		if(Debug.D)Log.d(Debug.TAG_FINDER, "...Arrayadapter");
		
		
		if(Debug.D)Log.d(Debug.TAG_FINDER, "ListView...");
		//Henter ut listview for aktuelle enheter, setter på tilhørende adapter, leger til lytter 
		ListView lagrede_enheter = (ListView) findViewById(R.id.Liste_parede_enheter);
		lagrede_enheter.setAdapter(pairedAdapter);
		lagrede_enheter.setOnItemClickListener(this);
		
		ListView nye_enheter = (ListView) findViewById(R.id.Liste_funnede_enheter);
		nye_enheter.setAdapter(foundAdapter);
		nye_enheter.setOnItemClickListener(this);
		if(Debug.D)Log.d(Debug.TAG_FINDER, "...ListView");
		
		if(Debug.D)Log.d(Debug.TAG_FINDER, "IntentFilter...");
		// Gjør klar til å motta broadcast når en enhet blir oppdaget
		IntentFilter i_filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		this.registerReceiver(receiver, i_filter);
		if(Debug.D)Log.d(Debug.TAG_FINDER, "...receiver.IntentFilter...");
		
		//Gjør klar til å motta broadcast når oppdaging avsluttes
		i_filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.registerReceiver(receiver, i_filter);
		if(Debug.D)Log.d(Debug.TAG_FINDER, "...receiver.IntentFilter");
		
		
		if(Debug.D)Log.d(Debug.TAG_FINDER, "Set<BluetoothDevice>...");
		//Hent ut lagrede enheter
		Set<BluetoothDevice> lagrede = bluetoothAdapter.getBondedDevices();
		
		if(Debug.D)Log.d(Debug.TAG_FINDER, "...Set<BluetoothDevice>");
		if(lagrede.size()>0){
			for(BluetoothDevice b:lagrede){
				pairedAdapter.add(b.getName()+"\n"+b.getAddress());
			}
		}else{
			String ingen_lagret = getResources().getText(R.string.device_non_paired).toString();
			pairedAdapter.add(ingen_lagret);
		}
		
		if(Debug.D)Log.d(Debug.TAG_FINDER, "...onCreate");
		
		
	}
	@Override
	public void onStart(){
		super.onStart();
	}
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
		if(bluetoothAdapter!=null)
			bluetoothAdapter.cancelDiscovery();
		this.unregisterReceiver(receiver);
	}
	
	public void sokEtterEnheter(View view){
		if(Debug.D)Log.d(Debug.TAG_FINDER, "sokEtterEnheter()");

		//Skjul knappen slik at den ikke kan trykkes på under søk
		findViewById(R.id.Button_scan).setVisibility(View.GONE);
		
		
		setProgressBarIndeterminateVisibility(true);
		setTitle(R.string.device_scanning);
		//Gjør teksten "Nye enheter synlig
		findViewById(R.id.Tittel_funn_enheter).setVisibility(View.VISIBLE);
		
		//Sjekker om blåtannadapteret allerede søker, starter på ny
		if(bluetoothAdapter.isDiscovering())
			bluetoothAdapter.cancelDiscovery();
		bluetoothAdapter.startDiscovery();
	}

	
	 // BroadcastReceiver, lytter etter oppdagede enheter
	
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if(Debug.D)Log.d(Debug.TAG_FINDER, "onReceive()");
			
			String action = intent.getAction();
			
			//Dersom en enhet er funnet
			if(BluetoothDevice.ACTION_FOUND.equals(action)){
	              // Hent ut BluetoothDevice objektet fra intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //Dersom den oppdagede enheten er paret går vi videre
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    foundAdapter.add(device.getName() + "\n" + device.getAddress());
                }
			}
                else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                	setProgressBarIndeterminateVisibility(false);
                	setTitle(R.string.app_name);
                	if(foundAdapter.getCount()==0){
                		String ingen_funnet = getResources().getText(R.string.device_non_found).toString();
                		foundAdapter.add(ingen_funnet);
                	}
                }
			
			
		}
	};


	public void onItemClick(AdapterView<?> arg0, View view, int arg2, long arg3) {
		//Det første vi gjør er å avbryte videre søk pga kostnad
		bluetoothAdapter.cancelDiscovery();
		
	      // De 17 siste tegnene er MAC-adressen
        String info = ((TextView) view).getText().toString();
        String adresse = info.substring(info.length() - 17);
        // ResultatsIntent opprettes og adressen legges til i Extrafeltet til Intentèn
        Intent intent = new Intent();
        intent.putExtra(Bluecone_intent.EXTRA_UNIT_ADDRESS, adresse);
        
        // Sett resultat og avslutt EnhetsListe
        setResult(Activity.RESULT_OK, intent);
  
        finish();
		
	}


}
