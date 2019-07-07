/*******************************************************************************
 * Copyright (c) 2018 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.time.partitioning.core.cgroup.provider;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.tracecompass.analysis.os.linux.core.kernel.KernelTidAspect;
import org.eclipse.tracecompass.analysis.os.linux.core.signals.TmfThreadSelectedSignal;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.threadstatus.ThreadEntryModel;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.threadstatus.ThreadStatusDataProvider;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.Activator;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.Messages;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.actions.FollowThreadAction;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.controlflow.ControlFlowCheckActiveProvider;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.controlflow.ControlFlowColumnComparators;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.controlflow.ControlFlowEntry;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.controlflow.IControlFlowEntryComparator;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.controlflow.NaiveOptimizationAlgorithm;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.controlflow.PackageMessages;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.controlflow.filters.ActiveThreadsFilter;
import org.eclipse.tracecompass.internal.analysis.os.linux.ui.views.controlflow.filters.DynamicFilterDialog;
import org.eclipse.tracecompass.internal.tmf.core.model.filters.TimeGraphStateQueryFilter;
import org.eclipse.tracecompass.statesystem.core.StateSystemUtils;
import org.eclipse.tracecompass.tmf.core.dataprovider.DataProviderManager;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.model.filters.SelectionTimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.filters.TimeQueryFilter;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphDataProvider;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphRowModel;
import org.eclipse.tracecompass.tmf.core.model.timegraph.ITimeGraphState;
import org.eclipse.tracecompass.tmf.core.model.timegraph.TimeGraphEntryModel;
import org.eclipse.tracecompass.tmf.core.response.ITmfResponse;
import org.eclipse.tracecompass.tmf.core.response.TmfModelResponse;
import org.eclipse.tracecompass.tmf.core.signal.TmfSelectionRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;
import org.eclipse.tracecompass.tmf.ui.views.FormatTimeUtils;
import org.eclipse.tracecompass.tmf.ui.views.FormatTimeUtils.Resolution;
import org.eclipse.tracecompass.tmf.ui.views.FormatTimeUtils.TimeFormat;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.BaseDataProviderTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.ITimeGraphEntryComparator;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ILinkEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry.Sampling;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.TimeGraphControl;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.Utils;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

/**
 * A test class to extend the control flow view
 * with per cgroup filtering.
 *
 * @author Guillaume Champagne
 */
@SuppressWarnings("restriction")
public class CgroupControlViewFilter extends BaseDataProviderTimeGraphView {

    // ------------------------------------------------------------------------
    // Constants
    // ------------------------------------------------------------------------
    /**
     * View ID.
     */
    public static final @NonNull String ID = "org.eclipse.tracecompass.incubator.time.partitioning.core.cgroup.controlflow"; //$NON-NLS-1$

    private static final String ICONS_PATH = "icons/"; //$NON-NLS-1$
    private static final String OPTIMIZE_ICON = ICONS_PATH + "elcl16/Optimization.png"; //$NON-NLS-1$


    private static final String PROCESS_COLUMN = Messages.ControlFlowView_processColumn;
    private static final String TID_COLUMN = Messages.ControlFlowView_tidColumn;
    private static final String PTID_COLUMN = Messages.ControlFlowView_ptidColumn;
    private static final String BIRTH_TIME_COLUMN = Messages.ControlFlowView_birthTimeColumn;
    private static final String INVISIBLE_COLUMN = Messages.ControlFlowView_invisibleColumn;
    private Action fOptimizationAction;

    private static final String NEXT_EVENT_ICON_PATH = "icons/elcl16/shift_r_edit.gif"; //$NON-NLS-1$
    private static final String PREV_EVENT_ICON_PATH = "icons/elcl16/shift_l_edit.gif"; //$NON-NLS-1$


    private static final String[] COLUMN_NAMES = new String[] {
            PROCESS_COLUMN,
            TID_COLUMN,
            PTID_COLUMN,
            BIRTH_TIME_COLUMN
    };

    private static final String[] FILTER_COLUMN_NAMES = new String[] {
            PROCESS_COLUMN,
            TID_COLUMN
    };

    private static final Comparator<ITimeGraphEntry>[] COLUMN_COMPARATORS;

    private static final Function<Collection<ILinkEvent>, Map<Integer, Long>> UPDATE_SCHEDULING_COLUMN_ALGO = new NaiveOptimizationAlgorithm();

    private static final int INITIAL_SORT_COLUMN_INDEX = 0;

    /**
     * Process Name comparator. This compares first the trace, then the process name, then the
     * birth time, then the TID finally the parent TID.
     */
    public static final ITimeGraphEntryComparator CGROUP_COLUMN_COMPARATOR = new ITimeGraphEntryComparator() {
        private final List<Comparator<ITimeGraphEntry>> SECONDARY_COMPARATORS = init();
        private int fDirection = SWT.DOWN;

        private int compareList(int prevResult, int direction, List<Comparator<ITimeGraphEntry>> comps, ITimeGraphEntry o1, ITimeGraphEntry o2) {
            int result = prevResult;
            for (Comparator<ITimeGraphEntry> comparator : comps) {
                if (result == 0) {
                    result = comparator.compare(o1, o2);
                    if (direction == SWT.UP) {
                        result = -result;
                    }
                }
            }
            return result;
        }

        @Override
        public int compare(@Nullable ITimeGraphEntry o1, @Nullable ITimeGraphEntry o2) {
            /* First sort by process name */
            if (o1 != null && o1.getName().equals("RUNNING PARTITION")) { //$NON-NLS-1$
                return -1;
            } else if (o2 != null && o2.getName().equals("RUNNING PARTITION")) {
                return 1;
            }

            int result = IControlFlowEntryComparator.PROCESS_NAME_COMPARATOR.compare(o1, o2);
            return compareList(result, fDirection, SECONDARY_COMPARATORS, o1, o2);
        }

        @Override
        public void setDirection(int direction) {
            fDirection = direction;
        }

        private List<Comparator<ITimeGraphEntry>> init() {
            ImmutableList.Builder<Comparator<ITimeGraphEntry>> builder = ImmutableList.builder();
            builder.add(IControlFlowEntryComparator.BIRTH_TIME_COMPARATOR)
                .add(IControlFlowEntryComparator.TID_COMPARATOR)
                .add(IControlFlowEntryComparator.PTID_COMPARATOR);
            return builder.build();
        }
    };

