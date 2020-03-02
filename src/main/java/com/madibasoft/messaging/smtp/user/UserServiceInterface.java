/*
 * 
 */
package com.madibasoft.messaging.smtp.user;

public interface UserServiceInterface {

	/**
	 * Look up the proxy email of the specified uid
	 * 
	 * @param uid
	 * @return
	 * @throws UserNotFoundException if the uid is not found
	 */
	String lookupProxyByUid(String uid) throws UserNotFoundException;

	/**
	 * This method will return the corresponding email for the specified uid
	 * 
	 * @param uid the uid of the user for which an email is required
	 * @return the email of the user with corresponding uid
	 * @throws UserNotFoundException thrown if the uid does not exist
	 */
	String lookupEmailByUid(String uid) throws UserNotFoundException;

	/**
	 * This method is called when an incoming mail is received, to decide what the
	 * corresponding uid is of the user who sent the mail. If it helps, we can add
	 * in as parameter the corresponding proxy that this email is sending a message
	 * to. !!!NEEDS PARTNER CONTEXT!!!
	 * 
	 * @param email the email of the user for which a uid is required
	 * @return the uid of the user with corresponding email
	 * @throws UserNotFoundException
	 */
	String lookupUidByEmail(String email) throws UserNotFoundException;

	/**
	 * Returns the uid of the user with the specified proxy email
	 * 
	 * @param proxy
	 * @return
	 * @throws UserNotFoundException
	 */
	String lookupUidByProxy(String proxy) throws UserNotFoundException;

	/**
	 * Returns the proxy email of the specified email !!!NEEDS PARTNER CONTEXT!!!!.
	 *
	 * @param email the email
	 * @return the string
	 * @throws UserNotFoundException the user not found exception
	 */
	String lookupProxyByEmail(String email) throws UserNotFoundException;

	/**
	 * Checks uid is valid.
	 *
	 * @param uid the uid to be validated
	 * @return true, if it is valid uid, otherwise return false, or throw an
	 *         exception
	 * @throws UserNotFoundException the user not found exception
	 */
	boolean isValidUid(String uid) throws UserNotFoundException;

}