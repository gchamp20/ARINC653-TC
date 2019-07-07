/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.time.partitioning.core.cgroup;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.kernel.handlers.KernelEventHandlerUtils;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystem;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

/**
 * State provider for the Container analysis.
 *
 * @author Loïc Gelle
 */
public class ContainerStateProvider extends AbstractTmfStateProvider {
    // ------------------------------------------------------------------------
    // Static fields
    // ------------------------------------------------------------------------

    /**
     * Version number of this state provider. Please bump this if you modify the
     * contents of the generated state history in some way.
     */
    private static final int VERSION = 0;

    /* Partition ID to start time map */
    private Map<String, Long> fPartitionIntervals;

    /* Per partition schedule in _NANOSECONDS_ */
    private Map<String, List<Long>> fSchedule;

    private Map<String, Long> fActivePartitions;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * Instantiate a new state provider plugin.
     *
     * @param experiment
     *            The experiment that will be analyzed.
     */
    public ContainerStateProvider(TmfExperiment experiment) {
        super(experiment, "Container State Provider"); //$NON-NLS-1$
        fSchedule = new HashMap<>();

        fSchedule.put("p1", Arrays.asList(100000000L));
        fSchedule.put("p11", Arrays.asList(100000000L));

        fPartitionIntervals = new HashMap<>();
        fActivePartitions = new HashMap<>();
    }

    // ------------------------------------------------------------------------
    // IStateChangeInput
    // ------------------------------------------------------------------------

    @Override
    public TmfExperiment getTrace() {
        ITmfTrace trace = super.getTrace();
        if (trace instanceof TmfExperiment) {
            return (TmfExperiment) trace;
        }
        throw new IllegalStateException("ContainerStateProvider: The associated trace should be an experiment"); //$NON-NLS-1$
    }

    @Override
    public int getVersion() {
        return VERSION;
    }

    @Override
    public ContainerStateProvider getNewInstance() {
        return new ContainerStateProvider(getTrace());
    }

    /**
     * Get the quark for the root cgroup corresponding to a given cgroup path.
     *
     * @param ss
     *            The state system.
     * @param path
     *            The path to the cgroup in the cgroup virtual filesystem.
     * @return The quark for the root cgroup, or null if root is not found.
     */
    private static @Nullable Integer getCgroupRootQuark(ITmfStateSystemBuilder ss, @Nullable String path) {
        if (path == null) {
            return null;
        }

        int hierarchiesQuark = ss.getQuarkAbsoluteAndAdd(ContainerAttributes.CGROUPS_HIERARCHIES);
        File cgrpRoot = new File(path);

        int q;
        while (cgrpRoot != null) {
            q = ss.optQuarkRelative(hierarchiesQuark, cgrpRoot.getPath());
            if (q == ITmfStateSystem.INVALID_ATTRIBUTE) {
                cgrpRoot = cgrpRoot.getParentFile();
            } else {
                return q;
            }
        }
        return null;
    }

    /**
     * Get the quark for the cgroup corresponding to a given cgroup path.
     *
     * @param ss
     *            The state system.
     * @param cgrpPath
     *            The path to the cgroup in the cgroup virtual filesystem.
     * @return The quark for the cgroup, or null if not found.
     */
    private static @Nullable Integer getCgroupQuark(ITmfStateSystemBuilder ss, @Nullable String cgrpPath) {
        Integer rootQuark = getCgroupRootQuark(ss, cgrpPath);

        if (rootQuark == null) {
            return null;
        }

        return getCgroupQuarkFromRoot(ss, rootQuark, cgrpPath);
    }

