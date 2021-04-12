package gov.nih.nci.evs.restapi.util;

import java.io.*;
import java.util.*;


public class RandomVariateGenerator {

    public RandomVariateGenerator() {

    }

    public int uniform(int min, int max) {
 	   Random random = new Random();
 	   int k = max - min + 1;
 	   if (k <= 0) k = 1;
 	   return random.nextInt(k) + min;
    }

    public boolean verifyFrequencies(int[] frequencies) {
        int sum = 0;
        for (int i=0; i < frequencies.length; i++) {
            if (frequencies[i] < 0) return false;
            sum = sum + frequencies[i];
        }

        if (sum != 100) return false;
        return true;
	}

    public int discreteRamdomVariate(int[] frequencies) {
        int rand = new Random().nextInt(100);
        int begin = 0, end = 0;
        for (int i=0; i < frequencies.length; i++) {
            end += frequencies[i];
            if (rand >= begin && rand < end)
                return i;
            begin = end;
        }
        return 0;
    }

    public void testDiscreteRamdomVariate(int knt) {
		int[] frequencies = {10, 10, 80};
		for (int i=0; i<knt; i++) {
			int j = i+1;
			int k = discreteRamdomVariate(frequencies);
			System.out.println("(" + j + ")" + k);
		}
	}

    public List selectWithNoReplacement(int n, int max) {
		ArrayList<Integer> arrayList = new ArrayList<Integer>();
		for (int i=0; i<max; i++) {
			arrayList.add(new Integer(i));
		}
		Collections.shuffle(arrayList);
		ArrayList<Integer> targetList = new ArrayList<Integer>();
		for (int j=0; j<n; j++) {
			Integer int_obj = arrayList.get(j);
			targetList.add(int_obj);
		}
		return targetList;
    }
}



