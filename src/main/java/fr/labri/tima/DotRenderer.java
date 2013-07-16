package fr.labri.tima;

import java.util.Collection;

import fr.labri.Utils;
import fr.labri.tima.ITimedAutomata.Action;
import fr.labri.tima.ITimedAutomata.Cursor;
import fr.labri.tima.ITimedAutomata.Predicate;
import fr.labri.tima.ITimedAutomata.State;

public class DotRenderer {
	public final static String INFINITY_SYMBOL = "+oo"; // "\u221E"

	public static <C> String toDot(ITimedAutomata<C> auto) {
		return toDot(auto, "G");
	}
	
	public static <C> String toDot(ITimedAutomata<C> auto, String name) {
		StringBuilder b = new StringBuilder("digraph ").append(name).append(" {\nedge [splines=ortho];\n");
		
		b.append(drawGraph(auto, ""));

		return b.append("};").toString();
	}


	public static <C> String toDot(Executor<C> exec, String name) {
		StringBuilder b = new StringBuilder("digraph ").append(name).append(" {\nedge [splines=ortho];\n");
		int i = 0;
		
		for(Cursor<C> cursor: exec.getCursors()) {
			b.append("subgraph cluster_").append(Integer.toString(i)).append(" {\n");
			b.append(drawGraph(cursor.getAutomata(), "_" + i ++));
			b.append("}");
		}

		return b.append("};").toString();
	}
	
	public static <C> String toDot(Collection<? extends ITimedAutomata<C>> autos, String name) {
		StringBuilder b = new StringBuilder("digraph ").append(name).append(" {\nedge [splines=ortho];\n");
		int i = 0;
		
		for(ITimedAutomata<C> auto: autos) {
			b.append("subgraph cluster_").append(Integer.toString(i)).append(" {\n");
			b.append(drawGraph(auto, "_" + i ++));
			b.append("}");
		}

		return b.append("};").toString();
	}
	
	public static <C> String drawGraph(ITimedAutomata<C> auto, String offset) {
		StringBuilder b = new StringBuilder();
		State<C>[] states = auto.getStates();

		for(State<C> state: states) {
			b.append(getNodeID(state, states, offset)).append(" [").append(getStateDecoration(state)).append("];\n");
			for(State<C> dst: auto.getFollowers(state))
				if(dst != null)
					b.append(getNodeID(state, states, offset)).append(" -> ").append(getNodeID(dst, states, offset)).append(" [").append(getTransitionDecoration(auto, state, dst)).append("];\n");
		}
		return b.toString();
	}
	
	private static <C> String getTransitionDecoration(ITimedAutomata<C> auto, State<C> src, State<C> dst) {
		StringBuilder b = new StringBuilder();
		int timeout = auto.getTimeout(src, dst);
		Predicate<C> pred = auto.getPredicate(src, dst);
		
		String sep = "";
		if(pred != null) {
			b.append("label=\"").append(pred.getType()).append("\"");
			sep = ", ";
		}
		
		if(timeout != TimedAutomata.TIMEOUT) {
			b.append(sep).append("taillabel=\"").append(timeout == TimedAutomata.INFINITY ? INFINITY_SYMBOL : Integer.toString(timeout)).append("\"");
			sep = ", ";
		}
		
		if(timeout == TimedAutomata.TIMEOUT)
			b.append(sep).append("style=dashed");

		return b.toString();
	}

	public static <C> String getNodeID(State<C> state, State<C>[] states, String offset) {
		return "node" + offset + Integer.toString(Utils.indexOf(state, states));
	}
	
//	public String getNodeName(State<C> state) {
//		String name = state.getName();
//		return (name == null) ? getNodeID(state) : name;
//	}
	
	public static <C> String getStateDecoration(State<C> state) {
		StringBuilder b = new StringBuilder();
		b.append("shape=\"record\", label=\"{").append(state.getName()).append("|{");
		String sep = "";
		for(Action<C> a: state.getActions()) {
			b.append(sep).append(a.getType());
			sep = "|";
		}
		b.append("}}\"");
		int mod = state.getModifier();
		b.append(", style=\"filled");
		if((mod & ITimedAutomata.INITIAL) > 0)
			b.append(",diagonals");
		if((mod & ITimedAutomata.URGENT) > 0)
			b.append(",bold");
		b.append("\"");

		if((mod & ITimedAutomata.TERMINATE) > 0)
			b.append(", color=\"red\"");
		
		if((mod & ITimedAutomata.SPAWN) > 0)
			b.append(", fillcolor=\"yellow\"");
		else
			b.append(", fillcolor=\"white\"");

		return b.toString();
	}
}
