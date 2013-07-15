package fr.labri.timedautomata;

import java.awt.Dimension;

import javax.swing.JFrame;

import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.visualization.BasicVisualizationServer;
import fr.labri.timedautomata.ITimedAutomata.Predicate;
import fr.labri.timedautomata.ITimedAutomata.State;

public class AutomataViewer<C> {
	public static JFrame viewAsFrame(ITimedAutomata<?> automata) {
		JFrame frame = new JFrame("Simple Graph View");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(new AutomataViewer<>(automata).asPanel());
		frame.pack();
		frame.setVisible(true);
		return frame;
	}

	final private ITimedAutomata<C> _auto;

	AutomataViewer(ITimedAutomata<C> auto) {
		_auto = auto;
	}

	BasicVisualizationServer<State<C>, Predicate<C>> asPanel() {
		DirectedGraph<State<C>, Predicate<C>> g = asGraph();
		Layout<State<C>, Predicate<C>> layout = new FRLayout<>(g);
		layout.setSize(new Dimension(300, 300));
		BasicVisualizationServer<State<C>, Predicate<C>> vv = new BasicVisualizationServer<State<C>, Predicate<C>>(
				layout);
		vv.setPreferredSize(new Dimension(350, 350)); // Sets the viewing area
														// size
		vv.getRenderContext().getPickedVertexState()
				.pick(_auto.getInitialState(), true);
		for (Predicate<C> e : g.getEdges())
			if (e instanceof DefaultTransition)
				vv.getRenderContext().getPickedEdgeState().pick(e, true);
		return vv;
	}

	DirectedGraph<State<C>, Predicate<C>> asGraph() {
		DirectedGraph<State<C>, Predicate<C>> sgv = new DirectedSparseGraph<State<C>, Predicate<C>>();
		for (State<C> state : _auto.getStates())
			sgv.addVertex(state);
		for (State<C> state : _auto.getStates())
			for (State<C> dst : _auto.getFollowers(state)) {
				final Predicate<C> pred = _auto.getPredicate(state, dst);
				int timeout = _auto.getTimeout(state, dst);
				if(timeout != TimedAutomata.TIMEOUT)
					sgv.addEdge(new Predicate<C>() {
						@Override
						public boolean isValid(C context, String key) {
							return pred.isValid(context, key);
						}

						@Override
						public String getType() {
							return pred.getType();
						}
					}, state, dst);
				else
					sgv.addEdge(new DefaultTransition<C>(pred), state, dst);

			}
		return sgv;
	}

	static class DefaultTransition<C> implements Predicate<C> {
		final Predicate<C> _pred;
		public DefaultTransition(Predicate<C> pred) {
			_pred = pred;
		}
		@Override
		public boolean isValid(C context, String key) {
			return _pred.isValid(context, key);
		}

		@Override
		public String getType() {
			return _pred.getType();
		}
	}

}
