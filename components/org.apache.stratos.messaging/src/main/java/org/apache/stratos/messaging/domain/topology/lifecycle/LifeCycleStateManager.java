/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.messaging.domain.topology.lifecycle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.event.topology.TopologyEvent;

import java.io.Serializable;
import java.util.Stack;

public class LifeCycleStateManager<T extends LifeCycleState> implements Serializable {

    private static Log log = LogFactory.getLog(LifeCycleStateManager.class);

    private Stack<T> stateStack;

    // a unique id which points to the relevant topology member
    // ex.: member id in a Member
    private String identifier;

    public LifeCycleStateManager(T initialState, String identifier) {

        this.identifier = identifier;
        stateStack = new Stack<T>();
        stateStack.push(initialState);
        log.info("Life Cycle State Manager started for Element [ " + identifier +
                " ], initial state: " + initialState.toString());
    }

    /**
     * checks if any conditions that should be met for the state transfer is valid
     *
     * @param nextState possible next state for the topology element
     * @param topologyEvent relevant ToplogyEvent
     * @param <S> subclass of Topology event
     * @return true if preconditions are valid and satisfied, else false
     */
    public <S extends TopologyEvent> boolean isPreConditionsValid (T nextState, S topologyEvent) {
        // TODO: implement
        return true;
    }

    /**
     * Checks if the state transition is valid
     *
     * @param nextState possible next state for the topology element
     * @return true if transitioning for nextState from current state is valid, else false
     */
    public boolean isStateTransitionValid (T nextState) {

        return stateStack.peek().getNextStates().contains(nextState);
    }

    /**
     * Changes the current state to nextState, if nextState is not as same as the current state
     *
     * @param nextState the next state to change
     * @return true if current state changed to nextState, else false.
     *         Returns false if a state transitions to the same state (currentState = nextState).
     */
    public synchronized boolean changeState (T nextState)  {

        boolean stateChanged = false;

        if (getCurrentState() != nextState) {
            stateStack.push(nextState);
            stateChanged = true;
            log.info("Element [" + identifier + "]'s lifecycle state changed from [" +
                    getPreviousState() + "] to [" + getCurrentState() + "]");
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Element [" + identifier +"]'s lifecycle state has been " +
                        "already updated to [" + nextState + "]");
            }
        }
        if (log.isDebugEnabled()) {
            printStateTransitions(stateStack, identifier);
        }

        return stateChanged;
    }

    /**
     * Get all the states this element has gone through
     *
     * @return Stack of states
     */
    public Stack<T> getStateStack () {
        return stateStack;
    }

    /**
     * Get the current state
     *
     * @return the current state
     */
    public T getCurrentState () {
        return stateStack.peek();
    }

    /**
     * Retrieves the previous state
     *
     * @return previous state
     */
    public T getPreviousState () {
        return stateStack.get(stateStack.size() - 2);
    }

    /**
     * Print utility to print transitioned states
     */
    private static <T extends LifeCycleState> void printStateTransitions (Stack<T> stateStack, String id) {

        // print all transitions till now
        StringBuilder stateTransitions = new StringBuilder("Transitioned States for " + id + ":  [ START --> ");
        for (T aStateStack : stateStack) {
            stateTransitions.append(aStateStack).append(" --> ");
        }
        stateTransitions.append(" END ]");
        log.debug(stateTransitions);
    }

    public String getIdentifier() {
        return identifier;
    }
}
