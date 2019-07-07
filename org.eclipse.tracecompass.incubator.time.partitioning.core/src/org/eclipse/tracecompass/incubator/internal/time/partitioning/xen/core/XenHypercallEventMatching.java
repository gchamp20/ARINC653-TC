/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.time.partitioning.xen.core;

import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.matching.IEventMatchingKey;
import org.eclipse.tracecompass.tmf.core.event.matching.ITmfMatchEventDefinition;
import org.eclipse.tracecompass.tmf.core.event.matching.TmfEventMatching.Direction;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTraceWithPreDefinedEvents;
import org.eclipse.tracecompass.tmf.core.trace.TmfEventTypeCollectionHelper;

import com.google.common.collect.ImmutableSet;

/**
 *
 * @author Guillaume Champagne
 */
public class XenHypercallEventMatching implements ITmfMatchEventDefinition {

    private static String HYPERCALL_VERSION = "hypercall_version"; //$NON-NLS-1$
    private static String HYPERCALL_VERSION_RETURN = "hypercall_version_return"; //$NON-NLS-1$
    private static String XEN_SYNC_HYPERCALL_VERSION = "xen_sync:hypercall_version_entry"; //$NON-NLS-1$
    private static String XEN_SYNC_HYPERCALL_VERSION_RETURN = "xen_sync:hypercall_version_exit"; //$NON-NLS-1$

    private static final ImmutableSet<String> REQUIRED_EVENTS = ImmutableSet.of(
            HYPERCALL_VERSION,
            HYPERCALL_VERSION_RETURN,
            XEN_SYNC_HYPERCALL_VERSION,
            XEN_SYNC_HYPERCALL_VERSION_RETURN);

    /**
     * @since 1.0
     */
    @Override
    public @Nullable Direction getDirection(@Nullable ITmfEvent event) {
        if (event == null) {
            return null;
        }

        String evname = event.getName();

        if (evname.equals(HYPERCALL_VERSION)) {
            return Direction.EFFECT;
        } else if (evname.equals(XEN_SYNC_HYPERCALL_VERSION)) {
            return Direction.CAUSE;
        } else if (evname.equals(HYPERCALL_VERSION_RETURN)) {
            return Direction.CAUSE;
        } else if (evname.equals(XEN_SYNC_HYPERCALL_VERSION_RETURN)) {
            return Direction.EFFECT;
        }

        return null;
    }

    @Override
    public @Nullable IEventMatchingKey getEventKey(@Nullable ITmfEvent event) {
        if (event == null) {
            return null;
        }

        ITmfEventField content = event.getContent();
        Long fId = content.getFieldValue(Long.class, "id"); //$NON-NLS-1$

        if (fId == null) {
            throw new IllegalArgumentException("Event does not have expected fields"); //$NON-NLS-1$
        }

        IEventMatchingKey key = new XenHypercallEventKey(fId);
        return key;
    }

    @Override
    public boolean canMatchTrace(@Nullable ITmfTrace trace) {
        if (!(trace instanceof ITmfTraceWithPreDefinedEvents)) {
            return true;
        }
        ITmfTraceWithPreDefinedEvents ktrace = (ITmfTraceWithPreDefinedEvents) trace;

        Set<String> traceEvents = TmfEventTypeCollectionHelper.getEventNames(ktrace.getContainedEventTypes());
        traceEvents.retainAll(REQUIRED_EVENTS);
        return !traceEvents.isEmpty();
    }

}
