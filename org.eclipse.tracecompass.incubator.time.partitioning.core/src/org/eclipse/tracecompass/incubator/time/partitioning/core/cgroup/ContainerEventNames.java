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
 * This file defines all the event names used in the container analysis.
 *
 * @author Loïc Gelle
 */
@SuppressWarnings({"nls"})
final class ContainerEventNames {

    /* Control groups events from UST */
    static final String CGRP_PATH_FIELD = "cgrp_path";
    static final String CGRP_FILENAME_FIELD = "filename";

    static final String CGRP_SUBSYS_ROOT = "cgroup_ust:cgroup_subsys_root";
    static final String CGRP_SUBSYS_ROOT_SUBSYS_FIELD = "subsys_name";
    static final String CGRP_SUBSYS_ROOT_ROOT_FIELD = "root";

    static final String CGRP_ATTACHED_PIDS = "cgroup_ust:cgroup_attached_pids";
    static final String CGRP_ATTACHED_PIDS_PIDS_FIELD = "pids";

    static final String CGRP_STATUS = "cgroup_ust:cgroup_path_status";
    static final String CGRP_STATUS_FIELD = "status";
    static final String CGRP_STATUS_CREATED_VALUE = "1";
    static final String CGRP_STATUS_INIT_VALUE = "0";
    static final String CGRP_STATUS_DESTROYED_VALUE = "-1";

    static final String CGRP_FILE_INT_VALUE = "cgroup_ust:cgroup_file_int_value";
    static final String CGRP_FILE_UINT_VALUE = "cgroup_ust:cgroup_file_uint_value";
    static final String CGRP_FILE_STRING_VALUE = "cgroup_ust:cgroup_file_string_value";
    static final String CGRP_FILE_UNIQUE_VALUE_FIELD = "val";

    static final String CGRP_FILE_STRING_PAIR_VALUES = "cgroup_ust:cgroup_file_string_pair_values";
    static final String CGRP_FILE_PAIR_CONTENT_FIELD = "content";

    static final String CGRP_FILE_BLKIO_VALUE = "cgroup_ust:cgroup_file_blkio_value";
    static final String CGRP_FILE_BLKIO_MAJOR_FIELD = "major";
    static final String CGRP_FILE_BLKIO_MINOR_FIELD = "minor";
    static final String CGRP_FILE_BLKIO_VALUE_FIELD = "val";

    static final String CGRP_FILE_DEVICES_VALUE = "cgroup_ust:cgroup_file_devices_value";
    static final String CGRP_FILE_DEVICES_MAJOR_FIELD = "major";
    static final String CGRP_FILE_DEVICES_MINOR_FIELD = "minor";
    static final String CGRP_FILE_DEVICES_VALUE_FIELD = "val";
    static final String CGRP_FILE_DEVICES_DEV_TYPE_FIELD = "dev_type";

    static final String CGRP_FILE_EMPTY = "cgroup_ust:cgroup_file_empty";

    static final String SCHED_PROCESS_FORK = "sched_process_fork";
    static final String SCHED_PROCESS_EXIT = "sched_process_exit";
    static final String SCHED_TID_FIELD = "tid";
    static final String SCHED_PARENT_TID_FIELD = "parent_tid";
    static final String SCHED_CHILD_TID_FIELD = "child_tid";

    /* Control groups filenames */
    static final String CGRP_FILENAME_PROCS = "cgroup.procs";

    private ContainerEventNames() {}

}