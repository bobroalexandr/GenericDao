package alex.bobro.genericdao.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public abstract class CollectionUtils {

	public static <T> int findItemIndex(@NonNull List<T> items, T newItem, @NonNull Comparator<T> comparator) {
		final int currentSize = items.size();

		int lo = 0, hi = currentSize - 1;
		while(lo <= hi) {
			int mid = lo + (hi - lo) / 2;
			int cmp = comparator.compare(newItem, items.get(mid));

			if 		(cmp < 0) 	hi = mid - 1;
			else if (cmp > 0)	lo = mid + 1;
			else {
				lo = mid + 1;
				break;
			}
		}
		return lo;
	}

	public static <T> boolean insertItem(@NonNull List<T> items, T newItem, @NonNull Comparator<T> comparator) {
		final int currentSize = items.size();

		items.add(findItemIndex(items, newItem, comparator), newItem);
		return items.size() > currentSize;
	}

	public static <T> boolean insertItems(@NonNull List<T> items, @Nullable Collection<T> newItems, @NonNull Comparator<T> comparator) {
		if (newItems == null || newItems.isEmpty())
			return false;

		final int currentSize = items.size();

		for (T newItem : newItems) {
			items.add(findItemIndex(items, newItem, comparator), newItem);
		}
		return items.size() > currentSize;
	}



	public interface Selection<T> {
		public boolean select(T item);
	}

	public static <T> boolean contains(Collection<T> collection, @NonNull Selection<T> where) {
		for (T item : collection) {
			if (where.select(item)) {
				return true;
			}
		}
		return false;
	}

	public static <T> int eraseAll(Collection<T> collection, @NonNull Selection<T> where) {
		int countErased = 0;
		for (Iterator<T> collectionIt = collection.iterator(); collectionIt.hasNext();) {
			T item = collectionIt.next();

			if (where.select(item)) {
				collectionIt.remove();
				++countErased;
			}
		}
		return countErased;
	}

    public static <T> boolean contains(@NonNull List<T> items, T item, @NonNull Comparator<T> comparator) {
        for (T i : items) {
            if(comparator.compare(i, item) == 0) {
                return true;
            }
        }
        return false;
    }
}
