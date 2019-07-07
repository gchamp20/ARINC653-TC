/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.time.partitioning.core.cgroup.provider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.time.partitioning.core.cgroup.ContainerAnalysis;
import org.eclipse.tracecompass.incubator.time.partitioning.core.cgroup.ContainerAttributes;
import org.eclipse.tracecompass.internal.tmf.core.model.AbstractTmfTraceDataProvider;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.FetchParametersUtils;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.exceptions.AttributeNotFoundException;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.statesystem.core.exceptions.TimeRangeException;
import org.eclipse.tracecompass.statesystem.core.interval.ITmfStateInterval;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderParameterUtils;
import org.eclipse.tracecompass.tmf.core.model.CommonStatusMessage;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphArrow;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphState;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.common.collect.Multimap;

/**
 * The cgroup data provider. Used for cgroup views.
 *
 * @author Guillaume Champagne
 */
@SuppressWarnings("restriction")
public class CgroupDataProvider extends AbstractTmfTraceDataProvider implements ITimeGraphDataProvider<@NonNull CgroupEntryModel>  {

    // ------------------------------------------------------------------------
    // Static fields
    // ------------------------------------------------------------------------

    /**
     * Extension point ID.
     */
    public static final @NonNull String ID = "org.eclipse.tracecompass.incubator.time.partitioning.core.cgroup.provider.CgroupDataProviderFactory"; //$NON-NLS-1$


    // ------------------------------------------------------------------------
    // Private fields
    // ------------------------------------------------------------------------

    /**
     * Atomic Long so that every {@link CgroupEntryModel} has a unique ID.
     */
    private static final AtomicLong fAtomicLong = new AtomicLong();

    /**
     * WILDCARD character in the state system.
     */
    private static final String WILDCARD = "*"; //$NON-NLS-1$

    /**
     * Current subsystem to query.
     */
    private String fSubSystem;

    /**
     * Handle to the analysis module building the Cgroup state system.
     */
    private ContainerAnalysis fModule;

    /* Quark to entry ID map */
    private Map<Integer, Long> fRunningPartPerCpuEntries;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * @param trace
     *          The trace on which the data provider applies.
     * @param module
     *          The {@link ContainerAnalysis} to access the underlying
     *          @{link ITmfStateSystem}
     */
    public CgroupDataProvider(ITmfTrace trace, ContainerAnalysis module) {
        super(trace);

        fSubSystem = "cpu"; //$NON-NLS-1$
        fModule = module;
        fRunningPartPerCpuEntries = new HashMap<>();
    }

    // ------------------------------------------------------------------------
    // ITmfTreeDataProvider
    // ------------------------------------------------------------------------

    @Deprecated
    @Override
    /**
     * DEV NOTES:
     * - Threads/processes can move cgroups during execution, we should handle this.
     */
    public TmfModelResponse<List<CgroupEntryModel>> fetchTree(TimeQueryFilter filter, @Nullable IProgressMonitor monitor) {

        // TODO: This very temporary...
        fModule.waitForCompletion();

        ITmfStateSystem ss = fModule.getStateSystem();
        if (ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }

        Integer subSysQuark = ITmfStateSystem.INVALID_ATTRIBUTE;
        try {
            subSysQuark = ss.getQuarkAbsolute(ContainerAttributes.CGROUPS_SUBSYS, fSubSystem);
        } catch (AttributeNotFoundException e) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }

