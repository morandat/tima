package fr.labri.tima;

import java.util.Collection;
import java.util.List;

public interface ITimedAutomata<C> {
	public final static int INITIAL = 1 << 0;
	public final static int URGENT = 1 << 1;
	public final static int SPAWN = 1 << 2;
	public final static int TERMINATE = 1 << 3;

	public State<C> getInitialState();
	public void setInitialState(State<C> initial);
	public State<C>[] getStates();
	public State<C>[] getFollowers(State<C> src);
	public int getTimeout(State<C> src, State<C> dst);
	public Predicate<C> getPredicate(State<C> src, State<C> dst);

	public Cursor<C> start(ContextProvider<C> context, String key);

	public interface NodeFactory<C> {
		Predicate<C> newPredicate(String type, String attr);
		Action<C> newAction(String type, String attr);
		Spawner<C> newSpawner(String type, String attr);
	}

	public interface Predicate<C> {
		boolean isValid(C context, String key);
		String getType();
	}
	
	public interface Action<C> {
		void preAction(C context, String key);
		void eachAction(C context, String key);
		void postAction(C context, String key);
		
		String getType();
	}
	
	public interface Spawner<C> {
		String getType();
		
		String getSpawnerKey(C context, String parentKey);
	}
	
	public interface State<C> {
		String getName();
		
		List<Action<C>> getActions();
		int getModifier();
		
		void preAction(C context, Executor<C> executor, String key);
		void eachAction(C context, Executor<C> executor, String key);
		void postAction(C context, Executor<C> executor, String key);

		List<ITimedAutomata<C>> getSpawnableAutomatas();
	}
	
	public interface Executor<C> {
		void start(ITimedAutomata<C> auto, String key);
		void next();
		
		Collection<Cursor<C>> getCursors();
	}
	
	
	public interface Cursor<C> {
		boolean next(Executor<C> executor);
		String getKey();
		ITimedAutomata<C> getAutomata();
	}
	
	public interface ContextProvider<C> {
		C getContext();
	}
	
	public class ActionAdapter<C> implements Action<C> {
		public String getType() {
			return getClass().getCanonicalName();
		}

		@Override
		public void preAction(C context, String key) {
		}

		@Override
		public void eachAction(C context, String key) {
		}

		@Override
		public void postAction(C context, String key) {
		}
		
		public String toString() {
			return getType();
		}
	}

	public class PredicateAdapter<C> implements Predicate<C> {
		public boolean isValid(C context, String key) {
			return false;
		}
	
		@Override
		public String getType() {
			return getClass().getCanonicalName();
		}
		
		public String toString() {
			return getType();
		}
	}


	public class SpawnAdapter<C> implements Spawner<C> {
		@Override
		public String getType() {
			return getClass().getCanonicalName();
		}

		@Override
		public String getSpawnerKey(C context, String parentKey) {
			return null;
		}
		
		public String toString() {
			return getType();
		}
	}
}

