package com.fr3ts0n.ecu.gui.androbd.widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tracks historical time-stamped measurement data points for live trend charts.
 */
public class ObdHistoryTracker
{
    public static class HistoryPoint
    {
        public final long timestampMs;
        public final float value;

        public HistoryPoint(long timestampMs, float value)
        {
            this.timestampMs = timestampMs;
            this.value = value;
        }
    }

    private static final Map<String, List<HistoryPoint>> historyMap = Collections.synchronizedMap(new HashMap<>());
    private static final long MAX_HISTORY_AGE_MS = 600000L; // 10 minutes max buffer

    public static void addPoint(String mnemonic, float value, long timestampMs)
    {
        if (mnemonic == null || mnemonic.isEmpty()) return;

        List<HistoryPoint> points = historyMap.get(mnemonic);
        if (points == null)
        {
            points = Collections.synchronizedList(new ArrayList<>());
            historyMap.put(mnemonic, points);
        }

        synchronized (points)
        {
            // Avoid inserting identical timestamp
            if (!points.isEmpty() && points.get(points.size() - 1).timestampMs == timestampMs)
            {
                return;
            }

            points.add(new HistoryPoint(timestampMs, value));

            // Prune points older than 10 minutes
            long cutoff = timestampMs - MAX_HISTORY_AGE_MS;
            while (!points.isEmpty() && points.get(0).timestampMs < cutoff)
            {
                points.remove(0);
            }
        }
    }

    public static List<HistoryPoint> getPoints(String mnemonic, long timeWindowMs)
    {
        List<HistoryPoint> result = new ArrayList<>();
        if (mnemonic == null || mnemonic.isEmpty()) return result;

        List<HistoryPoint> points = historyMap.get(mnemonic);
        if (points == null) return result;

        long cutoff = System.currentTimeMillis() - timeWindowMs;

        synchronized (points)
        {
            for (HistoryPoint p : points)
            {
                if (p.timestampMs >= cutoff)
                {
                    result.add(p);
                }
            }
        }

        return result;
    }
}
