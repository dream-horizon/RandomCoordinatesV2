package com.jolbol1.RandomCoordinates.managers.Util;

import java.util.Random;

import org.bukkit.scheduler.BukkitTask;

import com.jolbol1.RandomCoordinates.RandomCoords;
import com.jolbol1.RandomCoordinates.managers.DebugManager;

public class Benchmark {
	BukkitTask benchmarkTask;
	String benchmarkId;
	int ticks;
	long millis;
	final DebugManager debugmanager=new DebugManager();
	StackTraceElement[] stackTrace;
	final Random rand;
	
	public Benchmark() {
		rand = new Random();
	}
	
	public void start() {
		if (RandomCoords.getPlugin().config.getString("debug").equals("true") && benchmarkTask == null) {
			benchmarkId= Integer.toHexString(rand.nextInt(0xFF + 1));
			stackTrace = Thread.currentThread().getStackTrace();
			debugmanager.logToFile("Beginning Benchmark! ID: " + benchmarkId);
			benchmarkTask = RandomCoords.getPlugin().getServer().getScheduler().runTaskTimer(RandomCoords.getPlugin(),
					new Runnable() {

						@Override
						public void run() {
							tick();
						}
					}, 0L, 0L);

			millis = System.currentTimeMillis();
		}
	}

	public void stop() {
		if (RandomCoords.getPlugin().config.getString("debug").equals("true") && benchmarkTask != null) {
			millis = System.currentTimeMillis() - millis;
			benchmarkTask.cancel();
			float averageTPS = (float)ticks/((float) millis / 1000F);
			debugmanager.logToFile("Benchmark " + benchmarkId + " finished!");
			debugmanager.logToFile("\tTime passed: " + millis + " ms");
			debugmanager.logToFile("\tTicks passed: " + ticks + " Ticks");	
			debugmanager.logToFile("\tAverage Ticks per second: " + averageTPS + " TPS");
			debugmanager.logToFile("\tCalled from: " + stackTrace[2]);
			benchmarkTask=null;
			ticks= 0;
		}
	}
	
	private void tick() {
		ticks++;
	}
}