    /**
     * Get the quark for the cgroup given the cgroup path and the root cgroup quark.
     *
     * @param ss
     *            The state system.
     * @param rootQuark
     *            The quark corresponding to the root cgroup.
     * @param cgrpPath
     *            The path to the cgroup in the cgroup virtual filesystem.
     * @return The quark for the cgroup, or null if not found.
     */
    private static @Nullable Integer getCgroupQuarkFromRoot(ITmfStateSystemBuilder ss, Integer rootQuark, @Nullable String cgrpPath) {
        if (cgrpPath == null) {
            return null;
        }

        String rootPath = ss.getAttributeName(rootQuark);
        String[] pathArray = cgrpPath.substring(rootPath.length()).split("/"); //$NON-NLS-1$
        Integer currQuark = rootQuark;
        for (int i = 1; i < pathArray.length; i++) {
            currQuark = ss.getQuarkRelativeAndAdd(currQuark, ContainerAttributes.CGROUPS_HIERARCHIES_CHILDREN, pathArray[i]);
        }
        return currQuark;
    }

    /**
     * Extract the cgroup path from the trace event.
     *
     * @param eventContent
     *            The content of the event.
     * @return The cgroup path in the cgroup virtual filesystem, or null
     *            if not found in the event.
     */
    private static @Nullable String getCgroupPath(ITmfEventField eventContent) {
        return eventContent.getField(ContainerEventNames.CGRP_PATH_FIELD).getFormattedValue();
    }

    /**
     * Get the quark for the a given cgroup file, given the quark for the
     * cgroup and the trace event. The quark is created if it does not exist.
     *
     * @param ss
     *            The state system.
     * @param eventContent
     *            The content of the event. Contains the cgroup filename.
     * @param cgroupQuark
     *            The quark corresponding to the cgroup.
     * @return The quark for the cgroup file.
     */
    private static int getCgroupFileQuark(ITmfStateSystemBuilder ss, ITmfEventField eventContent, int cgroupQuark) {
        String filename = eventContent.getField(ContainerEventNames.CGRP_FILENAME_FIELD).getFormattedValue();
        return ss.getQuarkRelativeAndAdd(cgroupQuark, ContainerAttributes.CGROUPS_HIERARCHIES_FILES,
                filename);
    }

    /**
     * Empty the values associated to a given cgroup file.
     *
     * @param ss
     *            The state system.
     * @param event
     *            The trace event.
     * @param fileQuark
     *            The quark corresponding to the cgroup file.
     */
    private static void cgroupFileCleanValues(ITmfStateSystemBuilder ss, ITmfEvent event, int fileQuark) {
        ss.modifyAttribute(event.getTimestamp().toNanos(), ContainerAttributes.CGROUPS_FILE_EMPTY_VALUE, fileQuark);
        for (Integer q : ss.getSubAttributes(fileQuark, false)) {
            // Empty only the quarks having no child quark
            // Quarks having children are file keys
            boolean hasChildren = false;
            for (Integer qChild : ss.getSubAttributes(q, false)) {
                hasChildren = true;
                ss.modifyAttribute(event.getTimestamp().toNanos(), ContainerAttributes.CGROUPS_FILE_EMPTY_VALUE, qChild);
            }
            if (!hasChildren) {
                ss.modifyAttribute(event.getTimestamp().toNanos(), ContainerAttributes.CGROUPS_FILE_EMPTY_VALUE, q);
            }
        }
    }

    /**
     * Event handler for cgroup subsystem root statedump events.
     * - Associates cgroup root path to subsystem name
     *   in ContainerAttributes.CGROUPS_SUBSYS.
     * - Creates the root cgroup in
     *   ContainerAttributes.CGROUPS_HIERARCHIES.
     *
     * @param ss
     *            The state system.
     * @param event
     *            The trace event.
     */
    private static void newSubsysRootEventHandler(ITmfStateSystemBuilder ss, ITmfEvent event) {
        ITmfEventField eventContent = event.getContent();
        String subsysName = eventContent.getField(ContainerEventNames.CGRP_SUBSYS_ROOT_SUBSYS_FIELD).getFormattedValue();
        String subsysPath = eventContent.getField(ContainerEventNames.CGRP_SUBSYS_ROOT_ROOT_FIELD).getFormattedValue();

        // Associate root path to subsys name
        int quark = ss.getQuarkAbsoluteAndAdd(ContainerAttributes.CGROUPS_SUBSYS,
                    subsysName);
        ss.modifyAttribute(event.getTimestamp().toNanos(), subsysPath, quark);

        // Create empty hierarchy root
        int rootQuark = ss.getQuarkAbsoluteAndAdd(ContainerAttributes.CGROUPS_HIERARCHIES, subsysPath);
        int subsysQuark = ss.getQuarkRelativeAndAdd(rootQuark, ContainerAttributes.CGROUPS_HIERARCHIES_SUBSYS, subsysName);
        ss.modifyAttribute(event.getTimestamp().toNanos(), ContainerAttributes.CGROUPS_ACTIVE_VALUE, subsysQuark);
    }

