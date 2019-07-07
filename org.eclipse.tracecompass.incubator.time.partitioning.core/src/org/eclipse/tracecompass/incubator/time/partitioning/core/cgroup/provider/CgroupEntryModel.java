/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.time.partitioning.core.cgroup.provider;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;

import com.google.common.collect.ImmutableList;

/**
 * Cgroup entry model used with the {@link CgroupDataProvider}.
 *
 * @author Guillaume Champagne
 */
public class CgroupEntryModel extends TimeGraphEntryModel {
    // ------------------------------------------------------------------------
    // Static fields
    // ------------------------------------------------------------------------

    /**
     * Pid to use for cgroup entries. Cgroup entries are the parent entries of the
     * process entries.
     */
    public final static Integer CGROUP_PID = -1;

    // ------------------------------------------------------------------------
    // Private fields
    // ------------------------------------------------------------------------

    /**
     * PID of this entry, if it represent a process in the hierarchy. Equals CGROUP_ID otherwise
     */
    private int fPid;

    /**
     * Parent of this entry in the hierarchy
     */
    private CgroupEntryModel fParent;

    /**
     * Childrens of this entry that are processes in the hierachy
     */
    private @NonNull List<CgroupEntryModel> fProcessChildrens;

    /**
     * Childrens of this entry that are other cgroups
     */
    private @NonNull List<CgroupEntryModel> fCgroupChildrens;


    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

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
    public CgroupEntryModel(long id, long parentId, String name, int pid, CgroupEntryModel parent, long startTime, long endTime) {
        super(id, parentId, name, startTime, endTime);
        setPid(pid);
        setParent(parent);
        fProcessChildrens = new ArrayList<>();
        fCgroupChildrens = new ArrayList<>();
    }


    // ------------------------------------------------------------------------
    // Public methods
    // ------------------------------------------------------------------------

    /**
     * @return The associated with this entry.
     */
    public int getPid() {
        return fPid;
    }

    /**
     * @param pid
     *          The pid to associate this entry with.
     */
    public void setPid(int pid) {
        fPid = pid;
    }

    /**
     * @return The parent of this entry.
     */
    public CgroupEntryModel getParent() {
        return fParent;
    }

    /**
     * @param parent
     *          The new parent of this entry.
     */
    public void setParent(CgroupEntryModel parent) {
        fParent = parent;
    }

    /**
     * @param child
     *          The child process entry to add.
     */
    public void addProcessChild(CgroupEntryModel child) {
        fProcessChildrens.add(child);
    }

    /**
     * @return The list of childs processes of this entry.
     */
    public @NonNull List<CgroupEntryModel> getProcessChilds() {
        return ImmutableList.copyOf(fProcessChildrens);
    }


    /**
     * @param child
     *          The child cgroup entry to add.
     */
    public void addCgroupChild(CgroupEntryModel child) {
        fCgroupChildrens.add(child);
    }

    /**
     * @return The list of childs cgroups of this entry.
     */
    public @NonNull List<CgroupEntryModel> getCgroupChilds() {
        return ImmutableList.copyOf(fCgroupChildrens);
    }
}
