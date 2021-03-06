package fr.labri.tima;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaderXSDFactory;

import fr.labri.AutoQualifiedClassLoader;
import fr.labri.tima.ITimedAutomata.Action;
import fr.labri.tima.ITimedAutomata.ContextProvider;
import fr.labri.tima.ITimedAutomata.Executor;
import fr.labri.tima.ITimedAutomata.NodeFactory;
import fr.labri.tima.ITimedAutomata.Predicate;
import fr.labri.tima.ITimedAutomata.State;

public class TimedAutomataFactory<C> {
	public static final String AUTOMATA_TAG = "timedautomata";
	public static final String AUTOMATA_NAME_TAG = "name";

	public static final String STATE_TAG = "state";
	public static final String TRANSITION_TAG = "path";
	public static final String TIMEOUT_TAG = "default";
	
	public static final String STATE_URGENT_TAG = "urgent";
	public static final String STATE_NAME_TAG = "name";
	public static final String STATE_INITIAL_TAG = "initial";
	public static final String STATE_TERMINAL_TAG = "terminal";

	public static final String ACTION_TAG = "action";
	public static final String ACTION_NAME_TAG = "type";
	public static final String ACTION_ATTR_TAG = "attr";

	private static final String TERMINATE_TAG = "terminate";

	
	public static final String TRANSITION_TARGET_TAG = "to";
	public static final String TRANSITION_PREDICATE_TAG = "guard";
	public static final String TRANSITION_TIMEOUT_TAG = "timeout";
	public static final String TRANSITION_ATTR_TAG = "attr";
	
	public static final String XMLNS_XSI = "http://www.w3.org/2001/XMLSchema-instance";
	public static final String XSI_LOCATION = "http://www.labri.fr/~fmoranda/xsd/tima.xsd";
	
	private final NodeFactory<C> _factory;
	
	private final Map<String, Action<C>> _actionMap = new HashMap<String, Action<C>>();
	private final Map<String, Predicate<C>> _predicateMap = new HashMap<String, Predicate<C>>();
	
	private final List<ITimedAutomata<C>> _masters = new ArrayList<>();

	public TimedAutomataFactory(NodeFactory<C> factory) {
		_factory = factory;
	}


	public Document parseXML(InputStream stream, boolean validate) throws JDOMException, IOException {
		SAXBuilder sxb = new SAXBuilder(validate ? new XMLReaderXSDFactory(TimedAutomata.class.getResource("tima.xsd")) : null);

		Document document = sxb.build(stream);
		return document;
	}
	
	public List<ITimedAutomata<C>> loadXML(InputStream stream) throws JDOMException, IOException {
		return loadXML(parseXML(stream, false));
	}
	
	public List<ITimedAutomata<C>> loadXML(InputStream stream, boolean validate) throws JDOMException, IOException {
		return loadXML(parseXML(stream, validate));
	}
	
	public List<ITimedAutomata<C>> loadXML(Document root) throws JDOMException, IOException {
		Map<String, Element> autosMap = new HashMap<>();
		Map<Element, TimedAutomata<C>> autos = new HashMap<>();

		resolveAutomataName(root.getRootElement(), autosMap, autos);
		for(Element auto: autosMap.values())
			loadAutomata(auto, autosMap, autos);
		
		List<ITimedAutomata<C>> res = new ArrayList<>();
		res.addAll(autos.values());
		_masters.addAll(autos.values());
		
		return res;
	}

	final public Executor<C> getExecutor(ContextProvider<C> provider) {
		return getExecutor(provider, true);
	}

	public Executor<C> getExecutor(ContextProvider<C> provider, boolean compiled) {
		List<ITimedAutomata<C>> masters = compiled
            ? _masters.stream().map(m -> m.compile()).collect(Collectors.toList())
            : _masters;
		return new BasicExecutor<>(provider, masters).start();
	}
	
	protected void resolveAutomataName(Element auto, Map<String, Element> autosMap, Map<Element, TimedAutomata<C>> autos) {
		String name = auto.getAttributeValue(AUTOMATA_NAME_TAG);
		
		if(name == null)
			throw new RuntimeException("Automata has no name: " + auto.toString());
		
		if(autos.containsKey(name))
			throw new RuntimeException("There is more than one automata with the name: " + name);

		autosMap.put(name, auto);
		autos.put(auto, new TimedAutomata<C>());
		
		for(Element sub: auto.getChildren(AUTOMATA_TAG))
			resolveAutomataName(sub, autosMap, autos);
	}

