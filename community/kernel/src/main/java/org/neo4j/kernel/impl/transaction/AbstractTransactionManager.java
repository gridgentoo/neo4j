/**
 * Copyright (c) 2002-2012 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.transaction;

import javax.transaction.TransactionManager;

/**
 * This interface extends the TransactionManager, with the rationale that it
 * additionally provides an init method that is used for recovery and a stop
 * method for shutting down. Implementations are to hold an actual
 * TrasactionManager and forward operations to it and additionally provide an
 * implementation specific way of initializing it, ensuring tx recovery and an
 * implementation specific way of shutting down, for resource reclamation.
 *
 * @author Chris Gioran
 */
public abstract class AbstractTransactionManager implements TransactionManager
{
    /**
     * Begins the transaction manager, possibly triggering a recovery. The
     * passed xaDsManager, given the startup sequence of the neo kernel, is
     * assured to already have registered all xa resource adapters available for
     * this run, so they can be used for registration for recovery purposes.
     *
     * @param xaDsManager The XaDataSourceManager that has registered the Xa
     *            resources.
     */
    public abstract void init( XaDataSourceManager xaDsManager );

    /**
     * Stops the transaction manager, performing all implementation specific
     * cleanup.
     */
    public abstract void stop();
    
    /**
     * Prevents new transactions from being created by throwing exception in
     * beginTx and waits for all existing transactions to complete. When this method
     * returns there are no transactions active and no new transactions can be started.
     */
    public void attemptWaitForTxCompletionAndBlockFutureTransactions( long maxWaitTimeMillis )
    {
    }
}