    static {
        ImmutableList.Builder<Comparator<ITimeGraphEntry>> builder = ImmutableList.builder();
        builder.add(CGROUP_COLUMN_COMPARATOR)
            .add(ControlFlowColumnComparators.TID_COLUMN_COMPARATOR)
            .add(ControlFlowColumnComparators.PTID_COLUMN_COMPARATOR)
            .add(ControlFlowColumnComparators.BIRTH_TIME_COLUMN_COMPARATOR);
            //.add(CGROUP_COLUMN_COMPARATOR);
        List<Comparator<ITimeGraphEntry>> l = builder.build();
        COLUMN_COMPARATORS = l.toArray(new Comparator[l.size()]);
    }

    /**
     * Mutex rule for search action jobs, making sure they execute sequentially
     */
    private final ISchedulingRule fSearchActionMutexRule = new ISchedulingRule() {
        @Override
        public boolean isConflicting(ISchedulingRule rule) {
            return (rule == this);
        }

        @Override
        public boolean contains(ISchedulingRule rule) {
            return (rule == this);
        }
    };

    private final Set<ITmfTrace> fFlatTraces = new HashSet<>();

    private IAction fFlatAction;

    private IAction fHierarchicalAction;

    private @NonNull ActiveThreadsFilter fActiveThreadsFilter = new ActiveThreadsFilter(null, false, null);

    private final ActiveThreadsFilterAction fActiveThreadsRapidToggle = new ActiveThreadsFilterAction();

