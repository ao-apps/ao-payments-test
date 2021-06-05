/*
 * ao-payments-test - Provider for testing software.
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2015, 2016, 2019, 2020, 2021  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of ao-payments-test.
 *
 * ao-payments-test is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ao-payments-test is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ao-payments-test.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aoapps.payments.test;

import com.aoapps.payments.AuthorizationResult;
import com.aoapps.payments.CaptureResult;
import com.aoapps.payments.CreditCard;
import com.aoapps.payments.CreditResult;
import com.aoapps.payments.MerchantServicesProvider;
import com.aoapps.payments.SaleResult;
import com.aoapps.payments.TokenizedCreditCard;
import com.aoapps.payments.Transaction;
import com.aoapps.payments.TransactionRequest;
import com.aoapps.payments.TransactionResult;
import com.aoapps.payments.VoidResult;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Random;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Provider for testing software.
 * <br>
 * Configuration parameters:
 * <ol>
 *   <li>errorChance - percentage chance of some sort of error (during storeCard, payment, capture, void)</li>
 *   <li>declineChance - percentage chance of being declined, otherwise accepted (during payment)</li>
 * </ol>
 *
 * TODO: Add AVS, CVV, and ReviewReason responses.
 * TODO: Add hold
 *
 * @author  AO Industries, Inc.
 */
public class TestMerchantServicesProvider implements MerchantServicesProvider {

	/** Shared by all instances */
	private static final Random random = new Random();

	private final String providerId;
	private final byte errorChance;
	private final byte declineChance;

	public TestMerchantServicesProvider(String providerId, byte errorChance, byte declineChance) {
		this.providerId = providerId;
		this.errorChance = errorChance;
		this.declineChance = declineChance;
	}

	/**
	 * @throws  NumberFormatException when can't parse errorChance or declineChance
	 */
	public TestMerchantServicesProvider(String providerId, String errorChance, String declineChance) throws NumberFormatException {
		this(
			providerId,
			Byte.parseByte(errorChance),
			Byte.parseByte(declineChance)
		);
	}

	@Override
	public String getProviderId() {
		return providerId;
	}

	public byte getErrorChance() {
		return errorChance;
	}

	public byte getDeclineChance() {
		return declineChance;
	}

	@Override
	public SaleResult sale(TransactionRequest transactionRequest, CreditCard creditCard) {
		// First allow for random errors
		if(random.nextInt(100)<errorChance) {
			// Random error class
			TransactionResult.CommunicationResult communicationResult;
			int randomInt = random.nextInt(3);
			switch(randomInt) {
				case 0: {
					communicationResult = TransactionResult.CommunicationResult.LOCAL_ERROR;
					break;
				}
				case 1: {
					communicationResult = TransactionResult.CommunicationResult.IO_ERROR;
					break;
				}
				case 2: {
					communicationResult = TransactionResult.CommunicationResult.GATEWAY_ERROR;
					break;
				}
				default: throw new RuntimeException("random.nextInt(3) didn't return value between 0 and 2 inclusive: "+randomInt);
			}

			// Random error code
			TransactionResult.ErrorCode[] values = TransactionResult.ErrorCode.values();
			TransactionResult.ErrorCode errorCode = values[random.nextInt(values.length)];

			return new SaleResult(
				new AuthorizationResult(
					getProviderId(),
					communicationResult,
					null,
					errorCode,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null
				),
				new CaptureResult(
					getProviderId(),
					communicationResult,
					null,
					errorCode,
					null,
					null
				)
			);
		}

		// Second allow for declines
		if(random.nextInt(100)<declineChance) {
			// Random decline reason
			AuthorizationResult.DeclineReason[] values = AuthorizationResult.DeclineReason.values();
			AuthorizationResult.DeclineReason declineReason = values[random.nextInt(values.length)];

			// Random doesn't ensure uniquiness - but this is easily implemented without persistence
			String providerUniqueId = Long.toString(Math.abs(random.nextLong()), 16).toUpperCase();

			return new SaleResult(
				new AuthorizationResult(
					getProviderId(),
					TransactionResult.CommunicationResult.SUCCESS,
					null,
					null,
					null,
					providerUniqueId,
					null,
					null,
					AuthorizationResult.ApprovalResult.DECLINED,
					null,
					declineReason,
					null,
					null,
					null,
					null,
					null,
					null,
					null
				),
				new CaptureResult(
					getProviderId(),
					TransactionResult.CommunicationResult.SUCCESS,
					null,
					null,
					null,
					providerUniqueId
				)
			);
		}

		// Simulate success

		// Random doesn't ensure uniquiness - but this is easily implemented without persistence
		String providerUniqueId = Long.toString(Math.abs(random.nextLong()), 16).toUpperCase();

		String approvalCode =
			new StringBuilder()
				.append(random.nextInt(10))
				.append(random.nextInt(10))
				.append(random.nextInt(10))
				.append(random.nextInt(10))
				.append(random.nextInt(10))
				.append(random.nextInt(10))
				.toString();

		return new SaleResult(
			new AuthorizationResult(
				getProviderId(),
				TransactionResult.CommunicationResult.SUCCESS,
				null,
				null,
				null,
				providerUniqueId,
				null,
				null,
				AuthorizationResult.ApprovalResult.APPROVED,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				approvalCode
			),
			new CaptureResult(
				getProviderId(),
				TransactionResult.CommunicationResult.SUCCESS,
				null,
				null,
				null,
				providerUniqueId
			)
		);
	}

