/*
 * Copyright 2020 Gabor Varadi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhuinden.jetpacknavigationdaggersavedstatehandleftueexperiment.features.registration

import android.os.Bundle
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistryOwner
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.zhuinden.eventemitter.EventEmitter
import com.zhuinden.eventemitter.EventSource
import com.zhuinden.jetpacknavigationdaggersavedstatehandleftueexperiment.R
import com.zhuinden.jetpacknavigationdaggersavedstatehandleftueexperiment.application.AuthenticationManager
import com.zhuinden.jetpacknavigationdaggersavedstatehandleftueexperiment.core.navigation.NavigationCommand
import com.zhuinden.livedatacombinetuplekt.combineTuple

class RegistrationViewModel(
    private val authenticationManager: AuthenticationManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    class VmFactory @AssistedInject constructor(
        private val authenticationManager: AuthenticationManager,
        @Assisted savedStateRegistryOwner: SavedStateRegistryOwner,
        @Assisted defaultArgs: Bundle
    ) : AbstractSavedStateViewModelFactory(savedStateRegistryOwner, defaultArgs) {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(
            key: String,
            modelClass: Class<T>,
            savedStateHandle: SavedStateHandle
        ): T = RegistrationViewModel(authenticationManager, savedStateHandle) as T

        @AssistedInject.Factory // https://github.com/square/AssistedInject/issues/141
        interface Factory {
            fun createFactory(
                savedStateRegistryOwner: SavedStateRegistryOwner,
                defaultArgs: Bundle
            ): VmFactory
        }
    }

    private val navigationEmitter: EventEmitter<NavigationCommand> = EventEmitter()
    val navigationCommands: EventSource<NavigationCommand> get() = navigationEmitter

    enum class RegistrationState { // this is actually kinda superfluous/unnecessary but ok
        COLLECT_PROFILE_DATA,
        COLLECT_USER_PASSWORD,
        REGISTRATION_COMPLETED
    }

    private var currentState: RegistrationState = RegistrationState.COLLECT_PROFILE_DATA

    val fullName: MutableLiveData<String> = savedStateHandle.getLiveData("fullName", "")
    val bio: MutableLiveData<String> = savedStateHandle.getLiveData("bio", "")

    val isEnterProfileNextEnabled = combineTuple(fullName, bio).map { (fullName, bio) ->
        fullName!!.isNotBlank() && bio!!.isNotBlank()
    }

    val username: MutableLiveData<String> = savedStateHandle.getLiveData("username", "")
    val password: MutableLiveData<String> = savedStateHandle.getLiveData("password", "")

    val isRegisterAndLoginEnabled = combineTuple(username, password).map { (username, password) ->
        username!!.isNotBlank() && password!!.isNotBlank()
    }

    fun onEnterProfileNextClicked() {
        if (fullName.value!!.isNotBlank() && bio.value!!.isNotBlank()) {
            currentState = RegistrationState.COLLECT_USER_PASSWORD
            navigationEmitter.emit { navController, context ->
                navController.navigate(R.id.create_login_credentials_fragment)
            }
        }
    }

    fun onRegisterAndLoginClicked() {
        if (username.value!!.isNotBlank() && password.value!!.isNotBlank()) {
            currentState = RegistrationState.REGISTRATION_COMPLETED
            authenticationManager.saveRegistration()
            navigationEmitter.emit { navController, context ->
                navController.navigate(R.id.registration_to_logged_in)
            }
        }
    }

    fun onBackEvent() {
        if(currentState == RegistrationState.COLLECT_USER_PASSWORD) {
            currentState = RegistrationState.COLLECT_PROFILE_DATA
        }
        navigationEmitter.emit { navController, context ->
            navController.popBackStack()
        }
    }
}