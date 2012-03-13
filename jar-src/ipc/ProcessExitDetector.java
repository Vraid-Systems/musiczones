/*
 * detector class for ped_Process exit
 *
 * ped_Process is passed as constructor argument and entire thread does nothing else
 * than wait for the given ped_Process to finish and invokes the listeners
 */
package ipc;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Adrian BER
 * @see http://beradrian.wordpress.com/2008/11/03/detecting-ped_Process-exit-in-java/
 */
public class ProcessExitDetector extends Thread {

    /** The Process for which we have to detect the end. */
    private Process ped_Process;
    /** The associated listeners to be invoked at the end of the Process. */
    private List<ProcessListener> ped_ProcessListenerList = new ArrayList<ProcessListener>();

    /**
     * Starts the detection for the given Process
     * @param theProcess Process for which we have to detect when it is finished
     */
    public ProcessExitDetector(Process theProcess) {
        try {
            // test if the Process is finished
            theProcess.exitValue();
            throw new IllegalArgumentException("The process is already ended");
        } catch (IllegalThreadStateException exc) {
            this.ped_Process = theProcess;
        }
    }

    /** @return the Process that it is watched by this detector. */
    public Process getProcess() {
        return ped_Process;
    }

    @Override
    public void run() {
        try {
            // wait for the Process to finish
            ped_Process.waitFor();
            // invokes the listeners
            for (ProcessListener listener : ped_ProcessListenerList) {
                listener.processFinished(ped_Process);
            }
        } catch (InterruptedException ex) {
            System.err.println(ex);
        }
    }

    /** Adds a Process listener.
     * @param theProcessListener the ProcessListener to be added
     */
    public void addProcessListener(ProcessListener theProcessListener) {
        ped_ProcessListenerList.add(theProcessListener);
    }

    /** Removes a Process listener.
     * @param theProcessListener the ProcessListener to be removed
     */
    public void removeProcessListener(ProcessListener theProcessListener) {
        ped_ProcessListenerList.remove(theProcessListener);
    }
}
