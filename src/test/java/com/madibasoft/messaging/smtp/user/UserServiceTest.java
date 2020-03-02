package com.madibasoft.messaging.smtp.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.madibasoft.messaging.smtp.Config;

public class UserServiceTest {
	private static final Logger log = LoggerFactory.getLogger(UserServiceTest.class);

	@Test
	public void testLookup()
			throws UserNotFoundException, InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {
		UserServiceFactory.setInstance(Config.getInstance().getString(Config.MAILGUARD_USERSERVICE));
		UserServiceInterface us = UserServiceFactory.getInstance();
		assertNotNull(us.lookupEmailByUid("001"));
		assertNotNull(us.lookupEmailByUid("002"));
		assertNotNull(us.lookupEmailByUid("003"));
		assertNotNull(us.lookupEmailByUid("004"));
		assertNotNull(us.lookupEmailByUid("005"));
		try {
			us.lookupEmailByUid("444");
			fail("Expected exception");
		} catch (UserNotFoundException e) {
			log.info("Correct exception thrown");
		}
	}

	@Test
	public void lookupProxyByUid() {
		UserServiceInterface us = new UserServiceImpl();
		// no link should exist
		try {
			assertNull(us.lookupProxyByUid("98473443"));
			fail("Should not have found this user");
		} catch (UserNotFoundException ex) {
			log.info("Got correct exception");
		}
		try {
			assertNull(us.lookupEmailByUid("98473443"));
			fail("Should not have found this user");
		} catch (UserNotFoundException ex) {
			log.info("Got correct exception");
		}
	}

	@Test
	public void testUserServiceFactory()
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException, ClassNotFoundException, UserNotFoundException {
		UserServiceFactory.setInstance("com.madibasoft.messaging.smtp.user.RandomUserService");
		UserServiceInterface us = UserServiceFactory.getInstance();
		assertTrue(us instanceof RandomUserService);
		assertNotNull(us.lookupEmailByUid("fgjdfgjsdfgsdfg"));
		assertNotNull(us.lookupUidByEmail("fgjdfgjsdfgsdfg@something.com"));
	}

	@Test
	public void testDoubleEmailMappings() throws UserNotFoundException {
		UserServiceInterface us = new UserServiceImpl();
		assertEquals("005", us.lookupUidByEmail("005@here.com"));
		assertEquals("005", us.lookupUidByEmail("005@heredouble.com"));
	}

	@Test
	public void testLookups() throws UserNotFoundException {
		UserServiceInterface usList[] = { new RandomUserService(), new UserServiceImpl() };
		for (UserServiceInterface us : usList) {
			log.info("Testing {}", us.getClass().getName());
			assertEquals("001@here.com", us.lookupEmailByUid("001"));
			assertNotNull(us.lookupProxyByEmail(us.lookupEmailByUid("001")));
			assertEquals("001", us.lookupUidByEmail("001@here.com"));
			assertEquals("001", us.lookupUidByProxy("proxy.001@localhost"));
			assertEquals("proxy.001@localhost", us.lookupProxyByUid("001"));
			try {
				us.lookupProxyByEmail(null);
			} catch (UserNotFoundException enfe) {
				log.info("Correct exception caught");
			}
			try {
				us.lookupUidByEmail(null);
			} catch (UserNotFoundException enfe) {
				log.info("Correct exception caught");
			}
		}
	}
}
