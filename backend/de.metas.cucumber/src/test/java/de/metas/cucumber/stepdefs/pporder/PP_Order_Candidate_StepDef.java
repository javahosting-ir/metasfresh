/*
 * #%L
 * de.metas.cucumber
 * %%
 * Copyright (C) 2023 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package de.metas.cucumber.stepdefs.pporder;

import com.google.common.collect.ImmutableSet;
import de.metas.cucumber.stepdefs.C_OrderLine_StepDefData;
import de.metas.cucumber.stepdefs.DataTableRow;
import de.metas.cucumber.stepdefs.DataTableRows;
import de.metas.cucumber.stepdefs.DataTableUtil;
import de.metas.cucumber.stepdefs.IdentifierIds_StepDefData;
import de.metas.cucumber.stepdefs.ItemProvider;
import de.metas.cucumber.stepdefs.M_Product_StepDefData;
import de.metas.cucumber.stepdefs.StepDefConstants;
import de.metas.cucumber.stepdefs.StepDefDataIdentifier;
import de.metas.cucumber.stepdefs.StepDefUtil;
import de.metas.cucumber.stepdefs.attribute.M_AttributeSetInstance_StepDefData;
import de.metas.cucumber.stepdefs.billofmaterial.PP_Product_BOM_StepDefData;
import de.metas.cucumber.stepdefs.hu.M_HU_PI_Item_Product_StepDefData;
import de.metas.cucumber.stepdefs.hu.M_HU_StepDefData;
import de.metas.cucumber.stepdefs.pporder.maturing.M_Maturing_Configuration_Line_StepDefData;
import de.metas.cucumber.stepdefs.pporder.maturing.M_Maturing_Configuration_StepDefData;
import de.metas.cucumber.stepdefs.productplanning.PP_Product_Planning_StepDefData;
import de.metas.cucumber.stepdefs.resource.S_Resource_StepDefData;
import de.metas.cucumber.stepdefs.warehouse.M_Warehouse_StepDefData;
import de.metas.handlingunits.HUPIItemProductId;
import de.metas.handlingunits.model.I_M_HU;
import de.metas.i18n.AdMessageKey;
import de.metas.i18n.IMsgBL;
import de.metas.i18n.ITranslatableString;
import de.metas.logging.LogManager;
import de.metas.material.event.commons.AttributesKey;
import de.metas.material.planning.ProductPlanning;
import de.metas.order.OrderLineId;
import de.metas.process.PInstanceId;
import de.metas.product.ProductId;
import de.metas.product.ResourceId;
import de.metas.uom.IUOMDAO;
import de.metas.uom.UomId;
import de.metas.uom.X12DE355;
import de.metas.util.Check;
import de.metas.util.Services;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.adempiere.ad.dao.IQueryBL;
import org.adempiere.ad.dao.IQueryBuilder;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.mm.attributes.AttributeSetInstanceId;
import org.adempiere.mm.attributes.keys.AttributesKeys;
import org.adempiere.model.InterfaceWrapperHelper;
import org.assertj.core.api.SoftAssertions;
import org.compiere.SpringContextHolder;
import org.compiere.model.IQuery;
import org.compiere.model.I_C_UOM;
import org.compiere.model.I_M_AttributeSetInstance;
import org.compiere.model.I_M_Product;
import org.compiere.model.I_M_Warehouse;
import org.compiere.model.I_S_Resource;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;
import org.eevolution.api.ProductBOMId;
import org.eevolution.model.I_PP_Order_Candidate;
import org.eevolution.model.I_PP_Product_BOM;
import org.eevolution.productioncandidate.async.EnqueuePPOrderCandidateRequest;
import org.eevolution.productioncandidate.async.PPOrderCandidateEnqueuer;
import org.eevolution.productioncandidate.model.PPOrderCandidateId;
import org.eevolution.productioncandidate.service.PPOrderCandidateService;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static de.metas.cucumber.stepdefs.StepDefConstants.TABLECOLUMN_IDENTIFIER;
import static org.adempiere.model.InterfaceWrapperHelper.saveRecord;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.compiere.model.I_M_Warehouse.COLUMNNAME_M_Warehouse_ID;
import static org.eevolution.model.I_PP_Order_Candidate.COLUMNNAME_PP_Order_Candidate_ID;

@RequiredArgsConstructor
public class PP_Order_Candidate_StepDef
{
	private final static Logger logger = LogManager.getLogger(PP_Order_Candidate_StepDef.class);

	@NonNull private final PPOrderCandidateEnqueuer ppOrderCandidateEnqueuer = SpringContextHolder.instance.getBean(PPOrderCandidateEnqueuer.class);
	@NonNull private final PPOrderCandidateService ppOrderCandidateService = SpringContextHolder.instance.getBean(PPOrderCandidateService.class);
	@NonNull private final IUOMDAO uomDAO = Services.get(IUOMDAO.class);
	@NonNull private final IQueryBL queryBL = Services.get(IQueryBL.class);
	@NonNull private final IMsgBL msgBL = Services.get(IMsgBL.class);

	@NonNull private final M_Product_StepDefData productTable;
	@NonNull private final PP_Product_BOM_StepDefData productBOMTable;
	@NonNull private final PP_Product_Planning_StepDefData productPlanningTable;
	@NonNull private final PP_Order_Candidate_StepDefData ppOrderCandidateTable;
	@NonNull private final M_AttributeSetInstance_StepDefData attributeSetInstanceTable;
	@NonNull private final M_Warehouse_StepDefData warehouseTable;
	@NonNull private final M_HU_PI_Item_Product_StepDefData huPiItemProductTable;
	@NonNull private final IdentifierIds_StepDefData identifierIdsTable;
	@NonNull private final C_OrderLine_StepDefData orderLineTable;
	@NonNull private final M_Maturing_Configuration_StepDefData maturingConfigTable;
	@NonNull private final M_Maturing_Configuration_Line_StepDefData maturingConfigLineTable;
	@NonNull private final M_HU_StepDefData huTable;
	@NonNull private final S_Resource_StepDefData resourceTable;

	private static final AdMessageKey MSG_QTY_ENTERED_LOWER_THAN_QTY_PROCESSED = AdMessageKey.of("org.eevolution.productioncandidate.model.interceptor.QtyEnteredLowerThanQtyProcessed");
	private static final AdMessageKey MSG_QTY_TO_PROCESS_GREATER_THAN_QTY_LEFT = AdMessageKey.of("org.eevolution.productioncandidate.model.interceptor.QtyToProcessGreaterThanQtyLeftToBeProcessed");

	@And("^after not more than (.*)s, PP_Order_Candidates are found$")
	public void validatePP_Order_Candidate(
			final int timeoutSec,
			@NonNull final DataTable dataTable)
	{
		DataTableRows.of(dataTable).forEach(row -> validatePP_Order_Candidate(timeoutSec, row));
	}

	@And("update PP_Order_Candidates")
	public void updatePPOrderCandidate(@NonNull final DataTable dataTable)
	{
		final List<Map<String, String>> tableRows = dataTable.asMaps(String.class, String.class);
		for (final Map<String, String> row : tableRows)
		{
			updatePPOrderCandidate(row);
		}
	}

	@And("the following PP_Order_Candidates are enqueued for generating PP_Orders")
	public void enqueuePP_Order_Candidate(@NonNull final DataTable dataTable)
	{
		invokeGeneratePPOrderProcess(null, false, dataTable);
	}

	@And("the following PP_Order_Candidates are re-opened")
	public void reOpenPP_Order_Candidate(@NonNull final DataTable dataTable)
	{
		DataTableRows.of(dataTable).forEach(row -> ppOrderCandidateService.reopenCandidate(getPPOrderCandidate(row)));
	}

	@When("the following PP_Order_Candidates are closed")
	public void close_PP_Order_Candidate(@NonNull final DataTable dataTable)
	{
		DataTableRows.of(dataTable).forEach(row -> ppOrderCandidateService.closeCandidate(getPPOrderCandidate(row)));
	}

	@Then("the following PP_Order_Candidates are closed and cannot be re-opened")
	public void validate_closed_PP_Order_Candidates_cannot_be_reopened(@NonNull final DataTable dataTable)
	{
		DataTableRows.of(dataTable).forEach(this::validate_closed_PP_Order_Candidate_cannot_be_reopened);
	}

	private void validate_closed_PP_Order_Candidate_cannot_be_reopened(@NonNull final DataTableRow row)
	{
		final I_PP_Order_Candidate orderCandidateRecord = getPPOrderCandidate(row);
		assertThat(orderCandidateRecord.isClosed()).isTrue();
		assertThatThrownBy(() -> ppOrderCandidateService.reopenCandidate(orderCandidateRecord))
				.isInstanceOf(AdempiereException.class)
				.hasMessageContaining("Cannot reopen a closed candidate!");
	}

	@Then("update PP_Order_Candidate's qty to process to more than left to be processed expecting exception")
	public void update_qtyToProcess_to_invalid_qty(@NonNull final DataTable dataTable)
	{
		DataTableRows.of(dataTable).forEach(this::updateQtyToProcessAndExpectForException);
	}

	@Then("update PP_Order_Candidate's qty entered to less than processed expecting exception")
	public void update_qtyEntered_to_invalid_qty(@NonNull final DataTable dataTable)
	{
		DataTableRows.of(dataTable).forEach(this::updateQtyEnteredAndExpectForException);
	}

	@Given("metasfresh contains PP_Order_Candidates")
	public void metasfresh_contains_PP_Order_Candidate(@NonNull final DataTable dataTable)
	{
		final List<Map<String, String>> tableRows = dataTable.asMaps(String.class, String.class);
		for (final Map<String, String> row : tableRows)
		{
			createPPOrderCandidate(row);
		}
	}

	@And("validate there is no simulated PP_Order_Candidate")
	public void validate_no_simulated_PP_Order_Candidate()
	{
		final int noOfRecords = queryBL.createQueryBuilder(I_PP_Order_Candidate.class)
				.addEqualsFilter(I_PP_Order_Candidate.COLUMNNAME_IsSimulated, true)
				.create()
				.count();

		assertThat(noOfRecords).isZero();
	}

	@And("^no PP_Order_Candidate found for orderLine (.*)$")
	public void validate_no_PP_Order_Candidate_found(@NonNull final String orderLineIdentifier)
	{
		final OrderLineId orderLineId = getOrderLineIdByIdentifier(orderLineIdentifier);
		assertThat(orderLineId).isNotNull();

		try
		{
			assertThat(getQueryByOrderLineId(orderLineId).count()).isZero();
		}
		catch (final Throwable throwable)
		{
			logCurrentContextExpectedNoRecords(orderLineId);
		}
	}

	@And("^after not more than (.*)s, PP_Order_Candidate found for orderLine (.*)$")
	public void validate_PP_Order_Candidate_found_for_OrderLine(
			final int timeoutSec,
			@NonNull final String orderLineIdentifier,
			@NonNull final DataTable dataTable) throws InterruptedException
	{
		final Map<String, String> tableRow = dataTable.asMaps().get(0);

		final OrderLineId orderLineId = getOrderLineIdByIdentifier(orderLineIdentifier);
		assertThat(orderLineId).isNotNull();

		final Supplier<Optional<I_PP_Order_Candidate>> ppOrderCandSupplier = () -> Optional.ofNullable(getQueryByOrderLineId(orderLineId)
				.first());

		final I_PP_Order_Candidate ppOrderCandidate = StepDefUtil.tryAndWaitForItem(timeoutSec, 500, ppOrderCandSupplier);

		final String ppOrderCandidateIdentifier = DataTableUtil.extractStringForColumnName(tableRow, StepDefConstants.TABLECOLUMN_IDENTIFIER);
		ppOrderCandidateTable.putOrReplace(ppOrderCandidateIdentifier, ppOrderCandidate);
	}

	@And("^generate PP_Order process is invoked for selection, with completeDocument=(.*) and autoProcessCandidateAfterProduction=(.*)$")
	public void invokeGeneratePPOrderProcess(
			@Nullable final Boolean isDocComplete,
			final boolean autoProcessCandidate,
			@NonNull final DataTable table)
	{
		final Set<PPOrderCandidateId> ppOrderCandidatesId = DataTableRows.of(table)
				.stream()
				.map(this::getPPOrderCandidateId)
				.collect(ImmutableSet.toImmutableSet());

		final PInstanceId pInstanceId = queryBL.createQueryBuilder(I_PP_Order_Candidate.class)
				.addOnlyActiveRecordsFilter()
				.addInArrayFilter(I_PP_Order_Candidate.COLUMNNAME_PP_Order_Candidate_ID, ppOrderCandidatesId)
				.create()
				.createSelection();

		final EnqueuePPOrderCandidateRequest enqueuePPOrderCandidateRequest = EnqueuePPOrderCandidateRequest.builder()
				.adPInstanceId(pInstanceId)
				.ctx(Env.getCtx())
				.isCompleteDocOverride(isDocComplete)
				.autoProcessCandidatesAfterProduction(autoProcessCandidate)
				.build();

		final PPOrderCandidateEnqueuer.Result result = ppOrderCandidateEnqueuer
				.enqueueSelection(enqueuePPOrderCandidateRequest);

		assertThat(result.getEnqueuedPackagesCount()).isOne();
	}

	private void updatePPOrderCandidate(@NonNull final Map<String, String> tableRow)
	{
		final String ppOrderCandidateIdentifier = DataTableUtil.extractStringForColumnName(tableRow, COLUMNNAME_PP_Order_Candidate_ID + "." + StepDefConstants.TABLECOLUMN_IDENTIFIER);
		final I_PP_Order_Candidate ppOrderCandidateRecord = ppOrderCandidateTable.get(ppOrderCandidateIdentifier);

		final ZonedDateTime dateStartSchedule = DataTableUtil.extractZonedDateTimeOrNullForColumnName(tableRow, I_PP_Order_Candidate.COLUMNNAME_DateStartSchedule);
		final BigDecimal qtyEntered = DataTableUtil.extractBigDecimalOrNullForColumnName(tableRow, I_PP_Order_Candidate.COLUMNNAME_QtyEntered);
		final BigDecimal openQty = DataTableUtil.extractBigDecimalOrNullForColumnName(tableRow, "OPT." + I_PP_Order_Candidate.COLUMNNAME_QtyToProcess);

		if (dateStartSchedule != null)
		{
			ppOrderCandidateRecord.setDateStartSchedule(TimeUtil.asTimestampNotNull(dateStartSchedule));
		}

		if (qtyEntered != null)
		{
			ppOrderCandidateRecord.setQtyEntered(qtyEntered);
		}

		if (openQty != null)
		{
			ppOrderCandidateRecord.setQtyToProcess(openQty);
		}

		final Integer seqNo = DataTableUtil.extractIntegerOrNullForColumnName(tableRow, "OPT." + I_PP_Order_Candidate.COLUMNNAME_SeqNo);
		if (seqNo != null)
		{
			ppOrderCandidateRecord.setSeqNo(seqNo);
		}

		saveRecord(ppOrderCandidateRecord);
	}

	private I_PP_Order_Candidate getPPOrderCandidate(@NonNull final DataTableRow row)
	{
		return row.getAsIdentifier(COLUMNNAME_PP_Order_Candidate_ID).lookupIn(ppOrderCandidateTable);
	}

	private PPOrderCandidateId getPPOrderCandidateId(@NonNull final DataTableRow row)
	{
		return row.getAsIdentifier(COLUMNNAME_PP_Order_Candidate_ID).lookupIdIn(ppOrderCandidateTable);
	}

	private void validatePP_Order_Candidate(
			final int timeoutSec,
			@NonNull final DataTableRow row) throws InterruptedException
	{
		final ProductId productId = row.getAsIdentifier(I_M_Product.COLUMNNAME_M_Product_ID).lookupIdIn(productTable);
		final IQuery<I_PP_Order_Candidate> candidateQuery = toSqlQuery(row);

		final I_PP_Order_Candidate locatedPPOrderCandidate = StepDefUtil.tryAndWaitForItem(
				timeoutSec,
				500,
				() -> locatePPOrderCandidate(candidateQuery),
				() -> getPPOrderCandidateContextForProductId(productId)
		);

		final StepDefDataIdentifier orderCandidateRecordIdentifier = row.getAsIdentifier();
		ppOrderCandidateTable.putOrReplace(orderCandidateRecordIdentifier, locatedPPOrderCandidate);
		final I_PP_Order_Candidate ppOrderCandidate = ppOrderCandidateTable.get(orderCandidateRecordIdentifier);

		final SoftAssertions softly = new SoftAssertions();

		//validate asi
		row.getAsOptionalIdentifier(I_PP_Order_Candidate.COLUMNNAME_M_AttributeSetInstance_ID)
				.map(attributeSetInstanceTable::getId)
				.ifPresent(expectedASIId -> {
					final AttributesKey expectedAttributesKeys = AttributesKeys.createAttributesKeyFromASIStorageAttributes(expectedASIId).orElse(AttributesKey.NONE);
					final AttributesKey ppOrderCandidateAttributesKeys = AttributesKeys.createAttributesKeyFromASIStorageAttributes(AttributeSetInstanceId.ofRepoId(ppOrderCandidate.getM_AttributeSetInstance_ID())).orElse(AttributesKey.NONE);
					softly.assertThat(ppOrderCandidateAttributesKeys).isEqualTo(expectedAttributesKeys);
				});

		row.getAsOptionalIdentifier(I_PP_Order_Candidate.COLUMNNAME_M_HU_PI_Item_Product_ID)
				.ifPresent(itemProductIdentifier -> {
					final HUPIItemProductId huPiItemProductId = huPiItemProductTable.getIdOptional(itemProductIdentifier)
							.orElseGet(() -> itemProductIdentifier.getAsId(HUPIItemProductId.class));

					softly.assertThat(ppOrderCandidate.getM_HU_PI_Item_Product_ID()).isEqualTo(huPiItemProductId.getRepoId());
				});

		row.getAsOptionalInt("SeqNo")
				.ifPresent(seqNo -> softly.assertThat(ppOrderCandidate.getSeqNo()).isEqualTo(seqNo));

		softly.assertAll();
	}

	@NonNull
	private ItemProvider.ProviderResult<I_PP_Order_Candidate> locatePPOrderCandidate(@NonNull final IQuery<I_PP_Order_Candidate> query)
	{
		final I_PP_Order_Candidate candidate = query.firstOnly(I_PP_Order_Candidate.class);
		if (candidate != null)
		{
			return ItemProvider.ProviderResult.resultWasFound(candidate);
		}
		else
		{
			return ItemProvider.ProviderResult.resultWasNotFound("See query used: " + query);
		}
	}

	private IQuery<I_PP_Order_Candidate> toSqlQuery(final DataTableRow row)
	{
		final ProductId productId = row.getAsIdentifier(I_M_Product.COLUMNNAME_M_Product_ID).lookupIdIn(productTable);
		final ProductBOMId bomId = row.getAsIdentifier(I_PP_Product_BOM.COLUMNNAME_PP_Product_BOM_ID).lookupIdIn(productBOMTable);
		final ProductPlanning productPlanning = row.getAsIdentifier("PP_Product_Planning_ID").lookupIn(productPlanningTable);

		final StepDefDataIdentifier resourceIdentifier = row.getAsIdentifier(I_S_Resource.COLUMNNAME_S_Resource_ID);
		final ResourceId resourceId = resourceTable.getIdOptional(resourceIdentifier).orElseGet(() -> resourceIdentifier.getAsId(ResourceId.class));
		final int qtyEntered = row.getAsInt(I_PP_Order_Candidate.COLUMNNAME_QtyEntered);
		final int qtyToProcess = row.getAsInt(I_PP_Order_Candidate.COLUMNNAME_QtyToProcess);
		final int qtyProcessed = row.getAsInt(I_PP_Order_Candidate.COLUMNNAME_QtyProcessed);

		final String x12de355Code = row.getAsString(I_C_UOM.COLUMNNAME_C_UOM_ID + "." + X12DE355.class.getSimpleName());
		final UomId uomId = uomDAO.getUomIdByX12DE355(X12DE355.ofCode(x12de355Code));

		final Instant datePromised = row.getAsInstant(I_PP_Order_Candidate.COLUMNNAME_DatePromised);
		final Instant dateStartSchedule = row.getAsInstant(I_PP_Order_Candidate.COLUMNNAME_DateStartSchedule);
		final boolean isProcessed = row.getAsBoolean(I_PP_Order_Candidate.COLUMNNAME_Processed);
		final boolean isClosed = row.getAsBoolean(I_PP_Order_Candidate.COLUMNNAME_IsClosed);

		final IQueryBuilder<I_PP_Order_Candidate> builder = queryBL.createQueryBuilder(I_PP_Order_Candidate.class)
				.addEqualsFilter(I_PP_Order_Candidate.COLUMNNAME_M_Product_ID, productId)
				.addEqualsFilter(I_PP_Order_Candidate.COLUMNNAME_PP_Product_BOM_ID, bomId)
				.addEqualsFilter(I_PP_Order_Candidate.COLUMNNAME_PP_Product_Planning_ID, productPlanning.getId())
				.addEqualsFilter(I_PP_Order_Candidate.COLUMNNAME_S_Resource_ID, resourceId)
				.addEqualsFilter(I_PP_Order_Candidate.COLUMNNAME_QtyEntered, qtyEntered)
				.addEqualsFilter(I_PP_Order_Candidate.COLUMNNAME_QtyToProcess, qtyToProcess)
				.addEqualsFilter(I_PP_Order_Candidate.COLUMNNAME_QtyProcessed, qtyProcessed)
				.addEqualsFilter(I_PP_Order_Candidate.COLUMNNAME_C_UOM_ID, uomId.getRepoId())
				.addEqualsFilter(I_PP_Order_Candidate.COLUMNNAME_DatePromised, datePromised)
				.addEqualsFilter(I_PP_Order_Candidate.COLUMNNAME_DateStartSchedule, dateStartSchedule)
				.addEqualsFilter(I_PP_Order_Candidate.COLUMNNAME_Processed, isProcessed)
				.addEqualsFilter(I_PP_Order_Candidate.COLUMNNAME_IsClosed, isClosed);

		row.getAsOptionalBoolean(I_PP_Order_Candidate.COLUMNNAME_IsMaturing)
				.ifPresent(isMaturing -> builder.addEqualsFilter(I_PP_Order_Candidate.COLUMNNAME_IsMaturing, isMaturing));

		row.getAsOptionalIdentifier(I_PP_Order_Candidate.COLUMNNAME_M_Maturing_Configuration_ID)
				.map(maturingConfigTable::get)
				.ifPresent(maturingConfig -> builder.addEqualsFilter(I_PP_Order_Candidate.COLUMNNAME_M_Maturing_Configuration_ID, maturingConfig.getM_Maturing_Configuration_ID()));

		row.getAsOptionalIdentifier(I_PP_Order_Candidate.COLUMNNAME_M_Maturing_Configuration_Line_ID)
				.map(maturingConfigLineTable::get)
				.ifPresent(maturingConfigLine -> builder.addEqualsFilter(I_PP_Order_Candidate.COLUMNNAME_M_Maturing_Configuration_Line_ID, maturingConfigLine.getM_Maturing_Configuration_Line_ID()));

		row.getAsOptionalIdentifier(I_PP_Order_Candidate.COLUMNNAME_Issue_HU_ID)
				.map(huTable::get)
				.ifPresent(hu -> builder.addEqualsFilter(I_PP_Order_Candidate.COLUMNNAME_Issue_HU_ID, hu.getM_HU_ID()));

		return builder.create();
	}

	private void updateQtyEnteredAndExpectForException(@NonNull final DataTableRow row)
	{
		final I_PP_Order_Candidate orderCandidateRecord = getPPOrderCandidate(row);

		final BigDecimal qtyEntered = row.getAsBigDecimal(I_PP_Order_Candidate.COLUMNNAME_QtyEntered);
		assertThat(qtyEntered).isLessThan(orderCandidateRecord.getQtyProcessed());

		orderCandidateRecord.setQtyEntered(qtyEntered);

		try
		{
			InterfaceWrapperHelper.save(orderCandidateRecord);

			assertThat(false).as("InterfaceWrapperHelper.save should throw error!").isTrue();
		}
		catch (final AdempiereException adempiereException)
		{
			final String adLanguage = Env.getAD_Language();

			final String qtyEnteredColumnTrl = msgBL.translatable(I_PP_Order_Candidate.COLUMNNAME_QtyEntered).translate(adLanguage);
			final String qtyProcessedColumnTrl = msgBL.translatable(I_PP_Order_Candidate.COLUMNNAME_QtyProcessed).translate(adLanguage);

			final ITranslatableString message = msgBL.getTranslatableMsgText(MSG_QTY_ENTERED_LOWER_THAN_QTY_PROCESSED,
					qtyEnteredColumnTrl,
					qtyProcessedColumnTrl);

			assertThat(adempiereException.getMessage()).contains(message.translate(adLanguage));
		}
	}

	private void updateQtyToProcessAndExpectForException(@NonNull final DataTableRow row)
	{
		final I_PP_Order_Candidate orderCandidateRecord = getPPOrderCandidate(row);

		final BigDecimal qtyToProcess = row.getAsBigDecimal(I_PP_Order_Candidate.COLUMNNAME_QtyToProcess);

		final BigDecimal actualQtyLeft = orderCandidateRecord.getQtyEntered().subtract(orderCandidateRecord.getQtyProcessed());
		assertThat(qtyToProcess).isGreaterThan(actualQtyLeft);

		orderCandidateRecord.setQtyToProcess(qtyToProcess);

		try
		{
			InterfaceWrapperHelper.save(orderCandidateRecord);

			assertThat(false).as("InterfaceWrapperHelper.save should throw error!").isTrue();
		}
		catch (final AdempiereException adempiereException)
		{
			final String adLanguage = Env.getAD_Language();

			final String qtyToProcessColumnTrl = msgBL.translatable(I_PP_Order_Candidate.COLUMNNAME_QtyToProcess).translate(adLanguage);

			final ITranslatableString message = msgBL.getTranslatableMsgText(MSG_QTY_TO_PROCESS_GREATER_THAN_QTY_LEFT, qtyToProcessColumnTrl);

			assertThat(adempiereException.getMessage()).contains(message.translate(adLanguage));
		}
	}

	private void createPPOrderCandidate(@NonNull final Map<String, String> tableRow)
	{
		final I_PP_Order_Candidate ppOrderCandidateRecord = InterfaceWrapperHelper.newInstance(I_PP_Order_Candidate.class);

		final String productIdentifier = DataTableUtil.extractStringForColumnName(tableRow, I_M_Product.COLUMNNAME_M_Product_ID + "." + StepDefConstants.TABLECOLUMN_IDENTIFIER);
		final I_M_Product productRecord = productTable.get(productIdentifier);

		ppOrderCandidateRecord.setM_Product_ID(productRecord.getM_Product_ID());

		final String productBOMIdentifier = DataTableUtil.extractStringForColumnName(tableRow, I_PP_Product_BOM.COLUMNNAME_PP_Product_BOM_ID + "." + StepDefConstants.TABLECOLUMN_IDENTIFIER);
		final I_PP_Product_BOM productBOMRecord = productBOMTable.get(productBOMIdentifier);

		ppOrderCandidateRecord.setPP_Product_BOM_ID(productBOMRecord.getPP_Product_BOM_ID());

		final String productPlanningIdentifier = DataTableUtil.extractStringForColumnName(tableRow, "PP_Product_Planning_ID" + "." + StepDefConstants.TABLECOLUMN_IDENTIFIER);
		final ProductPlanning productPlanning = productPlanningTable.get(productPlanningIdentifier);

		ppOrderCandidateRecord.setPP_Product_Planning_ID(productPlanning.getIdNotNull().getRepoId());

		final StepDefDataIdentifier resourceIdentifier = StepDefDataIdentifier.ofString(DataTableUtil.extractStringForColumnName(tableRow, I_S_Resource.COLUMNNAME_S_Resource_ID));
		final ResourceId resourceId = resourceTable.getIdOptional(resourceIdentifier).orElseGet(() -> resourceIdentifier.getAsId(ResourceId.class));
		ppOrderCandidateRecord.setS_Resource_ID(resourceId.getRepoId());

		final BigDecimal qtyEntered = DataTableUtil.extractBigDecimalForColumnName(tableRow, I_PP_Order_Candidate.COLUMNNAME_QtyEntered);

		ppOrderCandidateRecord.setQtyEntered(qtyEntered);

		final BigDecimal qtyToProcess = DataTableUtil.extractBigDecimalForColumnName(tableRow, I_PP_Order_Candidate.COLUMNNAME_QtyToProcess);

		ppOrderCandidateRecord.setQtyToProcess(qtyToProcess);

		final BigDecimal qtyProcessed = DataTableUtil.extractBigDecimalForColumnName(tableRow, I_PP_Order_Candidate.COLUMNNAME_QtyProcessed);

		ppOrderCandidateRecord.setQtyProcessed(qtyProcessed);

		final String x12de355Code = DataTableUtil.extractStringForColumnName(tableRow, I_C_UOM.COLUMNNAME_C_UOM_ID + "." + X12DE355.class.getSimpleName());
		final UomId uomId = uomDAO.getUomIdByX12DE355(X12DE355.ofCode(x12de355Code));

		ppOrderCandidateRecord.setC_UOM_ID(uomId.getRepoId());

		final Instant datePromised = DataTableUtil.extractInstantForColumnName(tableRow, I_PP_Order_Candidate.COLUMNNAME_DatePromised);
		final Instant dateStartSchedule = DataTableUtil.extractInstantForColumnName(tableRow, I_PP_Order_Candidate.COLUMNNAME_DateStartSchedule);
		final boolean isProcessed = DataTableUtil.extractBooleanForColumnNameOr(tableRow, "OPT." + I_PP_Order_Candidate.COLUMNNAME_Processed, false);
		final boolean isClosed = DataTableUtil.extractBooleanForColumnNameOr(tableRow, "OPT." + I_PP_Order_Candidate.COLUMNNAME_IsClosed, false);

		ppOrderCandidateRecord.setDatePromised(Timestamp.from(datePromised));
		ppOrderCandidateRecord.setDateStartSchedule(Timestamp.from(dateStartSchedule));
		ppOrderCandidateRecord.setProcessed(isProcessed);
		ppOrderCandidateRecord.setIsClosed(isClosed);

		final String asiIdentifier = DataTableUtil.extractStringOrNullForColumnName(tableRow, "OPT." + I_PP_Order_Candidate.COLUMNNAME_M_AttributeSetInstance_ID + "." + TABLECOLUMN_IDENTIFIER);
		if (Check.isNotBlank(asiIdentifier))
		{
			final I_M_AttributeSetInstance asiRecord = attributeSetInstanceTable.get(asiIdentifier);
			assertThat(asiRecord).isNotNull();

			ppOrderCandidateRecord.setM_AttributeSetInstance_ID(asiRecord.getM_AttributeSetInstance_ID());
		}
		else
		{
			ppOrderCandidateRecord.setM_AttributeSetInstance_ID(AttributeSetInstanceId.NONE.getRepoId());
		}

		final String warehouseIdentifier = DataTableUtil.extractStringForColumnName(tableRow, COLUMNNAME_M_Warehouse_ID + "." + TABLECOLUMN_IDENTIFIER);
		final I_M_Warehouse warehouseRecord = warehouseTable.get(warehouseIdentifier);

		ppOrderCandidateRecord.setM_Warehouse_ID(warehouseRecord.getM_Warehouse_ID());

		InterfaceWrapperHelper.save(ppOrderCandidateRecord);

		ppOrderCandidateTable.putOrReplace(DataTableUtil.extractRecordIdentifier(tableRow, I_PP_Order_Candidate.Table_Name), ppOrderCandidateRecord);
	}

	@NonNull
	private IQuery<I_PP_Order_Candidate> getQueryByOrderLineId(@NonNull final OrderLineId orderLineId)
	{
		return queryBL.createQueryBuilder(I_PP_Order_Candidate.class)
				.addEqualsFilter(I_PP_Order_Candidate.COLUMNNAME_C_OrderLine_ID, orderLineId)
				.create();
	}

	private void logCurrentContextExpectedNoRecords(@NonNull final OrderLineId orderLineId)
	{
		final StringBuilder message = new StringBuilder();

		message.append("Validating no records found for orderLineID :").append("\n")
				.append(I_PP_Order_Candidate.COLUMNNAME_C_OrderLine_ID).append(" : ").append(orderLineId).append("\n");

		message.append("PP_Order_Candidate records:").append("\n");

		getQueryByOrderLineId(orderLineId)
				.stream(I_PP_Order_Candidate.class)
				.forEach(ppOrderCandidate -> message
						.append(COLUMNNAME_PP_Order_Candidate_ID).append(" : ").append(ppOrderCandidate.getPP_Order_Candidate_ID()).append(" ; ")
						.append("\n"));

		logger.error("*** Error while validating no PP_Order_Candidate found for orderLineId: " + orderLineId + ", see found records: \n" + message);
	}

	@Nullable
	private OrderLineId getOrderLineIdByIdentifier(@NonNull final String orderLineIdentifier)
	{
		return OrderLineId.ofRepoIdOrNull(identifierIdsTable.getOptional(orderLineIdentifier)
				.orElseGet(() -> orderLineTable.get(orderLineIdentifier).getC_OrderLine_ID()));
	}

	@NonNull
	private String getPPOrderCandidateContextForProductId(@NonNull final ProductId productId)
	{
		final List<I_PP_Order_Candidate> candidatesForProductId = queryBL.createQueryBuilder(I_PP_Order_Candidate.class)
				.addEqualsFilter(I_PP_Order_Candidate.COLUMNNAME_M_Product_ID, productId)
				.create()
				.listImmutable(I_PP_Order_Candidate.class);

		final StringBuilder messageBuilder = new StringBuilder("\n");
		candidatesForProductId
				.forEach(candidate ->
						messageBuilder.append(" -> PP_Order_Candidate_ID: ")
								.append(candidate.getPP_Order_Candidate_ID())
								.append(" M_Product_ID: ")
								.append(candidate.getM_Product_ID())
								.append(" QtyEntered: ")
								.append(candidate.getQtyEntered())
								.append(" QtyProcessed: ")
								.append(candidate.getQtyProcessed())
								.append(" C_UOM_ID: ")
								.append(candidate.getC_UOM_ID())
								.append(" DatePromised: ")
								.append(candidate.getDatePromised())
								.append(" DateStartSchedule: ")
								.append(candidate.getDateStartSchedule())
								.append(" PP_Product_Planning_ID: ")
								.append(candidate.getPP_Product_Planning_ID())
								.append("\n"));

		return "Current PP_Order_Candidates available for M_Product_ID:\n\n" + messageBuilder;
	}

	@And("after not more than {int}s, no PP_Order_Candidates are found for Issue_HU_ID: {string}")
	public void validatePP_Order_Candidate(
			final int timeoutSec,
			final String issueHuIdentifier) throws InterruptedException
	{
		final I_M_HU hu = huTable.get(issueHuIdentifier);

		final IQuery<I_PP_Order_Candidate> query = queryBL.createQueryBuilder(I_PP_Order_Candidate.class)
				.addEqualsFilter(I_PP_Order_Candidate.COLUMNNAME_Issue_HU_ID, hu.getM_HU_ID())
				.create();
		StepDefUtil.tryAndWait(timeoutSec, 500, () -> !query.anyMatch());
	}
}