/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.time.partitioning.core.cgroup;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.osgi.util.NLS;

/**
 * Externalized message strings from the Container Analysis.
 *
 * @author Guillaume Champagne
 */
@SuppressWarnings("javadoc")
public class Messages {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.incubator.internal.virtual.machine.analysis.core.container.messages"; //$NON-NLS-1$

    public static @Nullable String AspectName_Cgroup;
    public static @Nullable String AspectHelpText_Cgroup;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }

    /**
     * Helper method to expose externalized strings as non-null objects.
     */
    static String getMessage(@Nullable String msg) {
        if (msg == null) {
            return ""; //$NON-NLS-1$
        }
        return msg;
    }
}
