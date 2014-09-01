package com.atigeo.mesos.loader;

import org.apache.mesos.MesosNativeLibrary;


public class MessosLoader implements Runnable{

	public MessosLoader(){
		
	}

	@Override
	public void run() {
		System.out.println("Prepare to load native mesos library! " + Thread.currentThread().getId());
        try{
            MesosNativeLibrary.load();
            System.out.println("Loaded mesos native library	!" + Thread.currentThread().getId());
        }catch (UnsatisfiedLinkError error){
            System.out.println("[WARN] Mesos library not loaded. Assuming you are running on spark");
            System.out.println("[WARN] If you are running on mesos please provide a valid path for MESOS_NATIVE_LIBRARY");
        }
	}
}