    /**
     * Event handler for cgroup PID list dump events.
     * - Removes all PIDs from all the cgroups they
     *   were attached to in the concerned subsystems.
     * - Updates the PID list for the cgroup in the
     *   state system.
     *
     * @param ss
     *            The state system.
     * @param event
     *            The trace event.
     */
    private static void attachedPidsEventHandler(ITmfStateSystemBuilder ss, ITmfEvent event) {
        ITmfEventField eventContent = event.getContent();
        if (eventContent == null) {
            return;
        }
        String cgroupPath = getCgroupPath(eventContent);
        long[] pids = (long[]) eventContent.getField(ContainerEventNames.CGRP_ATTACHED_PIDS_PIDS_FIELD).getValue();
        Integer cgroupRootQuark = getCgroupRootQuark(ss, cgroupPath);
        if  (cgroupRootQuark == null) {
            return;
        }
        Integer cgroupQuark = getCgroupQuarkFromRoot(ss, cgroupRootQuark, cgroupPath);
        if  (cgroupQuark == null) {
            return;
        }

        // Update PIDs' cgroup list
        int cgroupRootSubsysQuark = ss.getQuarkRelativeAndAdd(cgroupRootQuark, ContainerAttributes.CGROUPS_HIERARCHIES_SUBSYS);
        for (Integer q : ss.getSubAttributes(cgroupRootSubsysQuark, false)) {
            String subsysName = ss.getAttributeName(q);
            String state;
            state = (String) ss.queryOngoing(q);
            if (state == ContainerAttributes.CGROUPS_ACTIVE_VALUE) {
                for (int i = 0; i < pids.length; i++) {
                    int pidSubsysQuark = ss.getQuarkAbsoluteAndAdd(ContainerAttributes.CGROUPS_PIDS,
                               String.valueOf(pids[i]), subsysName);
                    String oldCgroupPath = (String) ss.queryOngoing(pidSubsysQuark);
                    Integer oldCgroupQuark = getCgroupQuark(ss, oldCgroupPath);
                    if (oldCgroupQuark != null) {
                        int oldPidQuark = ss.getQuarkRelativeAndAdd(oldCgroupQuark,
                                ContainerAttributes.CGROUPS_HIERARCHIES_PIDS, String.valueOf(pids[i]));
                        ss.removeAttribute(event.getTimestamp().toNanos(), oldPidQuark);
                    }
                    ss.modifyAttribute(event.getTimestamp().toNanos(), cgroupPath, pidSubsysQuark);
                }
            }
        }

        // Update cgroup PIDs
        int cgroupPidsQuark = ss.getQuarkRelativeAndAdd(cgroupQuark, ContainerAttributes.CGROUPS_HIERARCHIES_PIDS);
        for (Integer q : ss.getSubAttributes(cgroupPidsQuark, false)) {
            ss.removeAttribute(event.getTimestamp().toNanos(), q);
        }
        for (int i = 0; i < pids.length; i++) {
            int q = ss.getQuarkRelativeAndAdd(cgroupPidsQuark, String.valueOf(pids[i]));
            ss.modifyAttribute(event.getTimestamp().toNanos(), ContainerAttributes.CGROUPS_ACTIVE_VALUE, q);
        }
    }

