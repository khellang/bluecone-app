package com.bluecone.connect;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.bluecone.BlueconeContext;
import com.bluecone.BlueconeHandler;
import com.bluecone.R;

import debug.Debug;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

public class DeviceConnector {


//	private static final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private BluetoothAdapter bluetoothAdapter;
	private BluetoothDevice bluetoothDevice;
	private ConnectThread connectThread;
	private ConnectedThread connectedThread;
	public static final int STATE_CHANGED = 0;
	public static final int STATE_NONE = 0;
	public static final int STATE_CONNECTING = 1;
	public static final int STATE_CONNECTED = 2;
	public static final int STATE_RECONNECT = 3;
	public static final String KEY_NAME = "name";
	public static final String KEY_TOAST = "toast";
	public static final int FLAG_NAME = 5;

	private String mac_adress;
	private int state;
	private int connection_attempts = -1;

	private static DeviceConnector CONNECTOR = new DeviceConnector();
	public DeviceConnector(){
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		state = STATE_NONE;
	}
	
	public static DeviceConnector getDeviceConnector(){
		return CONNECTOR;
	}
	
	private BluetoothDevice setDeviceByMAC(String mac){
		if(Debug.D)Log.d(Debug.TAG_CONNECTOR, "setDeviceByMac..."+mac);
		return bluetoothAdapter.getRemoteDevice(mac);
	}

	public void setCurrentMac(String mac){
		mac_adress = mac;
	}
	public String getCurrentMac(){
		return mac_adress;
	}
	public void reset() {
		connection_attempts = -1;
		
	}
	public int getAttempts(){
		return connection_attempts;
	}
	
    public synchronized void start() {
        if (Debug.D) Log.d(Debug.TAG_CONNECTOR, "start");

        // Cancel any thread attempting to make a connection
        if (connectThread != null) {connectThread.cancel(); connectThread = null;}

        // Cancel any thread currently running a connection
        if (connectedThread != null) {connectedThread.cancel(); connectedThread = null;}

        setState(STATE_NONE);
    }

	private  void setState(int newState) {
		if(Debug.D)Log.d(Debug.TAG_CONNECTOR, "setState: "+state +" --> "+newState);
		state = newState;
		BlueconeHandler.getHandler().obtainMessage(STATE_CHANGED, state, -1).sendToTarget();

	}
	public synchronized void connect(String mac){
		if(Debug.D)Log.d(Debug.TAG_CONNECTOR, "connect to.... ");
		bluetoothDevice = setDeviceByMAC(mac);
		cancelThreads();//Avslutter tilkoblingstråder
		connectThread = new ConnectThread(bluetoothDevice);//Oppretter en ny tråd for å koble til enheten
		connectThread.start();
		setState(STATE_CONNECTING);
	}

	public synchronized void connected(BluetoothSocket socket,BluetoothDevice device){
		if(Debug.D)Log.d(Debug.TAG_CONNECTOR, "connected");
		cancelThreads();//Avslutter alle tråder
		connectedThread = new ConnectedThread(socket);
		connectedThread.start();
		setCurrentMac(device.getAddress());
		Message msg = BlueconeHandler.getHandler().obtainMessage(FLAG_NAME);
		Bundle bundle = new Bundle();
		bundle.putString(KEY_NAME, device.getName());
		msg.setData(bundle);
		BlueconeHandler.getHandler().sendMessage(msg);		
		setState(STATE_CONNECTED);
		

	}
	public synchronized void stop(){
		if(Debug.D)Log.d(Debug.TAG_CONNECTOR, "stop");
		cancelThreads();
		setState(STATE_NONE);
	}