	@Override
	public AuthorizationResult authorize(TransactionRequest transactionRequest, CreditCard creditCard) {
		// First allow for random errors
		if(random.nextInt(100)<errorChance) {
			// Random error class
			TransactionResult.CommunicationResult communicationResult;
			int randomInt = random.nextInt(3);
			switch(randomInt) {
				case 0: {
					communicationResult = TransactionResult.CommunicationResult.LOCAL_ERROR;
					break;
				}
				case 1: {
					communicationResult = TransactionResult.CommunicationResult.IO_ERROR;
					break;
				}
				case 2: {
					communicationResult = TransactionResult.CommunicationResult.GATEWAY_ERROR;
					break;
				}
				default: throw new RuntimeException("random.nextInt(3) didn't return value between 0 and 2 inclusive: "+randomInt);
			}

			// Random error code
			TransactionResult.ErrorCode[] values = TransactionResult.ErrorCode.values();
			TransactionResult.ErrorCode errorCode = values[random.nextInt(values.length)];

			return new AuthorizationResult(
				getProviderId(),
				communicationResult,
				null,
				errorCode,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null
			);
		}

		// Second allow for declines
		if(random.nextInt(100)<declineChance) {
			// Random decline reason
			AuthorizationResult.DeclineReason[] values = AuthorizationResult.DeclineReason.values();
			AuthorizationResult.DeclineReason declineReason = values[random.nextInt(values.length)];

			// Random doesn't ensure uniquiness - but this is easily implemented without persistence
			String providerUniqueId = Long.toString(Math.abs(random.nextLong()), 16).toUpperCase();

			return new AuthorizationResult(
				getProviderId(),
				TransactionResult.CommunicationResult.SUCCESS,
				null,
				null,
				null,
				providerUniqueId,
				null,
				null,
				AuthorizationResult.ApprovalResult.DECLINED,
				null,
				declineReason,
				null,
				null,
				null,
				null,
				null,
				null,
				null
			);
		}

		// Simulate success

		// Random doesn't ensure uniquiness - but this is an easy implementation not requiring persistence
		String providerUniqueId = Long.toString(Math.abs(random.nextLong()), 16).toUpperCase();

		String approvalCode =
			new StringBuilder()
				.append(random.nextInt(10))
				.append(random.nextInt(10))
				.append(random.nextInt(10))
				.append(random.nextInt(10))
				.append(random.nextInt(10))
				.append(random.nextInt(10))
				.toString();

		return new AuthorizationResult(
			getProviderId(),
			TransactionResult.CommunicationResult.SUCCESS,
			null,
			null,
			null,
			providerUniqueId,
			null,
			null,
			AuthorizationResult.ApprovalResult.APPROVED,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			approvalCode
		);
	}

