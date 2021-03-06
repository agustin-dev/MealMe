package com.example.mealme.di

import com.example.mealme.ui.activities.MainActivityModule
import com.example.mealme.ui.activities.MainActivity
import com.example.mealme.ui.activities.NewMealActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class ActivityBuilder {

    @ContributesAndroidInjector(modules = [MainActivityModule::class])
    abstract fun bindMainActivity(): MainActivity

    @ContributesAndroidInjector
    abstract fun bindNewMealActivity(): NewMealActivity
}