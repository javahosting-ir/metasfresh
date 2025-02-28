package de.metas.material.event.ddorder;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.metas.document.engine.DocStatus;
import de.metas.material.event.pporder.MaterialDispoGroupId;
import de.metas.material.planning.ProductPlanningId;
import de.metas.organization.OrgId;
import de.metas.product.ResourceId;
import de.metas.shipping.ShipperId;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

import java.time.Instant;
import java.util.List;

/*
 * #%L
 * metasfresh-mrp
 * %%
 * Copyright (C) 2017 metas GmbH
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

/**
 * About the source and dest warehouse: those are taken from the orderlines' network lines
 * One DDOrder might end up being the source of multiple DD_Order records.
 */
@Value
public class DDOrder
{
	/**
	 * {@code AD_Org_ID} of the <b>receiving</b> organization.
	 */
	OrgId orgId;

	/**
	 * The {@link de.metas.product.ResourceId} of the plant, as specified by the respective product planning record.
	 */
	ResourceId plantId;

	ProductPlanningId productPlanningId;

	Instant datePromised;

	ShipperId shipperId;

	@Singular
	List<DDOrderLine> lines;

	int ddOrderId;

	DocStatus docStatus;

	MaterialDispoGroupId materialDispoGroupId;

	boolean simulated;

	@JsonCreator
	@Builder
	private DDOrder(
			@JsonProperty("orgId") @NonNull final OrgId orgId,
			@JsonProperty("plantId") final ResourceId plantId,
			@JsonProperty("productPlanningId") final ProductPlanningId productPlanningId,
			@JsonProperty("datePromised") @NonNull final Instant datePromised,
			@JsonProperty("shipperId") final ShipperId shipperId,
			@JsonProperty("lines") @Singular final List<DDOrderLine> lines,
			@JsonProperty("ddOrderId") final int ddOrderId,
			@JsonProperty("docStatus") final DocStatus docStatus,
			@JsonProperty("materialDispoGroupId") final MaterialDispoGroupId materialDispoGroupId,
			@JsonProperty("simulated") final boolean simulated)
	{
		this.orgId = orgId;

		// these two might be null if the DDOrder was created manually
		this.plantId = plantId;
		this.productPlanningId = productPlanningId;

		this.datePromised = datePromised;
		this.shipperId = shipperId;
		this.lines = lines;
		this.ddOrderId = ddOrderId;
		this.docStatus = docStatus;
		this.materialDispoGroupId = materialDispoGroupId;
		this.simulated = simulated;
	}
}