    /**
     * Event handler for cgroup status changes.
     *
     * Status can be either "created", "destroyed"
     * or "init" (i.e. the event was triggered during
     * the statedump).
     *
     * Updates the cgroup attribute
     * ContainerAttributes.CGROUPS_HIERARCHIES_CGROUP_IS_ACTIVE
     * in the state system.
     *
     * @param ss
     *            The state system.
     * @param event
     *            The trace event.
     */
    private static void cgroupStatusEventHandler(ITmfStateSystemBuilder ss, ITmfEvent event) {
        ITmfEventField eventContent = event.getContent();
        if (eventContent == null) {
            return;
        }
        String cgrpPath = getCgroupPath(eventContent);
        Integer cgroupQuark = getCgroupQuark(ss, cgrpPath);
        if (cgroupQuark == null) {
            return;
        }

        int cgroupActiveQuark = ss.getQuarkRelativeAndAdd(cgroupQuark, ContainerAttributes.CGROUPS_HIERARCHIES_CGROUP_IS_ACTIVE);
        String cgroupEventStatus = eventContent.getField(ContainerEventNames.CGRP_STATUS_FIELD).getFormattedValue();
        switch (cgroupEventStatus) {
            case ContainerEventNames.CGRP_STATUS_CREATED_VALUE:
            case ContainerEventNames.CGRP_STATUS_INIT_VALUE:
                ss.modifyAttribute(event.getTimestamp().toNanos(), ContainerAttributes.CGROUPS_ACTIVE_VALUE, cgroupActiveQuark);
                break;
            case ContainerEventNames.CGRP_STATUS_DESTROYED_VALUE:
                // Empty cgroup file values
                int cgroupFilesQuark = ss.getQuarkRelativeAndAdd(cgroupQuark, ContainerAttributes.CGROUPS_HIERARCHIES_FILES);
                for (Integer fileQuark : ss.getSubAttributes(cgroupFilesQuark, false)) {
                    cgroupFileCleanValues(ss, event, fileQuark);
                }
                ss.removeAttribute(event.getTimestamp().toNanos(), cgroupActiveQuark);
                break;
            default:
                break;
        }
    }

    /**
     * Event handler for cgroup file single value changes.
     *
     * Updates the file value for the cgroup in the state system.
     *
     * @param ss
     *            The state system.
     * @param event
     *            The trace event.
     */
    private static void cgroupFileUniqueValueEventHandler(ITmfStateSystemBuilder ss, ITmfEvent event) {
        ITmfEventField eventContent = event.getContent();
        if (eventContent == null) {
            return;
        }
        String cgrpPath = getCgroupPath(eventContent);
        Integer cgroupQuark = getCgroupQuark(ss, cgrpPath);
        if (cgroupQuark == null) {
            return;
        }

        int fileQuark = getCgroupFileQuark(ss, eventContent, cgroupQuark);
        String value = eventContent.getField(ContainerEventNames.CGRP_FILE_UNIQUE_VALUE_FIELD).getFormattedValue();
        ss.modifyAttribute(event.getTimestamp().toNanos(), value, fileQuark);
    }

    /**
     * Event handler for cgroup file pair value changes.
     * The pair is interpreted as a tuple (key, value).
     *
     * Adds the pair to the cgroup file in the state system.
     *
     * @param ss
     *            The state system.
     * @param event
     *            The trace event.
     */
    private static void cgroupFilePairValuesEventHandler(ITmfStateSystemBuilder ss, ITmfEvent event) {
        ITmfEventField eventContent = event.getContent();
        if (eventContent == null) {
            return;
        }
        String cgrpPath = getCgroupPath(eventContent);
        Integer cgroupQuark = getCgroupQuark(ss, cgrpPath);
        if (cgroupQuark == null) {
            return;
        }

        int fileQuark = getCgroupFileQuark(ss, eventContent, cgroupQuark);
        cgroupFileCleanValues(ss, event, fileQuark);

        String rawContent = eventContent.getField(ContainerEventNames.CGRP_FILE_PAIR_CONTENT_FIELD).getFormattedValue();
        String[] lines = rawContent.split(ContainerAttributes.CGROUPS_FILE_PAIR_VALUES_LINE_SEPARATOR);
        for (String line : lines) {
            String[] lineSplit = line.split(ContainerAttributes.CGROUPS_FILE_PAIR_VALUES_KEY_SEPARATOR);
            String key;
            String value;
            int valueQuark;
            switch (lineSplit.length) {
                case 2:
                    key = lineSplit[0];
                    value = lineSplit[1];
                    valueQuark = ss.getQuarkRelativeAndAdd(fileQuark, key);
                    ss.modifyAttribute(event.getTimestamp().toNanos(), value, valueQuark);
                    break;
                // Case specific to "devices" subsystem files
                case 3:
                    key = lineSplit[0] + ContainerAttributes.CGROUPS_FILE_PAIR_VALUES_KEY_SEPARATOR + lineSplit[1];
                    value = lineSplit[2];
                    valueQuark = ss.getQuarkRelativeAndAdd(fileQuark, key);
                    ss.modifyAttribute(event.getTimestamp().toNanos(), value, valueQuark);
                    break;
               default:
                   break;
            }
        }
    }

