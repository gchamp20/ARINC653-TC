/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.time.partitioning.core.cgroup;

/**
 * This file defines all the attribute names used in the container analysis.
 *
 * @author Loïc Gelle
 */
@SuppressWarnings({"nls", "javadoc"})
public final class ContainerAttributes {

    /* First-level attributes */
    public static final String CGROUPS_HIERARCHIES = "Cgroups Hierarchies";
    public static final String CGROUPS_SUBSYS = "Cgroups Subsystems";
    public static final String CGROUPS_PIDS = "Cgroups PIDs";
    public static final String CGROUPS_METRICS = "Cgroups Metrics";

    /* Cgroups subsystems description */
    public static final String CGROUP_SUBSYS_ROOT_PATH = "Root path";

    /* Cgroups hierachy attributes */
    public static final String CGROUPS_HIERARCHIES_CHILDREN = "CHILDREN";
    public static final String CGROUPS_HIERARCHIES_PIDS = "PIDS";
    public static final String CGROUPS_HIERARCHIES_SUBSYS = "SUBSYS";
    public static final String CGROUPS_HIERARCHIES_FILES = "FILES";
    public static final String CGROUPS_HIERARCHIES_CGROUP_IS_ACTIVE = "IsActive";

    /* Cgroups files attributes */
    public static final String CGROUPS_FILE_PAIR_VALUES_LINE_SEPARATOR = "\n";
    public static final String CGROUPS_FILE_PAIR_VALUES_KEY_SEPARATOR = " ";

    /* Cgroups constant values */
    public static final String CGROUPS_ACTIVE_VALUE = "1";
    public static final String CGROUPS_FILE_EMPTY_VALUE = "";

    /* Cgroups metrics attributes */
    public static final String CGROUP_METRICS_CPU = "cpu";
    public static final String CGROUP_METRICS_CPU_CGROUP_STATE = "METRIC_STATE";
    public static final String CGROUP_METRICS_CPU_CGROUP_STACK = "METRIC_STACK";

    private ContainerAttributes() {}

}
