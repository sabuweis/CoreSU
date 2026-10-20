package me.li.coresu.data.repository

import me.li.coresu.data.model.Module
import me.li.coresu.data.model.ModuleUpdateInfo

interface ModuleRepository {
    suspend fun getModules(): Result<List<Module>>
    suspend fun checkUpdate(module: Module): Result<ModuleUpdateInfo>
}
