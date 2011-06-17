/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.security;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ejb3.EJBMethodIdentifier;
import org.jboss.as.ejb3.component.EJBComponentDescription;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Holds security metadata of a method corresponding to a EJB's view.
 * <p/>
 * For security metadata that's applicable at EJB component level (for ex: security domain) take a look at {@link EJBSecurityMetaData}
 * <p/>
 * User: Jaikiran Pai
 */
public class EJBMethodSecurityMetaData {

    /**
     * True if access to the method is denied for all roles
     */
    private final boolean denyAll;

    /**
     * True if access to the method is permitted for all roles
     */
    private final boolean permitAll;

    /**
     * Set of roles allowed to access the method.
     */
    private final Set<String> rolesAllowed;

    /**
     * @param componentConfiguration The component configuration of a EJB
     * @param viewClassName          The view class name
     * @param viewMethod             The view method
     */
    public EJBMethodSecurityMetaData(final ComponentConfiguration componentConfiguration, final String viewClassName, final Method viewMethod) {
        if (componentConfiguration.getComponentDescription() instanceof EJBComponentDescription == false) {
            throw new IllegalArgumentException(componentConfiguration.getComponentName() + " is not an EJB component");
        }
        final EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) componentConfiguration.getComponentDescription();

        // process @DenyAll/exclude-list
        this.denyAll = this.isAccessDenied(componentConfiguration, viewClassName, viewMethod);
        // process @PermitAll list
        this.permitAll = this.isPermitAll(componentConfiguration, viewClassName, viewMethod);
        // if the method is marked for both @DenyAll (a.k.a exclude-list) and @PermitAll then it's an error
        if (this.denyAll && this.permitAll) {
            final Method componentMethod = this.findComponentMethod(componentConfiguration, viewMethod);
            throw new IllegalStateException("Method " + componentMethod + " for view " + viewClassName + " shouldn't be " +
                    "marked for both @PemitAll and @DenyAll at the same time");
        }
        // process @RolesAllowed/method-permission
        this.rolesAllowed = Collections.unmodifiableSet(this.getRolesAllowed(componentConfiguration, viewClassName, viewMethod));

    }

    /**
     * Returns true if access to the method corresponding to this {@link EJBMethodSecurityMetaData} is denied
     * for all roles. Else returns false
     *
     * @return
     */
    public boolean isAccessDenied() {
        return this.denyAll;
    }

    /**
     * Returns true if access to the method corresponding to this {@link EJBMethodSecurityMetaData} is permitted for
     * all roles. Else returns false.
     *
     * @return
     */
    public boolean isPermitAll() {
        return this.permitAll;
    }

    /**
     * Returns a set of roles which are allowed to access the method corresponding to this {@link EJBMethodSecurityMetaData}.
     * The returned set may be empty if there's no specific role assigned to the method.
     *
     * @return
     */
    public Set<String> getRolesAllowed() {
        return this.rolesAllowed;
    }


    private boolean isAccessDenied(final ComponentConfiguration componentConfiguration, final String viewClassName, final Method viewMethod) {
        final EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) componentConfiguration.getComponentDescription();
        Collection<EJBMethodIdentifier> denyAllMethodsForView = ejbComponentDescription.getDenyAllMethodsForView(viewClassName);
        if (denyAllMethodsForView == null) {
            denyAllMethodsForView = Collections.emptySet();
        }
        // find the component method corresponding to this view method
        final Method componentMethod = this.findComponentMethod(componentConfiguration, viewMethod);
        final EJBMethodIdentifier ejbMethodIdentifier = EJBMethodIdentifier.fromMethod(componentMethod);
        if (denyAllMethodsForView.contains(ejbMethodIdentifier)) {
            return true;
        }
        // check on class level
        final Class<?> declaringClass = componentMethod.getDeclaringClass();
        if (ejbComponentDescription.isDenyAllApplicableToClass(viewClassName, declaringClass.getName())) {
            return true;
        }
        return false;
    }

    private boolean isPermitAll(final ComponentConfiguration componentConfiguration, final String viewClassName, final Method viewMethod) {
        final EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) componentConfiguration.getComponentDescription();
        Collection<EJBMethodIdentifier> permitAllMethodsForView = ejbComponentDescription.getPermitAllMethodsForView(viewClassName);
        if (permitAllMethodsForView == null) {
            permitAllMethodsForView = Collections.emptySet();
        }
        // find the component method corresponding to this view method
        final Method componentMethod = this.findComponentMethod(componentConfiguration, viewMethod);
        final EJBMethodIdentifier ejbMethodIdentifier = EJBMethodIdentifier.fromMethod(componentMethod);
        if (permitAllMethodsForView.contains(ejbMethodIdentifier)) {
            return true;
        }
        // check on class level

        final Class<?> declaringClass = componentMethod.getDeclaringClass();
        if (ejbComponentDescription.isPermitAllApplicableToClass(viewClassName, declaringClass.getName())) {
            return true;
        }
        return false;
    }

    private Set<String> getRolesAllowed(final ComponentConfiguration componentConfiguration, final String viewClassName, final Method viewMethod) {
        final EJBComponentDescription ejbComponentDescription = (EJBComponentDescription) componentConfiguration.getComponentDescription();
        // find the component method corresponding to this view method
        final Method componentMethod = this.findComponentMethod(componentConfiguration, viewMethod);
        final EJBMethodIdentifier ejbMethodIdentifier = EJBMethodIdentifier.fromMethod(componentMethod);
        final Set<String> rolesAllowed = ejbComponentDescription.getRolesAllowed(viewClassName, ejbMethodIdentifier);
        if (!rolesAllowed.isEmpty()) {
            return rolesAllowed;
        }
        // there were no method level @RolesAllowed, so check on class level
        final Class<?> declaringClass = componentMethod.getDeclaringClass();
        final Set<String> classLevelRolesAllowed = ejbComponentDescription.getRolesAllowedForClass(viewClassName, declaringClass.getName());
        if (!classLevelRolesAllowed.isEmpty()) {
            return classLevelRolesAllowed;
        }
        return Collections.emptySet();
    }

    private Method findComponentMethod(final ComponentConfiguration componentConfiguration, final Method viewMethod) {
        final Class<?> componentClass = componentConfiguration.getComponentClass();
        try {
            return componentClass.getMethod(viewMethod.getName(), viewMethod.getParameterTypes());
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Method named " + viewMethod.getName() + " with params " + viewMethod.getParameterTypes()
                    + " not found on component class " + componentClass);
        }

    }

}
