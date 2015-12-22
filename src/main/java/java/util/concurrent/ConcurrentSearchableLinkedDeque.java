package java.util.concurrent;

/**
 * Created by morandat on 22/12/2015.
 */
public class ConcurrentSearchableLinkedDeque<E> extends ConcurrentLinkedDeque<E> {
    public E removeFirst(Object o) {
        checkNotNull(o);
        for (Node<E> p = first(); p != null; p = succ(p)) {
            E item = p.item;
            if (item != null && o.equals(item) && p.casItem(item, null)) {
                unlink(p);
                return item;
            }
        }
        return null;
    }

    /**
     * Throws NullPointerException if argument is null.
     *
     * @param v the element
     */
    private static void checkNotNull(Object v) {
        if (v == null)
            throw new NullPointerException();
    }

}
