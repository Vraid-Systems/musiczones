/*
 * interface for custom event listener that is triggered on process events
 */
package ipc;

import java.util.EventListener;

/**
 * @author Adrian BER
 * @see http://beradrian.wordpress.com/2008/11/03/detecting-process-exit-in-java/
 */
public interface ProcessListener extends EventListener {

    /**
     * triggered when a Process finishes
     * @param theProcess Process
     */
    void processFinished(Process theProcess);
}
