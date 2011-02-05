package com.bluecone.connect;

import java.util.Set;

import com.bluecone.R;

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
	
	   // Debugging
    private static final String TAG = "EnhetsListe";
    private static final boolean D = true;
    
    public static final String EXTRA_UNIT_ADDRESS = "unit_address";
    public static final String REQUEST_CONNECT = "com.bluecone.connect.REQUEST_CONNECT";
    
    //Bl�tannadapteret som hentes fra systemet
    private BluetoothAdapter bluetoothAdapter;
    
    // To Arrayadapteret som vil holde p� parede enheter og funnede enheter
    private ArrayAdapter<String> pairedAdapter;
    private ArrayAdapter<String> foundAdapter;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		if(D)Log.d(TAG, "onCreate...");
		//Vinduet f�r en progressbar 
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.device_layout);
		
		if(D)Log.d(TAG, "bluetoothAdapter...");
		//instansier bl�tannadapteret
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(D)Log.d(TAG, "...bluetoothAdapter");

		//Dersom brukeren angrer returneres resultatkoden kansellert
		setResult(RESULT_CANCELED);
	
		if(D)Log.d(TAG, "Arrayadapter...");
		pairedAdapter = new ArrayAdapter<String>(this, R.layout.device_name_layout);
		foundAdapter = new ArrayAdapter<String>(this, R.layout.device_name_layout);
		if(D)Log.d(TAG, "...Arrayadapter");
		
		
		if(D)Log.d(TAG, "ListView...");
		//Henter ut listview for aktuelle enheter, setter p� tilh�rende adapter, leger til lytter 
		ListView lagrede_enheter = (ListView) findViewById(R.id.Liste_parede_enheter);
		lagrede_enheter.setAdapter(pairedAdapter);
		lagrede_enheter.setOnItemClickListener(this);
		
		ListView nye_enheter = (ListView) findViewById(R.id.Liste_funnede_enheter);
		nye_enheter.setAdapter(foundAdapter);
		nye_enheter.setOnItemClickListener(this);
		if(D)Log.d(TAG, "...ListView");
		
		if(D)Log.d(TAG, "IntentFilter...");
		// Gj�r klar til � motta broadcast n�r en enhet blir oppdaget
		IntentFilter i_filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		this.registerReceiver(receiver, i_filter);
		if(D)Log.d(TAG, "...receiver.IntentFilter...");
		
		//Gj�r klar til � motta broadcast n�r oppdaging avsluttes
		i_filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.registerReceiver(receiver, i_filter);
		if(D)Log.d(TAG, "...receiver.IntentFilter");
		
		
		if(D)Log.d(TAG, "Set<BluetoothDevice>...");
		//Hent ut lagrede enheter
		Set<BluetoothDevice> lagrede = bluetoothAdapter.getBondedDevices();
		
		if(D)Log.d(TAG, "...Set<BluetoothDevice>");
		if(lagrede.size()>0){
			for(BluetoothDevice b:lagrede){
				pairedAdapter.add(b.getName()+"\n"+b.getAddress());
			}
		}else{
			String ingen_lagret = getResources().getText(R.string.device_non_paired).toString();
			pairedAdapter.add(ingen_lagret);
		}
		
		if(D)Log.d(TAG, "...onCreate");
		
		
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
		if(D)Log.d(TAG, "sokEtterEnheter()");

		//Skjul knappen slik at den ikke kan trykkes p� under s�k
		findViewById(R.id.Button_scan).setVisibility(View.GONE);
		
		
		setProgressBarIndeterminateVisibility(true);
		setTitle(R.string.device_scanning);
		//Gj�r teksten "Nye enheter synlig
		findViewById(R.id.Tittel_funn_enheter).setVisibility(View.VISIBLE);
		
		//Sjekker om bl�tannadapteret allerede s�ker, starter p� ny
		if(bluetoothAdapter.isDiscovering())
			bluetoothAdapter.cancelDiscovery();
		bluetoothAdapter.startDiscovery();
	}

	
	 // BroadcastReceiver, lytter etter oppdagede enheter
	
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			if(D)Log.d(TAG, "onReceive()");
			
			String action = intent.getAction();
			
			//Dersom en enhet er funnet
			if(BluetoothDevice.ACTION_FOUND.equals(action)){
	              // Hent ut BluetoothDevice objektet fra intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //Dersom den oppdagede enheten er paret g�r vi videre
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
		//Det f�rste vi gj�r er � avbryte videre s�k pga kostnad
		bluetoothAdapter.cancelDiscovery();
		
	      // De 17 siste tegnene er MAC-adressen
        String info = ((TextView) view).getText().toString();
        String adresse = info.substring(info.length() - 17);
        // ResultatsIntent opprettes og adressen legges til i Extrafeltet til Intent�n
        Intent intent = new Intent();
        intent.putExtra(EXTRA_UNIT_ADDRESS, adresse);
        
        // Sett resultat og avslutt EnhetsListe
        setResult(Activity.RESULT_OK, intent);
  
        finish();
		
	}


}