# ARINC 653 Trace Compass Plugin

The Trace Compass plugin contained in this project analyzes traces from space and time partitioning operating systems. To compile this plugin, refer to the [Trace Compass Developper Environement Setup Guide](https://wiki.eclipse.org/Trace_Compass/Development_Environment_Setup).␠

To generate traces compatible with the plugin, read the sections below.

## Xen

### Creating traces
1. Install and setup Xen domains.
2. Launch the xen-sync program in each domain.
3. Start LTTng in the domains.
4. Start Xentrace in the dom0.
5. Stop tracing in all the domains.
6. Convert the Xentrace trace with [this script](https://github.com/gchamp20/ARINC653-TC/blob/master/scripts/convert_to_ctf.py).

### Analyzing Traces
1. Create an experiment with the CTF traces from Xentrace and the domain (see traces folder),
2. Select Virtual Machine Experiment Type.
3. Synchronize the traces (right click on experiment -> synchronize).
4. Open the "Xen Partition View"

## Linux cgroup

### Creating traces

1. Install a linux version with CONFIG_RT_GROUP_SCHED turned on.
2. Create and configure CPU cgroup with [cgcreate](https://linux.die.net/man/1/cgcreate).
3. Launch the workload per partitions with [cgexec](https://linux.die.net/man/1/cgexec)
4. Start LTTng with kernel + userspace tracing and launch [container-state-dumper](https://github.com/loicgelle/container-state-dumper)
5. Stop tracing.

### Analyzing Traces

1. Create an experiment with the CTF traces with userspace event and kernel events (see traces folder),
2. Select Virtual Machine Experiment Type.
3. Synchronize the traces (right click on experiment -> synchronize).
4. Open the "Cgroup Partition View"
