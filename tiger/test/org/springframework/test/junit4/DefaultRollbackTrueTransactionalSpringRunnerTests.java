/*
 * Copyright 2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.test.junit4;

import static org.junit.Assert.assertEquals;

import javax.sql.DataSource;

import junit.framework.JUnit4TestAdapter;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.annotation.ContextConfiguration;
import org.springframework.test.annotation.TestExecutionListeners;
import org.springframework.test.annotation.TransactionConfiguration;
import org.springframework.test.context.listeners.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.listeners.DirtiesContextTestExecutionListener;
import org.springframework.test.context.listeners.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * JUnit 4 based unit test which verifies proper transactional behavior when the
 * {@link TransactionConfiguration#defaultRollback() defaultRollback} attribute
 * of the {@link TransactionConfiguration} annotation is set to <strong><code>true</code></strong>.
 * </p>
 *
 * @see TransactionConfiguration
 * @author Sam Brannen
 * @version $Revision: 1.3 $
 * @since 2.1
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@TestExecutionListeners( { DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class,
		TransactionalTestExecutionListener.class })
@TransactionConfiguration(defaultRollback = true)
public class DefaultRollbackTrueTransactionalSpringRunnerTests extends AbstractTransactionalSpringRunnerTests {

	// ------------------------------------------------------------------------|
	// --- STATIC VARIABLES ---------------------------------------------------|
	// ------------------------------------------------------------------------|

	protected static int				originalNumRows;

	protected static SimpleJdbcTemplate	simpleJdbcTemplate;

	// ------------------------------------------------------------------------|
	// --- STATIC METHODS -----------------------------------------------------|
	// ------------------------------------------------------------------------|

	// XXX Remove suite() once we've migrated to Ant 1.7 with JUnit 4 support.
	public static junit.framework.Test suite() {

		return new JUnit4TestAdapter(DefaultRollbackTrueTransactionalSpringRunnerTests.class);
	}

	@AfterClass
	public static void verifyFinalTestData() {

		assertEquals("Verifying the final number of rows in the person table after all tests.", originalNumRows,
				countRowsInPersonTable(simpleJdbcTemplate));
	}

	// ------------------------------------------------------------------------|
	// --- INSTANCE METHODS ---------------------------------------------------|
	// ------------------------------------------------------------------------|

	@Before
	public void verifyInitialTestData() {

		originalNumRows = clearPersonTable(simpleJdbcTemplate);
		assertEquals("Adding bob", 1, addPerson(simpleJdbcTemplate, BOB));
		assertEquals("Verifying the initial number of rows in the person table.", 1,
				countRowsInPersonTable(simpleJdbcTemplate));
	}

	// ------------------------------------------------------------------------|

	@Test
	@Transactional
	public void modifyTestDataWithinTransaction() {

		assertEquals("Adding jane", 1, addPerson(simpleJdbcTemplate, JANE));
		assertEquals("Adding sue", 1, addPerson(simpleJdbcTemplate, SUE));
		assertEquals("Verifying the number of rows in the person table within a transaction.", 3,
				countRowsInPersonTable(simpleJdbcTemplate));
	}

	// ------------------------------------------------------------------------|
	// --- TYPES --------------------------------------------------------------|
	// ------------------------------------------------------------------------|

	public static class DatabaseSetup {

		@Autowired
		void setDataSource(final DataSource dataSource) {

			simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
			createPersonTable(simpleJdbcTemplate);
		}
	}

	// ------------------------------------------------------------------------|

}