	public void write(byte[] out){
		ConnectedThread tmp;
		synchronized (this) {
			if(state!=STATE_CONNECTED)return;
			tmp = connectedThread;
		}
		tmp.write(out);
	}
	
	
	private void connectionFailed(){
		if(Debug.D)Log.d(Debug.TAG_CONNECTOR, "connection failed....");
		if((connection_attempts)>=0&&connection_attempts<200){
			++connection_attempts;
			setState(STATE_RECONNECT);
		}
		else{
		Message msg = BlueconeHandler.getHandler().obtainMessage(BlueconeHandler.TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(KEY_TOAST, BlueconeContext.getContext().getResources().getString(R.string.toast_connection_failed));
		msg.setData(bundle);
		BlueconeHandler.getHandler().sendMessage(msg);
			setState(STATE_NONE);
		}
	}
	private void connectionLost(){
		++connection_attempts;
		Message msg = BlueconeHandler.getHandler().obtainMessage(BlueconeHandler.TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(KEY_TOAST, BlueconeContext.getContext().getResources().getString(R.string.toast_connection_lost));
		msg.setData(bundle);
		BlueconeHandler.getHandler().sendMessage(msg);
		setState(STATE_NONE);
		setState(STATE_RECONNECT);
		
	}
	
	
	private void cancelThreads(){
		if(Debug.D)Log.d(Debug.TAG_CONNECTOR, "cancelThreads id ");	 
			if(connectThread!=null){
				connectThread.cancel();
				connectThread = null;
				if(Debug.D)Log.d(Debug.TAG_CONNECTOR, "ConnectThread cancelled");
			}
			if(connectedThread!=null){
				connectedThread.cancel();
				connectedThread = null;
				if(Debug.D)Log.d(Debug.TAG_CONNECTOR, "ConnectedThread cancelled");
			}
		

	}

	private class ConnectedThread extends Thread{

		private final BluetoothSocket socket;
		private final InputStreamReader input;
		private final BufferedReader reader;
		private final OutputStream output;
		
		public ConnectedThread(BluetoothSocket s) {
			if(Debug.D)Log.d(Debug.TAG_CONNECTOR, "ConnectedThread");
			socket = s;
			InputStream tmp_in = null;
			OutputStream tmp_out = null;
			
			try{
				tmp_in = socket.getInputStream();
				tmp_out = socket.getOutputStream();
			}catch(IOException e){
				Log.d(Debug.TAG_CONNECTOR, "Strømmer feilet");
			}
			input = new InputStreamReader(tmp_in);
			reader = new BufferedReader(input);
			output = tmp_out;
		}
		
		@Override
		public void run(){
			if(Debug.D)Log.d(Debug.TAG_CONNECTOR, "ConnectedThread har startet");
//			 byte[] buffer = new byte[2048];
//			
//			BufferedReader buf = new BufferedReader
//	            int bytes;
				
	            while(true){
	//            	try {
						String msg = null;
	            		try {
	            		
	            			if((msg = reader.readLine())!=null){
	            				
	            				if(Debug.D)Log.d(Debug.TAG_CONNECTOR, "msg: "+msg);
	            		
	            			BlueconeHandler.getHandler().obtainMessage(BlueconeHandler.INPUT, -1, -1,msg).sendToTarget();	
	            			}
	            		} catch (IOException e) {
	            			e.printStackTrace();
	            			connectionLost();
	            			break;
						}	
	            		/** Gammel kode ved bruk av kun inputstream*/
//						bytes = input.read(buffer);
//						String tmp = new String(buffer).trim();
//						Log.d(Debug.TAG_CONNECTOR, " "+tmp);
//						BlueconeHandler.getHandler().obtainMessage(BlueconeHandler.INPUT, bytes, -1,buffer).sendToTarget();	
//						
//						
//						buffer = new byte[2048];
						
//	            	} catch (IOException e) {
//						Log.d(Debug.TAG_CONNECTOR, "tilkobling tapt");
//						connectionLost();
//						break;
//					}
					
	           }
		}

		public void write(byte[] out) {
			try{
				output.write(out);
				BlueconeHandler.getHandler().obtainMessage(BlueconeHandler.OUTPUT, -1, -1, out)
                 .sendToTarget();
			}catch(IOException e){
				Log.d(Debug.TAG_CONNECTOR, "Noe gikk galt ved skriving");
			}
			
		}

		public void cancel() {
			try {
				socket.close();
			} catch (IOException e) {
				Log.d(Debug.TAG_CONNECTOR, "Klarte ikke å lukke socket",e);
			}

		}

	}
	private class ConnectThread extends Thread{
				private final BluetoothSocket socket;
				private final BluetoothDevice device;
		public ConnectThread(BluetoothDevice d) {
			device = d;
			BluetoothSocket tmp = null;
			
			// Dette er den originale setningen.
//			try{
//			tmp = device.createRfcommSocketToServiceRecord(mUUID);
//			}catch(IOException e){
//				Log.d(Debug.TAG_CONNECTOR, "create Rfcomm feilet");
//			}
			
			// Prøver med denne for HTC-kompabilitet.
			Method m;
			try {
				if(Debug.D)Log.d(Debug.TAG_CONNECTOR, "CreateRfcommSocket");
				m = device.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
				tmp = (BluetoothSocket)m.invoke(device, Integer.valueOf(1));
			} catch (SecurityException e) {
				Log.d(Debug.TAG_CONNECTOR, "SecurityException");
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				Log.d(Debug.TAG_CONNECTOR, "NoSuchMethodException");
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				Log.d(Debug.TAG_CONNECTOR, "IllegalArgumentException");
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				Log.d(Debug.TAG_CONNECTOR, "IllegalAccessException");
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				Log.d(Debug.TAG_CONNECTOR, "InvocationTargetException");
				e.printStackTrace();
			}
			
			socket = tmp;
		}
		@Override
		public void run(){
			if(Debug.D)Log.i(Debug.TAG_CONNECTOR, "BEGIN connectThread");
            setName("ConnectThread");
            
            bluetoothAdapter.cancelDiscovery();
            
            try{
            	socket.connect();
            }catch(IOException e){
            	connectionFailed();
           
            try{
            	socket.close();
            }catch(IOException f){
            	Log.d(Debug.TAG_CONNECTOR, "socket.close() feilet etter connect");
            }
           
            return;
		}
            synchronized (DeviceConnector.this) {
				connectThread = null;
			}
            connected(socket, device);
		}

		public void cancel() {
			try{
				socket.close();
			}catch(IOException e){
			Log.d(Debug.TAG_CONNECTOR, "close av tilkoblet socket feilet",e);
			}
		}

	}

	public int getState() {
		
		return state;
	}



}
