package ru.kefungus.debugger;

import com.sun.jdi.*;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.event.*;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequestManager;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.sun.jdi.Bootstrap.*;


public class Debugger {
    public static void main(String[] args) throws IOException, IllegalConnectorArgumentsException, IncompatibleThreadStateException, InterruptedException, AbsentInformationException, ClassNotLoadedException, InvalidTypeException {
        VirtualMachineManager vmm = virtualMachineManager();
        AttachingConnector at = vmm.attachingConnectors()
                .stream()
                .filter(it -> "dt_socket".equalsIgnoreCase(it.transport().name()))
                .findAny()
                .orElseThrow(RuntimeException::new);

        Map<String, Connector.Argument> arguments = at.defaultArguments();
        arguments.get("port").setValue("7896");
        arguments.get("hostname").setValue("127.0.0.1");

        VirtualMachine vm = at.attach(arguments);

        EventRequestManager erm = vm.eventRequestManager();
        ClassPrepareRequest r = erm.createClassPrepareRequest();
        r.addClassFilter("ru.kefungus.joke.App");
        r.enable();

        EventQueue queue = vm.eventQueue();
        boolean active = true;
        while (active) {
            EventSet eventSet = queue.remove();
            EventIterator it = eventSet.eventIterator();
            while (it.hasNext()) {
                Event event = it.nextEvent();
                if (event instanceof ClassPrepareEvent) {
                    ClassPrepareEvent evt = (ClassPrepareEvent) event;
                    ClassType classType = (ClassType) evt.referenceType();

                    classType.methodsByName("main").forEach(m -> {
                        List<Location> locations = null;
                        try {
                            locations = m.allLineLocations();
                        } catch (AbsentInformationException ex) {
                            throw new RuntimeException(ex);
                        }
                        Location location = locations.get(3);
                        BreakpointRequest bpReq = erm.createBreakpointRequest(location);
                        bpReq.enable();
                    });

                }
                if (event instanceof BreakpointEvent) {
                    event.request().disable();

                    ThreadReference thread = ((BreakpointEvent) event).thread();
                    StackFrame stackFrame = thread.frame(0);

                    Map<LocalVariable, Value> visibleVariables = stackFrame.getValues(stackFrame.visibleVariables());
                    for (Map.Entry<LocalVariable, Value> entry : visibleVariables.entrySet()) {
                        if (entry.getKey().name().equalsIgnoreCase("message")) {
                            stackFrame.setValue(entry.getKey(), vm.mirrorOf("Алексей Евгеньевич, доставьте макс балл за 2 лабку, пожалуйста ^_^"));
                        }
                    }
                    active = false;
                }
                vm.resume();
            }
        }
    }
}
