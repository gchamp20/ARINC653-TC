package org.eclipse.tracecompass.incubator.internal.time.partitioning.xen.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.internal.tmf.core.model.timegraph.AbstractTimeGraphDataProvider;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.tree.TmfTreeModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

public class PartitionStatusDataProvider  extends AbstractTimeGraphDataProvider<@NonNull PartitionAnalysis, @NonNull TimeGraphEntryModel> {

    /**
     * Extension point ID.
     */
    public static final @NonNull String ID = "org.eclipse.tracecompass.incubator.internal.time.partitioning.PartitionStatusDataProvider"; //$NON-NLS-1$

    public PartitionStatusDataProvider(@NonNull ITmfTrace trace, PartitionAnalysis module) {
        super(trace, module);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Deprecated
    @Override
    public TmfModelResponse<List<ITimeGraphArrow>> fetchArrows(TimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Deprecated
    @Override
    public TmfModelResponse<Map<String, String>> fetchTooltip(SelectionTimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    protected @Nullable TimeGraphModel getRowModel(ITmfStateSystem ss, Map<String, Object> parameters, @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {
        TreeMultimap<Integer, ITmfStateInterval> intervals = TreeMultimap.create(Comparator.naturalOrder(),
                Comparator.comparing(ITmfStateInterval::getStartTime));
        SelectionTimeQueryFilter filter = FetchParametersUtils.createSelectionTimeQuery(parameters);
        Map<@NonNull Long, @NonNull Integer> entries = getSelectedEntries(filter);
        Collection<Long> times = getTimes(filter, ss.getStartTime(), ss.getCurrentEndTime());
        /* Do the actual query */
        for (ITmfStateInterval interval : ss.query2D(entries.values(), times)) {
            if (monitor != null && monitor.isCanceled()) {
                return new TimeGraphModel(Collections.emptyList());
            }
            intervals.put(interval.getAttribute(), interval);
        }
        Map<@NonNull Integer, @NonNull Predicate<@NonNull Multimap<@NonNull String, @NonNull Object>>> predicates = new HashMap<>();
        Multimap<@NonNull Integer, @NonNull String> regexesMap = DataProviderParameterUtils.extractRegexFilter(parameters);
        if (regexesMap != null) {
            predicates.putAll(computeRegexPredicate(regexesMap));
        }
        List<@NonNull ITimeGraphRowModel> rows = new ArrayList<>();
        for (Map.Entry<@NonNull Long, @NonNull Integer> entry : entries.entrySet()) {
            if (monitor != null && monitor.isCanceled()) {
                return new TimeGraphModel(Collections.emptyList());
            }

            List<ITimeGraphState> eventList = new ArrayList<>();
            for (ITmfStateInterval interval : intervals.get(entry.getValue())) {
                long startTime = interval.getStartTime();
                long duration = interval.getEndTime() - startTime + 1;
                int state = interval.getValueInt();
                TimeGraphState value = new TimeGraphState(startTime, duration, state, state == 255 ? "" : String.valueOf(state));
                applyFilterAndAddState(eventList, value, entry.getKey(), predicates, monitor);
            }
            rows.add(new TimeGraphRowModel(entry.getKey(), eventList));

        }
        return new TimeGraphModel(rows);
    }

    @Override
    protected boolean isCacheable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected TmfTreeModel<TimeGraphEntryModel> getTree(ITmfStateSystem ss, Map<String, Object> fetchParameters, @Nullable IProgressMonitor monitor) throws StateSystemDisposedException {

        List<Integer> quarks = new ArrayList<>(ss.getQuarks("Partition", "*"));
        @NonNull List<@NonNull TimeGraphEntryModel> list = new ArrayList<>();
        for (Integer pQuark : quarks) {
            String name = ss.getAttributeName(pQuark);
            list.add(new TimeGraphEntryModel(getId(pQuark), -1, name, ss.getStartTime(), ss.getCurrentEndTime()));
        }

        return new TmfTreeModel(Collections.EMPTY_LIST, list);
    }
}
