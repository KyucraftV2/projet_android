package fr.enssat.kanaquiz.kreiner_ablamart_vrouet.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Remplace le dispatcher principal par un dispatcher de test.
 *
 * `viewModelScope` s'exécute sur `Dispatchers.Main`, qui n'existe pas dans un
 * test JVM : sans cette règle, tout lancement de coroutine dans un ViewModel
 * échouerait.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: kotlinx.coroutines.test.TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