    /**
     * Event handler for emptied files.
     *
     * Cleans all the values associated with the cgroup
     * file in the state system.
     *
     * @param ss
     *            The state system.
     * @param event
     *            The trace event.
     */
    private static void cgroupFileEmptyEventHandler(ITmfStateSystemBuilder ss, ITmfEvent event) {
        ITmfEventField eventContent = event.getContent();
        if (eventContent == null) {
            return;
        }
        String cgrpPath = getCgroupPath(eventContent);
        Integer cgroupQuark = getCgroupQuark(ss, cgrpPath);
        if (cgroupQuark == null) {
            return;
        }

        int fileQuark = getCgroupFileQuark(ss, eventContent, cgroupQuark);
        cgroupFileCleanValues(ss, event, fileQuark);
    }

    /**
     * Event handler for process exit.
     * - Removes PID from all the cgroups they
     *   were attached to in the concerned subsystems.
     * - Updates the PID list to clean PID
     *
     * @param ss
     *            The state system.
     * @param event
     *            The trace event.
     */
    private static void processExitEventHandler(ITmfStateSystemBuilder ss, ITmfEvent event) {
        ITmfEventField eventContent = event.getContent();
        if (eventContent == null) {
            return;
        }

        long tid = (long) eventContent.getField(ContainerEventNames.SCHED_TID_FIELD).getValue();
        int pidQuark = ss.getQuarkAbsoluteAndAdd(ContainerAttributes.CGROUPS_PIDS, String.valueOf(tid));

        for (Integer q : ss.getSubAttributes(pidQuark, false)) {
            String cgroupPath = (String) ss.queryOngoing(q);
            Integer cgroupQuark = getCgroupRootQuark(ss, cgroupPath);
            if (cgroupQuark == null) {
                continue;
            }
            int cgroupPidQuark = ss.getQuarkRelativeAndAdd(cgroupQuark,
                    ContainerAttributes.CGROUPS_HIERARCHIES_PIDS, String.valueOf(tid));
            ss.removeAttribute(event.getTimestamp().toNanos(), cgroupPidQuark);
            ss.removeAttribute(event.getTimestamp().toNanos(), q);
        }
    }

