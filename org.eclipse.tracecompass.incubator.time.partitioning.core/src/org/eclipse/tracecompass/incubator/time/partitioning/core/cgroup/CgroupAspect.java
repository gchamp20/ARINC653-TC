/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.time.partitioning.core.cgroup;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelTidAspect;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * This aspect find the Cgroup in which the thread running belong from
 * this event using the {@link KernelTidAspect} and the {@link ContainerAnalysis}
 *
 * @author Guillaume Champagne
 */
public class CgroupAspect implements ITmfEventAspect<String> {

    // ------------------------------------------------------------------------
    // Static field
    // ------------------------------------------------------------------------
    /** The singleton instance */
    public static final CgroupAspect INSTANCE = new CgroupAspect();

    private static final IProgressMonitor NULL_MONITOR = new NullProgressMonitor();

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------\

    /**
     * Private constructor to enforce singleton pattern.
     */
    private CgroupAspect() {

    }

    // ------------------------------------------------------------------------
    // ITmfEventAspect
    // ------------------------------------------------------------------------

    @Override
    public String getName() {
        return Messages.getMessage(Messages.AspectName_Cgroup);
    }

    @Override
    public String getHelpText() {
        return Messages.getMessage(Messages.AspectHelpText_Cgroup);
    }

    @Override
    public @Nullable String resolve(ITmfEvent event) {
        try {
            return resolve(event, false, NULL_MONITOR);
        } catch (InterruptedException e) {
            /* Should not happen since there is nothing to interrupt */
            return null;
        }
    }

    @Override
    public @Nullable String resolve(@NonNull ITmfEvent event, boolean block, final IProgressMonitor monitor) throws InterruptedException {

        /* Find the tid of the thread this event belongs to */
        Integer tid = KernelTidAspect.INSTANCE.resolve(event, block, monitor);

        if (tid == null) {
            return null;
        }

        /* Find the container analysis module for the trace. Normally, the trace to resolve this aspect
         * is part of an experiment, so we check if the parent provides the container analysis. */
        ITmfTrace parent = (ITmfTrace)event.getTrace().getParent();
        if (parent == null) {
            return null;
        }

        ContainerAnalysis analysis = TmfTraceUtils.getAnalysisModuleOfClass(parent,
                ContainerAnalysis.class, ContainerAnalysis.ID);

        if (analysis == null) {
            return null;
        }

        long ts = event.getTimestamp().toNanos();
        while (block && !analysis.isQueryable(ts) && !monitor.isCanceled()) {
            Thread.sleep(100);
        }

        return analysis.getCpuControllerCgroup(tid, ts);
    }

    /**
     * @param tid
     *          Tid of the thread
     * @param event
     *          Event
     * @param block
     *          Block or no
     * @param monitor
     *          Monitor
     * @return
     *          The cgroup
     * @throws InterruptedException
     *          When monitor
     */
    public @Nullable String resolve(long tid, ITmfEvent event, boolean block, final IProgressMonitor monitor) throws InterruptedException {

        /* Find the container analysis module for the trace. Normally, the trace to resolve this aspect
         * is part of an experiment, so we check if the parent provides the container analysis. */
        ITmfTrace parent = (ITmfTrace)event.getTrace().getParent();
        if (parent == null) {
            return null;
        }

        ContainerAnalysis analysis = TmfTraceUtils.getAnalysisModuleOfClass(parent,
                ContainerAnalysis.class, ContainerAnalysis.ID);

        if (analysis == null) {
            return null;
        }

        long ts = event.getTimestamp().toNanos();
        while (block && !analysis.isQueryable(ts) && !monitor.isCanceled()) {
            Thread.sleep(100);
        }

        return analysis.getCpuControllerCgroup(tid, ts);
    }
}
