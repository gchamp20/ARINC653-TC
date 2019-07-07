import babeltrace.writer as btw
import tempfile
import re

def to_ns(ts):
    #ts * (1 / (3570 * 10 ^ 6)) * 10 ^ 9
    return round(ts * 2.7841968984)

def create_ctf(trace):
    # temporary directory holding the CTF trace
    trace_path = tempfile.mkdtemp()

    # our writer
    writer = btw.Writer(trace_path)

    # create one default clock and register it to the writer
    clock = btw.Clock('my_clock')
    clock.description = 'this is my clock'
    #clock.frequency = 3570000000
    writer.add_clock(clock)

    # create our single stream
    streams = []
    event_types = []
    for i in range(8):
        event_types.append({})
        print("iter ", i)

        # create one default stream class and assign our clock to it
        stream_class = btw.StreamClass('stream{}'.format(i))
        stream_class.clock = clock

        # create one default event class
        event_class_cont = btw.EventClass('switch_infcont')
        event_class_prev = btw.EventClass('switch_infprev')
        event_class_next = btw.EventClass('switch_infnext')
        event_class_hypercall = btw.EventClass('hypercall')
        event_class_hypercall2 = btw.EventClass('hypercall2')
        event_class_hypercall_version = btw.EventClass('hypercall_version')
        event_class_hypercall_version_return = btw.EventClass('hypercall_version_return')
        event_class_running_runnable = btw.EventClass('running_to_runnable')
        event_class_runnable_running = btw.EventClass('runnable_to_running')
        event_class_running_block = btw.EventClass('running_to_blocked')
        event_class_blocked_runnable = btw.EventClass('blocked_to_runnable')

        # create one 32-bit signed integer field
        int32_field_decl = btw.IntegerFieldDeclaration(32)
        int32_field_decl.signed = True

        # create one 32-bit signed integer field
        int64_field_decl = btw.IntegerFieldDeclaration(64)
        int64_field_decl.signed = True

        # add this field declaration to our event class
        event_class_blocked_runnable.add_field(int32_field_decl, 'dom') 
        event_class_blocked_runnable.add_field(int32_field_decl, 'vcpu') 

        event_class_running_block.add_field(int32_field_decl, 'dom') 
        event_class_running_block.add_field(int32_field_decl, 'vcpu') 

        event_class_runnable_running.add_field(int32_field_decl, 'dom') 
        event_class_runnable_running.add_field(int32_field_decl, 'vcpu') 

        event_class_running_runnable.add_field(int32_field_decl, 'dom') 
        event_class_running_runnable.add_field(int32_field_decl, 'vcpu') 

        event_class_cont.add_field(int32_field_decl, 'dom')
        event_class_cont.add_field(int32_field_decl, 'vcpu')

        event_class_prev.add_field(int32_field_decl, 'dom')
        event_class_prev.add_field(int32_field_decl, 'vcpu')

        event_class_next.add_field(int32_field_decl, 'dom')
        event_class_next.add_field(int32_field_decl, 'vcpu')

        event_class_hypercall.add_field(int32_field_decl, 'op')
        event_class_hypercall2.add_field(int32_field_decl, 'op')

        event_class_hypercall_version.add_field(int32_field_decl, 'id')
        event_class_hypercall_version_return.add_field(int32_field_decl, 'id')

        event_types[i]['switch_infcont'] = event_class_cont
        event_types[i]['switch_infprev'] = event_class_prev
        event_types[i]['switch_infnext'] = event_class_next
        event_types[i]['hypercall'] = event_class_hypercall
        event_types[i]['hypercall2'] = event_class_hypercall2
        event_types[i]['hypercall_version'] = event_class_hypercall_version
        event_types[i]['hypercall_version_return'] = event_class_hypercall_version_return
        event_types[i]['blocked_to_runnable'] = event_class_blocked_runnable
        event_types[i]['running_to_blocked'] = event_class_running_block
        event_types[i]['runnable_to_running'] = event_class_runnable_running
        event_types[i]['running_to_runnable'] = event_class_running_runnable

        # register our event class to our stream class
        stream_class.add_event_class(event_class_cont)
        stream_class.add_event_class(event_class_prev)
        stream_class.add_event_class(event_class_next)
        stream_class.add_event_class(event_class_hypercall)
        stream_class.add_event_class(event_class_hypercall2)
        stream_class.add_event_class(event_class_hypercall_version)
        stream_class.add_event_class(event_class_hypercall_version_return)
        stream_class.add_event_class(event_class_running_block)
        stream_class.add_event_class(event_class_blocked_runnable)
        stream_class.add_event_class(event_class_running_runnable)
        stream_class.add_event_class(event_class_runnable_running)

        stream_class.packet_context_type.add_field(int32_field_decl, 'cpu_id')

        streams.append(writer.create_stream(stream_class))
        streams[-1].packet_context.field('cpu_id').value = i

    used_streams = set()

    done = False
    while not done:
        time = 2 ** 64
        e = None
        selected_s = None
        for s in trace:
            if len(s) > 0:
                temp = s[0]
                if temp["timestamp"] < time:
                    e = temp
                    time = e["timestamp"]
                    selected_s = s
        if e is None:
            done = True
            break
        else:
            e = selected_s.pop(0)
            
        event = None
        cpu = e["cpu"]
        if e["name"] == "switch_infcont":
            event = btw.Event(event_types[cpu]["switch_infcont"])
            event.payload('dom').value = e["dom"]
            event.payload('vcpu').value = e["vcpu"]
        elif e["name"] == "switch_infnext":
            event = btw.Event(event_types[cpu]["switch_infnext"])
            event.payload('dom').value = e["dom"]
            event.payload('vcpu').value = e["vcpu"]
        elif e["name"] == "switch_infprev":
            event = btw.Event(event_types[cpu]["switch_infprev"])
            event.payload('dom').value = e["dom"]
            event.payload('vcpu').value = e["vcpu"]

        elif e["name"] == "blocked_to_runnable":
            event = btw.Event(event_types[cpu]["blocked_to_runnable"])
            event.payload('dom').value = e["dom"]
            event.payload('vcpu').value = e["vcpu"]
        elif e["name"] == "running_to_blocked":
            event = btw.Event(event_types[cpu]["running_to_blocked"])
            event.payload('dom').value = e["dom"]
            event.payload('vcpu').value = e["vcpu"]
        elif e["name"] == "runnable_to_running":
            event = btw.Event(event_types[cpu]["runnable_to_running"])
            event.payload('dom').value = e["dom"]
            event.payload('vcpu').value = e["vcpu"]
        elif e["name"] == "running_to_runnable":
            event = btw.Event(event_types[cpu]["running_to_runnable"])
            event.payload('dom').value = e["dom"]
            event.payload('vcpu').value = e["vcpu"]

        elif e["name"] == "hypercall_op":
            class_ = event_types[cpu]["hypercall"]
            event = btw.Event(event_types[cpu]["hypercall"])
            #print("cpu={}' class={} ev={}".format(cpu, class_, event))
            event.payload('op').value = e["op"]
        elif e["name"] == "hypercall_op2":
            event = btw.Event(event_types[cpu]["hypercall2"])
            event.payload('op').value = e["op"]
        elif e["name"] == "hypercall_version":
            event = btw.Event(event_types[cpu]["hypercall_version"])
            event.payload('id').value = e["id"]

        used_streams.add(cpu)
        stream = streams[cpu]
        if event != None:
            clock.time = to_ns(e["timestamp"])
            stream.append_event(event)
            if e["name"] == "hypercall_version":
                event = btw.Event(event_types[cpu]["hypercall_version_return"])
                event.payload('id').value = e["id"]
                stream.append_event(event)

    # flush the stream
    print('trace path: {}'.format(trace_path))
    for s in used_streams:
        streams[s].flush()

