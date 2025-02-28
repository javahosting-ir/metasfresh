/*
 * #%L
 * de.metas.business
 * %%
 * Copyright (C) 2024 metas GmbH
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

package de.metas.bpartner.postfinance.interceptor;

import de.metas.bpartner.BPartnerId;
import de.metas.bpartner.postfinance.PostFinanceBPartnerConfig;
import de.metas.bpartner.postfinance.PostFinanceBPartnerConfigRepository;
import de.metas.i18n.AdMessageKey;
import de.metas.organization.OrgId;
import lombok.NonNull;
import org.adempiere.ad.modelvalidator.annotations.Interceptor;
import org.adempiere.ad.modelvalidator.annotations.ModelChange;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_C_BPartner;
import org.compiere.model.ModelValidator;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Interceptor(I_C_BPartner.class)
@Component
public class C_BPartner
{
	private static final AdMessageKey ERROR_ORG_BP_POST_FINANCE_CONFIG = AdMessageKey.of("C_BPartner.ERROR_ORG_BP_POSTFINANCE_CONFIG");

	private final PostFinanceBPartnerConfigRepository postFinanceBPartnerConfigRepository;

	public C_BPartner(@NonNull final PostFinanceBPartnerConfigRepository postFinanceBPartnerConfigRepository)
	{
		this.postFinanceBPartnerConfigRepository = postFinanceBPartnerConfigRepository;
	}

	@ModelChange(timings = ModelValidator.TYPE_AFTER_CHANGE,
			ifColumnsChanged = I_C_BPartner.COLUMNNAME_AD_OrgBP_ID)
	public void validatePostFinanceConfig(@NonNull final I_C_BPartner bpartner)
	{
		final OrgId orgBPId = OrgId.ofRepoIdOrNull(bpartner.getAD_OrgBP_ID());
		if (orgBPId == null)
		{
			return;
		}

		final BPartnerId bPartnerId = BPartnerId.ofRepoId(bpartner.getC_BPartner_ID());
		final Optional<PostFinanceBPartnerConfig> postFinanceConfigOptional = postFinanceBPartnerConfigRepository.getByBPartnerId(bPartnerId);
		if (postFinanceConfigOptional.isPresent())
		{
			throw new AdempiereException(ERROR_ORG_BP_POST_FINANCE_CONFIG);
		}
	}
}
