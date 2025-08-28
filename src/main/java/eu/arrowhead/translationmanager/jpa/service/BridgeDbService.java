/*******************************************************************************
 *
 * Copyright (c) 2025 AITIA
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 *
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  	AITIA - implementation
 *  	Arrowhead Consortia - conceptualization
 *
 *******************************************************************************/
package eu.arrowhead.translationmanager.jpa.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import eu.arrowhead.common.Utilities;
import eu.arrowhead.common.exception.ArrowheadException;
import eu.arrowhead.common.exception.ForbiddenException;
import eu.arrowhead.common.exception.InternalServerError;
import eu.arrowhead.common.exception.InvalidParameterException;
import eu.arrowhead.dto.enums.TranslationBridgeEventState;
import eu.arrowhead.dto.enums.TranslationBridgeStatus;
import eu.arrowhead.translationmanager.jpa.entity.BridgeDetails;
import eu.arrowhead.translationmanager.jpa.entity.BridgeDiscovery;
import eu.arrowhead.translationmanager.jpa.entity.BridgeHeader;
import eu.arrowhead.translationmanager.jpa.repository.BridgeDetailsRepository;
import eu.arrowhead.translationmanager.jpa.repository.BridgeDiscoveryRepository;
import eu.arrowhead.translationmanager.jpa.repository.BridgeHeaderRepository;
import eu.arrowhead.translationmanager.service.dto.NormalizedTranslationReportRequestDTO;
import eu.arrowhead.translationmanager.service.dto.TranslationDiscoveryModel;

@Service
public class BridgeDbService {

	//=================================================================================================
	// members

	private final Logger logger = LogManager.getLogger(this.getClass());

	@Autowired
	private BridgeHeaderRepository headerRepository;

	@Autowired
	private BridgeDetailsRepository detailsRepository;

	@Autowired
	private BridgeDiscoveryRepository discoveryRepository;