    class ActiveThreadsFilterAction extends Action {
        public ActiveThreadsFilterAction() {
            super(PackageMessages.ControlFlowView_DynamicFiltersActiveThreadToggleLabel, IAction.AS_CHECK_BOX);
            setToolTipText(PackageMessages.ControlFlowView_DynamicFiltersActiveThreadToggleToolTip);
            addPropertyChangeListener(new IPropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent event) {
                    if (!(event.getNewValue() instanceof Boolean)) {
                        return;
                    }

                    Boolean enabled = (Boolean) event.getNewValue();

                    /* Always remove the previous Active Threads filter */
                    getTimeGraphViewer().removeFilter(fActiveThreadsFilter);

                    if (enabled) {
                        fActiveThreadsFilter.setEnabled(true);
                        getTimeGraphViewer().addFilter(fActiveThreadsFilter);

                        /* Use flat representation */
                        if (fFlatAction != null) {
                            applyFlatPresentation();
                            fFlatAction.setChecked(true);
                            fHierarchicalAction.setChecked(false);
                        }
                    } else {
                        fActiveThreadsFilter.setEnabled(false);
                    }
                    startZoomThread(getTimeGraphViewer().getTime0(), getTimeGraphViewer().getTime1());
                }
            });
        }
    }

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Constructor
     */
    public CgroupControlViewFilter() {
        super(ID, new CgroupViewPresentationProvider(), ThreadStatusDataProvider.ID);
        setTreeColumns(COLUMN_NAMES, COLUMN_COMPARATORS, INITIAL_SORT_COLUMN_INDEX);
        setTreeLabelProvider(new ControlFlowTreeLabelProvider());
        setFilterColumns(FILTER_COLUMN_NAMES);
        setFilterLabelProvider(new ControlFlowFilterLabelProvider());
        setEntryComparator(ControlFlowColumnComparators.BIRTH_TIME_COLUMN_COMPARATOR);
        fAtomic = new AtomicLong(56312);
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        // add "Check active" Button to TimeGraphFilterDialog
        getTimeGraphViewer().getShowFilterDialogAction().getFilterDialog().addTimeGraphFilterCheckActiveButton(
                new ControlFlowCheckActiveProvider(Messages.ControlFlowView_checkActiveLabel, Messages.ControlFlowView_checkActiveToolTip));
        // add "Uncheck inactive" Button to TimeGraphFilterDialog
        getTimeGraphViewer().getShowFilterDialogAction().getFilterDialog().addTimeGraphFilterUncheckInactiveButton(
                new ControlFlowCheckActiveProvider(Messages.ControlFlowView_uncheckInactiveLabel, Messages.ControlFlowView_uncheckInactiveToolTip));
    }

    /**
     * @since 2.0
     */
    @Override
    protected void fillTimeGraphEntryContextMenu(@NonNull IMenuManager menuManager) {
        ISelection selection = getSite().getSelectionProvider().getSelection();
        if (selection instanceof StructuredSelection) {
            StructuredSelection sSel = (StructuredSelection) selection;
            if (sSel.getFirstElement() instanceof ControlFlowEntry) {
                ControlFlowEntry entry = (ControlFlowEntry) sSel.getFirstElement();
                menuManager.add(new FollowThreadAction(CgroupControlViewFilter.this, entry.getName(), entry.getThreadId(), getTrace(entry)));
            } else if (sSel.getFirstElement() instanceof RunningPartitionEntry) {
                RunningPartitionEntry entry = (RunningPartitionEntry) sSel.getFirstElement();
                if (entry.getName().equals("RUNNING PARTITION")) {
                    menuManager.add(new FollowThreadAction(CgroupControlViewFilter.this, entry.getName(), 123456789, getTrace(entry)));
                }
            }
        }
    }

    @Override
    protected void fillLocalToolBar(IToolBarManager manager) {
        // add "Optimization" Button to local tool bar of Controlflow
        IAction optimizationAction = getOptimizationAction();
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, optimizationAction);

        // add a separator to local tool bar
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new Separator());

        super.fillLocalToolBar(manager);
        IDialogSettings settings = Activator.getDefault().getDialogSettings();
        IDialogSettings section = settings.getSection(getClass().getName());
        if (section == null) {
            section = settings.addNewSection(getClass().getName());
        }

        IAction hideArrowsAction = getTimeGraphViewer().getHideArrowsAction(section);
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, hideArrowsAction);

        IAction followArrowBwdAction = getTimeGraphViewer().getFollowArrowBwdAction();
        followArrowBwdAction.setText(Messages.ControlFlowView_followCPUBwdText);
        followArrowBwdAction.setToolTipText(Messages.ControlFlowView_followCPUBwdText);
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, followArrowBwdAction);

        IAction followArrowFwdAction = getTimeGraphViewer().getFollowArrowFwdAction();
        followArrowFwdAction.setText(Messages.ControlFlowView_followCPUFwdText);
        followArrowFwdAction.setToolTipText(Messages.ControlFlowView_followCPUFwdText);
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, followArrowFwdAction);

        IAction previousEventAction = new SearchEventAction(false, PackageMessages.ControlFlowView_PreviousEventJobName);
        previousEventAction.setText(PackageMessages.ControlFlowView_PreviousEventActionName);
        previousEventAction.setToolTipText(PackageMessages.ControlFlowView_PreviousEventActionTooltip);
        previousEventAction.setImageDescriptor(Activator.getDefault().getImageDescripterFromPath(PREV_EVENT_ICON_PATH));
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, previousEventAction);

        IAction nextEventAction = new SearchEventAction(true, PackageMessages.ControlFlowView_NextEventJobName);
        nextEventAction.setText(PackageMessages.ControlFlowView_NextEventActionName);
        nextEventAction.setToolTipText(PackageMessages.ControlFlowView_NextEventActionTooltip);
        nextEventAction.setImageDescriptor(Activator.getDefault().getImageDescripterFromPath(NEXT_EVENT_ICON_PATH));
        manager.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, nextEventAction);
    }

    private IAction getOptimizationAction() {
        if (fOptimizationAction == null) {
            fOptimizationAction = new OptimizationAction();
            fOptimizationAction.setImageDescriptor(Activator.getDefault().getImageDescripterFromPath(OPTIMIZE_ICON));
            fOptimizationAction.setText(Messages.ControlFlowView_optimizeLabel);
            fOptimizationAction.setToolTipText(Messages.ControlFlowView_optimizeToolTip);
        }
        return fOptimizationAction;
    }

    @Override
    protected void fillLocalMenu(IMenuManager manager) {
        super.fillLocalMenu(manager);
        MenuManager item = new MenuManager(Messages.ControlFlowView_threadPresentation);
        fFlatAction = createFlatAction();
        item.add(fFlatAction);

        fHierarchicalAction = createHierarchicalAction();
        item.add(fHierarchicalAction);
        manager.add(item);

        item = new MenuManager(PackageMessages.ControlFlowView_DynamicFiltersMenuLabel);
        item.add(fActiveThreadsRapidToggle);
        item.add(new Separator());

        IAction dynamicFiltersConfigureAction = createDynamicFilterConfigureAction();
        item.add(dynamicFiltersConfigureAction);

        manager.add(item);
    }

    /**
     * Base Action for the "Go to Next/Previous Event for thread" actions
     */
    private class SearchEventAction extends Action {

        private final boolean ifDirection;
        private final String ifJobName;

        /**
         * Constructor
         *
         * @param direction
         *            The direction of the search, "true" for forwards and
         *            "false" for backwards.
         * @param jobName
         *            The name of the job that will be spawned
         */
        public SearchEventAction(boolean direction, String jobName) {
            ifDirection = direction;
            ifJobName = jobName;
        }

        @Override
        public void run() {
            Job job = new Job(ifJobName) {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    TimeGraphControl ctrl = getTimeGraphViewer().getTimeGraphControl();
                    ITimeGraphEntry traceEntry = ctrl.getSelectedTrace();

                    long ts = getTimeGraphViewer().getSelectionBegin();
                    ITimeEvent selectedState = Utils.findEvent(traceEntry, ts, 0);

                    if (selectedState == null) {
                        /* No selection currently in the view, do nothing */
                        return Status.OK_STATUS;
                    }
                    ITimeGraphEntry entry = selectedState.getEntry();
                    if (!(entry instanceof ControlFlowEntry)) {
                        return Status.OK_STATUS;
                    }
                    ControlFlowEntry cfEntry = (ControlFlowEntry) entry;
                    int tid = cfEntry.getThreadId();

                    ITmfTrace trace = getTrace(cfEntry);
                    ITmfContext ctx = trace.seekEvent(TmfTimestamp.fromNanos(ts));
                    long rank = ctx.getRank();
                    ctx.dispose();

                    /*
                     * TODO Specific to the Control Flow View and kernel traces for now. Could be
                     * eventually generalized to anything represented by the time graph row.
                     */
                    Predicate<@NonNull ITmfEvent> predicate = event -> Objects.equals(tid, KernelTidAspect.INSTANCE.resolve(event));

                    ITmfEvent event = (ifDirection ?
                            TmfTraceUtils.getNextEventMatching(trace, rank, predicate, monitor) :
                            TmfTraceUtils.getPreviousEventMatching(trace, rank, predicate, monitor));
                    if (event != null) {
                        TmfSignalManager.dispatchSignal(new TmfSelectionRangeUpdatedSignal(this, event.getTimestamp(), event.getTimestamp(), getTrace()));
                    }
                    return Status.OK_STATUS;

                }
            };
            /*
             * Make subsequent jobs not run concurrently, but wait after one
             * another.
             */
            job.setRule(fSearchActionMutexRule);
            job.schedule();
        }
    }

    private IAction createDynamicFilterConfigureAction() {
        return new Action(PackageMessages.ControlFlowView_DynamicFiltersConfigureLabel, IAction.AS_PUSH_BUTTON) {
            @Override
            public void run() {
                DynamicFilterDialog dialog = new DynamicFilterDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), fActiveThreadsFilter, getTrace());
                if (dialog.open() == Window.OK) {
                    /* Remove the previous Active Threads filter */
                    checkNotNull(getTimeGraphViewer()).removeFilter(fActiveThreadsFilter);

                    ActiveThreadsFilter newFilter = dialog.getActiveThreadsResult();
                    ActiveThreadsFilter previousFilter = fActiveThreadsFilter;

                    /* Set the filter to the view */
                    fActiveThreadsFilter = newFilter;

                    boolean enabled = fActiveThreadsFilter.isEnabled();
                    if (enabled) {
                        checkNotNull(getTimeGraphViewer()).addFilter(newFilter);
                    }

                    /*
                     * Prevent double refresh from change state of setChecked
                     * and ensure that a refresh is done if the mode of the
                     * filter is changed or options are changed
                     */
                    if (previousFilter.isEnabled() && newFilter.isEnabled()) {
                        boolean changed = !Objects.equals(previousFilter.getCpuRanges(), newFilter.getCpuRanges()) || previousFilter.isCpuRangesBased() != newFilter.isCpuRangesBased();
                        if (changed) {
                            refresh();
                        }
                    } else {
                        fActiveThreadsRapidToggle.setChecked(enabled);
                    }
                }
            }
        };
    }

    private IAction createHierarchicalAction() {
        IAction action = new Action(Messages.ControlFlowView_hierarchicalViewLabel, IAction.AS_RADIO_BUTTON) {
            @Override
            public void run() {
                ITmfTrace parentTrace = getTrace();
                synchronized (fFlatTraces) {
                    fFlatTraces.remove(parentTrace);
                    List<@NonNull TimeGraphEntry> entryList = getEntryList(parentTrace);
                    if (entryList != null) {
                        for (TimeGraphEntry traceEntry : entryList) {
                            Collection<TimeGraphEntry> controlFlowEntries = fEntries.row(getProvider(traceEntry)).values();
                            controlFlowEntries.forEach(e -> e.setParent(null));
                            addEntriesToHierarchicalTree(controlFlowEntries, traceEntry);
                        }
                    }
                }
                refresh();
            }
        };
        action.setChecked(true);
        action.setToolTipText(Messages.ControlFlowView_hierarchicalViewToolTip);
        return action;
    }

    private IAction createFlatAction() {
        IAction action = new Action(Messages.ControlFlowView_flatViewLabel, IAction.AS_RADIO_BUTTON) {
            @Override
            public void run() {
                applyFlatPresentation();
                refresh();
            }
        };
        action.setChecked(true);
        action.setToolTipText(Messages.ControlFlowView_flatViewToolTip);
        return action;
    }

    private void applyFlatPresentation() {
        ITmfTrace parentTrace = getTrace();
        synchronized (fFlatTraces) {
            fFlatTraces.add(parentTrace);
            List<@NonNull TimeGraphEntry> entryList = getEntryList(parentTrace);
            if (entryList != null) {
                for (TimeGraphEntry traceEntry : entryList) {
                    Collection<TimeGraphEntry> entries = fEntries.row(getProvider(traceEntry)).values();
                    addEntriesToFlatTree(entries, traceEntry);
                }
            }
        }
    }

    @Override
    protected String getNextText() {
        return Messages.ControlFlowView_nextProcessActionNameText;
    }

    @Override
    protected String getNextTooltip() {
        return Messages.ControlFlowView_nextProcessActionToolTipText;
    }

    @Override
    protected String getPrevText() {
        return Messages.ControlFlowView_previousProcessActionNameText;
    }

    @Override
    protected String getPrevTooltip() {
        return Messages.ControlFlowView_previousProcessActionToolTipText;
    }

    /**
     * Get the optimization function for the scheduling column. In the base
     * implementation, this optimizes by Line arrows, but can be overidden.
     * <p>
     * It takes a collection of link events, looking at the entries being
     * linked, and returns a list of the proposed order. The list of indexes
     * should be in ascending order. There can be duplicates, but the values and
     * order should always be the same for the same input.
     *
     * @return the returned column order, where the integer is the tid of the
     *         entry, and the return value is the position, there can be
     *         duplicates.
     */
    public Function<Collection<ILinkEvent>, Map<Integer, Long>> getUpdatedSchedulingColumn() {
        return UPDATE_SCHEDULING_COLUMN_ALGO;
    }

    /**
     * This is an optimization action used to find cliques of entries due to
     * links and put them closer together
     *
     * @author Samuel Gagnon
     */
    private final class OptimizationAction extends Action {

        @Override
        public void runWithEvent(Event event) {
            ITmfTrace parentTrace = getTrace();
            if (parentTrace == null) {
                return;
            }

            createFlatAction().run();

            /*
             * This method only returns the arrows in the current time interval
             * [a,b] of ControlFlowView. Thus, we only optimize for that time
             * interval
             */
            List<ILinkEvent> arrows = getTimeGraphViewer().getTimeGraphControl().getArrows();
            List<TimeGraphEntry> currentList = getEntryList(parentTrace);
            if (currentList == null) {
                return;
            }

            Map<Integer, Long> orderedTidMap = getUpdatedSchedulingColumn().apply(arrows);

            /*
             * Now that we have our list of ordered tid, it's time to assign a position for
             * each threads in the view. For this, we assign a value to an invisible column
             * and sort according to the values in this column.
             */
            synchronized (fEntries) {
                for (TimeGraphEntry entry : currentList) {
                    Collection<TimeGraphEntry> controlFlowEntries = fEntries.row(getProvider(entry)).values();
                    for (TimeGraphEntry child : controlFlowEntries) {
                        /*
                         * If the thread is in our list, we give it a position. Otherwise, it means
                         * there's no activity in the current interval for that thread. We set its
                         * position to Long.MAX_VALUE so it goes to the bottom.
                         */
                        ControlFlowEntry controlFlowEntry = (ControlFlowEntry) child;
                        controlFlowEntry.setSchedulingPosition(orderedTidMap.getOrDefault(controlFlowEntry.getThreadId(), Long.MAX_VALUE));
                    }
                }
            }

            setEntryComparator(ControlFlowColumnComparators.SCHEDULING_COLUMN_COMPARATOR);
            refresh();
        }

    }

    /**
     * @author gbastien
     *
     */
    protected static class ControlFlowTreeLabelProvider extends TreeLabelProvider {

        @Override
        public String getColumnText(Object element, int columnIndex) {
            if (columnIndex == 0 && element instanceof TimeGraphEntry) {
                return ((TimeGraphEntry) element).getName();
            }

            if (element instanceof ControlFlowEntry) {
                ControlFlowEntry entry = (ControlFlowEntry) element;

                if (COLUMN_NAMES[columnIndex].equals(Messages.ControlFlowView_tidColumn)) {
                    return Integer.toString(entry.getThreadId());
                } else if (COLUMN_NAMES[columnIndex].equals(Messages.ControlFlowView_ptidColumn)) {
                    if (entry.getParentThreadId() > 0) {
                        return Integer.toString(entry.getParentThreadId());
                    }
                } else if (COLUMN_NAMES[columnIndex].equals(Messages.ControlFlowView_birthTimeColumn)) {
                    return FormatTimeUtils.formatTime(entry.getStartTime(), TimeFormat.CALENDAR, Resolution.NANOSEC);
                } else if (COLUMN_NAMES[columnIndex].equals(Messages.ControlFlowView_traceColumn)) {
                    return getTrace(entry).getName();
                } else if (COLUMN_NAMES[columnIndex].equals(INVISIBLE_COLUMN)) {
                    return Long.toString(entry.getSchedulingPosition());
                }
            }
            return ""; //$NON-NLS-1$
        }

    }

    private static class ControlFlowFilterLabelProvider extends TreeLabelProvider {

        @Override
        public String getColumnText(Object element, int columnIndex) {
            if (columnIndex == 0 && element instanceof TimeGraphEntry) {
                return ((TimeGraphEntry) element).getName();
            } else if (columnIndex == 1 && element instanceof ControlFlowEntry) {
                return Integer.toString(((ControlFlowEntry) element).getThreadId());
            }
            return ""; //$NON-NLS-1$
        }
    }

    @TmfSignalHandler
    @Override
    public void traceClosed(TmfTraceClosedSignal signal) {
        ITmfTrace parentTrace = signal.getTrace();
        super.traceClosed(signal);
        synchronized (fFlatTraces) {
            fFlatTraces.remove(parentTrace);
        }
    }

    @TmfSignalHandler
    @Override
    public void traceSelected(TmfTraceSelectedSignal signal) {
        super.traceSelected(signal);

        /* Update the Flat and Hierarchical actions */
        synchronized (fFlatTraces) {
            if (fFlatTraces.contains(signal.getTrace())) {
                fHierarchicalAction.setChecked(false);
                fFlatAction.setChecked(true);
            } else {
                fFlatAction.setChecked(false);
                fHierarchicalAction.setChecked(true);
            }
        }

        /* Update the Dynamic Filters related actions */
        ViewerFilter activeThreadFilter = null;
        ViewerFilter[] traceFilters = getFiltersMap().get(signal.getTrace());
        if (traceFilters != null) {
            activeThreadFilter = getActiveThreadsFilter(traceFilters);
        }

        if (activeThreadFilter instanceof ActiveThreadsFilter) {
            fActiveThreadsFilter = (ActiveThreadsFilter) activeThreadFilter;
        } else {
            fActiveThreadsFilter = new ActiveThreadsFilter(null, false, getTrace());
        }

        fActiveThreadsRapidToggle.setChecked(fActiveThreadsFilter.isEnabled());
    }

    // ------------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------------
    /**
     * Add entries to the traces's child list in a flat fashion (no hierarchy).
     */
    private static void addEntriesToFlatTree(Collection<@NonNull TimeGraphEntry> entries, TimeGraphEntry traceEntry) {
        traceEntry.clearChildren();
        for (TimeGraphEntry e : entries) {
            // reset the entries
            e.setParent(null);
            e.clearChildren();
            traceEntry.addChild(e);
        }
    }

    /**
     * Add entries to the trace's child list in a hierarchical fashion.
     */
    private static void addEntriesToHierarchicalTree(Iterable<TimeGraphEntry> entryList, TimeGraphEntry traceEntry) {
        traceEntry.clearChildren();
        Map<Long, TimeGraphEntry> map = Maps.uniqueIndex(entryList, entry -> entry.getModel().getId());
        for (TimeGraphEntry e : entryList) {
            // reset children tree prior to rebuild
            e.clearChildren();
            e.setParent(null);
        }
        for (TimeGraphEntry entry : entryList) {
            TimeGraphEntry parent = map.get(entry.getModel().getParentId());
            /*
             * Associate the parent entry only if their time overlap. A child entry may
             * start before its parent, for example at the beginning of the trace if a
             * parent has not yet appeared in the state system. We just want to make sure
             * that the entry didn't start after the parent ended or ended before the parent
             * started.
             */
            if (parent != null) {
                parent.addChild(entry);
            } else {
                traceEntry.addChild(entry);
            }
        }
    }

    @Override
    protected void synchingToTime(long time) {
        List<TimeGraphEntry> traceEntries = getEntryList(getTrace());
        if (traceEntries == null) {
            return;
        }
        for (TraceEntry traceEntry : Iterables.filter(traceEntries, TraceEntry.class)) {
            Iterable<TimeGraphEntry> unfiltered = Utils.flatten(traceEntry);
            Map<Long, TimeGraphEntry> map = Maps.uniqueIndex(unfiltered, e -> e.getModel().getId());
            // use time -1 as a lower bound for the end of Time events to be included.
            SelectionTimeQueryFilter filter = new SelectionTimeQueryFilter(time - 1, time, 2, map.keySet());
            TmfModelResponse<@NonNull List<@NonNull ITimeGraphRowModel>> response = traceEntry.getProvider().fetchRowModel(filter, null);
            List<@NonNull ITimeGraphRowModel> model = response.getModel();
            if (model == null) {
                continue;
            }
            for (ITimeGraphRowModel row : model) {
                if (syncToRow(row, time, map)) {
                    return;
                }
            }
        }
    }

    private boolean syncToRow(ITimeGraphRowModel rowModel, long time, Map<Long, TimeGraphEntry> entryMap) {
        long id = rowModel.getEntryID();
        List<@NonNull ITimeGraphState> list = rowModel.getStates();
        if (list.isEmpty()) {
            return false;
        }
        ITimeGraphState event = list.get(0);
        if (event.getStartTime() + event.getDuration() <= time && list.size() > 1) {
            /*
             * get the second time graph state as passing time - 1 as a first argument to
             * the filter will get the previous state, if time is the beginning of an event
             */
            event = list.get(1);
        }

        if (time == event.getStartTime()) {
            TimeGraphEntry entry = entryMap.get(id);
            if (entry != null) {
                getTimeGraphViewer().setSelection(entry, true);
                return true;
            }
        }
        return false;
    }

    private static ActiveThreadsFilter getActiveThreadsFilter(ViewerFilter[] filters) {
        for (ViewerFilter viewerFilter : filters) {
            if ((viewerFilter instanceof ActiveThreadsFilter)) {
                return (ActiveThreadsFilter) viewerFilter;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------
    // Static fields
    // ------------------------------------------------------------------------

    /**
     * Maps containing new entry for the cgroups.
     */
    private static Map<CgroupEntryModel, TimeGraphEntry> createdCgroupMap = new HashMap<>();

    /**
     * Atomic index to add entires for the Cgroup.
     */
    private static AtomicLong fAtomic;

    private CgroupDataProvider fCgroupProvider;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    // ------------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------------

    /**
     * Get the {@link ITimeGraphDataProvider} from a {@link TimeGraphEntry}'s
     * parent.
     *
     * @param entry
     *            queried {@link TimeGraphEntry}.
     * @return the {@link ITimeGraphDataProvider}
     * @since 3.3
     */
    public static ITimeGraphDataProvider<? extends TimeGraphEntryModel> getProvider(TimeGraphEntry entry) {
        if (entry instanceof RunningPartitionEntry) { //$NON-NLS-1$
            RunningPartitionEntry e = (RunningPartitionEntry)entry;
            return e.getProvider();
        }
        return getTraceEntry(entry).getProvider();
    }

    private static TraceEntry getTraceEntry(TimeGraphEntry entry) {
        ITimeGraphEntry parent = entry;
        while (parent != null) {
            if (parent instanceof TraceEntry) {
                return ((TraceEntry) parent);
            }
            parent = parent.getParent();
        }
        throw new IllegalStateException(entry + " should have a TraceEntry parent"); //$NON-NLS-1$
    }


    private void zoomEntries(Map<Long, TimeGraphEntry> map, List<ITimeGraphRowModel> model, boolean completed, Sampling sampling) {
        boolean isZoomThread = Thread.currentThread() instanceof ZoomThread;
        for (ITimeGraphRowModel rowModel : model) {
            TimeGraphEntry entry;
            synchronized (fEntries) {
                entry = map.get(rowModel.getEntryID());
            }

            if (entry != null) {
                List<ITimeEvent> events = createTimeEvents(entry, rowModel.getStates());
                if (isZoomThread) {
                    applyResults(() -> {
                        entry.setZoomedEventList(events);
                        if (completed) {
                            entry.setSampling(sampling);
                        }
                    });
                } else {
                    entry.setEventList(events);
                }
            }
        }
    }

    /**
     * Filter the entries to return only the Non Null {@link TimeGraphEntry} which
     * intersect the time range.
     *
     * @param visible
     *            the input list of visible entries
     * @param zoomStartTime
     *            the leftmost time bound of the view
     * @param zoomEndTime
     *            the rightmost time bound of the view
     * @return A Multimap of data providers to their visible entries' model IDs.
     */
    private static Multimap<ITimeGraphDataProvider<? extends TimeGraphEntryModel>, Long> filterGroupEntries(Iterable<TimeGraphEntry> visible,
            long zoomStartTime, long zoomEndTime) {
        Multimap<ITimeGraphDataProvider<? extends TimeGraphEntryModel>, Long> providersToModelIds = HashMultimap.create();
        for (TimeGraphEntry entry : visible) {
            if (zoomStartTime <= entry.getEndTime() && zoomEndTime >= entry.getStartTime() && entry.hasTimeEvents()) {
                ITimeGraphDataProvider<? extends TimeGraphEntryModel> provider = getProvider(entry);
                if (provider != null) {
                    providersToModelIds.put(provider, entry.getModel().getId());
                }
            }
            if (entry instanceof RunningPartitionEntry) {
                ITimeGraphDataProvider<? extends TimeGraphEntryModel> provider = getProvider(entry);
                if (provider != null) {
                    providersToModelIds.put(provider, entry.getModel().getId());
                }
            }
        }
        return providersToModelIds;
    }

    @Override
    protected void zoomEntries(@NonNull Iterable<@NonNull TimeGraphEntry> entries, long zoomStartTime, long zoomEndTime, long resolution, @NonNull IProgressMonitor monitor) {
        if (resolution < 0) {
            // StateSystemUtils.getTimes would throw an illegal argument exception.
            return;
        }

        long start = Long.min(zoomStartTime, zoomEndTime);
        long end = Long.max(zoomStartTime, zoomEndTime);
        List<@NonNull Long> times = StateSystemUtils.getTimes(start, end, resolution);
        Sampling sampling = new Sampling(start, end, resolution);
        Multimap<ITimeGraphDataProvider<? extends TimeGraphEntryModel>, Long> providersToModelIds = filterGroupEntries(entries, zoomStartTime, zoomEndTime);
        SubMonitor subMonitor = SubMonitor.convert(monitor, getClass().getSimpleName() + "#zoomEntries", providersToModelIds.size()); //$NON-NLS-1$

        for (Entry<ITimeGraphDataProvider<? extends TimeGraphEntryModel>, Collection<Long>> entry : providersToModelIds.asMap().entrySet()) {
            ITimeGraphDataProvider<? extends TimeGraphEntryModel> dataProvider = entry.getKey();
            TimeGraphStateQueryFilter filter = new TimeGraphStateQueryFilter(times, entry.getValue(), getRegexes());
            TmfModelResponse<List<ITimeGraphRowModel>> response = dataProvider.fetchRowModel(filter, monitor);

            List<ITimeGraphRowModel> model = response.getModel();
            if (model != null) {
                zoomEntries(fEntries.row(dataProvider), model, response.getStatus() == ITmfResponse.Status.COMPLETED, sampling);
            }
            subMonitor.worked(1);
        }
    }

    @Override
    protected void buildEntryList(final ITmfTrace trace, final ITmfTrace parentTrace, final IProgressMonitor monitor) {
        ThreadStatusDataProvider dataProvider = DataProviderManager.getInstance()
                .getDataProvider(trace, ThreadStatusDataProvider.ID, ThreadStatusDataProvider.class);

        // We only want to this analysis kernel traces under a virtual machine experiment,
        // where we can have information about the cgroups via ust events and the thread status via kernel trace.
        if (! (parentTrace instanceof TmfExperiment)) {
            return;
        }

        CgroupDataProvider cgroupDataProvider = DataProviderManager.getInstance()
                .getDataProvider(parentTrace, CgroupDataProvider.ID, CgroupDataProvider.class);
        fCgroupProvider = cgroupDataProvider;
        if (dataProvider == null || cgroupDataProvider == null) {
            return;
        }

        createdCgroupMap.clear();

        boolean complete = false;
        TraceEntry traceEntry = null;
        while (!complete && !monitor.isCanceled()) {
            TmfModelResponse<List<ThreadEntryModel>> response = dataProvider.fetchTree(new TimeQueryFilter(0, Long.MAX_VALUE, 2), monitor);
            if (response.getStatus() == ITmfResponse.Status.FAILED) {
                Activator.getDefault().logError("Thread Status Data Provider failed: " + response.getStatusMessage()); //$NON-NLS-1$
                return;
            } else if (response.getStatus() == ITmfResponse.Status.CANCELLED) {
                return;
            }
            complete = response.getStatus() == ITmfResponse.Status.COMPLETED;

            TmfModelResponse<List<CgroupEntryModel>> cgroupResponse = cgroupDataProvider.fetchTree(new TimeQueryFilter(0, Long.MAX_VALUE, 2), monitor);

            List<ThreadEntryModel> model = response.getModel();
            List<CgroupEntryModel> cgroupModel = cgroupResponse.getModel();
            if (model != null && cgroupModel != null) {
                synchronized (fEntries) {
                    for (ThreadEntryModel entry : model) {
                        if (entry.getThreadId() != Integer.MIN_VALUE) {
                            if (traceEntry == null) {
                                break;
                            }
                            TimeGraphEntry e = fEntries.get(traceEntry.getProvider(), entry.getId());
                            if (e != null) {
                                e.updateModel(entry);
                            } else {
                                fEntries.put(traceEntry.getProvider(), entry.getId(), new ControlFlowEntry(entry));
                            }
                        } else {
                            setStartTime(Long.min(getStartTime(), entry.getStartTime()));
                            setEndTime(Long.max(getEndTime(), entry.getEndTime() + 1));

                            if (traceEntry != null) {
                                traceEntry.updateModel(entry);
                            } else {
                                traceEntry = new TraceEntry(entry, trace, dataProvider);
                                addToEntryList(parentTrace, Collections.singletonList(traceEntry));
                            }
                        }
                    }

                    // TODO: We got to the the add the new hierachy based on cgroups
                    addEntriesInCgroupHierarchicalTree(cgroupModel, traceEntry);
                }

                Objects.requireNonNull(traceEntry, "ControlFlow tree model should have a trace entry with PID=Integer.MIN_VALUE"); //$NON-NLS-1$

                Collection<TimeGraphEntry> controlFlowEntries = fEntries.row(getProvider(traceEntry)).values();
                List<TimeGraphEntry> l = new ArrayList<>(controlFlowEntries);
                Collection<TimeGraphEntry> newEnties = fEntries.row(fCgroupProvider).values();
                l.addAll(newEnties);

                Iterable<TimeGraphEntry> entries = Iterables.filter(l, TimeGraphEntry.class);
                final long resolution = Long.max(1, (traceEntry.getEndTime() - traceEntry.getStartTime()) / getDisplayWidth());
                zoomEntries(entries, traceEntry.getStartTime(), traceEntry.getEndTime(), resolution, monitor);
            }
            if (parentTrace.equals(getTrace())) {
                refresh();
            }

            if (!complete) {
                try {
                    Thread.sleep(BUILD_UPDATE_TIMEOUT);
                } catch (InterruptedException e) {
                    Activator.getDefault().logError("Failed to wait for analysis to finish", e); //$NON-NLS-1$
                }
            }
        }
    }

    /**
     * Add entries to the trace's child list in a hierarchical fashion where processes are grouped in the cgroup
     * they belong to.
     */
    private void addEntriesInCgroupHierarchicalTree(List<CgroupEntryModel> cgroups, TraceEntry traceEntry) {
        Collection<TimeGraphEntry> entries = fEntries.row(getProvider(traceEntry)).values();
        Map<Long, TimeGraphEntry> map = Maps.uniqueIndex(entries, entry -> entry.getModel().getId());
        Boolean rootIsAdded = false;

        List<TimeGraphEntry> newEntries = new ArrayList<>();
        for (TimeGraphEntry e : entries) {
            if (!(e instanceof ControlFlowEntry)) {
                continue;
            }

            TimeGraphEntry parent = map.get(e.getModel().getParentId());
            if (parent != null) {
                parent.addChild(e);
            } else {
                /*
                 * In this case, we are a top level process that needs to be put under the correct cgroup
                 */
                ControlFlowEntry cfEntry = (ControlFlowEntry)e;
                TimeGraphEntry root = walkCgroupTreeAndCreateEntries(cgroups.get(0), cfEntry, traceEntry, newEntries);
                if (!rootIsAdded && root != null) {
                    rootIsAdded = true;
                    traceEntry.addChild(root);
                } else if (root == null) {
                    traceEntry.addChild(e);
                }
            }
        }

        int i = -1;
        RunningPartitionEntry parentRunning = null;
        for (CgroupEntryModel e : cgroups) {
            if ((e instanceof RunningCgroupEntryModel)) {
                RunningPartitionEntry test = new RunningPartitionEntry(e.getName(), fCgroupProvider, 0, 100000);
                if (e.getName().equals("RUNNING PARTITION")) {
                    parentRunning = test;
                    traceEntry.addChild(parentRunning);
                }
                if (test != parentRunning && parentRunning != null) {
                    parentRunning.addChild(test);
                }
                fEntries.put(fCgroupProvider, 0xDEADBEEFL + i, test);
                i++;
            }
        }
        for (TimeGraphEntry e : newEntries) {
            if (e != null) {
                if (e.getName() != "Running Partition") {
                    fEntries.put(traceEntry.getProvider(), fAtomic.getAndIncrement(), e);
                } /*else {
                    fEntries.put(fCgroupProvider, 0xDEADBEEFL, e);
                }*/
            }
        }
    }

    /**
     * Walks the cgroup hierarchy by looking for the node that contains the threadID.
     * As it walks the hierarchy, it creates the entries if they were not previously created.
     *
     * @param cgroup
     *              The root cgroup to start the walk from.
     * @param threadID
     *              The thread ID that we are looking for.
     * @param traceEntry
     *              The traceEntry for wich the view is built.
     * @return The last {@link TimeGraphEntry} created by the walk. This is the parent entry of the thread
     */
    private TimeGraphEntry walkCgroupTreeAndCreateEntries(CgroupEntryModel cgroup, ControlFlowEntry entry, TraceEntry traceEntry, List<TimeGraphEntry> list) {
        if (cgroup == null) {
            return null;
        }

        TimeGraphEntry currentCgroupEntry = createdCgroupMap.get(cgroup);
        if (currentCgroupEntry == null) {
            if (cgroup instanceof RunningCgroupEntryModel) {
                currentCgroupEntry = new RunningPartitionEntry("Running Partition", fCgroupProvider, 0, 100000);
            } else {
                currentCgroupEntry = new TimeGraphEntry(cgroup.getName(), 0, 100000);
            }
            createdCgroupMap.put(cgroup, currentCgroupEntry);
            list.add(currentCgroupEntry);
        }

        for (CgroupEntryModel p : cgroup.getProcessChilds()) {
            if (p.getPid() == entry.getThreadId()) {
                currentCgroupEntry.addChild(entry);
                return currentCgroupEntry;
            }
        }

        for (CgroupEntryModel c : cgroup.getCgroupChilds()) {
            TimeGraphEntry e = walkCgroupTreeAndCreateEntries(c, entry, traceEntry, list);
            if (e != null) {
                if (!currentCgroupEntry.getChildren().contains(e)) {
                    currentCgroupEntry.addChild(e);
                }
                return currentCgroupEntry;
            }
        }

        return null;
    }

    /**
     * Signal handler for a thread selected signal.
     *
     * @param signal
     *            the thread selected signal
     * @since 2.0
     */
    @TmfSignalHandler
    public void listenToCurrentThread(TmfThreadSelectedSignal signal) {
        // HostThread data = signal.getThreadId() >= 0 ? signal.getHostThread() : null;
        ITmfTrace trace = getTrace();
        if (trace == null) {
            return;
        }

        restartZoomThread();
    }
}