	protected ITimedAutomata<C> loadAutomata(Element auto, Map<String, Element> autosMap, Map<Element, TimedAutomata<C>> autos) throws JDOMException, IOException {
		
		Map<String, Element> stateMap = new HashMap<>();
		resolveStates(auto, autosMap, stateMap);
		Map<Element, Element> transMap = new HashMap<>();
		resolveTransitions(auto, stateMap, transMap);
		
		TimedAutomata<C> cAuto = autos.get(auto);
		Map<Element, State<C>> states = new HashMap<>();
		buildStates(cAuto, stateMap, states, autos);
		buildTransitions(cAuto, states, transMap);
		
		return cAuto;
	}
	
	protected void buildTransitions(TimedAutomata<C> auto, Map<Element, State<C>> stateMap, Map<Element, Element> transMap) {
		for(Entry<Element, State<C>> entry: stateMap.entrySet()){
			Element srcElt = entry.getKey();
			State<C> src = entry.getValue();
			
			for(Element trans: srcElt.getChildren(TRANSITION_TAG)) {
				State<C> dest = stateMap.get(transMap.get(trans));
				
				String pred = trans.getAttributeValue(TRANSITION_PREDICATE_TAG);
				String timeoutval = trans.getAttributeValue(TRANSITION_TIMEOUT_TAG);
				String attr = trans.getAttributeValue(TRANSITION_ATTR_TAG);
				int timeout = (timeoutval == null) ? TimedAutomata.INFINITY : Integer.parseInt(timeoutval);
				
				Predicate<C> predicate = getPredicate(pred, attr);
				if(predicate == null)
					throw new RuntimeException("Unable to create predicate : " + pred +"(" + attr+")");
				auto.addTransition(src, timeout, predicate, dest);
			}
			
			Element timeout = srcElt.getChild(TIMEOUT_TAG);
			if(timeout != null) {
				State<C> dest = stateMap.get(transMap.get(timeout));
				auto.addDefaultTransition(src, dest);
			} 
		}
	}

	protected void buildStates(TimedAutomata<C> auto, Map<String, Element> stateMap, Map<Element, State<C>> states, Map<Element, TimedAutomata<C>> autos) {
		for(Entry<String, Element> entry: stateMap.entrySet()){
			boolean isTerm = false;
			Element state = entry.getValue();
			ArrayList<Action<C>> acts = new ArrayList<>();
			
			for(Element act: state.getChildren()) {
				if(isTerm)
					throw new RuntimeException("Unreachable code, actions after terminate in " + entry.getKey());
				String aName = act.getName();
				if(ACTION_TAG.equalsIgnoreCase(aName)) { // TODO remove this switch case
					Action<C> a = getAction(act.getAttributeValue(ACTION_NAME_TAG), act.getAttributeValue(ACTION_ATTR_TAG));
					if(a == null)
						throw new RuntimeException("Unable to create action : " + act.getAttributeValue(ACTION_NAME_TAG) +"(" + act.getAttributeValue(ACTION_ATTR_TAG)+")");
					acts.add(a);
				} else if(TERMINATE_TAG.equalsIgnoreCase(aName)) {
					isTerm = true;
				} else if(TRANSITION_TAG.equalsIgnoreCase(aName)) {
				} else if(TIMEOUT_TAG.equalsIgnoreCase(aName)) {
				} else
					throw new RuntimeException("Unknwown tag: " + aName);
			}
			
			
			String dfltAct = state.getAttributeValue(ACTION_TAG);
			if(dfltAct != null) {
				Action<C> a = getAction(dfltAct, state.getAttributeValue(ACTION_ATTR_TAG));
				if(a != null)
					acts.add(a);
			} else if(state.getAttributeValue(ACTION_ATTR_TAG) != null)
				throw new RuntimeException("Attribue without action in state: "+ entry.getKey());
			
			if ("true".equalsIgnoreCase(state.getAttributeValue(TimedAutomataFactory.STATE_TERMINAL_TAG)))
				if(isTerm)
					throw new RuntimeException("More than one terminate in " + entry.getKey());
				else {
					isTerm = true;
				}

			boolean isInitial = "true".equalsIgnoreCase(state.getAttributeValue(TimedAutomataFactory.STATE_INITIAL_TAG));

			int modifiers = ("true".equalsIgnoreCase(state.getAttributeValue(TimedAutomataFactory.STATE_URGENT_TAG)) ? ITimedAutomata.URGENT : 0)
					| (isInitial ? ITimedAutomata.INITIAL : 0)
					| (isTerm ? ITimedAutomata.TERMINATE : 0);
			
			State<C> st = newState(entry.getKey(), acts, modifiers);
			states.put(state, st);
			if(isInitial)
				auto.setInitial(st);
		}
	}

