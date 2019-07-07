/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.time.partitioning.xen.core;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.tmf.core.event.matching.IEventMatchingKey;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

/**
 * @author Guillaume Champagne
 */
@NonNullByDefault
public class XenHypercallEventKey implements IEventMatchingKey {

    private static final HashFunction HF = checkNotNull(Hashing.goodFastHash(32));
    private final long fId;

    /**
     * Constructor
     *
     * @param Id
     *            ID
     */
    public XenHypercallEventKey(long id) {
        fId = id;
    }

    @Override
    public int hashCode() {
        return HF.newHasher()
                .putLong(fId).hash().asInt();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o instanceof XenHypercallEventKey) {
            XenHypercallEventKey key = (XenHypercallEventKey) o;
            return (key.fId == fId);
        }
        return false;
    }
}
