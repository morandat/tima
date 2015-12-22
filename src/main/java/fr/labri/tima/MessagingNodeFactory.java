package fr.labri.tima;

import fr.labri.tima.ITimedAutomata.*;
import fr.labri.tima.MessagingNodeFactory.MessageExecutor;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSearchableLinkedDeque;

public class MessagingNodeFactory<C> implements ITimedAutomata.NodeFactory<MessageExecutor<C>.MessageContext> {
    final NodeFactory<C> _factory;

    MessagingNodeFactory(NodeFactory<C> factory) {
        _factory = factory;
    }

    @Override
    public Predicate<MessageExecutor<C>.MessageContext> newPredicate(String type, String attr) {
        if (type.startsWith("?"))
            return filterMessageFactory(type, attr);
        final Predicate<C> pred = _factory.newPredicate(type, attr);

        return new Predicate<MessageExecutor<C>.MessageContext>() {
            @Override
            public boolean isValid(MessageExecutor<C>.MessageContext context) {
                return pred.isValid(context.getContext());
            }

            @Override
            public String getType() {
                return pred.getType();
            }
        };
    }

    private Predicate<MessageExecutor<C>.MessageContext> filterMessageFactory(String type, String attr) {
        return new MessageFilter();
    }

    @Override
    public Action<MessageExecutor<C>.MessageContext> newAction(String type, String attr) {
        if (type.startsWith("!"))
            return sendMessageFactory(type, attr);
        final Action<C> action = _factory.newAction(type, attr);
        return new Action<MessageExecutor<C>.MessageContext>() {
            @Override
            public void preAction(MessageExecutor<C>.MessageContext context) {
                action.preAction(context.getContext());
            }

            @Override
            public void eachAction(MessageExecutor<C>.MessageContext context) {
                action.eachAction(context.getContext());
            }

            @Override
            public void postAction(MessageExecutor<C>.MessageContext context) {
                action.postAction(context.getContext());
            }

            @Override
            public String getType() {
                return action.getType();
            }
        };
    }

    private Action<MessageExecutor<C>.MessageContext> sendMessageFactory(String type, String attr) {
        return new SendMessage("someautomata", "hello");
    }

    class MessageFilter implements ITimedAutomata.Predicate<MessageExecutor<C>.MessageContext>{
        @Override
        public boolean isValid(MessageExecutor<C>.MessageContext context) {
            return context.select(new MessagePattern() {
                @Override
                public boolean match(Message other) {
                    return other._type == "hello";
                }
            });
        }

        @Override
        public String getType() {
            return null;
        }
    }

    class SendMessage implements ITimedAutomata.Action<MessageExecutor<C>.MessageContext> {
        private final String _message;
        final String _automaton;
        int address;

        public SendMessage(String to, String msg) {
            _automaton = to;
            _message = msg;
        }

        int resolveAddress(MessageExecutor<C>.MessageContext context) {
            if (address == -1)
                address = 1;
            return address;
        }
        @Override
        public void preAction(MessageExecutor<C>.MessageContext context) {
            context.sendMessage(resolveAddress(context), new Message(_message));
        }

        @Override
        public void eachAction(MessageExecutor<C>.MessageContext context) { }

        @Override
        public void postAction(MessageExecutor<C>.MessageContext context) { }

        @Override
        public String getType() {
            return null;
        }
    }

    static class MessageExecutor<C> implements Executor<MessageExecutor<C>.MessageContext> {
        private final ContextProvider<C> _context;
        private final Collection<ITimedAutomata<MessageContext>> _automatons;
        Cursor<MessageContext> _cursors[];
        Mailbox _mailboxes[];
        private MessageContextProvider[] _contexts;

        @SuppressWarnings("unchecked")
        MessageExecutor(ContextProvider<C> context, Collection<ITimedAutomata<C>> automatas) {
            _context = context;
            // FIXME this cast scares me ... but actually not I will one day explain why
            _automatons = (Collection<ITimedAutomata<MessageContext>>)(Collection<?>) automatas;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Executor<MessageExecutor<C>.MessageContext> start() {
            _mailboxes = new Mailbox[_automatons.size()];
            _cursors = new Cursor[_automatons.size()];
            _contexts = new MessageExecutor.MessageContextProvider[_automatons.size()];

            int i = 0;
            for (ITimedAutomata<MessageExecutor<C>.MessageContext> automata : _automatons) {
                _mailboxes[i] = new Mailbox();
                _contexts[i] = new MessageContextProvider(i);
                _cursors[i] = automata.start(null);//new MessageContextProvider<C>.MessageContext(_context, this)); // FIXME
                i ++;
            }
            return this;
        }

        @Override
        public boolean next() {
            int running = 0;
            for (int i = 0; i < _cursors.length; i++) {
                Cursor<MessageContext> cursor = _cursors[i];
                if (cursor != null && !cursor.next(_contexts[i]))
                    running ++;
            }
            return running > 0;
        }

        @Override
        public Collection<Cursor<MessageContext>> getCursors() {
            return Collections.unmodifiableCollection(Arrays.asList(_cursors)); // should trim to alive
        }

        class MessageContextProvider implements ContextProvider<MessageExecutor<C>.MessageContext> {
            final int _automataIndex;

            MessageContextProvider(int index) {
                _automataIndex = index;
            }

            @Override
            public MessageExecutor<C>.MessageContext getContext() {
                return _contexts[_automataIndex].getContext();
            }
        }

        class MessageContext {
            final int _automataIndex;
            private Message _currentMessage;

            MessageContext(int index) {
                _automataIndex = index;
            }

            C getContext() {
                return _context.getContext();
            }

            public void sendMessage(int address, Message msg) {
                mailbox().receive(msg);
            }

            public Message currentMessage() {
                return _currentMessage;
            }

            public boolean select(MessagePattern pattern) {
                Message m = mailbox().extractFrom(pattern);
                if (m == null)
                    return false;
                _currentMessage = m;
                return true;
            }

            final private Mailbox mailbox() {
                return _mailboxes[_automataIndex];
            }
        }
    }

    static class Message {
        final String _type;
        Message(String type) {
            _type = type.intern();
        }
    }

    abstract static class MessagePattern {
        abstract public boolean match(Message other);

        @Override
        final public boolean equals(Object obj) {
            Message other = (Message) obj;
            if (other == null)
                return false;
            return match(other);
        }
    }

    static class Mailbox {
        ConcurrentSearchableLinkedDeque<Message> queue = new ConcurrentSearchableLinkedDeque<>();

        public void receive(Message msg) {
            queue.addLast(msg);
        }

        public Message extractFrom(MessagePattern pattern) {
            return queue.removeFirst(pattern);
        }
    }
}