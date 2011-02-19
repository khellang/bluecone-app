package com.bluecone.connect;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import com.bluecone.BlueconeHandler;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

public class DeviceConnector {

	private static final String TAG = "DeviceConnector";
	private static boolean D = true;
	private static final UUID mUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private BluetoothAdapter bluetoothAdapter;
	private BluetoothDevice bluetoothDevice;
	
	private ConnectThread connectThread;
	private ConnectedThread connectedThread;
	
	public static final int STATE_CHANGED = 0;
	public static final int STATE_NONE = 0;
	public static final int STATE_CONNECTING = 1;
	public static final int STATE_CONNECTED = 2;
	
	public static final String KEY_NAME = "name";
	public static final String KEY_TOAST = "toast";
	public static final int FLAG_NAME = 5;

	
	private int state;

	
	public DeviceConnector(){
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		state = STATE_NONE;
	}
	
	private BluetoothDevice setDeviceByMAC(String mac){
		if(D)Log.d(TAG, "setDeviceByMac..."+mac);
		return bluetoothAdapter.getRemoteDevice(mac);
	}

	
    public synchronized void start() {
        if (D) Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (connectThread != null) {connectThread.cancel(); connectThread = null;}

        // Cancel any thread currently running a connection
        if (connectedThread != null) {connectedThread.cancel(); connectedThread = null;}

        setState(STATE_NONE);
    }

	private  void setState(int newState) {
		Log.d(TAG, "setState: "+state +" --> "+newState);
		state = newState;
		BlueconeHandler.getHandler().obtainMessage(STATE_CHANGED, state, -1).sendToTarget();

	}
	public synchronized void connect(String mac){
		if(D)Log.d(TAG, "connect to.... ");
		bluetoothDevice = setDeviceByMAC(mac);
		cancelThreads();//Avslutter tilkoblingstråder
		connectThread = new ConnectThread(bluetoothDevice);//Oppretter en ny tråd for å koble til enheten
		connectThread.start();
		setState(STATE_CONNECTING);
	}

	public synchronized void connected(BluetoothSocket socket,BluetoothDevice device){
		if(D)Log.d(TAG, "connected");
		cancelThreads();//Avslutter alle tråder
		connectedThread = new ConnectedThread(socket);
		connectedThread.start();
		
		Message msg = BlueconeHandler.getHandler().obtainMessage(FLAG_NAME);
		Bundle bundle = new Bundle();
		bundle.putString(KEY_NAME, device.getName());
		msg.setData(bundle);
		BlueconeHandler.getHandler().sendMessage(msg);
		
		setState(STATE_CONNECTED);

	}
	public synchronized void stop(){
		if(D)Log.d(TAG, "stop");
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
		if(D)Log.d(TAG, "connection failed....");
		Message msg = BlueconeHandler.getHandler().obtainMessage(BlueconeHandler.TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(KEY_TOAST, "kunne ikke koble til enheten");
		msg.setData(bundle);
		BlueconeHandler.getHandler().sendMessage(msg);
		setState(STATE_NONE);		
	}
	private void connectionLost(){
		Message msg = BlueconeHandler.getHandler().obtainMessage(BlueconeHandler.TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(KEY_TOAST, "tilkobling avbrutt");
		msg.setData(bundle);
		BlueconeHandler.getHandler().sendMessage(msg);
		setState(STATE_NONE);
	}
	
	
	private void cancelThreads(){
		if(D)Log.d(TAG, "cancelThreads id ");	 
			if(connectThread!=null){
				connectThread.cancel();
				connectThread = null;
				if(D)Log.d(TAG, "ConnectThread cancelled");
			}
			if(connectedThread!=null){
				connectedThread.cancel();
				connectedThread = null;
				if(D)Log.d(TAG, "ConnectedThread cancelled");
			}
		

	}

	private class ConnectedThread extends Thread{

		private final BluetoothSocket socket;
		private final InputStream input;
		private final OutputStream output;
		
		public ConnectedThread(BluetoothSocket s) {
			Log.d(TAG, "ConnectedThread");
			socket = s;
			InputStream tmp_in = null;
			OutputStream tmp_out = null;
			
			try{
				tmp_in = socket.getInputStream();
				tmp_out = socket.getOutputStream();
			}catch(IOException e){
				Log.d(TAG, "Strømmer feilet");
			}
			input = tmp_in;
			output = tmp_out;
		}
		
		@Override
		public void run(){
			Log.d(TAG, "ConnectedThread har startet");
			 byte[] buffer = new byte[2048];
	            int bytes;
	            
	            while(true){
	            	try {
						
						bytes = input.read(buffer);
						String tmp = new String(buffer).trim();
						Log.d(TAG, " "+tmp);
						BlueconeHandler.getHandler().obtainMessage(BlueconeHandler.INPUT, bytes, -1,buffer).sendToTarget();	
						
						
						buffer = new byte[2048];
						
	            	} catch (IOException e) {
						Log.d(TAG, "tilkobling tapt");
						connectionLost();
						break;
					}
					
	            }
		}

		public void write(byte[] out) {
			try{
				output.write(out);
				BlueconeHandler.getHandler().obtainMessage(BlueconeHandler.OUTPUT, -1, -1, out)
                 .sendToTarget();
			}catch(IOException e){
				Log.d(TAG, "Noe gikk galt ved skriving");
			}
			
		}

		public void cancel() {
			try {
				socket.close();
			} catch (IOException e) {
				Log.d(TAG, "Klarte ikke å lukke socket",e);
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
			try{
			tmp = device.createRfcommSocketToServiceRecord(mUUID);
			}catch(IOException e){
				Log.d(TAG, "create Rfcomm feilet");
			}
			
			// Prøver med denne for HTC-kompabilitet.
		/*	Method m;
			try {
				m = device.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
				tmp = (BluetoothSocket)m.invoke(device, Integer.valueOf(1));
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}*/
			
			socket = tmp;
		}
		@Override
		public void run(){
			Log.i(TAG, "BEGIN connectThread");
            setName("ConnectThread");
            
            bluetoothAdapter.cancelDiscovery();
            
            try{
            	socket.connect();
            }catch(IOException e){
            	connectionFailed();
           
            try{
            	socket.close();
            }catch(IOException f){
            	Log.d(TAG, "socket.close() feilet etter connect");
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
			Log.d(TAG, "close av tilkoblet socket feilet",e);
			}
		}

	}

	public int getState() {
		
		return state;
	}

}