	@Override
	public CaptureResult capture(AuthorizationResult authorizationResult) {
		// First allow for random errors
		if(random.nextInt(100)<errorChance) {
			// Random error class
			TransactionResult.CommunicationResult communicationResult;
			int randomInt = random.nextInt(3);
			switch(randomInt) {
				case 0: {
					communicationResult = TransactionResult.CommunicationResult.LOCAL_ERROR;
					break;
				}
				case 1: {
					communicationResult = TransactionResult.CommunicationResult.IO_ERROR;
					break;
				}
				case 2: {
					communicationResult = TransactionResult.CommunicationResult.GATEWAY_ERROR;
					break;
				}
				default: throw new RuntimeException("random.nextInt(3) didn't return value between 0 and 2 inclusive: "+randomInt);
			}

			// Random error code
			TransactionResult.ErrorCode[] values = TransactionResult.ErrorCode.values();
			TransactionResult.ErrorCode errorCode = values[random.nextInt(values.length)];

			return new CaptureResult(
				getProviderId(),
				communicationResult,
				null,
				errorCode,
				null,
				authorizationResult.getProviderUniqueId()
			);
		}

		// Simulate success
		return new CaptureResult(
			getProviderId(),
			TransactionResult.CommunicationResult.SUCCESS,
			null,
			null,
			null,
			authorizationResult.getProviderUniqueId()
		);
	}

	@Override
	public VoidResult voidTransaction(Transaction transaction) {
		// First allow for random errors
		if(random.nextInt(100)<errorChance) {
			// Random error class
			TransactionResult.CommunicationResult communicationResult;
			int randomInt = random.nextInt(3);
			switch(randomInt) {
				case 0: {
					communicationResult = TransactionResult.CommunicationResult.LOCAL_ERROR;
					break;
				}
				case 1: {
					communicationResult = TransactionResult.CommunicationResult.IO_ERROR;
					break;
				}
				case 2: {
					communicationResult = TransactionResult.CommunicationResult.GATEWAY_ERROR;
					break;
				}
				default: throw new RuntimeException("random.nextInt(3) didn't return value between 0 and 2 inclusive: "+randomInt);
			}

			// Random error code
			TransactionResult.ErrorCode[] values = TransactionResult.ErrorCode.values();
			TransactionResult.ErrorCode errorCode = values[random.nextInt(values.length)];

			return new VoidResult(
				getProviderId(),
				communicationResult,
				null,
				errorCode,
				null,
				transaction.getAuthorizationResult().getProviderUniqueId()
			);
		}

		// Simulate success
		return new VoidResult(
			getProviderId(),
			TransactionResult.CommunicationResult.SUCCESS,
			null,
			null,
			null,
			transaction.getAuthorizationResult().getProviderUniqueId()
		);
	}

	@Override
	public CreditResult credit(TransactionRequest transactionRequest, CreditCard creditCard) {
		throw new NotImplementedException("TODO");
	}

	@Override
	public boolean canStoreCreditCards() {
		return true;
	}

	@Override
	public String storeCreditCard(CreditCard creditCard) throws IOException {
		// First allow for random errors
		if(random.nextInt(100)<errorChance) throw new IOException("Test-mode simulated storeCreditCard error");

		return Long.toString(Math.abs(random.nextLong()), 16).toUpperCase();
	}

	@Override
	public void updateCreditCard(CreditCard creditCard) throws IOException {
		// First allow for random errors
		if(random.nextInt(100)<errorChance) throw new IOException("Test-mode simulated updateCreditCard error");
	}

	@Override
	public void updateCreditCardNumberAndExpiration(
		CreditCard creditCard,
		String cardNumber,
		byte expirationMonth,
		short expirationYear,
		String cardCode
	) throws IOException {
		// First allow for random errors
		if(random.nextInt(100)<errorChance) throw new IOException("Test-mode simulated updateCreditCardNumberAndExpiration error");
	}

	@Override
	public void updateCreditCardExpiration(
		CreditCard creditCard,
		byte expirationMonth,
		short expirationYear
	) throws IOException {
		// First allow for random errors
		if(random.nextInt(100)<errorChance) throw new IOException("Test-mode simulated updateCreditCardExpiration error");
	}

	@Override
	public void deleteCreditCard(CreditCard creditCard) throws IOException {
		// First allow for random errors
		if(random.nextInt(100)<errorChance) throw new IOException("Test-mode simulated deleteCreditCard error");
	}

	@Override
	public boolean canGetTokenizedCreditCards() {
		return false;
	}

	@Override
	public Map<String, TokenizedCreditCard> getTokenizedCreditCards(Map<String, CreditCard> persistedCards, PrintWriter verboseOut, PrintWriter infoOut, PrintWriter warningOut) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}
}
