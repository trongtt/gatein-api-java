/*
 * JBoss, a division of Red Hat
 * Copyright 2011, Red Hat Middleware, LLC, and individual
 * contributors as indicated by the @authors tag. See the
 * copyright.txt in the distribution for a full listing of
 * individual contributors.
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

package org.gatein.api.id;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:chris.laprun@jboss.com">Chris Laprun</a>
 * @version $Revision$
 */
public class ContextTestCase
{
   private static final String CONTAINER_COMPONENT_NAME = "containerComponent";
   private static final String PORTAL_COMPONENT_NAME = "portalComponent";
   private static final String INVOKER_COMPONENT_NAME = "invokerComponent";
   private static final String PORTLET_COMPONENT_NAME = "portletComponent";
   private static final String INSTANCE_COMPONENT_NAME = "instanceComponent";

   private static final GenericContext CONTEXT = GenericContext.builder().named("Context").withDefaultSeparator("=")
      .requiredComponent(CONTAINER_COMPONENT_NAME, Identifiable.class, Pattern.compile("container"))
      .requiredComponent(PORTAL_COMPONENT_NAME, Identifiable.class, Pattern.compile("portal"))
      .optionalComponent(INVOKER_COMPONENT_NAME, Identifiable.class, Pattern.compile(".*"))
      .optionalComponent(PORTLET_COMPONENT_NAME, Identifiable.class, Pattern.compile(".*"))
      .optionalComponent(INSTANCE_COMPONENT_NAME, Identifiable.class, Pattern.compile(".*Instance$"))
      .ignoreRemainingAfterFirstMissingOptional().build();

   @Test(expectedExceptions = IllegalStateException.class)
   public void contextBuilderBuildShouldProperlyFailOnEmptyContext()
   {
      GenericContext.builder().named("empty").build();
   }

   @Test(expectedExceptions = IllegalStateException.class)
   public void contextBuilderShouldFailOnUnspecifiedSeparatorWithSeveralComponents()
   {
      GenericContext.builder().named("several components no separator").requiredComponent("component1", Identifiable.class, Pattern.compile(".*"))
         .requiredComponent("component2", Identifiable.class, Pattern.compile(".*")).build();
   }

   @Test
   public void contextBuilderShouldWorkOnUnspecifiedSeparatorWithOnlyOneComponent()
   {
      final GenericContext context = GenericContext.builder().named("one component").requiredComponent("component1", Identifiable.class, Pattern.compile(".*")).build();
      assert context != null;
   }

   @Test
   public void simpleContext()
   {
      GenericContext context = GenericContext.builder().named("simple").withDefaultSeparator("-").requiredComponent("component", Identifiable.class, Pattern.compile(".*")).build();

      assert 0 == context.getIndexFor("component");
      assert context.isComponentRequired("component");
      context.validate("foo");
   }

   @Test(expectedExceptions = IllegalArgumentException.class)
   public void testExtraComponents()
   {
      CONTEXT.validate("container", "portal", "foo", "bar", "barInstance", "unknown");
   }

   @Test
   public void testPortletCase()
   {
      CONTEXT.validate("container", "portal");
      CONTEXT.validate("container", "portal", "foo");
      CONTEXT.validate("container", "portal", "foo", "bar");
      CONTEXT.validate("container", "portal", "foo", "bar", "barInstance");
   }

   @Test
   public void simpleHierarchicalContextShouldWork()
   {
      final GenericContext context = GenericContext.builder().named("hierarchical").withDefaultSeparator("/")
         .requiredComponent("foo", Identifiable.class, Pattern.compile(".*foo$"))
         .requiredUnboundedHierarchicalComponent("bar", Identifiable.class, Pattern.compile("^bar.*"))
         .build();
      assert context.isComponentUnboundedHierarchical("bar");
      assert context.isComponentRequired("bar");
      context.validate("foo", "bar");
      context.validate("foo", "bar", "bar");
   }

   @Test
   public void shouldProperlyWorkWhenSeparatorIsRequiredInFirstPosition()
   {
      final GenericContext context = GenericContext.builder().named("separator in first").withDefaultSeparator("/")
         .requiredComponent("root", Identifiable.class, Pattern.compile("\\w+"))
         .requiredUnboundedHierarchicalComponent("bar", Identifiable.class, Pattern.compile("\\w+"))
         .requireSeparatorInFirstPosition()
         .build();

      assert Arrays.equals(new String[]{"foo", "bar"}, context.extractComponents("/foo/bar"));

      try
      {
         context.extractComponents("foo/bar");
         assert false : "Should have failed";
      }
      catch (IllegalArgumentException iae)
      {
         // expected
      }
   }

   @Test(expectedExceptions = IllegalArgumentException.class)
   public void missingRequiredHierarchicalShouldBeDetected()
   {
      final GenericContext context = GenericContext.builder().named("missing required hierarchical").withDefaultSeparator("/")
         .requiredComponent("foo", Identifiable.class, Pattern.compile(".*foo$"))
         .requiredUnboundedHierarchicalComponent("bar", Identifiable.class, Pattern.compile("^bar.*"))
         .build();
      context.validate("foo");
   }

   @Test
   public void shouldValidateProperlyWhenThereAreRequiredComponentsAfterHierarchical()
   {
      final GenericContext context = GenericContext.builder().named("required after hierarchical").withDefaultSeparator("/")
         .requiredComponent("foo", Identifiable.class, Pattern.compile(".*foo$"))
         .requiredUnboundedHierarchicalComponent("bar", Identifiable.class, Pattern.compile("^bar.*"))
         .requiredComponent("baz", Identifiable.class, Pattern.compile("^baz\\d*"))
         .build();
      context.validate("foo", "bar", "baz");
      context.validate("foo", "bar", "bar2", "baz");
   }

   @Test
   public void parsingShouldWorkWhenSeparatorIsPeriod()
   {
      GenericContext context = GenericContext.builder().named("separator is period").withDefaultSeparator(".")
         .requiredComponent("foo", Identifiable.class, Pattern.compile(".*"))
         .requiredComponent("baz", Identifiable.class, Pattern.compile(".*"))
         .requiredComponent("bar", Identifiable.class, Pattern.compile(".*")).build();

      assert Arrays.equals(new String[]{"foo", "baz", "bar"}, context.extractComponents("foo.baz.bar"));
   }

   @Test
   public void parsingShouldWorkWhenSeparatorIsRegexp()
   {
      GenericContext context = GenericContext.builder().named("separator is regexp").withDefaultSeparator(".+")
         .requiredComponent("foo", Identifiable.class, Pattern.compile(".*"))
         .requiredComponent("baz", Identifiable.class, Pattern.compile(".*"))
         .requiredComponent("bar", Identifiable.class, Pattern.compile(".*")).build();

      assert Arrays.equals(new String[]{"foo", "baz", "bar"}, context.extractComponents("foo.+baz.+bar"));
   }
}
