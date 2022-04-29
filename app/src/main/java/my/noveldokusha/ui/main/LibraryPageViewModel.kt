package my.noveldokusha.ui.main

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import my.noveldokusha.AppPreferences
import my.noveldokusha.data.Repository
import my.noveldokusha.data.database.tables.Book
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.uiUtils.StateExtra_Boolean
import javax.inject.Inject

interface LibraryPageStateBundle
{
    var showCompleted: Boolean
}

@HiltViewModel
class LibraryPageViewModel @Inject constructor(
    private val repository: Repository,
    private val state: SavedStateHandle,
    private val preferences: AppPreferences
) : BaseViewModel(), LibraryPageStateBundle
{
    override var showCompleted by StateExtra_Boolean(state)

    val booksWithContext = repository.bookLibrary
        .getBooksInLibraryWithContextFlow
        .map { it.filter { book -> book.book.completed == showCompleted } }
        .combine(preferences.LIBRARY_FILTER_READ.flow()) { list, filterRead ->
            when (filterRead)
            {
                AppPreferences.TERNARY_STATE.active -> list.filter { it.chaptersCount == it.chaptersReadCount }
                AppPreferences.TERNARY_STATE.inverse -> list.filter { it.chaptersCount != it.chaptersReadCount }
                AppPreferences.TERNARY_STATE.inactive -> list
            }
        }.combine(preferences.LIBRARY_SORT_READ.flow()) { list, sortRead ->
            when (sortRead)
            {
                AppPreferences.TERNARY_STATE.active -> list.sortedBy { it.chaptersCount - it.chaptersReadCount }
                AppPreferences.TERNARY_STATE.inverse -> list.sortedByDescending { it.chaptersCount - it.chaptersReadCount }
                AppPreferences.TERNARY_STATE.inactive -> list
            }
        }
        .asLiveData()

    fun setBookAsCompleted(book: Book, completed: Boolean) = CoroutineScope(Dispatchers.IO).launch {
        repository.bookLibrary.update(book.copy(completed = completed))
    }
}