	protected State<C> newState(final String name, final ArrayList<Action<C>> actions, final int modifiers) {
		return new State<C>() {
			@Override
			public String getName() {
				return name;
			}

			@Override
			public List<Action<C>> getActions() {
				return actions;
			}

			@Override
			public int getModifier() {
				return modifiers;
			}

			@Override
			public void preAction(C context) {
				for (Action<C> action : getActions())
					action.preAction(context);
			}

			@Override
			public void eachAction(C context) {
				for (Action<C> action : getActions())
					action.eachAction(context);
			}

			@Override
			public void postAction(C context) {
				for (Action<C> action : getActions())
					action.postAction(context);
			}
		};
	}
	
	protected void resolveTransitions(Element root, Map<String, Element> stateMap, Map<Element, Element> transMap) {
		for(Element state: root.getChildren(STATE_TAG)){
			
			boolean hasTimeout = false;
			for(Element trans: state.getChildren(TIMEOUT_TAG)) {
				Element target = stateMap.get(trans.getAttributeValue(TRANSITION_TARGET_TAG));
				
				if(hasTimeout)
					throw new RuntimeException("State '"+target+"' has more than one timeout");
				if(target == null) 
					throw new RuntimeException("Timeout target does not exists in '" + root.getAttributeValue(AUTOMATA_NAME_TAG) + "/"+state+"': " + trans.getAttributeValue(TRANSITION_TARGET_TAG));
				transMap.put(trans, target);
				hasTimeout = true;
			}
			
			for(Element trans: state.getChildren(TRANSITION_TAG)) {
				Element target = stateMap.get(trans.getAttributeValue(TRANSITION_TARGET_TAG));
				if(hasTimeout) {
					String timeoutval = trans.getAttributeValue(TRANSITION_TIMEOUT_TAG);
					if (timeoutval != null && Integer.parseInt(timeoutval) == TimedAutomata.INFINITY)
						throw new RuntimeException("Cannot mix timeout and infinite guars in: "+ state);
				}
				if(target == null)
					throw new RuntimeException("Target does not exists in '" + root.getAttributeValue(AUTOMATA_NAME_TAG) + "': " + trans.getAttributeValue(TRANSITION_TARGET_TAG));
				transMap.put(trans, target);
			}
		}
	}

	protected Element resolveStates(Element root, Map<String, Element> autos, Map<String, Element> stateMap) throws JDOMException {
		Element initial = null;
		
		for(Element state: root.getChildren(STATE_TAG)){
			String name = state.getAttributeValue(STATE_NAME_TAG);
			if(stateMap.containsKey(name))
				throw new JDOMException("Node name is not unique: "+ name);
			stateMap.put(name, state);
			
			if("true".equalsIgnoreCase(state.getAttributeValue(TimedAutomataFactory.STATE_INITIAL_TAG))) {
				if(initial != null)
					throw new TimaException.InitialStateException("More than one initial state in " + root.getAttributeValue(AUTOMATA_NAME_TAG) + ": '"+initial+"', '"+state+"'");
				initial = state;
			}
		}
		if(initial == null)
			throw new TimaException.InitialStateException(root.getAttributeValue(AUTOMATA_NAME_TAG) + " automata has no initial state");
		return initial;
	}
	
	protected Action<C> getAction(final String type, final String attr) {
		if(type == null) return null;
		String name = type + ((attr == null ) ? "" : (":"+attr)); 
		if(_actionMap.containsKey(name))
			return _actionMap.get(name);
		Action<C> act = _factory.newAction(type, attr);
		if(act == null)
			throw new RuntimeException("Unable to create default action : " + name);
		_actionMap.put(name, act);
		return act;
	}
	
	public static final boolean hasModifier(int modifiers, int modifier) {
		return (modifiers & modifier) > 0;
	}
	
	protected Predicate<C> getPredicate(String type, String attr) {
		String name = type + ((attr == null ) ? "" : (":"+attr));
		if(_predicateMap.containsKey(name))
			return _predicateMap.get(name);
		Predicate<C> t = _factory.newPredicate(name, attr);
		_predicateMap.put(name, t);
		return t;
	}
	
	protected int getModifierFromNode(Element state) {
		String name = state.getAttributeValue(TimedAutomataFactory.STATE_NAME_TAG);
		return ("true".equalsIgnoreCase(state.getAttributeValue(TimedAutomataFactory.STATE_URGENT_TAG)) ? ITimedAutomata.URGENT : 0)
				| ("true".equalsIgnoreCase(state.getAttributeValue(TimedAutomataFactory.STATE_INITIAL_TAG)) ? ITimedAutomata.INITIAL : 0)
				| (("stop".equalsIgnoreCase(name) || "terminate".equalsIgnoreCase(name)) ? ITimedAutomata.TERMINATE : 0);
	}
}