STATE_REGEX = r"CPU(\d)\W*(\d*)\w\s\(\+\s*\d*\)\s*(running_to_blocked|runnable_to_running|running_to_runnable|running_to_blocked|blocked_to_runnable)\s*\[ (dom):vcpu=(0x[0-9a-zA-Z]*).*"
EV_REGEX = r"CPU(\d)\W*(\d*)\w\s\(\+\s*\d*\)\s*(switch\_\w*)\s*\[ (dom|new_dom):vcpu = (0x[0-9a-zA-Z]*).*"
HYPERCALL_EV_REGEX = r"CPU(\d)\W*(\d*)\w\s\(\+\s*\d*\)\s*(hyper\w*)  \[ op = (0x[0-9a-zA-Z]*).*"
HYPERCALL_VERSION_EV_REGEX = r"CPU(\d)\W*(\d*)\w\s\(\+\s*\d*\)\s*(hyper\w*)  \[ op = (0x[0-9a-zA-Z]*) a1 = (0x[0-9a-zA-Z]*).*"
def split_lines(f):
    lines = f.readlines()
    trace = [[] for e in range(8)]
    for l in lines:
        if re.match(EV_REGEX, l):
            match = re.search(EV_REGEX, l)
            new_dom_vcpu = match.group(5)
            new_dom = int(new_dom_vcpu[4:6], 16)
            vcpu = int(new_dom_vcpu[8:], 16)
            cpu = int(match.group(1))
            trace[cpu].append(
                {
                    "name": match.group(3),
                    "timestamp": int(match.group(2)),
                    "dom": new_dom,
                    "vcpu": vcpu,
                    "cpu": cpu,
                }
            )
        elif re.match(STATE_REGEX, l):
            match = re.search(STATE_REGEX, l)
            new_dom_vcpu = match.group(5)
            new_dom = int(new_dom_vcpu[4:6], 16)
            vcpu = int(new_dom_vcpu[8:], 16)
            cpu = int(match.group(1))
            trace[cpu].append(
                {
                    "name": match.group(3),
                    "timestamp": int(match.group(2)),
                    "dom": new_dom,
                    "vcpu": vcpu,
                    "cpu": cpu,
                }
            )
        elif re.match(HYPERCALL_EV_REGEX, l):
            match = re.search(HYPERCALL_EV_REGEX, l)
            cpu = int(match.group(1))
            op = int(match.group(4), 16)
            if op == 0x11:
                match = re.search(HYPERCALL_VERSION_EV_REGEX, l)
                trace[cpu].append(
                    {
                        "name": "hypercall_version",
                        "timestamp": int(match.group(2)),
                        "id": int(match.group(5), 16),
                        "cpu": cpu,
                    }
                )
            else:
                trace[cpu].append(
                    {
                        "name": match.group(3),
                        "timestamp": int(match.group(2)),
                        "op": int(match.group(4), 16),
                        "cpu": cpu,
                    }
                )
    return trace

with open("trace.txt") as f:
    trace = split_lines(f)
    #print(trace)
    create_ctf(trace)
