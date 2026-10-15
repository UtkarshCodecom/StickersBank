package com.stickers.bank.data

import android.content.Context
import com.stickers.bank.data.model.Bank
import com.stickers.bank.data.model.Sticker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

/**
 * Manages the user's banks (named sticker collections), persisted to
 * filesDir/banks.json.
 */
class BankRepository(context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val file = File(context.filesDir, "banks.json")

    private val _banks = MutableStateFlow(load())
    val banks: StateFlow<List<Bank>> = _banks.asStateFlow()

    private fun load(): List<Bank> = runCatching {
        if (!file.exists()) return emptyList()
        AppJson.decodeFromString(ListSerializer(Bank.serializer()), file.readText())
    }.getOrElse { emptyList() }

    private fun update(transform: (List<Bank>) -> List<Bank>) {
        val list = transform(_banks.value)
        _banks.value = list
        scope.launch {
            runCatching {
                file.writeText(AppJson.encodeToString(ListSerializer(Bank.serializer()), list))
            }
        }
    }

    /** Case-insensitive; used to prevent duplicate bank names. */
    fun bankNameExists(name: String): Boolean =
        _banks.value.any { it.name.equals(name.trim(), ignoreCase = true) }

    /**
     * Creates a bank, or returns the existing one if a bank with the same name already
     * exists (names are unique, case-insensitive).
     */
    fun createBank(name: String): Bank {
        val clean = name.trim().ifBlank { "My Bank" }
        _banks.value.firstOrNull { it.name.equals(clean, ignoreCase = true) }?.let { return it }
        val bank = Bank(
            id = "bank_${System.currentTimeMillis()}",
            name = clean,
            createdAt = System.currentTimeMillis(),
        )
        update { it + bank }
        return bank
    }

    fun renameBank(bankId: String, name: String) =
        update { list -> list.map { if (it.id == bankId) it.copy(name = name) else it } }

    fun deleteBank(bankId: String) =
        update { list -> list.filterNot { it.id == bankId } }

    fun addSticker(bankId: String, sticker: Sticker) = update { list ->
        list.map { bank ->
            if (bank.id != bankId || bank.stickers.any { it.id == sticker.id }) bank
            else bank.copy(stickers = bank.stickers + sticker)
        }
    }

    fun removeSticker(bankId: String, stickerId: String) = update { list ->
        list.map { bank ->
            if (bank.id != bankId) bank
            else bank.copy(stickers = bank.stickers.filterNot { it.id == stickerId })
        }
    }

    fun contains(bankId: String, stickerId: String): Boolean =
        _banks.value.firstOrNull { it.id == bankId }?.stickers?.any { it.id == stickerId } == true
}
