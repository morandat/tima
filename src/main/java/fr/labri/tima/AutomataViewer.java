package fr.labri.tima;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.JFrame;

import edu.uci.ics.jung.algorithms.layout.FRLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import fr.labri.tima.ITimedAutomata.Cursor;
import fr.labri.tima.ITimedAutomata.Predicate;
import fr.labri.tima.ITimedAutomata.State;

public class AutomataViewer {
	private static final int GAP = 50;

	public static JFrame viewAsFrame(ITimedAutomata<?> automata) {
		JFrame frame = new JFrame("Simple Graph View");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(createPanel(automata));
		frame.pack();
		frame.setVisible(true);
		return frame;
	}

	public static <C> VisualizationViewer<State<C>, Predicate<C>> createPanel(Executor<C> executor) {
		return createPanel(executor, new Dimension(300, 300));
	}

	public static <C> VisualizationViewer<State<C>, Predicate<C>> createPanel(ITimedAutomata<C> auto) {
		return createPanel(auto, new Dimension(300, 300));
	}

	public static <C> VisualizationViewer<State<C>, Predicate<C>> createPanel(Collection<? extends ITimedAutomata<C>> autos) {
		return createPanel(autos, new Dimension(300, 300));
	}

	public static <C> VisualizationViewer<State<C>, Predicate<C>> createPanel(ITimedAutomata<C> auto, Dimension dimension) {
		return createPanel(dimension, createGraph(auto));
	}

	public static <C> VisualizationViewer<State<C>, Predicate<C>> createPanel(Executor<C> executor, Dimension dimension) {
		ArrayList<ITimedAutomata<C>> lst = new ArrayList<>();
		for(Cursor<C> cursor: executor.getCursors())
			lst.add(cursor.getAutomata());
		return createPanel(dimension, createGraph(lst));
	}

	public static <C> VisualizationViewer<State<C>, Predicate<C>> createPanel(Collection<? extends ITimedAutomata<C>> autos, Dimension dimension) {
		return createPanel(dimension, createGraph(autos));
	}
	
	public static <C> VisualizationViewer<State<C>, Predicate<C>> createPanel(Dimension dimension, DirectedGraph<State<C>, Predicate<C>> g) {
		Layout<State<C>, Predicate<C>> layout = new FRLayout<>(g, dimension);
		VisualizationViewer<State<C>, Predicate<C>> vv = new VisualizationViewer<State<C>, Predicate<C>>(
				layout);
		vv.setAutoscrolls(true);
		vv.setPreferredSize(new Dimension(dimension.width + GAP, dimension.height + GAP)); // Sets the viewing area
														// size
//		vv.getRenderContext().getPickedVertexState()
//				.pick(auto.getInitialState(), true);
//		for (Predicate<C> e : g.getEdges())
//			if (e instanceof DefaultTransition)
//				vv.getRenderContext().getPickedEdgeState().pick(e, true);
		return vv;
	}

	public static <C> DirectedGraph<State<C>, Predicate<C>> createGraph(Collection<? extends ITimedAutomata<C>> autos) {
		DirectedSparseGraph<State<C>, Predicate<C>> graph = new DirectedSparseGraph<>();
		for(ITimedAutomata<C> auto: autos)
			createGraph(auto, graph);
		return graph;
	}
	
	public static <C> DirectedGraph<State<C>, Predicate<C>> createGraph(ITimedAutomata<C> autos[]) {
		return createGraph(Arrays.asList(autos));
	}

	public static <C> DirectedGraph<State<C>, Predicate<C>> createGraph(ITimedAutomata<C> auto) {
		DirectedSparseGraph<State<C>, Predicate<C>> graph = new DirectedSparseGraph<>();
		createGraph(auto, graph);
		return graph;
	}
	
	private static <C> DirectedGraph<State<C>, Predicate<C>> createGraph(ITimedAutomata<C> auto, DirectedGraph<State<C>, Predicate<C>> sgv) {
		for (State<C> state : auto.getStates())
			sgv.addVertex(state);
		for (State<C> state : auto.getStates())
			for (State<C> dst : auto.getFollowers(state)) {
				final Predicate<C> pred = auto.getPredicate(state, dst);
				int timeout = auto.getTimeout(state, dst);
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

	private static class DefaultTransition<C> implements Predicate<C> {
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
