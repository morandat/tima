package fr.labri;



public class Utils {
	public static final void debug(Object recv, Object... strs) {
		StringBuilder builder = new StringBuilder("[").append(recv.getClass())
				.append("]\t");
		for(Object str: strs)
			builder.append(str);
		System.out.println(builder);
	}

	public static final void pause(long wait) {
		try {
			Thread.sleep(wait);
		} catch (InterruptedException e) {
		}
	}
	
	public static final long nthBit(long number, int nth) {
	    while(nth-- > 0) {
	        number &= ~(number & -number);
	    }
	    return number &= -number;
	}
	
	public static final long nthLowerBit(long number, int nth) {
	    long res = 0;
	    while(nth-- > 0) {
	        long out = number & -number;
	        res |= out;
	        number &= ~(out);
	    }
	    return res;
	}
	
	public static <T> int indexOf(T needle, T[] haystack) {
	    for (int i=0; i<haystack.length; i++) {
	        if (haystack[i] != null && haystack[i].equals(needle)
	            || needle == null && haystack[i] == null) return i;
	    }
	    return -1;
	}
}
