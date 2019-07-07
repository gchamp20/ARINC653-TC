package org.eclipse.tracecompass.incubator.internal.time.partitioning.xen.core;

import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfCpuAspect;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 *
 * @author gchamp
 *
 */
public class PartitionStateProvider extends AbstractTmfStateProvider {

    /**
     *
     * @param trace The trace
     */
    public PartitionStateProvider(ITmfTrace trace) {
        super(trace, "Partition"); //$NON-NLS-1$
    }

    @Override
    public int getVersion() {
        return 0;
    }

    @Override
    public ITmfStateProvider getNewInstance() {
        return new PartitionStateProvider(this.getTrace());
    }

    @Override
    protected void eventHandle(ITmfEvent event) {
        final ITmfStateSystemBuilder ss = NonNullUtils.checkNotNull(getStateSystemBuilder());
        String name = event.getName();
        if (name.equals("switch_infnext")) { //$NON-NLS-1$
            handle_infnext(event, ss);
        } else if (name.equals("switch_infprev")) { //$NON-NLS-1$
            handle_infprev(event ,ss);
        } else if (name.equals("switch_infcont")) { //$NON-NLS-1$
            handle_infcont(event ,ss);
        }
    }

    private static void handle_infcont(ITmfEvent event, ITmfStateSystemBuilder ss) {
        ITmfEventField content = event.getContent();
        Integer dom = content.getFieldValue(Integer.class, "dom"); //$NON-NLS-1$
        // Integer vpcu = content.getFieldValue(Integer.class, "vcpu");

        Integer cpu = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), TmfCpuAspect.class, event);

        int pQuark = ss.getQuarkAbsoluteAndAdd("Partition", "CPU" + String.valueOf(cpu)); //$NON-NLS-1$
        ss.modifyAttribute(event.getTimestamp().toNanos(), dom, pQuark);

        pQuark = ss.getQuarkAbsoluteAndAdd("Domain", String.valueOf(dom)); //$NON-NLS-1$
        ss.modifyAttribute(event.getTimestamp().toNanos(), 1, pQuark);
        /*pQuark = ss.getQuarkAbsoluteAndAdd("Partition");  //$NON-NLS-1$
        ss.modifyAttribute(event.getTimestamp().toNanos(), dom, pQuark); */
    }

    private static void handle_infprev(ITmfEvent event, ITmfStateSystemBuilder ss) {
        ITmfEventField content = event.getContent();
        Integer dom = content.getFieldValue(Integer.class, "dom"); //$NON-NLS-1$
        // Integer vpcu = content.getFieldValue(Integer.class, "vcpu");
        Integer cpu = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), TmfCpuAspect.class, event);

        int pQuark = ss.getQuarkAbsoluteAndAdd("Partition", "CPU" + String.valueOf(cpu)); //$NON-NLS-1$
        ss.modifyAttribute(event.getTimestamp().toNanos(), 0, pQuark);

        pQuark = ss.getQuarkAbsoluteAndAdd("Domain", String.valueOf(dom)); //$NON-NLS-1$
        ss.modifyAttribute(event.getTimestamp().toNanos(), 0, pQuark);
    }

    private static void handle_infnext(ITmfEvent event, ITmfStateSystemBuilder ss) {
        ITmfEventField content = event.getContent();
        Integer dom = content.getFieldValue(Integer.class, "dom"); //$NON-NLS-1$
        // Integer vpcu = content.getFieldValue(Integer.class, "vcpu");

        Integer cpu = TmfTraceUtils.resolveIntEventAspectOfClassForEvent(event.getTrace(), TmfCpuAspect.class, event);

        int pQuark = ss.getQuarkAbsoluteAndAdd("Partition", "CPU" + String.valueOf(cpu)); //$NON-NLS-1$
        ss.modifyAttribute(event.getTimestamp().toNanos(), dom, pQuark);

        pQuark = ss.getQuarkAbsoluteAndAdd("Domain", String.valueOf(dom)); //$NON-NLS-1$
        ss.modifyAttribute(event.getTimestamp().toNanos(), 1, pQuark);
    }

}
