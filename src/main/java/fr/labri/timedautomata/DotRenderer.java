package fr.labri.timedautomata;

import fr.labri.Utils;
import fr.labri.timedautomata.ITimedAutomata.Action;
import fr.labri.timedautomata.ITimedAutomata.Predicate;
import fr.labri.timedautomata.ITimedAutomata.State;

public class DotRenderer<C> {
	public final static String INFINITY_SYMBOL = "+oo"; // "\u221E" 
	public final ITimedAutomata<C> _auto;
	public final State<C>[] _states;
	public final int start = 0;
	
	public DotRenderer(ITimedAutomata<C> auto) {
		_auto = auto;
		_states = auto.getStates();
	}
	
	public String toDot(String name) {
		StringBuilder b = new StringBuilder("digraph ").append(name).append(" {\nedge [splines=ortho];\n");
		
		for(State<C> state: _states) {
			b.append(getNodeID(state)).append(" [").append(getStateDecoration(state)).append("];\n");
			for(State<C> dst: _auto.getFollowers(state))
				if(dst != null)
					b.append(getNodeID(state)).append(" -> ").append(getNodeID(dst)).append(" [").append(getTransitionDecoration(state, dst)).append("];\n");
		}

		return b.append("};").toString();
	}

	private String getTransitionDecoration(State<C> src, State<C> dst) {
		StringBuilder b = new StringBuilder();
		int timeout = _auto.getTimeout(src, dst);
		Predicate<C> pred = _auto.getPredicate(src, dst);
		
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

	public String getNodeID(State<C> state) {
		return "node" + Integer.toString(start + Utils.indexOf(state, _states));
	}
	
	public String getNodeName(State<C> state) {
		String name = state.getName();
		return (name == null) ? getNodeID(state) : name;
	}
	
	public String getStateDecoration(State<C> state) {
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
