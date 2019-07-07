package org.eclipse.tracecompass.incubator.internal.time.partitioning.xen.core;

import java.util.Collection;
import java.util.Collections;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.internal.tmf.core.model.DataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderDescriptor.ProviderType;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

public class PartitionStatusDataProviderFactory implements IDataProviderFactory {

    private static final IDataProviderDescriptor DESCRIPTOR = new DataProviderDescriptor.Builder()
            .setId(PartitionStatusDataProvider.ID)
            .setName("Partition Status Data Provider")
            .setDescription("Provides state of partitions")
            .setProviderType(ProviderType.TIME_GRAPH)
            .build();

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(ITmfTrace trace) {
        PartitionAnalysis module = TmfTraceUtils.getAnalysisModuleOfClass(trace, PartitionAnalysis.class, PartitionAnalysis.ID);
        if (module != null) {
            module.schedule();
            return new PartitionStatusDataProvider(trace, module);
        }

        return null;
    }

    @Override
    public Collection<IDataProviderDescriptor> getDescriptors(@NonNull ITmfTrace trace) {
        PartitionAnalysis module = TmfTraceUtils.getAnalysisModuleOfClass(trace, PartitionAnalysis.class, PartitionAnalysis.ID);
        return module != null ? Collections.singletonList(DESCRIPTOR) : Collections.emptyList();
    }

}
