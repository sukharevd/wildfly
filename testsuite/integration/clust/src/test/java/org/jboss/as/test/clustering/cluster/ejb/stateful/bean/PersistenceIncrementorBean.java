/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.clustering.cluster.ejb.stateful.bean;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;
import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.transaction.UserTransaction;

import org.jboss.ejb3.annotation.Clustered;

@Stateful
@Clustered
@TransactionManagement(TransactionManagementType.BEAN)
public class PersistenceIncrementorBean implements Incrementor {

    @Resource
    private UserTransaction transaction;

    @PersistenceUnit(unitName = "test")
    private EntityManagerFactory factory;

    @PersistenceContext(unitName = "test")
    private EntityManager manager;

    private final AtomicInteger count = new AtomicInteger(0);

    @Override
    public int increment() {
        return this.count.incrementAndGet();
    }
}
