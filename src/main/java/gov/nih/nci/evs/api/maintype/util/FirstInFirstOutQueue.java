package gov.nih.nci.evs.api.maintype.util;

import java.util.LinkedList;
import java.util.Queue;

public class FirstInFirstOutQueue {
	Queue<String> fifo = null;

	public FirstInFirstOutQueue() {
		fifo = new LinkedList<String>();
	}

	public void add(String link) {
		fifo.add(link);
	}

	public String remove() {
		String link = fifo.remove();
		return link;
	}

	public boolean isEmpty() {
		return fifo.isEmpty();
	}

    public static void main(String args[]) {
        char arr[] = {'3','1','4','1','5','9','2','6','5','3','5','8','9'};
        Queue<String> fifo = new LinkedList<String>();

        for (int i = 0; i < arr.length; i++) {
			String s = "" + arr[i];
            fifo.add(s);
		}

        System.out.print (fifo.remove() + ".");
        while (!fifo.isEmpty()) {
			String link = fifo.remove();
            System.out.print(link);
		}
        System.out.println();

    }
}