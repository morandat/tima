package fr.labri.timedautomata;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.JList;

import fr.labri.timedautomata.ITimedAutomata.ContextProvider;
import fr.labri.timedautomata.ITimedAutomata.Cursor;

public class Executor<C> implements ITimedAutomata.Executor<C> {
	List<Cursor<C>> _cursors = new LinkedList<>();
	ContextProvider<C> _context;

	TAViewer _viewer;
	
	public Executor(ContextProvider<C> context) {
		_context = context;
	}
	
	@Override
	public void start(ITimedAutomata<C> auto, String key) {
		_cursors.add(auto.start(_context, key));
		if(_viewer != null)
			_viewer.update();
	}

	@Override
	public void next() {
		for(Iterator<Cursor<C>> it = _cursors.iterator(); it.hasNext() ;) {
			Cursor<C> c = it.next();
			if(c.next(this)) {
				it.remove();
				_viewer.update();
			}
		}
	}

	@Override
	public Collection<Cursor<C>> getCursors() {
		return Collections.unmodifiableCollection(_cursors);
	}
	
	public JList<String> getViewer() {
		_viewer = new TAViewer();
		return new JList<>(_viewer);
	}
	
	@SuppressWarnings("serial")
	class TAViewer extends AbstractListModel<String> {
        public int getSize() { return _cursors.size(); }
        public String getElementAt(int i) { return _cursors.get(i).toString(); }
        public void update() { fireContentsChanged(this, 0, _cursors.size() - 1); }
    }
}
