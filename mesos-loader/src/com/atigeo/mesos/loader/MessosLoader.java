package com.atigeo.mesos.loader;

import org.apache.mesos.MesosNativeLibrary;


public class MessosLoader implements Runnable{

	public MessosLoader(){
		
	}

	@Override
	public void run() {
		System.out.println("Prepare to load native mesos librar!" + Thread.currentThread().getId());
		MesosNativeLibrary.load();
		System.out.println("Loaded mesos native library	!" + Thread.currentThread().getId());
	}
	
}