        Integer rootCgroupQuark = ITmfStateSystem.INVALID_ATTRIBUTE;
        try {
            String subSysMount = ss.querySingleState(ss.getCurrentEndTime(), subSysQuark).getValueString();
            rootCgroupQuark = ss.getQuarkAbsolute(ContainerAttributes.CGROUPS_HIERARCHIES, subSysMount);
        } catch (Exception e) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.FAILED, CommonStatusMessage.ANALYSIS_INITIALIZATION_FAILED);
        }

        List<CgroupEntryModel> list = new ArrayList<>();
        createEntriesForCgroup(ss, rootCgroupQuark, null, -1, list);

        return new TmfModelResponse<>(list, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Override
    public String getId() {
        return ID;
    }

    // ------------------------------------------------------------------------
    // Public methods
    // ------------------------------------------------------------------------
    /**
     * Change the subsystem currently queried.
     *
     * @param subSystemName
     *          Name of the new subsystem to use.
     */
    public void setSubSystem(String subSystemName) {
        fSubSystem = subSystemName;
    }

    // ------------------------------------------------------------------------
    // Private methods
    // ------------------------------------------------------------------------

    /**
     *
     * @param ss
     *          The state system used by this data provider.
     * @param cgroupQuark
     *          The quarks of the cgroup to create entries for in the hierarchy.
     * @param parentEntry
     *          The parent entry to build the hierarchy from.
     * @param list
     *          The list in which the results are accumulated.
     */
    private void createEntriesForCgroup(ITmfStateSystem ss, Integer cgroupQuark, CgroupEntryModel parentEntry, long parentID, List<CgroupEntryModel> list) {
        if (cgroupQuark == ITmfStateSystem.INVALID_ATTRIBUTE) {
            return;
        }

        String name = ss.getAttributeName(cgroupQuark);

        // Create Entry Model for this group
        long cgroupEntryID = fAtomicLong.getAndIncrement();

        CgroupEntryModel cgroupEntry;
        if (parentEntry == null) {
            // fRunningPartPerCpuEntries.add(0xDEADBEEFL);


            cgroupEntry = new CgroupEntryModel(cgroupEntryID, parentID, name,
                    CgroupEntryModel.CGROUP_PID, parentEntry, 0 ,ss.getCurrentEndTime());

            list.add(cgroupEntry);

            CgroupEntryModel other = new RunningCgroupEntryModel(0xDEADBEEFL, parentID, "RUNNING PARTITION",
                    CgroupEntryModel.CGROUP_PID, parentEntry, 0 ,ss.getCurrentEndTime());

            list.add(other );


            /* Create entries for CurrentPartition/CPUX */
            int i = 0;
            List<Integer> quarks = ss.getQuarks("CurrentPartition", WILDCARD);
            for (Integer q : quarks) {
                String cpuName = ss.getAttributeName(q);
                fRunningPartPerCpuEntries.put(q ,0xDEADBEEFL + i);
                list.add(new RunningCgroupEntryModel(0xDEADBEEFL + i, 0xDEADBEEFL, cpuName,
                        CgroupEntryModel.CGROUP_PID, parentEntry, 0 ,ss.getCurrentEndTime()));
                i++;
            }

        } else {
            cgroupEntry = new CgroupEntryModel(cgroupEntryID, parentID, name,
                    CgroupEntryModel.CGROUP_PID, parentEntry, 0, ss.getCurrentEndTime());
            list.add(cgroupEntry);
        }

        if (parentEntry != null) {
            parentEntry.addCgroupChild(cgroupEntry);
        }

        if (parentEntry != null) {
            // Add all the process entries for this cgroup
            for (Integer quark : ss.getQuarks(cgroupQuark, ContainerAttributes.CGROUPS_HIERARCHIES_PIDS, WILDCARD)) {
                name = ss.getAttributeName(quark);
                int pid = 0;

                try {
                    pid = Integer.parseInt(name);
                } catch (NumberFormatException e) {
                    continue;
                }

                CgroupEntryModel c = new CgroupEntryModel(fAtomicLong.getAndIncrement(), cgroupEntryID, name, pid, cgroupEntry, 0, ss.getCurrentEndTime());
                list.add(c);
                cgroupEntry.addProcessChild(c);
            }
        }

        // Visit the children cgroups
        for (Integer quark : ss.getQuarks(cgroupQuark, ContainerAttributes.CGROUPS_HIERARCHIES_CHILDREN, WILDCARD)) {
            createEntriesForCgroup(ss, quark, cgroupEntry, cgroupEntryID, list);
        }
    }

    @SuppressWarnings("null")
    @Override
    public @NonNull TmfModelResponse<@NonNull TimeGraphModel> fetchRowModel(@NonNull Map<@NonNull String, @NonNull Object> fetchParameters, @Nullable IProgressMonitor monitor) {
        ITmfStateSystem ss = fModule.getStateSystem();

        if (ss == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.STATE_SYSTEM_FAILED);
        }

        List<Integer> partCpuQuark;
        partCpuQuark = ss.getQuarks("CurrentPartition", WILDCARD); //$NON-NLS-1$

        List<Integer> quarks = new ArrayList<>();
        quarks.addAll(partCpuQuark);
        Iterable<ITmfStateInterval> states;
        SelectionTimeQueryFilter filter = FetchParametersUtils.createSelectionTimeQuery(fetchParameters);

        if (filter == null) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.STATE_SYSTEM_FAILED);
        }

        try {
            states = ss.query2D(quarks, filter.getStart(), filter.getEnd());
        } catch (IndexOutOfBoundsException | TimeRangeException | StateSystemDisposedException e) {
            return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.STATE_SYSTEM_FAILED);
        }

        Map<@NonNull Integer, @NonNull Predicate<@NonNull Multimap<@NonNull String, @NonNull Object>>> predicates = new HashMap<>();
        Multimap<@NonNull Integer, @NonNull String> regexesMap = DataProviderParameterUtils.extractRegexFilter(fetchParameters);
        if (regexesMap != null) {
            predicates.putAll(computeRegexPredicate(regexesMap));
        }

        List<ITimeGraphRowModel> rows = new ArrayList<>();
        List<ITimeGraphState> eventList = new ArrayList<>();
        for (Integer key : fRunningPartPerCpuEntries.keySet()) {
            eventList.clear();
            states.forEach(i -> {
                int attributeQuark = i.getAttribute();
                Long diff = null;
                if (attributeQuark == key) {
                    try {
                        int diffQuark = ss.getQuarkRelative(attributeQuark, "diff");
                        ITmfStateInterval val = ss.querySingleState(i.getStartTime(), diffQuark);
                        diff = val.getValueLong();
                    } catch (Exception e) {

                    }
                    Long parentKey = fRunningPartPerCpuEntries.get(attributeQuark);
                    String name = i.getValueString();
                    int val = (name != null) ? Integer.parseInt(name.substring(1)) : 0x0;
                    // int val = (name == null || name.equals("")) ? 0x65500 : 0x65501; //$NON-NLS-1$
                    String fullName = (diff != null && name != null) ? getPrettyName(name ,diff) : name;
                    applyFilterAndAddState(eventList, new TimeGraphState(i.getStartTime(), i.getEndTime() - i.getStartTime(), val, fullName), parentKey, predicates, monitor);
                }
            });

            Long entryId = fRunningPartPerCpuEntries.get(key);
            if (entryId != null) {
                rows.add(new TimeGraphRowModel(entryId, new ArrayList<>(eventList)));
            }
        }

        return new TmfModelResponse<>(new TimeGraphModel(rows), ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    private static String getPrettyName(String name, Long diff) {
        Double timeInms = diff / 1000000.0;
        String symbol = timeInms > 0.0 ? "+" : "-";
        timeInms = Math.abs(timeInms);
        return String.format("%s (%s%7.4f ms)", name, symbol, timeInms);
    }

    @Deprecated
    @Override
    public TmfModelResponse<@NonNull List<@NonNull ITimeGraphArrow>> fetchArrows(TimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        // TODO Auto-generated method stub
        return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Deprecated
    @Override
    public TmfModelResponse<@NonNull Map<@NonNull String, @NonNull String>> fetchTooltip(SelectionTimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        // TODO Auto-generated method stub
        return new TmfModelResponse<>(null, ITmfResponse.Status.COMPLETED, CommonStatusMessage.COMPLETED);
    }

    @Deprecated
    @Override
    public TmfModelResponse<@NonNull List<@NonNull ITimeGraphRowModel>> fetchRowModel(SelectionTimeQueryFilter filter, @Nullable IProgressMonitor monitor) {
        @NonNull Map<@NonNull String, @NonNull Object> parameters = FetchParametersUtils.selectionTimeQueryToMap(filter);
        TmfModelResponse<@NonNull TimeGraphModel> response = fetchRowModel(parameters, monitor);
        TimeGraphModel model = response.getModel();
        List<@NonNull ITimeGraphRowModel> rows = null;
        if (model != null) {
            rows = model.getRows();
        }
        return new TmfModelResponse<>(rows, response.getStatus(), response.getStatusMessage());
    }
}