    /**
     * Event handler for sched switchs.
     * @param ss
     * @param event
     */
    private void processSchedSwitchEventHandler(ITmfStateSystemBuilder ss, ITmfEvent event) {
        ITmfEventField eventContent = event.getContent();
        if (eventContent == null) {
            return;
        }

        long prevTid = (long) eventContent.getField("prev_tid").getValue(); //$NON-NLS-1$
        long nextTid = (long) eventContent.getField("next_tid").getValue(); //$NON-NLS-1$

        int prevTidQuark = ss.getQuarkAbsoluteAndAdd(ContainerAttributes.CGROUPS_PIDS,
                String.valueOf(prevTid), "cpu"); //$NON-NLS-1$
        int nextTidQuark = ss.getQuarkAbsoluteAndAdd(ContainerAttributes.CGROUPS_PIDS,
                String.valueOf(nextTid), "cpu"); //$NON-NLS-1$

        Object objPrevCgroup = ss.queryOngoing(prevTidQuark);
        Object objNextCgroup = ss.queryOngoing(nextTidQuark);

        String prevCgroup = (objPrevCgroup == null) ? null : (String)objPrevCgroup;
        String nextCgroup = (objNextCgroup == null) ? null : (String)objNextCgroup;

        boolean hasUpdated = false;
        if (nextCgroup != null) {
            // prevCgroup = prevCgroup.replaceFirst("/sys/fs/cgroup/cpu,cpuacct", ""); //$NON-NLS-1$ //$NON-NLS-2$
            nextCgroup = nextCgroup.replaceFirst("/sys/fs/cgroup/cpu,cpuacct", ""); //$NON-NLS-1$ //$NON-NLS-2$

            if (!nextCgroup.equals("")) { //$NON-NLS-1$]

                fActivePartitions.putIfAbsent(nextCgroup, 0L);
                Long curVal = fActivePartitions.get(nextCgroup);
                if (curVal != null) {
                    fActivePartitions.put(nextCgroup, curVal + 1);
                }

                if (curVal != null && curVal == 0) {
                    nextCgroup = nextCgroup.replaceFirst("/", ""); //$NON-NLS-1$ //$NON-NLS-2$
                    int pQuark = ss.getQuarkAbsoluteAndAdd("CurrentPartition"); //$NON-NLS-1$
                    ss.modifyAttribute(event.getTimestamp().toNanos(), nextCgroup, pQuark);

                    Integer cpu = KernelEventHandlerUtils.getCpu(event);
                    int cpuQuark = ss.getQuarkAbsoluteAndAdd("Partition", nextCgroup, "CPU" + String.valueOf(cpu)); //$NON-NLS-1$ //$NON-NLS-2$
                    ss.modifyAttribute(event.getTimestamp().toNanos(), nextTid, cpuQuark);

                    cpuQuark = ss.getQuarkRelativeAndAdd(pQuark, "CPU" + String.valueOf(cpu)); //$NON-NLS-1$
                    ss.modifyAttribute(event.getTimestamp().toNanos(), nextCgroup, cpuQuark);

                    fPartitionIntervals.put(nextCgroup, event.getTimestamp().toNanos());

                    hasUpdated = true;
                }
            }
        }

        if (prevCgroup != null) {
            prevCgroup = prevCgroup.replaceFirst("/sys/fs/cgroup/cpu,cpuacct", ""); //$NON-NLS-1$ //$NON-NLS-2$

            fActivePartitions.putIfAbsent(prevCgroup, 0L);
            Long curVal = fActivePartitions.get(prevCgroup);

            if (curVal != null && curVal > 0) {
                fActivePartitions.put(prevCgroup, curVal - 1);
            }

            if (curVal != null && (curVal - 1) == 0) {
                if (!prevCgroup.equals("") && !hasUpdated) { //$NON-NLS-1$
                    prevCgroup = prevCgroup.replaceFirst("/", ""); //$NON-NLS-1$ //$NON-NLS-2$
                    int pQuark = ss.getQuarkAbsoluteAndAdd("CurrentPartition"); //$NON-NLS-1$
                    ss.removeAttribute(event.getTimestamp().toNanos(), pQuark);

                    Integer cpu = KernelEventHandlerUtils.getCpu(event);
                    int cpuQuark = ss.getQuarkRelativeAndAdd(pQuark, "CPU" + String.valueOf(cpu)); //$NON-NLS-1$ //$NON-NLS-2$
                    ss.removeAttribute(event.getTimestamp().toNanos(), cpuQuark);

                    hasUpdated = true;
                }

                if (!prevCgroup.equals("")) { //$NON-NLS-1$
                    prevCgroup = prevCgroup.replaceFirst("/", ""); //$NON-NLS-1$ //$NON-NLS-2$
                    Integer cpu = KernelEventHandlerUtils.getCpu(event);
                    int cpuQuark = ss.getQuarkAbsoluteAndAdd("Partition", prevCgroup, "CPU" + String.valueOf(cpu)); //$NON-NLS-1$ //$NON-NLS-2$
                    ss.removeAttribute(event.getTimestamp().toNanos(), cpuQuark);

                    int diffQuark = ss.getQuarkAbsoluteAndAdd("CurrentPartition", "CPU" + String.valueOf(cpu), "diff"); //$NON-NLS-1$ //$NON-NLS-2$
                    Long startTime = fPartitionIntervals.get(prevCgroup);

                    List<Long> sched = fSchedule.get(prevCgroup);
                    if (sched != null && startTime != null) {
                        Long diff = ((event.getTimestamp().toNanos()) - startTime) - sched.get(0);
                        ss.modifyAttribute(startTime, diff, diffQuark);
                        ss.removeAttribute(event.getTimestamp().toNanos(), diffQuark);
                    }
            }
            }
        }

    }

