package org.eclipse.tracecompass.incubator.internal.time.partitioning.ui;

import org.eclipse.osgi.util.NLS;

@SuppressWarnings("javadoc")
public class PackageMessages extends NLS {

    private static final String BUNDLE_NAME = PackageMessages.class.getPackage().getName() + ".messages"; //$NON-NLS-1$

    public static String ControlFlowView_NextEventActionName;
    public static String ControlFlowView_NextEventActionTooltip;
    public static String ControlFlowView_NextEventJobName;

    public static String ControlFlowView_PreviousEventActionName;
    public static String ControlFlowView_PreviousEventActionTooltip;
    public static String ControlFlowView_PreviousEventJobName;

    public static String ControlFlowView_DynamicFiltersActiveThreadToggleLabel;
    public static String ControlFlowView_DynamicFiltersActiveThreadToggleToolTip;
    public static String ControlFlowView_DynamicFiltersConfigureLabel;
    public static String ControlFlowView_DynamicFiltersMenuLabel;

    static {
        NLS.initializeMessages(BUNDLE_NAME, PackageMessages.class);
    }

    private PackageMessages() {
    }
}