package com.stickers.bank.data

import android.content.Context

/**
 * Minimal manual DI. Initialized once from [com.stickers.bank.StickersBankApplication].
 */
object ServiceLocator {
    lateinit var catalogRepository: CatalogRepository
        private set
    lateinit var customStickerRepository: CustomStickerRepository
        private set
    lateinit var bankRepository: BankRepository
        private set
    lateinit var filterPreferences: FilterPreferences
        private set
    lateinit var favoritesRepository: FavoritesRepository
        private set
    lateinit var themePreferences: ThemePreferences
        private set
    lateinit var onboardingPreferences: OnboardingPreferences
        private set
    lateinit var analytics: Analytics
        private set
    lateinit var ratePrompt: RatePrompt
        private set

    @Volatile
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        val app = context.applicationContext
        catalogRepository = CatalogRepository(app)
        customStickerRepository = CustomStickerRepository(app)
        bankRepository = BankRepository(app)
        filterPreferences = FilterPreferences(app)
        favoritesRepository = FavoritesRepository(app)
        themePreferences = ThemePreferences(app)
        onboardingPreferences = OnboardingPreferences(app)
        analytics = Analytics(app)
        ratePrompt = RatePrompt(app)
        initialized = true
    }
}