    /**
     * Event handler for process creation. Adds the PID to the same cgroup as the parent.
     *
     * @param ss
     *            The state system.
     * @param event
     *            The trace event.
     */
    private static void processForkEventHandler(ITmfStateSystemBuilder ss, ITmfEvent event) {
        ITmfEventField eventContent = event.getContent();
        if (eventContent == null) {
            return;
        }

        long parentTid = (long) eventContent.getField(ContainerEventNames.SCHED_PARENT_TID_FIELD).getValue();
        long childTid = (long) eventContent.getField(ContainerEventNames.SCHED_CHILD_TID_FIELD).getValue();
        int parentPidQuark = ss.getQuarkAbsoluteAndAdd(ContainerAttributes.CGROUPS_PIDS, String.valueOf(parentTid));
        int childPidQuark = ss.getQuarkAbsoluteAndAdd(ContainerAttributes.CGROUPS_PIDS, String.valueOf(childTid));

        for (Integer q : ss.getSubAttributes(parentPidQuark, false)) {
            // Add child to parent cgroup
            String parentCgroupPath = (String) ss.queryOngoing(q);
            Integer parentCgroupQuark = getCgroupRootQuark(ss, parentCgroupPath);
            if (parentCgroupQuark == null) {
                continue;
            }
            int cgroupPidQuark = ss.getQuarkRelativeAndAdd(parentCgroupQuark,
                    ContainerAttributes.CGROUPS_HIERARCHIES_PIDS, String.valueOf(childTid));
            ss.modifyAttribute(event.getTimestamp().toNanos(), ContainerAttributes.CGROUPS_ACTIVE_VALUE, cgroupPidQuark);

            // Add child cgroup to PID list
            int childPidSubsysCgroupQuark = ss.getQuarkRelativeAndAdd(childPidQuark, ss.getAttributeName(q));
            ss.modifyAttribute(event.getTimestamp().toNanos(), parentCgroupPath, childPidSubsysCgroupQuark);
        }
    }

    @Override
    protected void eventHandle(@Nullable ITmfEvent event) {
        if (event == null) {
            return;
        }

        final ITmfStateSystemBuilder ss = checkNotNull(getStateSystemBuilder());

        switch (event.getType().getName()) {
            case ContainerEventNames.CGRP_SUBSYS_ROOT:
                newSubsysRootEventHandler(ss, event);
                break;
            case ContainerEventNames.CGRP_ATTACHED_PIDS:
                attachedPidsEventHandler(ss, event);
                break;
            case ContainerEventNames.CGRP_STATUS:
                cgroupStatusEventHandler(ss, event);
                break;
            case ContainerEventNames.CGRP_FILE_INT_VALUE:
            case ContainerEventNames.CGRP_FILE_UINT_VALUE:
            case ContainerEventNames.CGRP_FILE_STRING_VALUE:
                cgroupFileUniqueValueEventHandler(ss, event);
                break;
            case ContainerEventNames.CGRP_FILE_STRING_PAIR_VALUES:
                cgroupFilePairValuesEventHandler(ss, event);
                break;
            case ContainerEventNames.CGRP_FILE_EMPTY:
                cgroupFileEmptyEventHandler(ss, event);
                break;
            case ContainerEventNames.SCHED_PROCESS_EXIT:
                processExitEventHandler(ss, event);
                break;
            case ContainerEventNames.SCHED_PROCESS_FORK:
                processForkEventHandler(ss, event);
                break;
            case "sched_switch": //$NON-NLS-1$
                processSchedSwitchEventHandler(ss, event);
                break;
            default:
                break;
        }
    }
}