/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.time.partitioning.core.cgroup.provider;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.time.partitioning.core.cgroup.ContainerAnalysis;
import org.eclipse.tracecompass.tmf.core.dataprovider.IDataProviderFactory;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Factory for {@link CgroupDataProvider}
 *
 * @author Guillaume Champagne
 */
public class CgroupDataProviderFactory implements IDataProviderFactory {

    // ------------------------------------------------------------------------
    // IDataProviderFactory
    // ------------------------------------------------------------------------

    @Override
    public @Nullable ITmfTreeDataProvider<? extends ITmfTreeDataModel> createProvider(@NonNull ITmfTrace trace) {
        ContainerAnalysis module = TmfTraceUtils.getAnalysisModuleOfClass(trace, ContainerAnalysis.class, ContainerAnalysis.ID);
        if (module != null) {
            module.schedule();
            return new CgroupDataProvider(trace, module);
        }

        return null;
    }

}
