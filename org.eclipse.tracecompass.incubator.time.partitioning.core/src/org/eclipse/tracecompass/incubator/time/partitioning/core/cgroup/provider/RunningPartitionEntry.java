package org.eclipse.tracecompass.incubator.time.partitioning.core.cgroup.provider;

import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

public class RunningPartitionEntry extends TimeGraphEntry {

    private final CgroupDataProvider fProvider;

    public RunningPartitionEntry(String name, CgroupDataProvider provider, long startTime, long endTime) {
        super(name, startTime, endTime);
        fProvider = provider;
    }

    public ITimeGraphDataProvider<? extends TimeGraphEntryModel> getProvider() {
        return fProvider;
    }
}
