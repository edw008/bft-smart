/**
 * Copyright (c) 2007-2009 Alysson Bessani, Eduardo Alchieri, Paulo Sousa, and the authors indicated in the @author tags
 * 
 * This file is part of SMaRt.
 * 
 * SMaRt is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * SMaRt is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with SMaRt.  If not, see <http://www.gnu.org/licenses/>.
 */

package navigators.smart.statemanagment;

/**
 * This classes serves as a log for the state associated with the last checkpoint, and the message
 * batches received since the same checkpoint until the present. The state associated with the last
 * checkpoint together with all the batches of messages received so far, comprises this replica
 * current state
 * 
 * @author Jo�o Sousa
 */
public class StateLog {

    private BatchInfo[] messageBatches; // batches received since the last checkpoint.
    private int lastCheckpointEid; // Execution ID for the last checkpoint
    private byte[] state; // State associated with the last checkpoint
    private int position; // next position in the array of batches to be written
    private int lastEid; // Execution ID for the last messages batch delivered to the application

    /**
     * Constructs a State log
     * @param k The chekpoint period
     */
    public StateLog(int k) {

        this.messageBatches = new BatchInfo[k - 1];
        this.lastCheckpointEid = -1;
        this.state = null;
        this.position = 0;
        this.lastEid = -1;
    }
    
    /**
     * Sets the state associated with the last checkpoint, and updates the execution ID associated with it
     * @param state State associated with the last checkpoint
     */
    public void newCheckpoint(byte[] state) {

        for (int i = 0; i < this.messageBatches.length; i++)
            messageBatches[i] = null;

        position = 0;
        this.state = state;

    }

    /**
     * Retrieves the execution ID for the last messages batch delivered to the application
     * @return Execution ID for the last messages batch delivered to the application, or -1 if none was delivered
     */
    public void setLastCheckpointEid(int lastCheckpointEid) {

        this.lastCheckpointEid = lastCheckpointEid;
    }

    /**
     * Retrieves the execution ID for the last messages batch delivered to the application
     * @return Execution ID for the last messages batch delivered to the application, or -1 if none was delivered
     */
    public int getLastCheckpointEid() {
        
        return lastCheckpointEid ;
    }

    /**
     * Sets the execution ID for the last messages batch delivered to the application
     * @param lastEid the execution ID for the last messages batch delivered to the application
     */
    public void setLastEid(int lastEid) {

        this.lastEid = lastEid;
    }

    /**
     * Retrieves the execution ID for the last messages batch delivered to the application
     * @return Execution ID for the last messages batch delivered to the application
     */
    public int getLastEid() {

        return lastEid;
    }

    /**
     * Retrieves the state associated with the last checkpoint
     * @return State associated with the last checkpoint
     */
    public byte[] getState() {
        return state;
    }

    /**
     * Adds a message batch to the log. This batches should be added to the log
     * in the same order in which they are delivered to the application. Only
     * the 'k' batches received after the last checkpoint are supposed to be kept
     * @param batch The batch of messages to be kept.
     * @return True if the batch was added to the log, false otherwise
     */
    public void addMessageBatch(byte[] batch, int round, int leader) {

        if (position < messageBatches.length) {

            messageBatches[position] = new BatchInfo(batch, round, leader);
            position++;
        }
    }

    /**
     * Returns a batch of messages, given its correspondent execution ID
     * @param eid Execution ID associated with the batch to be fetched
     * @return The batch of messages associated with the batch correspondent execution ID
     */
    public BatchInfo getMessageBatch(int eid) {
        if (eid > lastCheckpointEid && eid <= lastEid) {
            return messageBatches[eid - lastCheckpointEid - 1];
        }
        else return null;
    }

    /**
     * Retrieves all the stored batches kept since the last checkpoint
     * @return All the stored batches kept since the last checkpoint
     */
    public BatchInfo[] getMessageBatches() {
        return messageBatches;
    }

    /**
     * Retrieves the total number of stored batches kept since the last checkpoint
     * @return The total number of stored batches kept since the last checkpoint
     */
    public int getNumBatches() {
        return position;
    }
    /**
     * Constructs a TransferableState using this log information
     * @param eid Execution ID correspondent to desired state
     * @return TransferableState Object containing this log information
     */
    public TransferableState getTransferableState(int eid) {

        if (lastCheckpointEid > -1 && eid >= lastCheckpointEid) {

            BatchInfo[] batches = null;

             if  (eid <= lastEid) {
                int size = eid - lastCheckpointEid ;
            
                if (size > 0) {
                    batches = new BatchInfo[size];

                    for (int i = 0; i < size; i++)
                        batches[i] = messageBatches[i];
                }
             } else if (lastEid > -1) {

                    batches = messageBatches;
             }
            return new TransferableState(batches, lastCheckpointEid, eid, state);

        }
        else return null;
    }

    /**
     * Updates this log, according to the information contained in the TransferableState object
     * @param transState TransferableState object containing the information which is used to updated this log
     */
    public void update(TransferableState transState) {

        position = 0;
        if (transState.getMessageBatches() != null) {
            for (int i = 0; i < transState.getMessageBatches().length; i++, position = i) {
                this.messageBatches[i] = transState.getMessageBatches()[i];
            }
        }

        this.lastCheckpointEid = transState.getLastCheckpointEid();

        this.state = transState.getState();

        this.lastEid = transState.getLastEid();
    }

}