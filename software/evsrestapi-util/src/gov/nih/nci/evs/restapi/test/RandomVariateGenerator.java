package gov.nih.nci.evs.restapi.test;

import java.io.*;
import java.util.*;


/**
 * <!-- LICENSE_TEXT_START -->
 * Copyright 2008-2015 NGIT. This software was developed in conjunction
 * with the National Cancer Institute, and so to the extent government
 * employees are co-authors, any rights in such works shall be subject
 * to Title 17 of the United States Code, section 105.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *   1. Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the disclaimer of Article 3,
 *      below. Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials provided
 *      with the distribution.
 *   2. The end-user documentation included with the redistribution,
 *      if any, must include the following acknowledgment:
 *      "This product includes software developed by NGIT and the National
 *      Cancer Institute."   If no such end-user documentation is to be
 *      included, this acknowledgment shall appear in the software itself,
 *      wherever such third-party acknowledgments normally appear.
 *   3. The names "The National Cancer Institute", "NCI" and "NGIT" must
 *      not be used to endorse or promote products derived from this software.
 *   4. This license does not authorize the incorporation of this software
 *      into any third party proprietary programs. This license does not
 *      authorize the recipient to use any trademarks owned by either NCI
 *      or NGIT
 *   5. THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESSED OR IMPLIED
 *      WARRANTIES, (INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *      OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE) ARE
 *      DISCLAIMED. IN NO EVENT SHALL THE NATIONAL CANCER INSTITUTE,
 *      NGIT, OR THEIR AFFILIATES BE LIABLE FOR ANY DIRECT, INDIRECT,
 *      INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 *      BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *      LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *      CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 *      LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 *      ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *      POSSIBILITY OF SUCH DAMAGE.
 * <!-- LICENSE_TEXT_END -->
 */

/**
 * @author EVS Team
 * @version 1.0
 *
 * Modification history:
 *     Initial implementation kim.ong@ngc.com
 *
 */


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
		for (int j=0; j<max; j++) {
			Integer int_obj = arrayList.get(j);
			targetList.add(int_obj);
		}
		return targetList;
    }

	public static void main(String [] args) {
        RandomVariateGenerator generator = new RandomVariateGenerator();
        List list = generator.selectWithNoReplacement(0, 10);
        for (int i=0; i<list.size(); i++) {
			Integer int_obj = (Integer) list.get(i);
			System.out.println(int_obj.intValue());
		}
	}
}



