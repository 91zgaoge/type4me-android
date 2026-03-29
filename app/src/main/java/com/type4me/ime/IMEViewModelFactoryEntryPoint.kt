package com.type4me.ime

import com.type4me.ime.viewmodel.IMEViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface IMEViewModelFactoryEntryPoint {
    fun imeViewModelFactory(): IMEViewModel.Factory
}
