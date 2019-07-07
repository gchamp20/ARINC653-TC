package org.eclipse.tracecompass.incubator.internal.time.partitioning.xen.core;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.TmfStateSystemAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

/**
 *
 * @author gchamp
 */
public class PartitionAnalysis extends TmfStateSystemAnalysisModule {

    /**
     * Extension point ID.
     */
    public static final @NonNull String ID = "org.eclipse.tracecompass.incubator.internal.time.partitioning.core.PartitionAnalysis"; //$NON-NLS-1$

    @Override
    protected ITmfStateProvider createStateProvider() {
        ITmfTrace trace = checkNotNull(getTrace());
        return new PartitionStateProvider(trace);
    }
}
