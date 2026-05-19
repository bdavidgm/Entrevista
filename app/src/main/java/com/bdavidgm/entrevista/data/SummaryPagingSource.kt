package com.bdavidgm.entrevista.data

import androidx.paging.PagingSource
import androidx.paging.PagingState

/**
 * Pagina sobre una lista ya filtrada en memoria (solo resúmenes, sin respuestas).
 */
class SummaryPagingSource(
    private val items: List<QuestionSummary>
) : PagingSource<Int, QuestionSummary>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, QuestionSummary> {
        val page = params.key ?: 0
        val pageSize = params.loadSize.coerceAtLeast(1)
        val start = page * pageSize
        if (start >= items.size) {
            return LoadResult.Page(
                data = emptyList(),
                prevKey = if (page > 0) page - 1 else null,
                nextKey = null
            )
        }
        val end = minOf(start + pageSize, items.size)
        val data = items.subList(start, end)
        return LoadResult.Page(
            data = data,
            prevKey = if (page == 0) null else page - 1,
            nextKey = if (end < items.size) page + 1 else null
        )
    }

    override fun getRefreshKey(state: PagingState<Int, QuestionSummary>): Int? {
        val anchor = state.anchorPosition ?: return null
        val page = state.closestPageToPosition(anchor) ?: return null
        return page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
    }
}
