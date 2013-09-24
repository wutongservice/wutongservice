package com.borqs.information.rpc.service;

public class AvroServiceLauncherTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			final AvroServiceLauncher laucher = new AvroServiceLauncher();
			new Thread(new Runnable(){
				@Override
				public void run() {
					try {
						Thread.sleep(1000*30);
						laucher.stop();
						synchronized(laucher) {
							laucher.notify();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();
			
			laucher.init(args);
			laucher.start();
			synchronized(laucher) {
				laucher.wait();
			}
			System.out.println("end!");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
