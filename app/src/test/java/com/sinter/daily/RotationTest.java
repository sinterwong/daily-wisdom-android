package com.sinter.daily;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class RotationTest {
    @Test public void everyCycleContainsEveryQuoteWithoutAdjacentRepeat() {
        List<Integer> queue = new ArrayList<>();
        Random random = new Random(20260912L);
        int previous = -1;
        for (int cycle = 0; cycle < 4; cycle++) {
            Set<Integer> seen = new HashSet<>();
            for (int i = 0; i < 500; i++) {
                int current = Rotation.draw(queue, 500, previous, random);
                assertTrue(seen.add(current));
                assertNotEquals(previous, current);
                previous = current;
            }
            assertEquals(500, seen.size());
            assertTrue(queue.isEmpty());
        }
    }
    @Test public void sameDayStaysPutAndNewDayAdvances() {
        assertFalse(Rotation.needsAdvance(7,false,"2026-09-12","2026-09-12"));
        assertTrue(Rotation.needsAdvance(7,false,"2026-09-12","2026-09-13"));
        assertTrue(Rotation.needsAdvance(7,false,"2026-09-12","2026-09-20"));
    }
    @Test public void holdSurvivesDateChangesButFirstLaunchAlwaysDraws() {
        assertFalse(Rotation.needsAdvance(7,true,"2026-09-12","2026-09-13"));
        assertTrue(Rotation.needsAdvance(-1,true,"","2026-09-12"));
    }
    @Test public void reloadedQueueContinuesWithoutRepeating() {
        List<Integer> queue = new ArrayList<>();Random random=new Random(3);
        int first=Rotation.draw(queue,500,-1,random);
        List<Integer> restored=new ArrayList<>(queue);
        assertEquals(queue.get(0).intValue(),Rotation.draw(restored,500,first,random));
        assertFalse(restored.contains(first));
    }
    @Test public void oneQuoteLibraryDoesNotFail() {
        List<Integer> queue=new ArrayList<>();
        assertEquals(0,Rotation.draw(queue,1,0,new Random(1)));
    }
    @Test(expected=IllegalArgumentException.class) public void emptyLibraryIsRejected() {
        Rotation.draw(new ArrayList<>(),0,-1,new Random());
    }
}
