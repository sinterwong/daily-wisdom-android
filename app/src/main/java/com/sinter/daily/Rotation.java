package com.sinter.daily;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Pure Java scheduling decisions, independent of Android's background scheduler. */
final class Rotation {
    private Rotation() {}

    static boolean needsAdvance(int current, boolean held, String lastDay, String today) {
        return current < 0 || (!held && !today.equals(lastDay));
    }

    static int draw(List<Integer> queue, int count, int previous, Random random) {
        if (count < 1) throw new IllegalArgumentException("Empty quote library");
        if (queue.isEmpty()) {
            for (int i = 0; i < count; i++) queue.add(i);
            Collections.shuffle(queue, random);
            if (count > 1 && queue.get(0) == previous) Collections.swap(queue, 0, 1);
        }
        return queue.remove(0);
    }
}
