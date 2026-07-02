package app.eob.me

import android.app.Application
import app.eob.me.billing.SubscriptionState
import app.eob.me.data.AppLanguage
import app.eob.me.data.BillingInterval
import app.eob.me.data.HistoryBentoFilter
import app.eob.me.data.EobStrings
import app.eob.me.data.SubscriptionCatalog
import app.eob.me.data.SubscriptionTier
import app.eob.me.data.TaxVaultFilterState
import app.eob.me.data.repository.FirestoreSubscriptionSnapshot
import app.eob.me.viewmodel.AppViewModel
import app.eob.me.viewmodel.EobViewModel
import app.eob.me.viewmodel.SubscriptionViewModel
import app.eob.me.viewmodel.mergeSubscriptionStatus
import app.eob.me.viewmodel.subscriptionStateToTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubscriptionBillingTest {
    @Test
    fun subscriptionViewModelConstructorMatchesAndroidViewModelFactory() {
        val constructor = SubscriptionViewModel::class.java.getConstructor(Application::class.java)
        assertNotNull(constructor)
    }

    @Test
    fun appViewModelConstructorMatchesAndroidViewModelFactory() {
        val constructor = AppViewModel::class.java.getConstructor(Application::class.java)
        assertNotNull(constructor)
    }

    @Test
    fun subscriptionCatalogDefinesPlayBasePlanStructure() {
        assertEquals("eobme_silver", SubscriptionCatalog.SILVER_SUBSCRIPTION_ID)
        assertEquals("eobme_gold", SubscriptionCatalog.GOLD_SUBSCRIPTION_ID)
        assertEquals("monthly", SubscriptionCatalog.BASE_PLAN_MONTHLY)
        assertEquals("annual", SubscriptionCatalog.BASE_PLAN_ANNUAL)
        assertEquals("premium_access_tier", SubscriptionCatalog.LEGACY_PREMIUM_PRODUCT_ID)
        assertEquals(
            setOf("eobme_silver", "eobme_gold", "premium_access_tier"),
            SubscriptionCatalog.ALL_SUBSCRIPTION_PRODUCT_IDS
        )
    }

    @Test
    fun subscriptionCatalogOfferRefsMatchRevenueCatIdentifiers() {
        assertEquals(
            SubscriptionCatalog.SubscriptionOfferRef("eobme_silver", "monthly"),
            SubscriptionCatalog.offerRef(SubscriptionTier.Silver, BillingInterval.MONTHLY)
        )
        assertEquals(
            "eobme_silver:annual",
            SubscriptionCatalog.revenueCatProductIdentifier(SubscriptionTier.Silver, BillingInterval.ANNUAL)
        )
        assertEquals(
            "eobme_gold:monthly",
            SubscriptionCatalog.revenueCatProductIdentifier(SubscriptionTier.Gold, BillingInterval.MONTHLY)
        )
        assertEquals(
            "eobme_gold:annual",
            SubscriptionCatalog.revenueCatProductIdentifier(SubscriptionTier.Gold, BillingInterval.ANNUAL)
        )
    }

    @Test
    fun tierForProductIdResolvesParentSubscriptionIds() {
        assertEquals(SubscriptionTier.Silver, SubscriptionCatalog.tierForProductId("eobme_silver"))
        assertEquals(SubscriptionTier.Gold, SubscriptionCatalog.tierForProductId("eobme_gold"))
        assertEquals(SubscriptionTier.Gold, SubscriptionCatalog.tierForProductId("premium_access_tier"))
    }

    @Test
    fun subscriptionCatalogMarketingPricesMatchStrategy() {
        assertEquals("$2.99/mo", SubscriptionCatalog.displayPrice(SubscriptionTier.Silver, BillingInterval.MONTHLY))
        assertEquals("$29.99/yr", SubscriptionCatalog.displayPrice(SubscriptionTier.Silver, BillingInterval.ANNUAL))
        assertEquals("$5.49/mo", SubscriptionCatalog.displayPrice(SubscriptionTier.Gold, BillingInterval.MONTHLY))
        assertEquals("$49.99/yr", SubscriptionCatalog.displayPrice(SubscriptionTier.Gold, BillingInterval.ANNUAL))
    }

    @Test
    fun mergeSubscriptionStatusPrefersGoldFromRevenueCat() {
        assertEquals(
            SubscriptionState.Gold,
            mergeSubscriptionStatus(
                playTier = SubscriptionTier.Free,
                firestoreSnapshot = FirestoreSubscriptionSnapshot.Resolved(SubscriptionTier.Free),
                revenueCatTier = SubscriptionTier.Gold
            )
        )
    }

    @Test
    fun mergeSubscriptionStatusPrefersGoldFromEitherSource() {
        assertEquals(
            SubscriptionState.Gold,
            mergeSubscriptionStatus(
                playTier = SubscriptionTier.Gold,
                firestoreSnapshot = FirestoreSubscriptionSnapshot.Resolved(SubscriptionTier.Free)
            )
        )
        assertEquals(
            SubscriptionState.Gold,
            mergeSubscriptionStatus(
                playTier = SubscriptionTier.Free,
                firestoreSnapshot = FirestoreSubscriptionSnapshot.Resolved(SubscriptionTier.Gold)
            )
        )
    }

    @Test
    fun mergeSubscriptionStatusResolvesSilverTier() {
        assertEquals(
            SubscriptionState.Silver,
            mergeSubscriptionStatus(
                playTier = SubscriptionTier.Silver,
                firestoreSnapshot = FirestoreSubscriptionSnapshot.Resolved(SubscriptionTier.Free)
            )
        )
    }

    @Test
    fun mergeSubscriptionStatusReturnsFreeWhenBothSourcesResolvedFree() {
        assertEquals(
            SubscriptionState.Free,
            mergeSubscriptionStatus(
                playTier = SubscriptionTier.Free,
                firestoreSnapshot = FirestoreSubscriptionSnapshot.Resolved(SubscriptionTier.Free)
            )
        )
    }

    @Test
    fun mergeSubscriptionStatusStaysLoadingUntilBothSourcesResolve() {
        assertEquals(
            SubscriptionState.Loading,
            mergeSubscriptionStatus(
                playTier = null,
                firestoreSnapshot = FirestoreSubscriptionSnapshot.Resolved(SubscriptionTier.Free)
            )
        )
        assertEquals(
            SubscriptionState.Loading,
            mergeSubscriptionStatus(
                playTier = SubscriptionTier.Free,
                firestoreSnapshot = FirestoreSubscriptionSnapshot.Loading
            )
        )
    }

    @Test
    fun mergeSubscriptionStatusSurfacesFirestoreErrorWhenPlayIsFree() {
        val state = mergeSubscriptionStatus(
            playTier = SubscriptionTier.Free,
            firestoreSnapshot = FirestoreSubscriptionSnapshot.Error("permission denied")
        )
        assertTrue(state is SubscriptionState.Error)
        assertEquals("permission denied", (state as SubscriptionState.Error).message)
    }

    @Test
    fun eobViewModelApplySubscriptionStateUpdatesHubTier() {
        val viewModel = EobViewModel()
        viewModel.applySubscriptionState(SubscriptionState.Gold)
        assertEquals(SubscriptionTier.Gold, viewModel.uiState.value.hubSettings.subscriptionTier)
        viewModel.applySubscriptionState(SubscriptionState.Silver)
        assertEquals(SubscriptionTier.Silver, viewModel.uiState.value.hubSettings.subscriptionTier)
        viewModel.applySubscriptionState(SubscriptionState.Free)
        assertEquals(SubscriptionTier.Free, viewModel.uiState.value.hubSettings.subscriptionTier)
        viewModel.applySubscriptionState(SubscriptionState.Loading)
        assertEquals(SubscriptionTier.Free, viewModel.uiState.value.hubSettings.subscriptionTier)
        viewModel.applySubscriptionState(SubscriptionState.Error("offline"))
        assertEquals(SubscriptionTier.Free, viewModel.uiState.value.hubSettings.subscriptionTier)
    }

    @Test
    fun eobViewModelPaywallStateIsHubSourceOfTruth() {
        val viewModel = EobViewModel()
        viewModel.showPaywall("Upgrade required")
        assertTrue(viewModel.uiState.value.paywallVisible)
        assertEquals("Upgrade required", viewModel.uiState.value.paywallMessage)
        viewModel.dismissPaywall()
        assertFalse(viewModel.uiState.value.paywallVisible)
    }

    @Test
    fun eobViewModelReopensPaywallWhenPurchaseBillingFails() {
        val viewModel = EobViewModel()
        viewModel.showPaywall()
        viewModel.beginPaywallPurchase()
        assertFalse(viewModel.uiState.value.paywallVisible)
        assertTrue(viewModel.uiState.value.paywallPurchasePending)

        viewModel.handleBillingNoticeForPaywall(AppLanguage.English, "billing_flow_failed")

        assertFalse(viewModel.uiState.value.paywallPurchasePending)
        assertTrue(viewModel.uiState.value.paywallVisible)
        assertEquals(
            EobStrings.t(AppLanguage.English, "billingFlowFailed"),
            viewModel.uiState.value.paywallMessage
        )
        assertEquals(
            EobStrings.t(AppLanguage.English, "billingFlowFailed"),
            viewModel.uiState.value.hubSettings.settingsNotice
        )
    }

    @Test
    fun handleBillingNoticeForPaywallIgnoresBackgroundBillingErrors() {
        val viewModel = EobViewModel()
        viewModel.handleBillingNoticeForPaywall(AppLanguage.English, "billing_not_ready")
        assertEquals("", viewModel.uiState.value.hubSettings.settingsNotice)
        assertFalse(viewModel.uiState.value.paywallVisible)
    }

    @Test
    fun eobViewModelApplySubscriptionStateDismissesPaywall() {
        val viewModel = EobViewModel()
        viewModel.showPaywall()
        viewModel.beginPaywallPurchase()
        viewModel.applySubscriptionState(SubscriptionState.Gold)
        assertFalse(viewModel.uiState.value.paywallVisible)
        assertFalse(viewModel.uiState.value.paywallPurchasePending)
    }

    @Test
    fun subscriptionStateToTierMapsActiveStates() {
        assertEquals(SubscriptionTier.Gold, subscriptionStateToTier(SubscriptionState.Gold))
        assertEquals(SubscriptionTier.Silver, subscriptionStateToTier(SubscriptionState.Silver))
        assertEquals(SubscriptionTier.Free, subscriptionStateToTier(SubscriptionState.Free))
    }

    @Test
    fun firestoreRepositoryUsesSubscriptionTierFieldWithLegacyFallback() {
        val source = readSource("data/remote/FirestoreSubscriptionRepository.kt")
        assertTrue(source.contains("FIELD_SUBSCRIPTION_TIER = \"subscriptionTier\""))
        assertTrue(source.contains("FIELD_IS_PREMIUM = \"isPremium\""))
    }

    @Test
    fun subscriptionViewModelBindUserBlankResolvesFreeWithoutMergingPlayTier() {
        val source = readSource("viewmodel/SubscriptionViewModel.kt")
        assertTrue(source.contains("if (userId.isBlank())"))
        assertTrue(source.contains("_subscriptionState.value = SubscriptionState.Free"))
    }

    @Test
    fun paywallDialogSourcesTierFeaturesFromSubscriptionCatalog() {
        val paywallSource = readSource("ui/screens/PaywallDialog.kt")
        assertTrue(paywallSource.contains("SubscriptionCatalog.features(SubscriptionTier.Silver)"))
        assertTrue(paywallSource.contains("SubscriptionCatalog.features(SubscriptionTier.Gold)"))
        assertTrue(paywallSource.contains("paywallPricing.displayPrice"))
        assertTrue(paywallSource.contains("paywallPricing.checkoutPrice"))
        assertTrue(paywallSource.contains("currentSubscriptionTier"))
        assertTrue(paywallSource.contains("alreadySubscribedLabel"))
        assertTrue(paywallSource.contains("SubscriptionCatalog.features(SubscriptionTier.Free)"))
        assertTrue(paywallSource.contains("Column(verticalArrangement = Arrangement.spacedBy"))
    }

    @Test
    fun pr146ManageSubscriptionTierFeatureCountsAndGoldMonthlyPrice() {
        assertEquals(9, SubscriptionCatalog.features(SubscriptionTier.Silver).size)
        assertEquals(12, SubscriptionCatalog.features(SubscriptionTier.Gold).size)
        assertTrue(SubscriptionCatalog.features(SubscriptionTier.Silver).contains("Appointment Calendar"))
        assertTrue(SubscriptionCatalog.features(SubscriptionTier.Silver).contains("Y-T-D Expense Tracker"))
        assertTrue(SubscriptionCatalog.features(SubscriptionTier.Gold).contains("4 Smart Cards (CareTeam)"))
        assertTrue(SubscriptionCatalog.features(SubscriptionTier.Gold).contains("Tax Vault Filter"))
        assertTrue(SubscriptionCatalog.features(SubscriptionTier.Gold).contains("Tax Vault Claim Packager"))
        assertEquals("$5.49/mo", SubscriptionCatalog.displayPrice(SubscriptionTier.Gold, BillingInterval.MONTHLY))
        assertEquals("$5.49", SubscriptionCatalog.checkoutPrice(SubscriptionTier.Gold, BillingInterval.MONTHLY))
    }

    @Test
    fun subscriptionViewModelRefreshesRevenueCatWhenPlayTierChanges() {
        val source = readSource("viewmodel/SubscriptionViewModel.kt")
        assertTrue(source.contains("billingRepository.activePlayTier.collect"))
        assertTrue(source.contains("revenueCatBillingRepository.refreshCustomerInfo()"))
        assertTrue(source.contains("revenueCatBillingRepository.refreshOfferings()"))
    }

    @Test
    fun billingRepositoryResolvesBasePlanOfferTokens() {
        val billingSource = readSource("billing/BillingRepository.kt")
        assertTrue(billingSource.contains("resolveOfferToken"))
        assertTrue(billingSource.contains("basePlanId"))
        assertTrue(billingSource.contains("SubscriptionCatalog.offerRef"))
        assertTrue(billingSource.contains("ALL_SUBSCRIPTION_PRODUCT_IDS"))
        assertTrue(billingSource.contains("billing_user_canceled"))
        assertTrue(billingSource.contains("ITEM_ALREADY_OWNED"))
        assertFalse(
            "Offer token must match exact base plan, not fall back to first offer",
            billingSource.contains("offers.firstOrNull()?.offerToken")
        )
    }

    @Test
    fun eobViewModelRestoresPaywallWhenPurchaseCanceled() {
        val viewModel = EobViewModel()
        viewModel.showPaywall()
        viewModel.beginPaywallPurchase()
        assertFalse(viewModel.uiState.value.paywallVisible)
        assertTrue(viewModel.uiState.value.paywallPurchasePending)

        viewModel.handleBillingNoticeForPaywall(AppLanguage.English, "billing_user_canceled")

        assertFalse(viewModel.uiState.value.paywallPurchasePending)
        assertTrue(viewModel.uiState.value.paywallVisible)
        assertEquals("", viewModel.uiState.value.paywallMessage)
    }

    @Test
    fun subscriptionCatalogDefinesAllFourBasePlanCombinations() {
        val silverMonthly = SubscriptionCatalog.offerRef(SubscriptionTier.Silver, BillingInterval.MONTHLY)
        val silverAnnual = SubscriptionCatalog.offerRef(SubscriptionTier.Silver, BillingInterval.ANNUAL)
        val goldMonthly = SubscriptionCatalog.offerRef(SubscriptionTier.Gold, BillingInterval.MONTHLY)
        val goldAnnual = SubscriptionCatalog.offerRef(SubscriptionTier.Gold, BillingInterval.ANNUAL)
        assertEquals("eobme_silver:monthly", silverMonthly?.revenueCatProductIdentifier)
        assertEquals("eobme_silver:annual", silverAnnual?.revenueCatProductIdentifier)
        assertEquals("eobme_gold:monthly", goldMonthly?.revenueCatProductIdentifier)
        assertEquals("eobme_gold:annual", goldAnnual?.revenueCatProductIdentifier)
    }

    @Test
    fun subscriptionNavHostClearsBillingNoticeAfterHandling() {
        val navSource = readSource("navigation/EobNavHost.kt")
        assertTrue(navSource.contains("clearBillingNotice"))
        assertTrue(navSource.contains("handleBillingNoticeForPaywall"))
        assertTrue(navSource.contains("launchTierPurchaseFlow"))
        assertTrue(navSource.contains("onPremiumFeatureLocked"))
    }

    @Test
    fun revenueCatPublicApiKeyAndPurchaseFlowAreWired() {
        assertEquals("goog_rmhYQIPDsEWnEBFWUzMRYYlpYMo", app.eob.me.billing.RevenueCatConfig.PUBLIC_API_KEY)
        val revenueCatBillingSource = readSource("billing/RevenueCatBillingRepository.kt")
        val subscriptionSource = readSource("viewmodel/SubscriptionViewModel.kt")
        assertTrue(revenueCatBillingSource.contains("awaitPurchase"))
        assertTrue(revenueCatBillingSource.contains("PurchaseParams.Builder"))
        assertTrue(revenueCatBillingSource.contains("awaitLogIn"))
        assertTrue(revenueCatBillingSource.contains("awaitLogOut"))
        assertTrue(revenueCatBillingSource.contains("awaitRestore"))
        assertTrue(revenueCatBillingSource.contains("restoreUserPurchases"))
        assertTrue(revenueCatBillingSource.contains("setEmail"))
        assertTrue(revenueCatBillingSource.contains("setDisplayName"))
        assertTrue(revenueCatBillingSource.contains("setAttributes"))
        assertTrue(revenueCatBillingSource.contains("attachUserMetadata"))
        assertTrue(revenueCatBillingSource.contains("UpdatedCustomerInfoListener"))
        assertTrue(revenueCatBillingSource.contains("RevenueCatEntitlementMapper"))
        assertFalse("ViewModel must not call RevenueCat directly", subscriptionSource.contains("com.revenuecat"))
        assertTrue(subscriptionSource.contains("restoreUserPurchases"))
        assertTrue(subscriptionSource.contains("refreshOfferings()"))
        val applicationSource = readSource("EobApplication.kt")
        assertTrue(applicationSource.contains("RevenueCatConfig.PUBLIC_API_KEY"))
        assertTrue(applicationSource.contains("entitlementVerificationMode"))
        assertTrue(applicationSource.contains("EntitlementVerificationMode.INFORMATIONAL"))
    }

    @Test
    fun revenueCatPurchaseErrorsMapToUserFacingNoticeKeys() {
        val mapperSource = readSource("billing/RevenueCatPurchaseErrorMapper.kt")
        assertTrue(mapperSource.contains("billing_user_canceled"))
        assertTrue(mapperSource.contains("billing_payment_declined"))
        assertTrue(mapperSource.contains("billing_payment_pending"))
        assertTrue(mapperSource.contains("billing_product_unavailable"))
        assertTrue(mapperSource.contains("billing_already_subscribed"))
    }

    @Test
    fun eobViewModelMapsPaymentDeclinedBillingNotice() {
        val viewModel = EobViewModel()
        viewModel.showPaywall()
        viewModel.beginPaywallPurchase()
        viewModel.handleBillingNoticeForPaywall(AppLanguage.English, "billing_payment_declined")
        assertTrue(viewModel.uiState.value.paywallVisible)
        assertEquals(
            EobStrings.t(AppLanguage.English, "billingPaymentDeclined"),
            viewModel.uiState.value.paywallMessage
        )
    }

    @Test
    fun paywallDialogExposesRestorePurchasesControl() {
        val paywallSource = readSource("ui/screens/PaywallDialog.kt")
        val navSource = readSource("navigation/EobNavHost.kt")
        assertTrue(paywallSource.contains("onRestorePurchasesClicked"))
        assertTrue(paywallSource.contains("restorePurchasesLabel"))
        assertTrue(navSource.contains("billingRestorePurchases"))
        assertTrue(navSource.contains("onRestorePurchasesClicked = ::restorePurchases"))
    }

    @Test
    fun bindUserPushesRevenueCatCustomerMetadata() {
        val subscriptionSource = readSource("viewmodel/SubscriptionViewModel.kt")
        val navSource = readSource("navigation/EobNavHost.kt")
        assertTrue(subscriptionSource.contains("attachUserMetadata"))
        assertTrue(subscriptionSource.contains("revenueCatBillingRepository.logIn(userId)"))
        val bindLaunchBlock = subscriptionSource.substringAfter("observeJob = viewModelScope.launch {")
        val logInIndex = bindLaunchBlock.indexOf("revenueCatBillingRepository.logIn(userId)")
        val metadataIndex = bindLaunchBlock.indexOf("revenueCatBillingRepository.attachUserMetadata")
        assertTrue("Metadata must attach after RevenueCat logIn", logInIndex in 0 until metadataIndex)
        assertTrue(navSource.contains("displayName = profile.fullName"))
        assertTrue(navSource.contains("email = profile.email"))
    }

    @Test
    fun paywallRemainsAccessibleForEverySubscriptionTier() {
        val viewModel = EobViewModel()
        SubscriptionTier.entries.forEach { tier ->
            viewModel.setSubscriptionTier(tier)
            viewModel.showPaywall("Upgrade for tier ${tier.name}")
            assertTrue("Paywall must open on $tier", viewModel.uiState.value.paywallVisible)
            viewModel.dismissPaywall()
            assertFalse("Paywall must dismiss on $tier", viewModel.uiState.value.paywallVisible)
        }
    }

    @Test
    fun billingNoticeForPaywallCarriesAllLocalizedBillingMessages() {
        val viewModel = EobViewModel()
        val language = AppLanguage.English
        listOf(
            "billing_payment_declined" to "billingPaymentDeclined",
            "billing_payment_pending" to "billingPaymentPending",
            "billing_restore_none" to "billingRestoreNone",
            "billing_restore_failed" to "billingRestoreFailed",
            "billing_restore_success" to "billingRestoreSuccess"
        ).forEach { (noticeKey, stringKey) ->
            viewModel.updateBillingNotice(language, noticeKey)
            assertEquals(
                EobStrings.t(language, stringKey),
                viewModel.billingNoticeForPaywall(language)
            )
        }
    }

    @Test
    fun settingsManageSubscriptionIsNotTierGated() {
        val settingsSource = readSource("ui/screens/SettingsScreen.kt")
        val manageSource = readSource("ui/screens/ManageSubscriptionScreen.kt")
        assertTrue(settingsSource.contains("SubscriptionManagementSection"))
        assertTrue(settingsSource.contains("onManageSubscription"))
        assertFalse(settingsSource.contains("onSubscribe"))
        assertFalse(settingsSource.contains("onCancelSubscription"))
        assertFalse(settingsSource.contains("onResubscribe"))
        assertTrue(manageSource.contains("onCancelSubscription"))
        assertTrue(manageSource.contains("onResubscribe"))
        assertTrue(manageSource.contains("showSubscribeAction"))
        assertTrue(manageSource.contains("showCancelSubscriptionAction"))
        assertTrue(manageSource.contains("showResubscribeAction"))
        assertFalse(
            "Manage subscription entry must remain available for Free, Silver, and Gold",
            settingsSource.contains("if (subscriptionTier") && settingsSource.contains("onManageSubscription")
        )
    }

    @Test
    fun eobViewModelSubscriptionManagementActionsMatchTier() {
        val viewModel = EobViewModel()
        viewModel.setSubscriptionTier(SubscriptionTier.Free)
        assertTrue(viewModel.shouldShowSubscribeAction())
        assertTrue(viewModel.shouldShowResubscribeAction())
        assertFalse(viewModel.shouldShowCancelSubscriptionAction())
        assertEquals(null, viewModel.subscriptionManagementProductId())

        viewModel.setSubscriptionTier(SubscriptionTier.Silver)
        assertTrue(viewModel.shouldShowSubscribeAction())
        assertFalse(viewModel.shouldShowResubscribeAction())
        assertTrue(viewModel.shouldShowCancelSubscriptionAction())
        assertEquals(SubscriptionCatalog.SILVER_SUBSCRIPTION_ID, viewModel.subscriptionManagementProductId())

        viewModel.setSubscriptionTier(SubscriptionTier.Gold)
        assertFalse(viewModel.shouldShowSubscribeAction())
        assertFalse(viewModel.shouldShowResubscribeAction())
        assertTrue(viewModel.shouldShowCancelSubscriptionAction())
        assertEquals(SubscriptionCatalog.GOLD_SUBSCRIPTION_ID, viewModel.subscriptionManagementProductId())
        assertEquals(
            "Choose a plan to resubscribe, or restore a previous purchase.",
            viewModel.resubscribePaywallMessage(app.eob.me.data.AppLanguage.English)
        )
    }

    @Test
    fun navHostWiresSubscribeCancelAndResubscribeFlows() {
        val navSource = readSource("navigation/EobNavHost.kt")
        assertTrue(navSource.contains("ManageSubscriptionScreen"))
        assertTrue(navSource.contains("EobRoute.ManageSubscription.route"))
        assertTrue(navSource.contains("handleManageSubscriptionTierSelection"))
        assertTrue(navSource.contains("launchCancelSubscriptionFlow"))
        assertTrue(navSource.contains("launchResubscribeFlow"))
        assertTrue(navSource.contains("resubscribePaywallMessage"))
        assertTrue(navSource.contains("updateManageSubscriptionNotice"))
        assertTrue(navSource.contains("isOnManageSubscriptionRoute"))
        assertTrue(navSource.contains("PlaySubscriptionManagement.buildManagementIntent"))
        assertTrue(navSource.contains("shouldShowSubscribeAction()"))
        assertTrue(navSource.contains("shouldShowCancelSubscriptionAction()"))
        assertTrue(navSource.contains("shouldShowResubscribeAction()"))
    }

    @Test
    fun revenueCatPackageResolverMatchesCatalogIdentifiers() {
        val resolverSource = readSource("billing/RevenueCatPackageResolver.kt")
        assertTrue(resolverSource.contains("revenueCatProductIdentifier"))
        assertTrue(resolverSource.contains("SubscriptionCatalog.offerRef"))
    }

    @Test
    fun eobViewModelBlocksRepurchaseOfActiveTier() {
        val viewModel = EobViewModel()
        viewModel.setSubscriptionTier(SubscriptionTier.Gold)
        assertFalse(viewModel.canPurchaseSubscriptionTier(SubscriptionTier.Gold))
        assertTrue(viewModel.canPurchaseSubscriptionTier(SubscriptionTier.Silver))
        assertTrue(viewModel.isSubscriptionTierDowngrade(SubscriptionTier.Silver))
        assertTrue(viewModel.isSubscriptionTierAlreadyOwned(SubscriptionTier.Gold))
        viewModel.setSubscriptionTier(SubscriptionTier.Silver)
        assertTrue(viewModel.canPurchaseSubscriptionTier(SubscriptionTier.Gold))
        assertFalse(viewModel.canPurchaseSubscriptionTier(SubscriptionTier.Silver))
    }

    @Test
    fun pr148ManageSubscriptionAllowsGoldToSilverDowngrade() {
        val viewModel = EobViewModel()
        viewModel.setSubscriptionTier(SubscriptionTier.Gold)
        assertEquals(
            "Already purchased by user.",
            viewModel.alreadyPurchasedByUserMessage(AppLanguage.English)
        )
        assertEquals(
            "Plan change takes effect at your next billing cycle.",
            viewModel.downgradeNextCycleMessage(AppLanguage.English)
        )
        val manageSource = readSource("ui/screens/ManageSubscriptionScreen.kt")
        assertTrue(manageSource.contains("goldHighlightFeatures"))
        assertTrue(manageSource.contains("goldStandardFeatures"))
        assertTrue(manageSource.contains("billingAlreadyPurchasedByUser"))
        assertTrue(manageSource.contains("billingDowngradeNextCycle"))
        assertTrue(manageSource.contains("billingGoldHighlightsTitle"))
        assertEquals(
            listOf(
                "Smart Card Summaries",
                "Tax Vault Filter",
                "Tax Vault Claim Packager"
            ),
            SubscriptionCatalog.goldHighlightFeatures()
        )
    }

    @Test
    fun manageSubscriptionPurchaseFailureRoutesNoticeToTierPage() {
        val viewModel = EobViewModel()
        viewModel.beginManageSubscriptionPurchase()
        assertTrue(viewModel.uiState.value.manageSubscriptionPurchasePending)

        viewModel.handleBillingNoticeForPaywall(AppLanguage.English, "billing_flow_failed")

        assertFalse(viewModel.uiState.value.manageSubscriptionPurchasePending)
        assertFalse(viewModel.uiState.value.paywallVisible)
        assertEquals(
            EobStrings.t(AppLanguage.English, "billingFlowFailed"),
            viewModel.uiState.value.hubSettings.manageSubscriptionNotice
        )
    }

    @Test
    fun manageSubscriptionUsesDedicatedNoticeChannel() {
        val navSource = readSource("navigation/EobNavHost.kt")
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertTrue(navSource.contains("manageSubscriptionNotice"))
        assertTrue(navSource.contains("clearManageSubscriptionNotice"))
        assertTrue(navSource.contains("beginManageSubscriptionPurchase"))
        assertTrue(navSource.contains("isOnManageSubscriptionRoute"))
        assertTrue(viewModelSource.contains("manageSubscriptionPurchasePending"))
        assertTrue(viewModelSource.contains("fun updateManageSubscriptionNotice"))
    }

    @Test
    fun paywallDialogUsesLocalizedBillingCopy() {
        val paywallSource = readSource("ui/screens/PaywallDialog.kt")
        assertTrue(paywallSource.contains("language: AppLanguage"))
        assertTrue(paywallSource.contains("billingPaywallTitle"))
        assertTrue(paywallSource.contains("billingIntervalMonthly"))
        assertTrue(paywallSource.contains("billingSubscribeForPrice"))
    }

    @Test
    fun billingRepositoryMapsAlreadyOwnedToAlreadySubscribedNotice() {
        val billingSource = readSource("billing/BillingRepository.kt")
        assertTrue(billingSource.contains("billing_already_subscribed"))
    }

    @Test
    fun eobViewModelApplySubscriptionStateClearsFlaggedFilterOnFree() {
        val viewModel = EobViewModel()
        viewModel.setSubscriptionTier(SubscriptionTier.Silver)
        viewModel.setHistoryBentoFilter(HistoryBentoFilter.Flagged)
        viewModel.applySubscriptionState(SubscriptionState.Free)
        assertEquals(HistoryBentoFilter.All, viewModel.uiState.value.historyBentoFilter)
    }

    @Test
    fun eobViewModelApplySubscriptionStateClearsGoldExclusiveStateOnDowngrade() {
        val viewModel = EobViewModel()
        viewModel.setSubscriptionTier(SubscriptionTier.Gold)
        viewModel.setTaxVaultFilterState(TaxVaultFilterState.HSA)
        viewModel.applySubscriptionState(SubscriptionState.Silver)
        assertEquals(TaxVaultFilterState.OFF, viewModel.taxVaultFilterState.value)
        viewModel.setTaxVaultFilterState(TaxVaultFilterState.FSA)
        viewModel.applySubscriptionState(SubscriptionState.Free)
        assertEquals(TaxVaultFilterState.OFF, viewModel.taxVaultFilterState.value)
    }

    @Test
    fun eobViewModelAppealGateMessageUsesUpgradeCopyForFreeTier() {
        val viewModel = EobViewModel()
        assertEquals(
            EobStrings.t(AppLanguage.English, "premiumUpgradeToUnlock"),
            viewModel.appealGateMessage(AppLanguage.English)
        )
    }

    @Test
    fun eobViewModelActivateAppealBentoDoesNotRequireRegenerate() {
        val viewModelSource = readSource("viewmodel/EobViewModel.kt")
        assertTrue(viewModelSource.contains("fun canAccessAppealGenerator"))
        assertTrue(viewModelSource.contains("fun reconcileTierExclusiveFeatures"))
        assertFalse(
            "Appeal bento must not burn quota via regenerateAppeal on navigation",
            viewModelSource.contains("if (!regenerateAppeal(profile, language)) return false")
        )
    }

    @Test
    fun navHostGuardsAppealAndScanNavigationPaths() {
        val navSource = readSource("navigation/EobNavHost.kt")
        assertTrue(navSource.contains("if (eobViewModel.openAppealForRecord"))
        assertTrue(navSource.contains("requestEobScanOrPaywall"))
        assertTrue(navSource.contains("EobRoute.YearlyExpense.route"))
        assertTrue(navSource.contains("EobmeFeatureGate.hasYtdExpenseTracker(subscriptionTier)"))
        assertTrue(navSource.contains("!EobmeFeatureGate.hasRealTimeNews(subscriptionTier)"))
    }

    @Test
    fun mergeSubscriptionStatusPrefersHighestTierAcrossAllSources() {
        assertEquals(
            SubscriptionState.Gold,
            mergeSubscriptionStatus(
                playTier = SubscriptionTier.Silver,
                firestoreSnapshot = FirestoreSubscriptionSnapshot.Resolved(SubscriptionTier.Gold),
                revenueCatTier = SubscriptionTier.Silver
            )
        )
        assertEquals(
            SubscriptionState.Silver,
            mergeSubscriptionStatus(
                playTier = SubscriptionTier.Free,
                firestoreSnapshot = FirestoreSubscriptionSnapshot.Resolved(SubscriptionTier.Silver),
                revenueCatTier = SubscriptionTier.Free
            )
        )
    }

    @Test
    fun paywallDialogBlocksPurchaseUntilRevenueCatPricingLoads() {
        val pricingSource = readSource("billing/PaywallPricing.kt")
        val paywallSource = readSource("ui/screens/PaywallDialog.kt")
        assertTrue(pricingSource.contains("SubscriptionCatalog.displayPrice"))
        assertTrue(pricingSource.contains("isStorePricingLoaded"))
        assertFalse(paywallSource.contains("Loading prices"))
    }

    @Test
    fun androidManifestDeclaresBillingPermissionForPlayAndRevenueCat() {
        val manifest = readManifest()
        assertTrue(manifest.contains("com.android.vending.BILLING"))
        assertTrue(manifest.contains("ext-firestore-revenuecat-purchases-handler"))
        assertTrue(manifest.contains("com.android.vending"))
    }

    private fun readManifest(): String {
        val candidates = listOf(
            java.io.File("src/main/AndroidManifest.xml"),
            java.io.File("app/src/main/AndroidManifest.xml")
        )
        return candidates.first { it.isFile }.readText()
    }

    private fun readSource(relativePath: String): String {
        val candidates = listOf(
            java.io.File("src/main/java/app/eob/me/$relativePath"),
            java.io.File("app/src/main/java/app/eob/me/$relativePath")
        )
        val file = candidates.first { it.isFile }
        return file.readText()
    }
}