	//=================================================================================================
	// methods

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void handleTranslationReport(final NormalizedTranslationReportRequestDTO dto) {
		logger.debug("handleTranslationReport started...");
		Assert.notNull(dto, "dto is null");
		Assert.notNull(dto.bridgeId(), "bridgeId is null");
		Assert.notNull(dto.timestamp(), "timestamp is null");

		try {
			final Optional<BridgeHeader> headerOpt = headerRepository.findByUuid(dto.bridgeId().toString());
			if (headerOpt.isEmpty()) {
				throw new InvalidParameterException("Invalid bridge id: " + dto.bridgeId().toString());
			}

			final BridgeHeader header = headerOpt.get();
			final Optional<BridgeDetails> detailsOpt = detailsRepository.findByHeader(header);
			if (detailsOpt.isEmpty()) {
				throw new InvalidParameterException("Invalid bridge id: " + dto.bridgeId().toString());
			}

			if (!detailsOpt.get().getInterfaceTranslator().equals(dto.requester())) {
				// only the related interface translator has the right to report anything about the bridge
				throw new ForbiddenException("Requester has no permission to report about the specified translation bridge");
			}

			final TranslationBridgeStatus toStatus = TranslationBridgeEventState.transformToBridgeStatus(dto.state());
			if (!header.getStatus().isActiveStatus() || !TranslationBridgeStatus.isValidTransition(header.getStatus(), toStatus)) {
				// bridge should not reporting or the reported transition is invalid
				throw new InvalidParameterException("Invalid reporting case");
			}

			header.setStatus(toStatus);
			header.setAliveAt(dto.timestamp());
			if (TranslationBridgeStatus.USED == toStatus) {
				header.setUsageReportCount(header.getUsageReportCount() + 1);
			}
			if (!Utilities.isEmpty(dto.errorMessage())) {
				header.setMessage(dto.errorMessage());
			}

			headerRepository.saveAndFlush(header);
		} catch (final InvalidParameterException | ForbiddenException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public Pair<BridgeHeader, List<BridgeDiscovery>> storeBridgeDiscoveries(final UUID bridgeId, final String createdBy, final List<TranslationDiscoveryModel> models) {
		logger.debug("storeBridgeDiscoveries started...");
		Assert.notNull(bridgeId, "bridgeId is null");
		Assert.isTrue(!Utilities.isEmpty(models), "models list is missing");
		Assert.isTrue(!Utilities.containsNull(models), "models list contains null element");

		try {
			BridgeHeader header = new BridgeHeader(bridgeId, createdBy);
			List<BridgeDiscovery> discoveries = new ArrayList<>(models.size());
			for (final TranslationDiscoveryModel model : models) {
				discoveries.add(new BridgeDiscovery(header, Utilities.toJson(model)));
			}

			header = headerRepository.saveAndFlush(header);
			discoveries = discoveryRepository.saveAllAndFlush(discoveries);

			header.setStatus(TranslationBridgeStatus.DISCOVERED);
			header = headerRepository.saveAndFlush(header);

			return Pair.of(header, discoveries);
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public Pair<TranslationDiscoveryModel, BridgeDetails> selectBridgeFromDiscoveries(final UUID bridgeId, final String instanceId) {
		logger.debug("selectBridgeFromDiscoveries started...");
		Assert.notNull(bridgeId, "bridgeId is null");
		Assert.isTrue(!Utilities.isEmpty(instanceId), "instance id is missing");

		try {
			// finding related header
			final Optional<BridgeHeader> headerOpt = headerRepository.findByUuid(bridgeId.toString());
			if (headerOpt.isEmpty()) {
				throw new InvalidParameterException("Invalid bridge identifier: " + bridgeId);
			}

			BridgeHeader header = headerOpt.get();
			if (!TranslationBridgeStatus.isValidTransition(header.getStatus(), TranslationBridgeStatus.PENDING)) {
				throw new InvalidParameterException("Invalid bridge identifier: " + bridgeId);
			}

			// finding related discovery model
			final List<BridgeDiscovery> discoveries = discoveryRepository.findByHeader(header);
			final Optional<TranslationDiscoveryModel> modelOpt = findDiscoveryModelByInstanceId(discoveries, instanceId);
			if (modelOpt.isEmpty()) {
				throw new InvalidParameterException("Invalid bridge identifier: " + bridgeId);
			}

			// setting status
			header.setStatus(TranslationBridgeStatus.PENDING);
			header = headerRepository.saveAndFlush(header);

			// removing discovery records
			discoveryRepository.deleteByHeader(header);
			discoveryRepository.flush();

			// creating details record
			final TranslationDiscoveryModel model = modelOpt.get();
			BridgeDetails details = new BridgeDetails(
					header,
					model.getConsumer(),
					model.getProvider(),
					model.getServiceDefinition(),
					model.getOperation(),
					model.getInterfaceTranslator(),
					"", // will be set later
					model.getInputDataModelTranslator(),
					null, // will be set later if necessary
					model.getOutputDataModelTranslator(),
					null); // will be set later if necessary
			details = detailsRepository.saveAndFlush(details);

			return Pair.of(model, details);
		} catch (final InvalidParameterException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public boolean updateDetailsRecord(final BridgeDetails record) {
		logger.debug("updateDetailsRecord started...");
		Assert.notNull(record, "record is missing");

		try {
			final Optional<BridgeDetails> currentDetailsOpt = detailsRepository.findById(record.getId());
			if (currentDetailsOpt.isPresent()) {
				if (currentDetailsOpt.get().getHeader().getStatus().isEndStatus()) {
					return true;
				}

				detailsRepository.saveAndFlush(record);
			}

			return false;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public boolean bridgeInitialized(final BridgeHeader header) {
		logger.debug("bridgeInitialized started...");
		Assert.notNull(header, "header is missing");

		try {
			if (TranslationBridgeStatus.PENDING == header.getStatus()) {
				header.setStatus(TranslationBridgeStatus.INITIALIZED);
				headerRepository.saveAndFlush(header);
			} else if (header.getStatus().isEndStatus()) {
				return true;
			}

			return false;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public void storeBridgeProblem(final UUID bridgeId, final String errorMessage) {
		logger.debug("storeBridgeProblem started...");
		Assert.notNull(bridgeId, "bridgeId is missing");
		Assert.isTrue(!Utilities.isEmpty(errorMessage), "errorMessage is missing");

		try {
			final Optional<BridgeHeader> headerOpt = headerRepository.findByUuid(bridgeId.toString());
			if (headerOpt.isPresent() && TranslationBridgeStatus.isValidTransition(headerOpt.get().getStatus(), TranslationBridgeStatus.ERROR)) {
				final BridgeHeader header = headerOpt.get();
				header.setStatus(TranslationBridgeStatus.ERROR);
				header.setMessage(errorMessage);
				headerRepository.saveAndFlush(header);
			}
		} catch (final Exception ex) {
			// can't store a bridge problem in the db, so we store it at least in the log
			logger.error("Bridge {} should be in ERROR state with the following error message: {}", bridgeId.toString(), errorMessage);

			logger.error(ex.getMessage());
			logger.debug(ex);

			// no point to throw an exception since this method is about handling an original exception
		}
	}

	//-------------------------------------------------------------------------------------------------
	@Transactional(rollbackFor = ArrowheadException.class)
	public AbortResult abortBridge(final UUID bridgeId, final String createdByRequirement) {
		logger.debug("abortBridge started...");
		Assert.notNull(bridgeId, "bridgeId is missing");

		try {
			final Optional<BridgeHeader> headerOpt = headerRepository.findByUuid(bridgeId.toString());
			if (headerOpt.isEmpty()) {
				return new AbortResult(false, null, null);
			}

			BridgeHeader header = headerOpt.get();
			if (!Utilities.isEmpty(createdByRequirement)
					&& !createdByRequirement.equals(header.getCreatedBy())) {
				// no permission to abort this bridge
				throw new ForbiddenException("No permission to abort bridge: " + bridgeId.toString());
			}

			final TranslationBridgeStatus oldStatus = header.getStatus();
			if (TranslationBridgeStatus.isValidTransition(header.getStatus(), TranslationBridgeStatus.ABORTED)) {
				// delete related discovery records (if any)
				discoveryRepository.deleteByHeader(header);

				header.setStatus(TranslationBridgeStatus.ABORTED);
				header = headerRepository.saveAndFlush(header);

				final Optional<BridgeDetails> detailsOpt = detailsRepository.findByHeader(header);

				return new AbortResult(true, oldStatus, detailsOpt.isEmpty() ? null : detailsOpt.get());
			}

			// already aborted, closed or in error state
			return new AbortResult(false, oldStatus, null);
		} catch (final ForbiddenException ex) {
			throw ex;
		} catch (final Exception ex) {
			logger.error(ex.getMessage());
			logger.debug(ex);
			throw new InternalServerError("Database operation error");
		}
	}

	//=================================================================================================
	// assistant methods

	//-------------------------------------------------------------------------------------------------
	private Optional<TranslationDiscoveryModel> findDiscoveryModelByInstanceId(final List<BridgeDiscovery> discoveries, final String instanceId) {
		logger.debug("findDiscoveryModelByInstanceId started...");

		for (final BridgeDiscovery record : discoveries) {
			final TranslationDiscoveryModel model = Utilities.fromJson(record.getData(), TranslationDiscoveryModel.class);
			if (model.getTargetInstanceId().equals(instanceId)) {
				return Optional.of(model);
			}
		}

		return Optional.empty();
	}

	//=================================================================================================
	// nested structures

	//-------------------------------------------------------------------------------------------------
	public record AbortResult(
			boolean abortHappened,
			TranslationBridgeStatus fromStatus,
			BridgeDetails detailsRecord) {
	}

}