package fr.labri;

public class Pair<T1, T2> {
	public final T1 fst;
	public final T2 snd;
	
	private Pair(T1 f, T2 s) {
		fst = f;
		snd = s;
	}
	
	public static <A, B> Pair<A,B> of(A f, B s)  {
		return new Pair<A,B>(f, s);
	}
}
