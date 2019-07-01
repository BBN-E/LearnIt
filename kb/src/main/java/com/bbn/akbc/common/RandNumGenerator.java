package com.bbn.akbc.common;
import java.util.Random;


public class RandNumGenerator {
	static Long seed = 1L; // fix seed
	static Random randomGenerator = new Random(seed);

	public static double generateNextDouble() {
		return randomGenerator.nextDouble();
	}

	public static int generateNextInt(int max) {
		return randomGenerator.nextInt(max);
	}
}
