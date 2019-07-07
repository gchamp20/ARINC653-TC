package org.eclipse.tracecompass.incubator.time.partitioning.core.cgroup.provider;

/**
 * @author gchamp
 */
public class RunningCgroupEntryModel extends CgroupEntryModel {

    /**
     * @param id
     *          Unique identifier for this entry model for this trace.
     * @param parentId
     *          Unique identifier of this entry's parent.
     * @param name
     *          The name of this entry.
     * @param pid
     *          The pid of this entry. Use CGROUP_ID for nesting entries (cgroups).
     * @param parent
     *          The parent of this entry in the entry hierarchy.
     */
    public RunningCgroupEntryModel(long id, long parentId, String name, int pid, CgroupEntryModel parent, long start, long end) {
        super(id, parentId, name, pid, parent, start, end);
    }

}
