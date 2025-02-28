package de.metas.material.event.ddordercandidate;

import de.metas.common.util.time.SystemTime;
import de.metas.material.event.EventTestHelper;
import de.metas.material.event.MaterialEventSerializerTests;
import de.metas.material.event.commons.EventDescriptor;
import de.metas.material.event.commons.MinMaxDescriptor;
import de.metas.material.event.pporder.MaterialDispoGroupId;
import de.metas.material.planning.ProductPlanningId;
import de.metas.material.planning.ddorder.DistributionNetworkAndLineId;
import de.metas.organization.ClientAndOrgId;
import de.metas.organization.OrgId;
import de.metas.product.ResourceId;
import de.metas.shipping.ShipperId;
import org.adempiere.warehouse.WarehouseId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static de.metas.material.event.EventTestHelper.assertEventEqualAfterSerializeDeserialize;

class DDOrderCandidateAdvisedEventTest
{
	@Test
	void testSerializeDeserialize()
	{
		final EventDescriptor eventDescriptor = EventDescriptor.ofClientOrgAndTraceId(ClientAndOrgId.ofClientAndOrg(1, 2), "traceId");
		final DDOrderCandidateAdvisedEvent event = DDOrderCandidateAdvisedEvent.builder()
				.eventDescriptor(eventDescriptor)
				.ddOrderCandidate(DDOrderCandidateData.builder()
						.productPlanningId(ProductPlanningId.ofRepoId(20))
						.distributionNetworkAndLineId(DistributionNetworkAndLineId.ofRepoIds(30, 40))
						.orgId(OrgId.ofRepoId(2))
						.sourceWarehouseId(WarehouseId.ofRepoId(45))
						.targetWarehouseId(WarehouseId.ofRepoId(46))
						.targetPlantId(ResourceId.ofRepoId(50))
						.shipperId(ShipperId.ofRepoId(60))
						.salesOrderLineId(70)
						.customerId(80)
						.productDescriptor(EventTestHelper.newMaterialDescriptor())
						.fromWarehouseMinMaxDescriptor(MinMaxDescriptor.builder()
								.min(new BigDecimal("0.01"))
								.max(new BigDecimal("0.09"))
								.build())
						.datePromised(SystemTime.asInstant())
						.qty(new BigDecimal("12.3456789"))
						.uomId(90)
						.durationDays(123)
						.simulated(true)
						.materialDispoGroupId(MaterialDispoGroupId.ofInt(100))
						.build())
				.supplyRequiredDescriptor(MaterialEventSerializerTests.newSupplyRequiredDescriptor())
				.advisedToCreateDDOrderCandidate(true)
				.pickIfFeasible(true)
				.build();

		assertEventEqualAfterSerializeDeserialize(event);
	}
}